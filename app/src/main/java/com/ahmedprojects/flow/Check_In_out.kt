package com.ahmedprojects.flow

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class Check_In_out : AppCompatActivity() {

    private lateinit var btnCheckIn: LinearLayout
    private lateinit var btnCheckOut: LinearLayout
    private lateinit var tvActiveTime: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvProjectName: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvTotalTimeWorked: TextView

    private var userId = -1
    private var projectId = -1
    private var projectName = ""
    private var ip = IP_String().IP

    private var handler = Handler()
    private var isSessionActive = false
    private var elapsedSeconds = 0
    private var totalSeconds = 0

    private val updateTimer = object : Runnable {
        override fun run() {
            updateCurrentTime()
            if (isSessionActive) {
                elapsedSeconds++
                tvActiveTime.text = "Active Time: ${formatTime(elapsedSeconds)}"
                tvTotalTimeWorked.text = "Total Worked: ${formatTime(totalSeconds + elapsedSeconds)}"
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.check_in_out)
        load_session()
        // Initialize views
        btnCheckIn = findViewById(R.id.btnCheckIn)
        btnCheckOut = findViewById(R.id.btnCheckOut)
        tvActiveTime = findViewById(R.id.tvActiveTime)
        tvStatus = findViewById(R.id.tvStatus)
        tvProjectName = findViewById(R.id.tvProjectName)
        tvTime = findViewById(R.id.tvTime)
        tvDate = findViewById(R.id.tvDate)
        tvLocation = findViewById(R.id.tvLocation)
        tvTotalTimeWorked = findViewById(R.id.tvTotalTimeWorked)

        // Load user info
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        userId = prefs.getInt("id", -1)
        if (userId == -1) {
            Toast.makeText(this, "Invalid session!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load project info
        projectId = intent.getIntExtra("project_id", -1)
        projectName = intent.getStringExtra("project_name") ?: ""
        tvProjectName.text = projectName
        if (projectId == -1) {
            Toast.makeText(this, "Invalid project!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set date and location placeholder
        updateCurrentDate()
        tvLocation.text = "Islamabad, Pakistan"

        // Initialize buttons
        btnCheckIn.setOnClickListener { startSession() }
        btnCheckOut.setOnClickListener { endSession() }

        // Start timer
        handler.post(updateTimer)

        // Load total worked time from server and setup UI based on session_start
        getTotalWorkedTime()
    }
    private fun load_session() {
        val queue = Volley.newRequestQueue(this)
        val url = "$ip/load_session.php"

        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                val json = JSONObject(response)

                if (json.getString("status") == "success") {

                    // If session_start is NOT null => Session Active
                    if (json.has("session_start") && !json.isNull("session_start")) {

                        isSessionActive = true
                        val sessionStartStr = json.getString("session_start")

                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val sessionStartTime =
                            sdf.parse(sessionStartStr)?.time ?: System.currentTimeMillis()

                        // Calculate elapsed time
                        elapsedSeconds =
                            ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()

                        // Update UI → Session Active
                        btnCheckIn.visibility = View.GONE
                        btnCheckOut.visibility = View.VISIBLE
                        tvActiveTime.visibility = View.VISIBLE
                        tvStatus.text = "Checked In"

                    } else {

                        // No active session → user must Check In
                        isSessionActive = false
                        elapsedSeconds = 0

                        // Update UI → No Active Session
                        btnCheckIn.visibility = View.VISIBLE
                        btnCheckOut.visibility = View.GONE
                        tvActiveTime.visibility = View.GONE
                        tvStatus.text = "Not Checked In"
                    }
                } else {
                    Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
                }
            },
            {
                Toast.makeText(this, "Failed to load session", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                return mutableMapOf(
                    "user_id" to userId.toString(),
                    "project_id" to projectId.toString()
                )
            }
        }

        queue.add(stringRequest)
    }

    private fun startSession() {
        val queue = Volley.newRequestQueue(this)
        val url = "$ip/start_session.php"
        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                val json = JSONObject(response)
                if (json.getString("status") == "success") {
                    Toast.makeText(this, "Checked In", Toast.LENGTH_SHORT).show()
                    isSessionActive = true
                    elapsedSeconds = 0
                    tvStatus.text = "Checked In"
                    btnCheckIn.visibility = View.GONE
                    btnCheckOut.visibility = View.VISIBLE
                    tvActiveTime.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error!", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                return mutableMapOf(
                    "user_id" to userId.toString(),
                    "project_id" to projectId.toString()
                )
            }
        }
        queue.add(stringRequest)
    }

    private fun endSession() {
        val queue = Volley.newRequestQueue(this)
        val url = "$ip/end_session.php"
        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                val json = JSONObject(response)
                if (json.getString("status") == "success") {
                    Toast.makeText(this, "Checked Out", Toast.LENGTH_SHORT).show()
                    isSessionActive = false
                    totalSeconds += elapsedSeconds
                    elapsedSeconds = 0
                    tvActiveTime.visibility = View.GONE
                    tvStatus.text = "Not Checked In"
                    btnCheckIn.visibility = View.VISIBLE
                    btnCheckOut.visibility = View.GONE
                    getTotalWorkedTime() // refresh total from server
                } else {
                    Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error!", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                return mutableMapOf(
                    "user_id" to userId.toString(),
                    "project_id" to projectId.toString()
                )
            }
        }
        queue.add(stringRequest)
    }

    private fun getTotalWorkedTime() {
        val queue = Volley.newRequestQueue(this)
        val url = "$ip/get_total_hours.php?user_id=$userId&project_id=$projectId"
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                val json = JSONObject(response)
                if (json.getString("status") == "success") {
                    totalSeconds = json.getInt("total_seconds")
                    tvTotalTimeWorked.text = "Total Worked: ${formatTime(totalSeconds)}"

                    // Check if session_start exists
                    if (json.has("session_start") && !json.isNull("session_start")) {
                        isSessionActive = true
                        val sessionStartStr = json.getString("session_start")
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val sessionStartTime = sdf.parse(sessionStartStr)?.time ?: System.currentTimeMillis()
                        elapsedSeconds = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()

                        // Update UI for active session
                        btnCheckIn.visibility = View.GONE
                        btnCheckOut.visibility = View.VISIBLE
                        tvActiveTime.visibility = View.VISIBLE
                        tvStatus.text = "Checked In"
                    } else {
                        // No active session
                        isSessionActive = false
                        elapsedSeconds = 0
                        btnCheckIn.visibility = View.VISIBLE
                        btnCheckOut.visibility = View.GONE
                        tvActiveTime.visibility = View.GONE
                        tvStatus.text = "Not Checked In"
                    }
                } else {
                    Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Failed to fetch total time", Toast.LENGTH_SHORT).show()
            })
        queue.add(stringRequest)
    }

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    private fun updateCurrentTime() {
        val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        tvTime.text = sdf.format(Date())
    }

    private fun updateCurrentDate() {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        tvDate.text = sdf.format(Date())
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateTimer)
    }
}
