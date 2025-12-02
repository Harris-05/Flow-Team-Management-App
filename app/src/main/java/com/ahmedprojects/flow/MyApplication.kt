package com.ahmedprojects.flow
import android.app.Application
import android.content.IntentFilter
import android.net.ConnectivityManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(NetworkReceiver(), filter)

    }
}
