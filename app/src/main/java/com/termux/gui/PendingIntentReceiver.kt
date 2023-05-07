package com.termux.gui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.gson.JsonObject
import java.util.*

/**
 * Receives PendingIntents from notifications and widgets and dispatches them to the correct thread.
 */
class PendingIntentReceiver : BroadcastReceiver() {
    companion object {
        const val THREAD = "thread"
        const val RID = "rid"
        const val ID = "id"
        const val ACTION = "action"
        const val NID = "nid"
        const val DISMISSED = "dismissed"
        val threadCallbacks: MutableMap<Long, (JsonObject) -> Unit> = Collections.synchronizedMap(HashMap())
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val dat = ConnectionHandler.gson.fromJson(intent.dataString, JsonObject::class.java)
            threadCallbacks[dat[THREAD].asLong]?.let { it(dat) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}