package com.ahmedprojects.flow

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream

class Task_Details : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvAssignedBy: TextView
    private lateinit var tvAssignedTo: TextView
    private lateinit var tvDeadline: TextView
    private lateinit var tvPriority: TextView
    private lateinit var tvStatus: TextView

    private lateinit var btnRequestUpdate: Button
    private lateinit var btnMarkCompleted: Button
    private lateinit var memberUpdateSection: LinearLayout
    private lateinit var managerButtons: LinearLayout
    private lateinit var btnChooseImage: Button
    private lateinit var etUpdateMessage: EditText
    private lateinit var btnSubmitUpdate: Button
    private lateinit var imgPreview: ImageView

    private lateinit var rvUpdates: RecyclerView
    private lateinit var updatesAdapter: TaskUpdatesAdapter
    private val updatesList = mutableListOf<TaskUpdate>()

    private var selectedImageUri: Uri? = null
    private var taskId = -1
    private var userId = -1
    private var userRole = "member"

    private val IP = IP_String().IP

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                imgPreview.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.task_details)

        taskId = intent.getIntExtra("task_id", -1)
        if (taskId == -1) {
            Toast.makeText(this, "Invalid task", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        userId = prefs.getInt("id", -1)

        bindViews()
        setupRecyclerView()
        setupActions()
        loadTaskDetails()
    }

    private fun bindViews() {
        tvTitle = findViewById(R.id.tvTaskTitle)
        tvDescription = findViewById(R.id.tvDescription)
        tvAssignedBy = findViewById(R.id.tvAssignedBy)
        tvAssignedTo = findViewById(R.id.tvAssignedTo)
        tvDeadline = findViewById(R.id.tvDeadline)
        tvPriority = findViewById(R.id.tvPriority)
        tvStatus = findViewById(R.id.tvStatus)
        managerButtons = findViewById(R.id.managerButtons)
        btnRequestUpdate = findViewById(R.id.btnRequestUpdate)
        btnMarkCompleted = findViewById(R.id.btnMarkCompleted)
        memberUpdateSection = findViewById(R.id.memberUpdateSection)
        btnChooseImage = findViewById(R.id.btnChooseImage)
        etUpdateMessage = findViewById(R.id.etUpdateMessage)
        btnSubmitUpdate = findViewById(R.id.btnSubmitUpdate)
        imgPreview = findViewById(R.id.imgPreview)

        rvUpdates = findViewById(R.id.rvUpdates)
    }

    private fun setupRecyclerView() {
        rvUpdates.layoutManager = LinearLayoutManager(this)
        updatesAdapter = TaskUpdatesAdapter(updatesList)
        rvUpdates.adapter = updatesAdapter
    }

    private fun setupActions() {
        btnChooseImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        btnSubmitUpdate.setOnClickListener {
            sendUpdate()
        }

        btnRequestUpdate.setOnClickListener {
            requestUpdate()
        }

        btnMarkCompleted.setOnClickListener {
            markTaskCompleted()
        }
    }

    private fun loadTaskDetails() {
        val url = "$IP/get_task_details.php"
        val jsonBody = JSONObject().apply {
            put("task_id", taskId)
            put("user_id", userId)
        }

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                if (!response.getBoolean("success")) {
                    Toast.makeText(this, response.optString("message"), Toast.LENGTH_SHORT).show()
                    return@JsonObjectRequest
                }

                val taskObj = response.getJSONObject("task")
                tvTitle.text = taskObj.getString("title")
                tvDescription.text = taskObj.getString("description")
                val assignedById = taskObj.getInt("assigned_by")
                val assignedToId = taskObj.getInt("assigned_to")

                fetchUserName(assignedById) { name ->
                    tvAssignedBy.text = name
                }
                fetchUserName(assignedToId) { name ->
                    tvAssignedTo.text = name
                }

                tvDeadline.text = taskObj.getString("deadline")
                tvPriority.text = taskObj.getString("priority").capitalize()
                tvStatus.text = taskObj.getString("status").replace("_", " ").capitalize()

                userRole = if (userId == taskObj.getInt("assigned_by")) "manager" else "member"


                if (userRole == "manager") {
                    Log.e("USER_ROLE",userRole)
                    managerButtons.visibility = Button.VISIBLE
                    btnRequestUpdate.visibility = Button.VISIBLE
                    btnMarkCompleted.visibility = Button.VISIBLE
                    memberUpdateSection.visibility = LinearLayout.GONE
                } else {
                    btnRequestUpdate.visibility = Button.GONE
                    btnMarkCompleted.visibility = Button.GONE
                    memberUpdateSection.visibility = LinearLayout.VISIBLE
                }

                // Load updates
                updatesList.clear()
                val updatesArray = response.getJSONArray("updates")
                for (i in 0 until updatesArray.length()) {
                    val u = updatesArray.getJSONObject(i)
                    updatesList.add(
                        TaskUpdate(
                            id = u.getInt("id"),
                            userName = u.getString("user_name"),
                            message = u.optString("message", ""),
                            imageUrl = u.optString("image_url", ""),
                            createdAt = u.getString("created_at")
                        )
                    )
                }
                updatesAdapter.notifyDataSetChanged()
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            })
        queue.add(request)
    }
    private fun fetchUserName(userId: Int, callback: (String) -> Unit){
        val url = "$IP/get_profile_username.php"

        val body = JSONObject().apply {
            put("userId", userId)
        }

        val req = JsonObjectRequest(Request.Method.POST, url, body,
            { res ->
                if (res.optBoolean("success"))
                    callback(res.getString("name"))
                else
                    callback("Unknown")
            },
            { callback("Unknown") }
        )

        Volley.newRequestQueue(this).add(req)
    }

    private fun requestUpdate() {
        val url = "$IP/request_update.php"
        val jsonBody = JSONObject().apply {
            put("task_id", taskId)
            put("user_id", userId)
        }

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                if (response.getBoolean("success")) {
                    Toast.makeText(this, "Update requested successfully", Toast.LENGTH_SHORT).show()
                    loadTaskDetails()
                } else {
                    Toast.makeText(this, response.optString("message"), Toast.LENGTH_SHORT).show()
                }
            }, { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            })
        queue.add(request)
    }

    private fun sendUpdate() {
        val message = etUpdateMessage.text.toString().trim()
        if (message.isEmpty() && selectedImageUri == null) {
            Toast.makeText(this, "Please enter a message or select image", Toast.LENGTH_SHORT).show()
            return
        }

        var imageBase64 = ""
        selectedImageUri?.let { uri ->
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
                imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                imageBase64 = imageBase64.replace("\n", "").replace("\r", "")
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val url = "$IP/submit_update.php"
        val jsonBody = JSONObject().apply {
            put("task_id", taskId)
            put("user_id", userId)
            put("message", message)
            put("image_url", imageBase64)
        }

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                if (response.getBoolean("success")) {
                    Toast.makeText(this, "Update submitted", Toast.LENGTH_SHORT).show()
                    etUpdateMessage.text.clear()
                    selectedImageUri = null
                    imgPreview.setImageDrawable(null)
                    loadTaskDetails()
                } else {
                    Toast.makeText(this, "Failed: ${response.optString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.d("NETWORK_ERROR Updatesned", "Network error: ${error.message}")
            })
        queue.add(request)
    }

    private fun markTaskCompleted() {
        val url = "$IP/mark_task_completed.php"
        val jsonBody = JSONObject().apply {
            put("task_id", taskId)
            put("user_id", userId)
        }

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                if (response.getBoolean("success")) {
                    Toast.makeText(this, "Task marked as completed", Toast.LENGTH_SHORT).show()
                    loadTaskDetails()
                } else {
                    Toast.makeText(this, "Failed: ${response.optString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            })
        queue.add(request)
    }

    data class TaskUpdate(
        val id: Int,
        val userName: String,
        val message: String,
        val imageUrl: String,
        val createdAt: String
    )
}
