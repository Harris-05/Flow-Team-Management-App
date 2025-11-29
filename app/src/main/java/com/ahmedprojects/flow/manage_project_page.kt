package com.ahmedprojects.flow

import android.content.Intent
import android.os.Bundle
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class manage_project_page : AppCompatActivity() {

    private lateinit var tvProjectTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvOwner: TextView
    private lateinit var tvJoinCode: TextView
    private lateinit var btnInviteUsers: RelativeLayout
    private lateinit var rvTasks: RecyclerView

    private var ip = IP_String()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manage_project_page)

        // Views
        tvProjectTitle = findViewById(R.id.tvProjectTitle)
        tvDescription = findViewById(R.id.tvDescription)
        tvOwner = findViewById(R.id.tvOwner)
        tvJoinCode = findViewById(R.id.tvJoinCode)
        btnInviteUsers = findViewById(R.id.btnInviteUsers)
        rvTasks = findViewById(R.id.rvTasks)
        val btnMembers = findViewById<RelativeLayout>(R.id.btnMembers)

        rvTasks.layoutManager = LinearLayoutManager(this)

        // Get project id from intent
        val projectId = intent.getIntExtra("project_id", -1)
        if (projectId == -1) {
            Toast.makeText(this, "Invalid Project!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load details
        loadProjectDetails(projectId)

        // Invite page button
        /*btnInviteUsers.setOnClickListener {
            val intent = Intent(this, invite_users_page::class.java)
            intent.putExtra("project_id", projectId)
            startActivity(intent)
        }*/

        btnMembers.setOnClickListener {
            val intent = Intent (this, project_members_page::class.java)
            intent.putExtra("project_id", projectId)
            startActivity(intent)

        }
    }

    private fun loadProjectDetails(projectId: Int) {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val currentUserId = prefs.getInt("id", -1)

        val queue = Volley.newRequestQueue(this)
        val url = ip.IP + "get_project_details.php"

        val jsonBody = JSONObject().apply {
            put("projectId", projectId)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                try {
                    if (response.getBoolean("success")) {

                        val name = response.getString("name")
                        val description = response.getString("description")
                        val ownerName = response.getString("owner_name")
                        val joinCode = response.getString("join_code")
                        val ownerId = response.getInt("owner_id")

                        tvProjectTitle.text = name
                        tvDescription.text = description
                        tvOwner.text = "$ownerName"

                        // Only owner sees join code
                        tvJoinCode.text = if (currentUserId == ownerId)
                            "$joinCode"
                        else
                            "******"

                    } else {
                        Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Bad response", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            })

        queue.add(request)
    }
}
