package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.PreferencesManager
import com.example.service.NeverSilentService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (Intent.ACTION_BOOT_COMPLETED == action || Intent.ACTION_MY_PACKAGE_REPLACED == action) {
            val prefs = PreferencesManager(context)
            if (prefs.isProtectionEnabled.value) {
                NeverSilentService.start(context)
            }
        }
    }
}
