package com.ahmedprojects.flow

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class home_page : AppCompatActivity() {

    private var ip: IP_String = IP_String()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.home_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var pro = findViewById<ImageView>(R.id.ivNotificationButton)
        pro.setOnClickListener {
            Toast.makeText(this, ip.IP, Toast.LENGTH_SHORT).show()
        }

        var projectsBtn = findViewById<LinearLayout>(R.id.Projects)
        var tasksBtn = findViewById<LinearLayout>(R.id.Tasks)
        var notificationsBtn = findViewById<LinearLayout>(R.id.Notifications)
        var profileBtn = findViewById<LinearLayout>(R.id.Profile)
        
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
}