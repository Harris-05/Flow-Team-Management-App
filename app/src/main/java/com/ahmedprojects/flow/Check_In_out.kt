package com.ahmedprojects.flow

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class Check_In_out : AppCompatActivity() {

    private val TAG = "CheckInOut"

    private var ip = IP_String().IP
    private lateinit var tvProjectName: TextView
    private lateinit var btnCheckIn: LinearLayout
    private lateinit var btnCheckOut: LinearLayout
    private lateinit var tvStatus: TextView
    private lateinit var tvActiveTime: TextView
    private lateinit var tvTotalTimeWorked: TextView
    private lateinit var backBtn : ImageView

    private var userId = -1
    private var projectId = -1
    private var projectName = ""
    private var sessionStart: String? = null
    private var totalSeconds: Int = 0

    private val handler = Handler()
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateActiveTime()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.check_in_out)

        Log.d(TAG, "onCreate called")

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        userId = prefs.getInt("id", -1)
        if (userId == -1) {
            Toast.makeText(this, "Invalid session!", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "User session invalid")
            finish()
            return
        }

        tvProjectName = findViewById(R.id.tvProjectName)
        btnCheckIn = findViewById(R.id.btnCheckIn)
        btnCheckOut = findViewById(R.id.btnCheckOut)
        backBtn = findViewById(R.id.backBtn)
        tvStatus = findViewById(R.id.tvStatus)
        tvActiveTime = findViewById(R.id.tvActiveTime)
        tvTotalTimeWorked = findViewById(R.id.tvTotalTimeWorked)

        projectId = intent.getIntExtra("project_id", -1)
        projectName = intent.getStringExtra("project_name") ?: ""
        tvProjectName.text = projectName

        Log.d(TAG, "Project ID: $projectId, Project Name: $projectName")

        getSession()

        btnCheckIn.setOnClickListener { startSession() }
        btnCheckOut.setOnClickListener { endSession() }

        backBtn.setOnClickListener { finish() }
    }

    // -------------------- API Calls --------------------

    private fun getSession() {
        val url = "$ip/get_session.php"
        Log.d(TAG, "getSession called - URL: $url, userId: $userId, projectId: $projectId")

        val request = object : StringRequest(Method.POST, url,
            { response ->
                Log.d(TAG, "getSession response: $response")
                try {
                    val obj = JSONObject(response)
                    if (obj.getString("status") == "success") {
                        sessionStart = obj.optString("session_start", null)
                        totalSeconds = obj.optInt("total_seconds", 0)

                        Log.d(TAG, "Session Start: $sessionStart, Total Seconds: $totalSeconds")

                        if (sessionStart.isNullOrEmpty() || sessionStart == "null") {
                            showCheckIn()
                        } else {
                            showCheckOut()
                            handler.post(timeRunnable)
                        }
                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "getSession failed: ${obj.getString("message")}")
                        showCheckIn()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "Error parsing getSession response", e)
                    Toast.makeText(this, "Error parsing session", Toast.LENGTH_SHORT).show()
                    showCheckIn()
                }
            },
            { error ->
                error.printStackTrace()
                Log.e(TAG, "Network error in getSession", error)
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
                showCheckIn()
            }) {
            override fun getParams(): MutableMap<String, String> {
                return mutableMapOf(
                    "user_id" to userId.toString(),
                    "project_id" to projectId.toString()
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun startSession() {
        val url = "$ip/start_session.php"
        Log.d(TAG, "startSession called - URL: $url")

        val request = object : StringRequest(Method.POST, url,
            { response ->
                Log.d(TAG, "startSession response: $response")
                try {
                    val obj = JSONObject(response)
                    if (obj.getString("status") == "success") {
                        Toast.makeText(this, "Checked In", Toast.LENGTH_SHORT).show()
                        getSession()
                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "startSession failed: ${obj.getString("message")}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "Error parsing startSession response", e)
                    Toast.makeText(this, "Error starting session", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Log.e(TAG, "Network error in startSession", error)
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                return mutableMapOf(
                    "user_id" to userId.toString(),
                    "project_id" to projectId.toString()
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun endSession() {
        val url = "$ip/end_session.php"
        Log.d(TAG, "endSession called - URL: $url")

        val request = object : StringRequest(Method.POST, url,
            { response ->
                Log.d(TAG, "endSession response: $response")
                try {
                    val obj = JSONObject(response)
                    if (obj.getString("status") == "success") {
                        Toast.makeText(this, "Checked Out", Toast.LENGTH_SHORT).show()
                        sessionStart = null
                        totalSeconds = obj.optInt("total_seconds", totalSeconds)
                        showCheckIn()
                        handler.removeCallbacks(timeRunnable)
                        updateTotalTime()
                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "endSession failed: ${obj.getString("message")}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "Error parsing endSession response", e)
                    Toast.makeText(this, "Error ending session", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Log.e(TAG, "Network error in endSession", error)
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                return mutableMapOf(
                    "user_id" to userId.toString(),
                    "project_id" to projectId.toString()
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    // -------------------- UI Updates --------------------

    private fun showCheckIn() {
        btnCheckIn.visibility = View.VISIBLE
        btnCheckOut.visibility = View.GONE
        tvStatus.text = "Not Checked In"
        tvActiveTime.visibility = View.GONE
        updateTotalTime()
        Log.d(TAG, "UI updated to CheckIn state")
    }

    private fun showCheckOut() {
        btnCheckIn.visibility = View.GONE
        btnCheckOut.visibility = View.VISIBLE
        tvStatus.text = "Checked In"
        tvActiveTime.visibility = View.VISIBLE
        updateTotalTime()
        Log.d(TAG, "UI updated to CheckOut state")
    }

    private fun updateActiveTime() {
        if (!sessionStart.isNullOrEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val startTime = sdf.parse(sessionStart!!)
                val now = Date()
                val duration = ((now.time - startTime.time) / 1000).toInt()
                val hours = duration / 3600
                val minutes = (duration % 3600) / 60
                val seconds = duration % 60
                tvActiveTime.text = "Active Time: %02dh %02dm %02ds".format(hours, minutes, seconds)
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating active time", e)
            }
        }
        updateTotalTime()
    }

    private fun updateTotalTime() {
        try {
            val total = if (!sessionStart.isNullOrEmpty()) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val startTime = sdf.parse(sessionStart!!)
                totalSeconds + ((Date().time - startTime.time) / 1000).toInt()
            } else {
                totalSeconds
            }

            val hours = total / 3600
            val minutes = (total % 3600) / 60
            val seconds = total % 60
            tvTotalTimeWorked.text = "Total Worked: %02d:%02d:%02d".format(hours, minutes, seconds)
            Log.d(TAG, "Total worked updated: $total seconds")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating total time", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timeRunnable)
        Log.d(TAG, "Handler callbacks removed in onDestroy")
    }
}
