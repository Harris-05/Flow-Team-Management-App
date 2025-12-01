package com.ahmedprojects.flow

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import org.json.JSONObject
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Base64
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import java.io.ByteArrayOutputStream
import java.io.InputStream


class create_project_page : AppCompatActivity() {

    private lateinit var etProjectName: EditText
    private lateinit var etProjectDescription: EditText
    private lateinit var btnCreateProject: RelativeLayout
    private lateinit var btnPickImage: RelativeLayout
    private lateinit var ivProjectImage: ImageView

    private var ip: IP_String = IP_String()
    private var selectedImageBase64: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_project_page)

        etProjectName = findViewById(R.id.etProjectName)
        etProjectDescription = findViewById(R.id.etProjectDescription)
        btnCreateProject = findViewById(R.id.btnCreateProject)
        btnPickImage = findViewById(R.id.btnPickImage)
        ivProjectImage = findViewById(R.id.ivProjectImage)

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val userId = prefs.getInt("id", -1)
        if (userId == -1) {
            Toast.makeText(this, "Invalid session!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // --- Image picker launcher ---
        val pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val imageUri: Uri? = data?.data
                imageUri?.let { uri ->
                    ivProjectImage.setImageURI(uri)  // preview

                    // Convert to Base64
                    selectedImageBase64 = convertUriToBase64(uri)
                }
            }
        }

        btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        btnCreateProject.setOnClickListener {
            val name = etProjectName.text.toString().trim()
            val description = etProjectDescription.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Enter project name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createProject(userId, name, description, selectedImageBase64)
        }
    }

    private fun convertUriToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createProject(userId: Int, name: String, description: String, base64Image: String?) {
        val db = AppDatabase.getInstance(this)
        val pendingDao = db.pendingProjectDao()
        val joinCode = UUID.randomUUID().toString().substring(0, 8)

        if (!isOnline()) {
            lifecycleScope.launch {
                pendingDao.insertPending(
                    PendingProjectEntity(
                        ownerId = userId,
                        name = name,
                        description = description,
                        joinCode = joinCode,
                        pictureUrl = base64Image ?: ""
                    )
                )
                Toast.makeText(this@create_project_page, "Saved offline. Will sync later.", Toast.LENGTH_SHORT).show()
                finish()
            }
            return
        }

        createProjectOnline(userId, name, description, joinCode, base64Image)
    }

    private fun createProjectOnline(userId: Int, name: String, description: String, joinCode: String, base64Image: String?) {
        val queue = Volley.newRequestQueue(this)
        val url = ip.IP + "create_project.php"
        val db = AppDatabase.getInstance(this)
        val pendingDao = db.pendingProjectDao()

        val jsonBody = JSONObject().apply {
            put("ownerId", userId)
            put("name", name)
            put("description", description)
            put("joinCode", joinCode)
            put("picture_base64", base64Image ?: "")
        }

        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                if (response.getBoolean("success")) {
                    Toast.makeText(this, "Project created!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            },
            { error ->
                Toast.makeText(this, "Online failed. Saved to offline queue.", Toast.LENGTH_SHORT).show()
                lifecycleScope.launch {
                    pendingDao.insertPending(
                        PendingProjectEntity(
                            ownerId = userId,
                            name = name,
                            description = description,
                            joinCode = joinCode,
                            pictureUrl = base64Image ?: ""
                        )
                    )
                    finish()
                }
            })

        queue.add(request)
    }

    fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetwork != null
    }
}
