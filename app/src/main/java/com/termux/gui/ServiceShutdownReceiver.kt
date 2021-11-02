package com.termux.gui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ServiceShutdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.stopService(Intent(context, GUIService::class.java))
    }
}