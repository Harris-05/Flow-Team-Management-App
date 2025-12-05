package com.ahmedprojects.flow

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class view_attendance : AppCompatActivity() {

    private lateinit var tvEmployeeName: TextView
    private lateinit var tvTotalHours: TextView

    private var userId = -1
    private var projectId = -1
    private var memberName = ""
    private var ip = IP_String().IP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.view_attendance)

        // --- Get Views ---
        tvEmployeeName = findViewById(R.id.tvEmployeeName)
        tvTotalHours = findViewById(R.id.tvTotalHours)

        // --- Receive values from intent ---
        userId = intent.getIntExtra("user_id", -1)
        projectId = intent.getIntExtra("project_id", -1)
        memberName = intent.getStringExtra("name") ?: ""

        // --- Set name at top ---
        tvEmployeeName.text = memberName

        if (userId == -1 || projectId == -1) {
            Toast.makeText(this, "Invalid intent data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // --- Fetch hours ---
        fetchTotalHours()
    }

    private fun fetchTotalHours() {
        val url = "$ip/get_member_total_hours.php?user_id=$userId&project_id=$projectId"

        val queue = Volley.newRequestQueue(this)

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                val json = JSONObject(response)

                if (json.getString("status") == "success") {
                    val totalSeconds = json.getInt("total_seconds")
                    tvTotalHours.text = formatTime(totalSeconds)
                } else {
                    Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show()
                }
            },
            {
                Toast.makeText(this, "Network Error!", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }
}
