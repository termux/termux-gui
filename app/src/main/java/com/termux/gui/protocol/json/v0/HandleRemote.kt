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
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.termux.gui.*
import com.termux.gui.protocol.shared.v0.DataClasses
import com.termux.gui.protocol.shared.v0.V0Shared
import java.io.DataOutputStream
import java.util.*

class HandleRemote {
    companion object {
        
        @SuppressLint("ApplySharedPref", "LaunchActivityFromNotification")
        fun handleRemoteMessage(m: ConnectionHandler.Message, remoteviews: MutableMap<Int, DataClasses.RemoteLayoutRepresentation>, rand: Random, out: DataOutputStream, app: Context, notifications: MutableSet<Int>) : Boolean {
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
                                                Pair(PendingIntentReceiver.RID, m.params?.get("rid")?.asInt),
                                                Pair(PendingIntentReceiver.ID, id),
                                                Pair(PendingIntentReceiver.THREAD, Thread.currentThread().id))).toString()),
                                    app, PendingIntentReceiver::class.java),
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
                val id = m.params?.get("id")?.asString
                val importance = m.params?.get("importance")?.asInt
                val name = m.params?.get("name")?.asString
                if (id != null && importance != null && name != null) {
                    val impmap = mapOf(0 to NotificationManagerCompat.IMPORTANCE_MIN, 1 to NotificationManagerCompat.IMPORTANCE_LOW, 2 to NotificationManagerCompat.IMPORTANCE_DEFAULT, 3 to NotificationManagerCompat.IMPORTANCE_HIGH, 4 to NotificationManagerCompat.IMPORTANCE_MAX)
                    NotificationManagerCompat.from(app).createNotificationChannel(
                        NotificationChannelCompat.Builder(id, impmap[importance] ?: NotificationManagerCompat.IMPORTANCE_MIN).setName(name).build())
                }
                return true
            }
            if ("createNotification" == m.method) {
                val id = m.params?.get("id")?.asInt ?: Util.generateIndex(rand, notifications)
                val ongoing = m.params?.get("ongoing")?.asBoolean ?: false
                val layout = remoteviews[m.params?.get("layout")?.asInt]
                val expandedLayout = remoteviews[m.params?.get("expandedLayout")?.asInt]
                val hudLayout = remoteviews[m.params?.get("hudLayout")?.asInt]
                val icon = m.params?.get("icon")?.asString
                val channel = m.params?.get("channel")?.asString
                val importance = m.params?.get("importance")?.asInt
                val alertOnce = m.params?.get("alertOnce")?.asBoolean ?: false
                val showTimestamp = m.params?.get("showTimestamp")?.asBoolean ?: false
                val timestamp = m.params?.get("timestamp")?.asLong
                val title = m.params?.get("title")?.asString
                val content = m.params?.get("content")?.asString
                val largeImage = m.params?.get("largeImage")?.asString
                val largeText = m.params?.get("largeText")?.asString
                val largeImageAsThumbnail = m.params?.get("largeImageAsThumbnail")?.asBoolean ?: false
                val actions = m.params?.get("actions")?.asJsonArray
                if ((layout != null || title != null) && channel != null && importance != null && ! (largeImage != null && largeText != null)) {
                    val b = NotificationCompat.Builder(app, channel)
                    val impmap = mapOf(0 to NotificationCompat.PRIORITY_MIN, 1 to NotificationCompat.PRIORITY_LOW, 2 to NotificationCompat.PRIORITY_DEFAULT, 3 to NotificationCompat.PRIORITY_HIGH, 4 to NotificationCompat.PRIORITY_MAX)
                    b.priority = impmap[importance] ?: NotificationCompat.PRIORITY_DEFAULT
                    if (icon != null) {
                        val bin = Base64.decode(icon, Base64.DEFAULT)
                        b.setSmallIcon(IconCompat.createWithData(bin, 0, bin.size))
                    } else {
                        b.setSmallIcon(R.drawable.ic_service_notification)
                    }
                    if (timestamp != null) {
                        b.setWhen(timestamp)
                    }
                    b.setShowWhen(showTimestamp)
                    b.setOngoing(ongoing)
                    b.setOnlyAlertOnce(alertOnce)
                    if (layout != null) {
                        b.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                        b.setCustomContentView(layout.root)
                        if (expandedLayout != null) {
                            b.setCustomBigContentView(expandedLayout.root)
                        }
                        if (hudLayout != null) {
                            b.setCustomHeadsUpContentView(hudLayout.root)
                        }
                    } else {
                        b.setContentTitle(title)
                        if (largeImage != null) {
                            val bin = Base64.decode(largeImage, Base64.DEFAULT)
                            val bmp = BitmapFactory.decodeByteArray(bin, 0, bin.size)
                            val style = NotificationCompat.BigPictureStyle().bigPicture(bmp)
                            if (largeImageAsThumbnail) {
                                style.bigLargeIcon(null)
                                b.setLargeIcon(bmp)
                            }
                            b.setStyle(style)
                        }
                        if (largeText != null) {
                            b.setStyle(NotificationCompat.BigTextStyle().bigText(largeText))
                        } else {
                            b.setContentText(content)
                        }
                    }
                    if (actions != null) {
                        for ((index, a) in actions.withIndex()) {
                            val action = a.asString
                            b.addAction(0, action, PendingIntent.getBroadcast(app, 0, Intent(
                                Intent.ACTION_DEFAULT, Uri.parse(
                                    ConnectionHandler.gson.toJsonTree(mapOf(
                                        Pair(PendingIntentReceiver.NID, id),
                                        Pair(PendingIntentReceiver.ACTION, index),
                                        Pair(PendingIntentReceiver.THREAD, Thread.currentThread().id))).toString()),
                                app, PendingIntentReceiver::class.java),
                                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT))
                        }
                    }
                    b.setContentIntent(PendingIntent.getBroadcast(app, 0, Intent(
                        Intent.ACTION_DEFAULT, Uri.parse(
                            ConnectionHandler.gson.toJsonTree(mapOf(
                                Pair(PendingIntentReceiver.NID, id),
                                Pair(PendingIntentReceiver.THREAD, Thread.currentThread().id))).toString()),
                        app, PendingIntentReceiver::class.java),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT))
                    NotificationManagerCompat.from(app).notify(Thread.currentThread().id.toString(), id, b.build())
                    notifications.add(id)
                    Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                }
                return true
            }
            if ("cancelNotification" == m.method) {
                val id = m.params?.get("id")?.asInt
                if (id != null) {
                    notifications.remove(id)
                    NotificationManagerCompat.from(app).cancel(Thread.currentThread().id.toString(), id)
                }
                return true
            }
            return false
        }
        
        
        
    }
}