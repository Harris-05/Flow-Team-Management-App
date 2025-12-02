package com.ahmedprojects.flow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

class NetworkReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (!isOnline(context)) return

        val db = AppDatabase.getInstance(context)
        val pendingProjectDao = db.pendingProjectDao()
        val pendingTaskDao = db.pendingTaskDao()

        CoroutineScope(Dispatchers.IO).launch {
            // Sync pending projects
            val pendingProjects = pendingProjectDao.getPending()
            pendingProjects.forEach { project ->
                val success = syncPendingProject(context, project)
                if (success) pendingProjectDao.deletePending(project)
            }

            // Sync pending tasks
            val pendingTasks = pendingTaskDao.getAllTasks()
            pendingTasks.forEach { task ->
                val success = syncPendingTask(context, task)
                if (success) pendingTaskDao.deleteTask(task)
            }
        }
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // 🔹 Sync pending project to server
    private suspend fun syncPendingProject(context: Context, p: PendingProjectEntity): Boolean {
        return try {
            val queue = Volley.newRequestQueue(context)
            val url = IP_String().IP + "create_project.php"

            val jsonBody = JSONObject().apply {
                put("ownerId", p.ownerId)
                put("name", p.name)
                put("description", p.description)
                put("joinCode", p.joinCode)
            }

            suspendCancellableCoroutine<Boolean> { cont ->
                val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    { response -> cont.resume(response.optBoolean("success")) {} },
                    { cont.resume(false) {} }
                )
                queue.add(request)
            }
        } catch (e: Exception) {
            false
        }
    }

    // 🔹 Sync pending task to server
    private suspend fun syncPendingTask(context: Context, t: PendingTaskEntity): Boolean {
        return try {
            val queue = Volley.newRequestQueue(context)
            val url = IP_String().IP + "create_task.php"

            val jsonBody = JSONObject().apply {
                put("project_id", t.projectId)
                put("assigned_to", t.assignedTo)
                put("assigned_by", t.assignedBy)
                put("title", t.title)
                put("description", t.description)
                put("priority", t.priority)
                put("deadline", t.deadline)
            }

            suspendCancellableCoroutine<Boolean> { cont ->
                val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    { response -> cont.resume(response.optBoolean("success")) {} },
                    { cont.resume(false) {} }
                )
                queue.add(request)
            }
        } catch (e: Exception) {
            false
        }
    }
}
