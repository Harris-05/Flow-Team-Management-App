package com.ahmedprojects.flow

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class create_task : AppCompatActivity() {

    private lateinit var spinnerUsers: Spinner
    private lateinit var spinnerPriority: Spinner
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var etDeadline: DatePicker
    private lateinit var btnCreate: Button
    private lateinit var tvProjectName: TextView

    private var IP = IP_String().IP
    private var userId = -1
    private var projectId = -1

    private var membersList = mutableListOf<Member>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.create_task)

        // session user
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        userId = prefs.getInt("id", -1)

        // project id passed from previous page
        projectId = intent.getIntExtra("project_id", -1)

        // views
        tvProjectName = findViewById(R.id.tvProjectName)
        spinnerUsers = findViewById(R.id.spinnerAssignTo)
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etDeadline = findViewById(R.id.etDeadline)
        spinnerPriority = findViewById(R.id.spinnerPriority)
        btnCreate = findViewById(R.id.btnCreateTask)

        // priority dropdown
        val priorityList = listOf("High", "Medium", "Low")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, priorityList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = adapter

        // Load data
        loadProjectDetails()
        loadProjectMembers()

        btnCreate.setOnClickListener {
            createTask()
        }
    }

    private fun loadProjectDetails() {
        val url = "$IP/get_project_details.php"

        val payload = JSONObject().apply {
            put("projectId", projectId)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, payload,
            { response ->
                if (response.optBoolean("success")) {
                    val name = response.optString("name", "Unknown Project")
                    tvProjectName.text = name
                }
            },
            { error ->
                Toast.makeText(this, "Failed to load project", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun loadProjectMembers() {
        val url = "$IP/get_project_members.php"

        val payload = JSONObject().apply {
            put("project_id", projectId)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, payload,
            { response ->
                Log.d("PROJECT_MEMBERS", response.toString())

                if (response.optBoolean("success")) {
                    val arr = response.optJSONArray("members")

                    membersList.clear()

                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val id = obj.optInt("user_id", -1)
                            val name = obj.optString("name")

                            if (id != -1) {
                                membersList.add(Member(id, name))
                            }
                        }
                    }

                    val names = membersList.map { it.name }
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerUsers.adapter = adapter
                }
            },
            { error ->
                Log.e("PROJECT_MEMBERS", "Error: ${error.message}")
                Toast.makeText(this, "Failed to load members", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun createTask() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()

        if (title.isEmpty()) {
            etTitle.error = "Title required"
            return
        }

        val assignedUserId =
            if (membersList.isNotEmpty()) membersList[spinnerUsers.selectedItemPosition].id else -1

        val day = etDeadline.dayOfMonth
        val month = etDeadline.month + 1
        val year = etDeadline.year
        val deadline = "$year-$month-$day"

        val priority = spinnerPriority.selectedItem.toString()

        val payload = JSONObject().apply {
            put("project_id", projectId)
            put("assigned_to", assignedUserId)
            put("assigned_by", userId)
            put("title", title)
            put("description", description)
            put("priority", priority)
            put("deadline", deadline)
        }

        val url = "$IP/create_task.php"

        val request = JsonObjectRequest(Request.Method.POST, url, payload,
            { response ->
                if (response.optBoolean("success")) {
                    Toast.makeText(this, "Task Created!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, response.optString("message"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Error creating task", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    data class Member(val id: Int, val name: String)
}
