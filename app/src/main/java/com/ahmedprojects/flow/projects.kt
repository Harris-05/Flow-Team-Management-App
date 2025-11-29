package com.ahmedprojects.flow

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class projects : AppCompatActivity() {

    private var ip: IP_String = IP_String()
    private lateinit var etJoinCode: EditText
    private lateinit var joinBtn: LinearLayout
    private lateinit var createBtn: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProjectAdapter
    private val projectList = ArrayList<Project>()

    // Bottom nav buttons
    private lateinit var homeBtn: LinearLayout
    private lateinit var projectsBtn: LinearLayout
    private lateinit var tasksBtn: LinearLayout
    private lateinit var notificationsBtn: LinearLayout
    private lateinit var profileBtn: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.projects)

        // --- Initialize views ---
        etJoinCode = findViewById(R.id.etJoinCode)
        joinBtn = findViewById(R.id.joinBtn)
        createBtn = findViewById(R.id.createBtn)
        recyclerView = findViewById(R.id.orgRecyclerView)

        homeBtn = findViewById(R.id.homeBtn)
        projectsBtn = findViewById(R.id.projectsBtn)
        tasksBtn = findViewById(R.id.tasksBtn)
        notificationsBtn = findViewById(R.id.notificationsBtn)
        profileBtn = findViewById(R.id.profileBtn)

        // OR, if you prefer IDs, add IDs to each LinearLayout in XML:
        // android:id="@+id/homeBtn"
        // android:id="@+id/projectsBtn"
        // etc.
        // then simply use findViewById(R.id.homeBtn)

        adapter = ProjectAdapter(projectList) { clickedProject ->
            val intent = Intent(this, manage_project_page::class.java)
            intent.putExtra("project_id", clickedProject.id)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter


        // --- Load user's projects ---
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val userId = prefs.getInt("id", -1)
        if (userId == -1) {
            Toast.makeText(this, "Invalid user session!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadUserProjects(userId)

        // --- Join project functionality ---
        joinBtn.setOnClickListener {
            val joinCode = etJoinCode.text.toString().trim()
            if (joinCode.isEmpty()) {
                Toast.makeText(this, "Enter a project code", Toast.LENGTH_SHORT).show()
            } else {
                joinProject(userId, joinCode)
            }
        }

        // --- Create project functionality ---
        createBtn.setOnClickListener {
            val intent = Intent(this, create_project_page::class.java)
            startActivity(intent)
        }

        // --- Bottom navigation functionality ---
        homeBtn.setOnClickListener {
            val intent = Intent(this, home_page::class.java)
            startActivity(intent)
        }

        projectsBtn.setOnClickListener {
            val intent = Intent(this, projects::class.java)
            startActivity(intent)
        }

        tasksBtn.setOnClickListener {
            val intent = Intent(this, tasks_page::class.java)
            startActivity(intent)
        }

        /*notificationsBtn.setOnClickListener {
            val intent = Intent(this, notifications_page::class.java)
            startActivity(intent)
        }

        profileBtn.setOnClickListener {
            val intent = Intent(this, profile_page::class.java)
            startActivity(intent)
        }*/
    }

    private fun loadUserProjects(userId: Int) {
        val queue = Volley.newRequestQueue(this)
        val url = ip.IP + "get_user_projects.php"

        val jsonBody = JSONObject().apply {
            put("userId", userId)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        val projects = response.getJSONArray("projects")
                        projectList.clear()
                        for (i in 0 until projects.length()) {
                            val obj = projects.getJSONObject(i)
                            val project = Project(
                                id = obj.getInt("id"),
                                name = obj.getString("name"),
                                description = obj.getString("description"),
                                role = obj.getString("role"),
                                membersCount = obj.getInt("membersCount")
                            )
                            projectList.add(project)
                        }
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this, "No projects found", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.d("PROJECTS_ERROR", e.toString())
                    Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            })

        queue.add(request)
    }

    private fun joinProject(userId: Int, joinCode: String) {
        val queue = Volley.newRequestQueue(this)
        val url = ip.IP + "join_project.php"

        val jsonBody = JSONObject().apply {
            put("userId", userId)
            put("joinCode", joinCode)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        Toast.makeText(this, "Joined project!", Toast.LENGTH_SHORT).show()
                        loadUserProjects(userId) // refresh list
                    } else {
                        Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid response", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            })
        queue.add(request)
    }
}
