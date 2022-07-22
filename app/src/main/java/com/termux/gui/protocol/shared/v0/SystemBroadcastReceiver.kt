package com.termux.gui.protocol.shared.v0

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SystemBroadcastReceiver(private val v0: V0Shared) : BroadcastReceiver() {
    
    override fun onReceive(c: Context?, i: Intent?) {
        if (c == null || i == null) return
        when (i.action) {
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                v0.onAirplaneModeChanged(c, i)
            }
            Intent.ACTION_LOCALE_CHANGED-> {
                v0.onLocaleChanged(c, i)
            }
            Intent.ACTION_SCREEN_OFF -> {
                v0.onScreenOff(c, i)
            }
            Intent.ACTION_SCREEN_ON -> {
                v0.onScreenOn(c, i)
            }
            Intent.ACTION_TIMEZONE_CHANGED -> {
                v0.onTimezoneChanged(c, i)
            }
        }
    }
}