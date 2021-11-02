package com.termux.gui

import android.app.ForegroundServiceStartNotAllowedException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class GUIReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            println("received")
            GUIService.requests.add(GUIService.ConnectionRequest(intent.getStringExtra("mainSocket"),
                    intent.getStringExtra("eventSocket")))
            context.startService(Intent(context, GUIService::class.java))
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                Log.d("GUIReceiver", "could not start service due to foreground service restriction")
            }
            e.printStackTrace()
        }
    }
}