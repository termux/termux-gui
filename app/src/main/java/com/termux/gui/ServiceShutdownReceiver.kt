package com.termux.gui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Stops the service if a broadcast is received, for use with am from Termux.
 */
class ServiceShutdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.stopService(Intent(context, GUIService::class.java))
    }
}