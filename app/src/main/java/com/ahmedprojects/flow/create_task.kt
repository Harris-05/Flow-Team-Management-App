package com.ahmedprojects.flow

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.*

class create_task : AppCompatActivity() {

    private lateinit var spinnerUsers: Spinner
    private lateinit var spinnerPriority: Spinner
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var etDeadline: TextView
    private lateinit var btnCreate: RelativeLayout
    private lateinit var tvProjectName: TextView

    private var IP = IP_String().IP
    private var userId = -1
    private var projectId = -1
    private var membersList = mutableListOf<Member>()

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.create_task)

        db = AppDatabase.getInstance(this)

        // Load session info
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        userId = prefs.getInt("id", -1)
        projectId = intent.getIntExtra("project_id", -1)

        // Initialize views
        tvProjectName = findViewById(R.id.tvProjectName)
        spinnerUsers = findViewById(R.id.spinnerAssignTo)
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etDeadline = findViewById(R.id.etDeadline)
        spinnerPriority = findViewById(R.id.spinnerPriority)
        btnCreate = findViewById(R.id.btnCreateTask)

        // Priority dropdown
        val priorityList = listOf("High", "Medium", "Low")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, priorityList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = adapter

        etDeadline.setOnClickListener { showDatePicker() }

        // Load project details and members
        loadProjectDetails()
        loadProjectMembers()

        // Queue task offline
        btnCreate.setOnClickListener {
            queueTaskOffline()
        }

        // ✅ No need to call syncPendingTasks(); the BroadcastReceiver handles syncing automatically
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(this, { _, y, m, d ->
            etDeadline.text = "$y-${m + 1}-$d"
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePicker.show()
    }

    private fun loadProjectDetails() {
        val url = "$IP/get_project_details.php"
        val payload = JSONObject().apply { put("projectId", projectId) }

        val request = JsonObjectRequest(Request.Method.POST, url, payload,
            { response ->
                if (response.optBoolean("success")) {
                    tvProjectName.text = response.optString("name", "Unknown Project")
                }
            },
            { _ ->
                Toast.makeText(this, "Failed to load project", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun loadProjectMembers() {
        val url = "$IP/get_project_members.php"
        val payload = JSONObject().apply { put("project_id", projectId) }

        val request = JsonObjectRequest(Request.Method.POST, url, payload,
            { response ->
                if (response.optBoolean("success")) {
                    val arr = response.optJSONArray("members")
                    membersList.clear()
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val id = obj.optInt("user_id", -1)
                            val name = obj.optString("name")
                            if (id != -1) membersList.add(Member(id, name))
                        }
                    }
                    val names = membersList.map { it.name }
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerUsers.adapter = adapter
                }
            },
            { _ ->
                Toast.makeText(this, "Failed to load members", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun queueTaskOffline() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()

        if (title.isEmpty()) {
            etTitle.error = "Title required"
            return
        }

        val assignedUserId = if (membersList.isNotEmpty()) membersList[spinnerUsers.selectedItemPosition].id else -1
        val deadline = etDeadline.text.toString()
        val priority = spinnerPriority.selectedItem.toString()

        val task = PendingTaskEntity(
            projectId = projectId,
            assignedTo = assignedUserId,
            assignedBy = userId,
            title = title,
            description = description,
            priority = priority,
            deadline = deadline
        )

        lifecycleScope.launch(Dispatchers.IO) {
            db.pendingTaskDao().insertTask(task)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@create_task, "Task queued offline", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    data class Member(val id: Int, val name: String)
}
