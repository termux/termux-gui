package com.termux.gui

import android.app.ForegroundServiceStartNotAllowedException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Receives broadcasts from clients with the address of the main and event socket, adds the requests to the queue and starts the foreground service if necessary.
 */
class GUIReceiver : BroadcastReceiver() {
    companion object {
        private val TAG: String? = GUIReceiver::class.java.canonicalName
    }


    override fun onReceive(context: Context, intent: Intent) {
        try {
            Logger.log(1, TAG, "received")
            GUIService.requests.add(GUIService.ConnectionRequest(intent.getStringExtra("mainSocket"),
                    intent.getStringExtra("eventSocket")))
            context.startService(Intent(context, GUIService::class.java))
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                Log.e(TAG, "could not start service due to foreground service restriction")
            }
            e.printStackTrace()
        }
    }
}