package com.ahmedprojects.flow

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class Login : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: TextView
    private lateinit var signupLink: TextView

    private val BASE_URL = IP_String().IP   // <-- USING YOUR GLOBAL CLASS
    private val LOGIN_URL = BASE_URL + "login.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        emailInput = findViewById(R.id.input_email)
        passwordInput = findViewById(R.id.input_password)
        loginBtn = findViewById(R.id.btn_login)
        signupLink = findViewById(R.id.signup_link)

        loginBtn.setOnClickListener { loginUser() }

        signupLink.setOnClickListener {
            startActivity(Intent(this, sign_up_page::class.java))
        }
    }

    private fun loginUser() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        loginBtn.isEnabled = false
        loginBtn.text = "Logging in..."

        val queue = Volley.newRequestQueue(this)

        // --- Create JSON payload ---
        val jsonBody = JSONObject()
        jsonBody.put("email", email)
        jsonBody.put("password", password)

        val request = JsonObjectRequest(
            Request.Method.POST,
            LOGIN_URL,
            jsonBody,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

                        // Save session
                        val user = response.getJSONObject("user")
                        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
                        prefs.edit().apply {
                            putInt("id", user.getInt("id"))
                            putString("name", user.getString("name"))
                            putString("email", user.getString("email"))
                            apply()
                        }

                        startActivity(Intent(this, home_page::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid server response", Toast.LENGTH_SHORT).show()
                    Log.d("PAKROMUJHE", "Invalid server response: $e")
                }

                loginBtn.isEnabled = true
                loginBtn.text = "Login"
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.d("PAKROMUJHE", "Network error: ${error.message}")
                loginBtn.isEnabled = true
                loginBtn.text = "Login"
            }
        )

        // --- Add request to queue ---
        queue.add(request)
    }
}