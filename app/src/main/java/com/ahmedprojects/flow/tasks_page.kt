package com.ahmedprojects.flow

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
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
import org.json.JSONObject

class tasks_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskAdapter

    private val tasksForYou = mutableListOf<TaskModel>()
    private val tasksByYou = mutableListOf<TaskModel>()

    var userId = -1
    private var IP = IP_String().IP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.tasks_page)

        val homebtn= findViewById<LinearLayout>(R.id.homeBtn)
        val projectsbtn= findViewById<LinearLayout>(R.id.projectsBtn)
        val notificationsbtn= findViewById<LinearLayout>(R.id.notificationsBtn)
        val profilebtn= findViewById<LinearLayout>(R.id.profileBtn)

        homebtn.setOnClickListener {
            val intent = Intent(this, home_page::class.java)
            startActivity(intent)
         }
        projectsbtn.setOnClickListener {
            val intent = Intent(this, projects::class.java)
            startActivity(intent)

        }


        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val CuserId = prefs.getInt("id", -1)
        userId = CuserId

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recyclerViewTasks)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TaskAdapter(mutableListOf(), onClick = { task ->
            val intent = Intent(this, Task_Details::class.java)
            intent.putExtra("task_id", task.id)
            startActivity(intent)
        })
        recyclerView.adapter = adapter

        setupTabs()
        setupStatusFilters()
        fetchTasks()
    }

    private fun setupTabs() {
        val tabForYou: TextView = findViewById(R.id.tabForYou)
        val tabByYou: TextView = findViewById(R.id.tabByYou)

        tabForYou.setOnClickListener {
            adapter.updateTasks(tasksForYou)
            tabForYou.setTextColor(resources.getColor(R.color.blue))
            tabByYou.setTextColor(resources.getColor(R.color.gray))
            highlightTab(findViewById(R.id.all), findViewById(R.id.pending), findViewById(R.id.completed))
        }

        tabByYou.setOnClickListener {
            val intent = Intent(this, tasks_you_page::class.java)
            startActivity(intent)
        }
    }

    // 🔵 NEW FILTER SETUP
    private fun setupStatusFilters() {
        val all: TextView = findViewById(R.id.all)
        val pending: TextView = findViewById(R.id.pending)
        val completed: TextView = findViewById(R.id.completed)

        all.setOnClickListener {
            adapter.updateTasks(tasksForYou)
            highlightTab(all, pending, completed)
        }

        pending.setOnClickListener {
            val filtered = tasksForYou.filter { it.status.equals("pending", ignoreCase = true) }
            adapter.updateTasks(filtered)
            highlightTab(pending, all, completed)
        }

        completed.setOnClickListener {
            val filtered = tasksForYou.filter { it.status.equals("completed", ignoreCase = true) }
            adapter.updateTasks(filtered)
            highlightTab(completed, all, pending)
        }
    }

    // 🔵 Highlight the active filter
    private fun highlightTab(active: TextView, vararg others: TextView) {
        active.setBackgroundResource(R.drawable.oblong_blue)
        active.setTextColor(resources.getColor(R.color.white))

        others.forEach {
            it.setBackgroundResource(R.drawable.oblong_white)
            it.setTextColor(resources.getColor(R.color.blue))
        }
    }

    private fun fetchTasks() {
        val url = "$IP/get_user_tasks.php?user_id=$userId"
        val queue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response -> parseTasks(response) },
            { error ->
                Toast.makeText(this, "Failed to fetch tasks: ${error.message}", Toast.LENGTH_SHORT).show()
            })

        queue.add(request)
    }

    private fun parseTasks(response: JSONObject) {
        if (!response.getBoolean("success")) {
            Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
            return
        }

        tasksForYou.clear()
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
                percentageCompleted = obj.optInt("completion", 0),
                dueDate = obj.optString("deadline", "N/A")
            )

            if (obj.getInt("assigned_to") == userId) tasksForYou.add(task)
            if (obj.getInt("assigned_by") == userId) tasksByYou.add(task)
        }

        // Default: ALL tab selected
        adapter.updateTasks(tasksForYou)
        highlightTab(findViewById(R.id.all), findViewById(R.id.pending), findViewById(R.id.completed))
    }
}
