package com.ahmedprojects.flow

import android.os.Bundle
import android.os.Handler
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

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        userId = prefs.getInt("id", -1)
        if (userId == -1) {
            Toast.makeText(this, "Invalid session!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        tvProjectName = findViewById(R.id.tvProjectName)
        btnCheckIn = findViewById(R.id.btnCheckIn)
        btnCheckOut = findViewById(R.id.btnCheckOut)
        backBtn = findViewById<ImageView>(R.id.backBtn)
        tvStatus = findViewById(R.id.tvStatus)
        tvActiveTime = findViewById(R.id.tvActiveTime)
        tvTotalTimeWorked = findViewById(R.id.tvTotalTimeWorked)

        // Receive values from intent

        projectId = intent.getIntExtra("project_id", -1)
        projectName = intent.getStringExtra("project_name") ?: ""

        tvProjectName.text = projectName

        // Load session from server
        getSession()

        btnCheckIn.setOnClickListener { startSession() }
        btnCheckOut.setOnClickListener { endSession() }

        backBtn.setOnClickListener { finish() }
    }

    private fun getSession() {
        val url = "$ip/get_session.php"
        val request = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val obj = JSONObject(response)
                    if (obj.getString("status") == "success") {
                        sessionStart = obj.optString("session_start", null)
                        totalSeconds = obj.optInt("total_seconds", 0)

                        if (sessionStart.isNullOrEmpty() || sessionStart == "null") {
                            showCheckIn()
                        } else {
                            showCheckOut()
                            handler.post(timeRunnable)
                        }
                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                        showCheckIn()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error parsing session", Toast.LENGTH_SHORT).show()
                    showCheckIn()
                }
            },
            { error ->
                error.printStackTrace()
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
        val request = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val obj = JSONObject(response)
                    if (obj.getString("status") == "success") {
                        Toast.makeText(this, "Checked In", Toast.LENGTH_SHORT).show()
                        getSession()
                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error starting session", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
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
        val request = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val obj = JSONObject(response)
                    if (obj.getString("status") == "success") {
                        Toast.makeText(this, "Checked Out", Toast.LENGTH_SHORT).show()
                        sessionStart = null
                        totalSeconds = obj.optInt("total_seconds", 0)
                        showCheckIn()
                        handler.removeCallbacks(timeRunnable)
                        updateTotalTime()
                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error ending session", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
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

    private fun showCheckIn() {
        btnCheckIn.visibility = android.view.View.VISIBLE
        btnCheckOut.visibility = android.view.View.GONE
        tvStatus.text = "Not Checked In"
        tvActiveTime.visibility = android.view.View.GONE
        updateTotalTime()
    }

    private fun showCheckOut() {
        btnCheckIn.visibility = android.view.View.GONE
        btnCheckOut.visibility = android.view.View.VISIBLE
        tvStatus.text = "Checked In"
        tvActiveTime.visibility = android.view.View.VISIBLE
        updateTotalTime()
    }

    private fun updateActiveTime() {
        if (!sessionStart.isNullOrEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val startTime = sdf.parse(sessionStart!!)
            val now = Date()
            val duration = ((now.time - startTime.time) / 1000).toInt()
            val hours = duration / 3600
            val minutes = (duration % 3600) / 60
            val seconds = duration % 60
            tvActiveTime.text = "Active Time: %02dh %02dm %02ds".format(hours, minutes, seconds)
        }
        updateTotalTime()
    }

    private fun updateTotalTime() {
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
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timeRunnable)
    }
}
