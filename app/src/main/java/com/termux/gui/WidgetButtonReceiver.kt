package com.termux.gui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap

class WidgetButtonReceiver : BroadcastReceiver() {
    companion object {
        const val THREAD = "thread";
        const val RID = "rid";
        const val ID = "id"
        val threadCallbacks: MutableMap<Long, (JsonObject) -> Unit> = Collections.synchronizedMap(HashMap<Long, (JsonObject)->Unit>())
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val dat = ConnectionHandler.gson.fromJson(intent.dataString, JsonObject::class.java)
            threadCallbacks[dat[THREAD].asLong]?.let { it(dat) }
        } catch (e:Exception) {
            e.printStackTrace()
        }
    }
}