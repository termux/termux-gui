package com.termux.gui.protocol.json.v0

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.TypedValue
import android.view.View
import android.widget.*
import com.termux.gui.*
import com.termux.gui.protocol.shared.v0.DataClasses
import com.termux.gui.protocol.shared.v0.V0Shared
import java.io.DataOutputStream
import java.util.*

class HandleRemote {
    companion object {
        
        @SuppressLint("ApplySharedPref")
        fun handleRemoteMessage(m: ConnectionHandler.Message, remoteviews: MutableMap<Int, DataClasses.RemoteLayoutRepresentation>, rand: Random, out: DataOutputStream, app: Context) : Boolean {
            if ("createRemoteLayout" == m.method) {
                val id = Util.generateIndex(rand, remoteviews.keys)
                remoteviews[id] = DataClasses.RemoteLayoutRepresentation(RemoteViews(app.packageName, R.layout.remote_view_root))
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
                Util.sendMessage(out, ConnectionHandler.gson.toJson(V0Shared.addRemoteView(FrameLayout::class, remoteviews[m.params?.get("rid")?.asInt], m.params?.get("parent")?.asInt, app)))
                return true
            }
            // Doesn't work currently
            /*
            if ("addRemoteLinearLayout" == m.method) {
                if (m.params?.get("vertical")?.asBoolean == false) {
                    Util.sendMessage(out, ConnectionHandler.gson.toJson(V0Shared.addRemoteView(LinearLayout::class, remoteviews[m.params?.get("rid")?.asInt], m.params?.get("parent")?.asInt, app, "_horizontal")))
                } else {
                    Util.sendMessage(out, ConnectionHandler.gson.toJson(V0Shared.addRemoteView(LinearLayout::class, remoteviews[m.params?.get("rid")?.asInt], m.params?.get("parent")?.asInt, app)))
                }
                return true
            }
             */
            if ("addRemoteTextView" == m.method) {
                Util.sendMessage(out, ConnectionHandler.gson.toJson(V0Shared.addRemoteView(TextView::class, remoteviews[m.params?.get("rid")?.asInt], m.params?.get("parent")?.asInt, app)))
                return true
            }
            if ("addRemoteButton" == m.method) {
                val id = V0Shared.addRemoteView(Button::class, remoteviews[m.params?.get("rid")?.asInt], m.params?.get("parent")?.asInt, app)
                if (id != -1) {
                    remoteviews[m.params?.get("rid")?.asInt]?.root?.setOnClickPendingIntent(id, 
                            PendingIntent.getBroadcast(app, 0, Intent(
                                    Intent.ACTION_DEFAULT, Uri.parse(
                                        ConnectionHandler.gson.toJsonTree(mapOf(
                                                Pair(WidgetButtonReceiver.RID, m.params?.get("rid")?.asInt),
                                                Pair(WidgetButtonReceiver.ID, id),
                                                Pair(WidgetButtonReceiver.THREAD, Thread.currentThread().id))).toString()),
                                    app, WidgetButtonReceiver::class.java),
                                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT))
                }
                Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                return true
            }
            if ("addRemoteImageView" == m.method) {
                Util.sendMessage(out, ConnectionHandler.gson.toJson(V0Shared.addRemoteView(ImageView::class, remoteviews[m.params?.get("rid")?.asInt], m.params?.get("parent")?.asInt, app)))
                return true
            }
            if ("addRemoteProgressBar" == m.method) {
                Util.sendMessage(out, ConnectionHandler.gson.toJson(V0Shared.addRemoteView(ProgressBar::class, remoteviews[m.params?.get("rid")?.asInt], m.params?.get("parent")?.asInt, app)))
                return true
            }
            if ("setRemoteBackgroundColor" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                val rv = remoteviews[rid]
                val id = m.params?.get("id")?.asInt
                val c = m.params?.get("color")?.asInt
                if (rv != null && id != null && c != null) {
                    rv.root?.setInt(id, "setBackgroundColor", c)
                }
                return true
            }
            if ("setRemoteProgressBar" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                val rv = remoteviews[rid]
                val id = m.params?.get("id")?.asInt
                val progress = m.params?.get("progress")?.asInt
                val max = m.params?.get("max")?.asInt
                if (rv != null && id != null && progress != null && max != null) {
                    rv.root?.setProgressBar(id, max, progress, false)
                }
                return true
            }
            if ("setRemoteText" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                val rv = remoteviews[rid]
                val id = m.params?.get("id")?.asInt
                val text = m.params?.get("text")?.asString
                if (rv != null && id != null && text != null) {
                    rv.root?.setTextViewText(id, text)
                }
                return true
            }
            if ("setRemoteTextSize" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                val rv = remoteviews[rid]
                val id = m.params?.get("id")?.asInt
                val size = m.params?.get("size")?.asInt
                val px = m.params?.get("px")?.asBoolean ?: false
                if (rv != null && id != null && size != null) {
                    rv.root?.setTextViewTextSize(id, if (px) TypedValue.COMPLEX_UNIT_PX else TypedValue.COMPLEX_UNIT_DIP, size.toFloat())
                }
                return true
            }
            if ("setRemoteTextColor" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                val rv = remoteviews[rid]
                val id = m.params?.get("id")?.asInt
                val c = m.params?.get("color")?.asInt
                if (rv != null && id != null && c != null) {
                    rv.root?.setTextColor(id, c)
                }
                return true
            }
            if ("setRemoteVisibility" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                val rv = remoteviews[rid]
                val id = m.params?.get("id")?.asInt
                val vis = m.params?.get("vis")?.asInt
                if (rv != null && id != null && vis != null && vis >= 0 && vis <= 2) {
                    rv.root?.setViewVisibility(id, when (vis) {
                        0 -> View.GONE
                        1 -> View.INVISIBLE
                        else -> View.VISIBLE
                    })
                }
                return true
            }
            if ("setRemotePadding" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                val rv = remoteviews[rid]
                val id = m.params?.get("id")?.asInt
                val left = m.params?.get("left")?.asInt
                val top = m.params?.get("top")?.asInt
                val right = m.params?.get("right")?.asInt
                val bottom = m.params?.get("bottom")?.asInt
                if (rv != null && id != null && left != null && top != null && right != null && bottom != null) {
                    rv.root?.setViewPadding(id, left, top, right, bottom)
                }
                return true
            }
            if ("setRemoteImage" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                val rv = remoteviews[rid]
                val id = m.params?.get("id")?.asInt
                val img = m.params?.get("img")?.asString
                if (rv != null && id != null && img != null) {
                    val bin = Base64.decode(img, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bin, 0, bin.size)
                    rv.root?.setImageViewBitmap(id, bitmap)
                }
                return true
            }
            
            
            if ("setWidgetLayout" == m.method) {
                val rid = m.params?.get("rid")?.asInt
                val wid = m.params?.get("wid")?.asString
                val w = GUIWidget.getIDMappingPrefs(app)?.getInt(wid, AppWidgetManager.INVALID_APPWIDGET_ID)
                val rv = remoteviews[rid]
                if (rv != null && w != null && w != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val p = GUIWidget.getIDMappingPrefs(app)
                    if (p != null) {
                        val l = p.getInt("$w-layout", 1)
                        if (l == 1) {
                            p.edit().putInt("$w-layout", 2).commit()
                        } else {
                            p.edit().putInt("$w-layout", 1).commit()
                        }
                        val layout = R.layout::class.java
                        val id = try {
                            layout.getDeclaredField("remote_view_root_real$l").getInt(null)
                        } catch (_:Exception) {
                            null
                        }
                        if (id != null) {
                            val rvReal = RemoteViews(app.packageName, id)
                            rvReal.addView(R.id.root_real, rv.root)
                            Util.runOnUIThreadBlocking {
                                AppWidgetManager.getInstance(app).updateAppWidget(w, rvReal)
                            }
                        }
                    }
                }
                return true
            }
            
            
            if ("createNotificationChannel" == m.method) {

                return true
            }
            if ("createNotification" == m.method) {

                return true
            }
            
            return false
        }
        
        
        
    }
}