package com.termux.gui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.lang.Exception
import java.util.*

class WidgetButtonReceiver : BroadcastReceiver() {
    companion object {
        val events: MutableList<Pair<Int,Int>> = LinkedList()
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (events.size > 100) {
                events.removeAt(events.size - 1)
            }
            val parts = intent.dataString?.split(":")
            if (parts != null && parts.size == 2) {
                events.add(Pair(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}