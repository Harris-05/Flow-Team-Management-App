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
import org.json.JSONObject


class NetworkReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (!isOnline(context)) return

        val db = AppDatabase.getInstance(context)
        val pendingDao = db.pendingProjectDao()

        CoroutineScope(Dispatchers.IO).launch {
            val pending = pendingDao.getPending()
            if (pending.isEmpty()) return@launch

            pending.forEach { project ->
                val success = syncPendingProject(context, project)
                if (success) {
                    pendingDao.deletePending(project)
                }
            }
        }
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun syncPendingProject(context: Context, p: PendingProjectEntity): Boolean {
        return try {
            val queue = Volley.newRequestQueue(context)
            val ip = IP_String()
            val url = ip.IP + "create_project.php"

            val jsonBody = JSONObject().apply {
                put("ownerId", p.ownerId)
                put("name", p.name)
                put("description", p.description)
                put("joinCode", p.joinCode)
            }

            val response = kotlinx.coroutines.suspendCancellableCoroutine<Boolean> { cont ->
                val req = JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    { json ->
                        cont.resume(json.optBoolean("success")) {}
                    },
                    { cont.resume(false) {} }
                )
                queue.add(req)
            }

            response
        } catch (e: Exception) {
            false
        }
    }
}

