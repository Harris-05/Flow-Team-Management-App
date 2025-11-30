package com.ahmedprojects.flow

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject

class tasks_you_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskAdapter

    private val tasksByYou = mutableListOf<TaskModel>()

    private var userId = -1
    private var IP = IP_String().IP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.tasks_you_page)

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        userId = prefs.getInt("id", -1)

        // Handle system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



        recyclerView = findViewById(R.id.recyclerViewTasks)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TaskAdapter(mutableListOf())
        recyclerView.adapter = adapter
        setupTabs()

        fetchTasks()
    }
    private fun setupTabs() {
        val tabForYou: TextView = findViewById(R.id.tabForYou)
        val tabByYou: TextView = findViewById(R.id.tabByYou)

        // Click "Tasks for You" → go back to tasks_page
        tabForYou.setOnClickListener {
            val intent = Intent(this, tasks_page::class.java)
            startActivity(intent)
            finish() // optional, close this page
        }

        // Highlight "Tasks by You" tab
        tabByYou.setTextColor(resources.getColor(R.color.blue))
        tabForYou.setTextColor(resources.getColor(R.color.gray))
    }

    private fun fetchTasks() {
        val url = "$IP/get_tasks_by_you.php?user_id=$userId" // <-- Different API
        val queue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response -> parseTasks(response) },
            { error ->
                Toast.makeText(
                    this,
                    "Failed to fetch tasks: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("API_FETCH_TASKS", "Failed to fetch tasks: ${error.message}")
            }
        )

        queue.add(request)
    }

    private fun parseTasks(response: JSONObject) {
        if (!response.getBoolean("success")) {
            Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
            return
        }

        tasksByYou.clear()

        val tasksArray = response.getJSONArray("tasks")
        for (i in 0 until tasksArray.length()) {
            val obj = tasksArray.getJSONObject(i)
            val task = TaskModel(
                id = obj.getInt("id"),
                title = obj.getString("title"),
                description = obj.getString("description"),
                priority = obj.getString("priority"),
                status = obj.getString("status"),
                assignedBy = obj.getInt("assigned_by"),
                organisationName = obj.getString("project_name"),
                updateRequested = obj.optInt("update_requested", 0) == 1,

                percentageCompleted = obj.optInt("completion", 0),  // NEW
                dueDate = obj.optString("deadline", "N/A")                    // NEW
            )

            tasksByYou.add(task)
        }

        adapter.updateTasks(tasksByYou)
    }
}
