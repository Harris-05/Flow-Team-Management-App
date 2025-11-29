package com.ahmedprojects.flow

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.Date

class create_task : AppCompatActivity() {

    private lateinit var spinnerUsers: Spinner
    private lateinit var spinnerProjects: Spinner

    private lateinit var spinnerPriority: Spinner
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText

    private lateinit var etDeadline: DatePicker
    private lateinit var btnCreate: Button


    private var IP = IP_String().IP
    private var userId=-1
    private var usersList = mutableListOf<User>()
    private var projectsList = mutableListOf<Project>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.create_task)
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val CuserId = prefs.getInt("id", -1)
        userId=CuserId

        spinnerUsers = findViewById(R.id.spinnerAssignTo)
        spinnerProjects = findViewById(R.id.spinnerProject)
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        btnCreate = findViewById(R.id.btnCreateTask)
        spinnerPriority = findViewById(R.id.spinnerPriority)
        etDeadline = findViewById(R.id.etDeadline)


// Create the list
        val priorityList = listOf("High", "Medium", "Low")

// Create adapter for spinner
        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            priorityList
        )

// Set dropdown style
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

// Attach adapter to spinner
        spinnerPriority.adapter = priorityAdapter



        loadUsers()
        loadProjects()

        btnCreate.setOnClickListener {
            createTask()
        }
    }

    private fun loadUsers() {
        val url = "$IP/getAllUsers.php"
        val queue = Volley.newRequestQueue(this)

        // clear previous
        usersList.clear()

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    // debug: raw response
                    Log.d("API_LOAD_USERS", response.toString())

                    if (response.optBoolean("success", false)) {
                        val usersArray = response.optJSONArray("users")
                        if (usersArray != null) {
                            for (i in 0 until usersArray.length()) {
                                val obj = usersArray.getJSONObject(i)
                                val id = obj.optInt("id", -1)
                                val name = obj.optString("name", "Unknown")
                                if (id != -1) usersList.add(User(id, name))
                            }
                        }

                        // create adapter with names (if list empty, show placeholder)
                        val names = if (usersList.isNotEmpty()) usersList.map { it.name } else listOf("No users")
                        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerUsers.adapter = adapter
                    } else {
                        val msg = response.optString("message", "Failed to load users")
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        Log.w("API_LOAD_USERS", "success=false, message=$msg")
                    }
                } catch (e: Exception) {
                    Log.e("API_LOAD_USERS", "parse error", e)
                    Toast.makeText(this, "Parse error loading users", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("API_LOAD_USERS", "network error", error)
                Toast.makeText(this, "Error loading users: ${error.message ?: "network"}", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }

    private fun loadProjects() {
        val url = "$IP/getAllProjects.php"
        val queue = Volley.newRequestQueue(this)

        // clear previous
        projectsList.clear()

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    Log.d("API_LOAD_PROJECTS", response.toString())

                    if (response.optBoolean("success", false)) {
                        val projectsArray = response.optJSONArray("projects")
                        if (projectsArray != null) {
                            for (i in 0 until projectsArray.length()) {
                                val obj = projectsArray.getJSONObject(i)
                                val id = obj.optInt("id", -1)
                                val name = obj.optString("name", "Untitled")
                                if (id != -1) projectsList.add(Project(id, name))
                            }
                        }

                        val names = if (projectsList.isNotEmpty()) projectsList.map { it.name } else listOf("No projects")
                        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        // IMPORTANT: use the spinner id in your layout. Replace spinnerProjects if different
                        spinnerProjects.adapter = adapter
                    } else {
                        val msg = response.optString("message", "Failed to load projects")
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        Log.w("API_LOAD_PROJECTS", "success=false, message=$msg")
                    }
                } catch (e: Exception) {
                    Log.e("API_LOAD_PROJECTS", "parse error", e)
                    Toast.makeText(this, "Parse error loading projects", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("API_LOAD_PROJECTS", "network error", error)
                Toast.makeText(this, "Error loading projects: ${error.message ?: "network"}", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }


    private fun createTask() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()

        if (title.isEmpty()) {
            etTitle.error = "Title required"
            return
        }

        // get selected user and project
        val assignedUserId = usersList[spinnerUsers.selectedItemPosition].id
        val projectId = projectsList[spinnerProjects.selectedItemPosition].id
        val assignedById = userId

        // get priority
        val priority = spinnerPriority.selectedItem.toString()

        // get deadline
        val day = etDeadline.dayOfMonth
        val month = etDeadline.month + 1
        val year = etDeadline.year
        val deadline = "$year-$month-$day"

        // create payload
        val payload = JSONObject().apply {
            put("project_id", projectId)
            put("assigned_to", assignedUserId)
            put("assigned_by", assignedById)
            put("title", title)
            put("description", description)
            put("deadline", deadline)
            put("priority", priority)
        }

        val url = "$IP/create_task.php"
        val queue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(Request.Method.POST, url, payload,
            { response ->
                if (response.getBoolean("success")) {
                    Toast.makeText(this, "Task created successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Error creating task: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }


    data class User(val id: Int, val name: String)
    data class Project(val id: Int, val name: String)
}
