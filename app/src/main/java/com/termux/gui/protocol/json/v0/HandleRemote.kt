package com.termux.gui.protocol.json.v0

import android.content.Context
import android.widget.RemoteViews
import com.termux.gui.ConnectionHandler
import com.termux.gui.R
import com.termux.gui.Util
import java.io.DataOutputStream
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class HandleRemote {
    companion object {

        fun handleRemoteMessage(m: ConnectionHandler.Message, remoteviews: MutableMap<Int, RemoteViews>, rand: Random, out: DataOutputStream, app: Context,
                                eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) : Boolean {
            if ("createRemoteLayout" == m.method) {
                val id = Util.generateIndex(rand, remoteviews.keys)
                remoteviews[id] = RemoteViews(app.packageName, R.layout.remote_view_root)
                Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                return true
            }
            if ("deleteRemoteLayout" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                if (rid != null) {
                    remoteviews.remove(rid)
                }
                return true
            }
            if ("addRemoteFrameLayout" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                if (rid != null) {
                    
                }
                return true
            }
            if ("addRemoteLinearLayout" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                if (rid != null) {

                }
                return true
            }
            if ("addRemoteTextView" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                if (rid != null) {

                }
                return true
            }
            if ("addRemoteButton" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                if (rid != null) {

                }
                return true
            }
            if ("addRemoteImageView" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                if (rid != null) {

                }
                return true
            }
            if ("addRemoteProgressBar" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                if (rid != null) {

                }
                return true
            }
            if ("setRemoteBackgroundColor" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                if (rid != null) {

                }
                return true
            }
            if ("setRemoteProgressBar" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                if (rid != null) {

                }
                return true
            }
            if ("setRemoteText" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                if (rid != null) {

                }
                return true
            }
            if ("setRemoteTextSize" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                if (rid != null) {

                }
                return true
            }
            if ("setRemoteTextColor" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                if (rid != null) {

                }
                return true
            }
            if ("setRemoteVisibility" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                if (rid != null) {

                }
                return true
            }
            if ("setRemotePadding" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                if (rid != null) {

                }
                return true
            }
            if ("setRemoteImage" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                if (rid != null) {

                }
                return true
            }
            
            
            if ("setWidgetLayout" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                if (rid != null) {

                }
                return true
            }
            
            
            if ("createNotificationChannel" == m.method) {

                return true
            }
            if ("createNotification" == m.method) {

                return true
            }
            
            return false;
        }
        
        
        
    }
}