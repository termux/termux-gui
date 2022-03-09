package com.termux.gui.protocol.json.v0

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.termux.gui.ConnectionHandler
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class SystemBroadcastReceiver(val eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) : BroadcastReceiver() {
    
    override fun onReceive(c: Context?, i: Intent?) {
        if (c == null || i == null) return;
        when (i.action) {
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                eventQueue.offer(ConnectionHandler.Event("airplane", ConnectionHandler.gson.toJsonTree(i.getBooleanExtra("state", false))))
            }
            Intent.ACTION_LOCALE_CHANGED-> {
                eventQueue.offer(ConnectionHandler.Event("locale", ConnectionHandler.gson.toJsonTree(c.resources.configuration.locales.get(0).language)))
            }
            Intent.ACTION_SCREEN_OFF -> {
                eventQueue.offer(ConnectionHandler.Event("screen_off",null))
            }
            Intent.ACTION_SCREEN_ON -> {
                eventQueue.offer(ConnectionHandler.Event("screen_on",null))
            }
            Intent.ACTION_TIMEZONE_CHANGED -> {
                eventQueue.offer(ConnectionHandler.Event("timezone", ConnectionHandler.gson.toJsonTree(TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT, c.resources.configuration.locales.get(0)))))
            }
        }
    }
}