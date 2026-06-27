package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.example.data.datastore.SettingsDataStore
import com.example.service.PointerOverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val dataStore = SettingsDataStore(context.applicationContext)
            
            CoroutineScope(Dispatchers.IO).launch {
                val isAutoStart = dataStore.autoStartFlow.first()
                if (isAutoStart) {
                    // Wait 3 seconds as requested before launching
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            val serviceIntent = Intent(context, PointerOverlayService::class.java)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 3000L)
                }
            }
        }
    }
}
