package com.ahmedprojects.flow

import android.os.Bundle
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.*

class create_project_page : AppCompatActivity() {

    private lateinit var etProjectName: EditText
    private lateinit var etProjectDescription: EditText
    private lateinit var btnCreateProject: RelativeLayout
    private var ip: IP_String = IP_String()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_project_page)

        // --- Initialize views ---
        etProjectName = findViewById(R.id.etProjectName)
        etProjectDescription = findViewById(R.id.etProjectDescription)
        btnCreateProject = findViewById(R.id.btnCreateProject)

        // --- Get userId from shared preferences ---
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val userId = prefs.getInt("id", -1)
        if(userId == -1){
            Toast.makeText(this, "Invalid session!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnCreateProject.setOnClickListener {
            val name = etProjectName.text.toString().trim()
            val description = etProjectDescription.text.toString().trim()

            if(name.isEmpty()){
                Toast.makeText(this, "Enter project name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createProject(userId, name, description)
        }
    }

    private fun createProject(userId: Int, name: String, description: String) {
        val queue = Volley.newRequestQueue(this)
        val url = ip.IP + "create_project.php"

        // Generate random join code
        val joinCode = UUID.randomUUID().toString().substring(0, 8)

        val jsonBody = JSONObject().apply {
            put("ownerId", userId)
            put("name", name)
            put("description", description)
            put("joinCode", joinCode)
        }

        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                try {
                    if(response.getBoolean("success")){
                        Toast.makeText(this, "Project created successfully!", Toast.LENGTH_SHORT).show()
                        finish() // go back to projects page
                    } else {
                        Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception){
                    Toast.makeText(this, "Invalid server response", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            })

        queue.add(request)
    }
}
