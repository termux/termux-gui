package com.termux.gui.protocol.protobuf.v0

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.termux.gui.*
import com.termux.gui.protocol.protobuf.ProtoUtils
import com.termux.gui.protocol.protobuf.v0.GUIProt0.*
import com.termux.gui.protocol.shared.v0.DataClasses
import java.io.OutputStream
import java.util.*

class HandleNotification(val main: OutputStream, val remoteviews: MutableMap<Int, DataClasses.RemoteLayoutRepresentation>,
                         val rand: Random, val app: Context, val notifications: MutableSet<Int>,
                         val activities: MutableMap<Int, DataClasses.ActivityState>, val logger: V0Proto.ProtoLogger) {
    
    
    fun createChannel(m: CreateNotificationChannelRequest) {
        val ret = CreateNotificationChannelResponse.newBuilder()
        try {
            val impmap = mapOf(
                Importance.MIN to NotificationManagerCompat.IMPORTANCE_MIN,
                Importance.LOW to NotificationManagerCompat.IMPORTANCE_LOW,
                Importance.DEFAULT to NotificationManagerCompat.IMPORTANCE_DEFAULT,
                Importance.HIGH to NotificationManagerCompat.IMPORTANCE_HIGH,
                Importance.MAX to NotificationManagerCompat.IMPORTANCE_MAX)
            NotificationManagerCompat.from(app).createNotificationChannel(
                NotificationChannelCompat.Builder(m.id, impmap[m.importance] ?: NotificationManagerCompat.IMPORTANCE_MIN).setName(m.name).build())
            ret.success = true
        } catch (e: java.lang.Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }
    
    
    @SuppressLint("LaunchActivityFromNotification")
    fun create(m: CreateNotificationRequest) {
        val ret = CreateNotificationResponse.newBuilder()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (app.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                    var a: GUIActivity? = null
                    for (ac in activities) {
                        if (ac.value.a?.isDestroyed == false) {
                            a = ac.value.a
                            break
                        }
                    }
                    if (a == null) {
                        Toast.makeText(app, "Please grant Termux:GUI the notification permission", Toast.LENGTH_LONG).show()
                    } else {
                        a.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
                    }
                    ret.id = -1
                    ret.code = Error.INTERNAL_ERROR
                    ProtoUtils.write(ret, main)
                    return
                }
            }
            val id = if (m.id >= 0) m.id else Util.generateIndex(rand, notifications)
            val b = NotificationCompat.Builder(app, m.channel)
            
            when (m.typeCase!!) {
                CreateNotificationRequest.TypeCase.NORMAL -> {
                    b.setContentTitle(m.normal.title)
                    b.setContentTitle(m.normal.content)
                }
                CreateNotificationRequest.TypeCase.LONGTEXT -> {
                    b.setContentTitle(m.longText.title)
                    b.setStyle(NotificationCompat.BigTextStyle().bigText(m.longText.content))
                }
                CreateNotificationRequest.TypeCase.BIGIMAGE -> {
                    b.setContentTitle(m.bigImage.title)
                    b.setContentText(m.bigImage.content)
                    val bin = m.bigImage.image.toByteArray()
                    val bmp = BitmapFactory.decodeByteArray(bin, 0, bin.size)
                    val style = NotificationCompat.BigPictureStyle().bigPicture(bmp)
                    if (m.bigImage.asThumbnail) {
                        style.bigLargeIcon(null)
                        b.setLargeIcon(bmp)
                    }
                    b.setStyle(style)
                }
                CreateNotificationRequest.TypeCase.CUSTOM -> {
                    b.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                    val layout = remoteviews[m.custom.layout]
                    val layoutExpanded = remoteviews[m.custom.layoutExpanded]
                    val layoutHUD = remoteviews[m.custom.layoutHUD]
                    if (layout == null) {
                        ret.id = -1
                        ret.code = Error.INVALID_REMOTE_LAYOUT
                        ProtoUtils.write(ret, main)
                        return
                    }
                    b.setCustomContentView(layout.root)
                    if (layoutExpanded != null) {
                        b.setCustomBigContentView(layoutExpanded.root)
                    }
                    if (layoutHUD != null) {
                        b.setCustomHeadsUpContentView(layoutHUD.root)
                    }
                }
                CreateNotificationRequest.TypeCase.TYPE_NOT_SET -> {
                    ret.id = -1
                    ret.code = Error.INVALID_ENUM
                    ProtoUtils.write(ret, main)
                    return
                }
            }
            
            val impmap = mapOf(
                Importance.MIN to NotificationCompat.PRIORITY_MIN,
                Importance.LOW to NotificationCompat.PRIORITY_LOW,
                Importance.DEFAULT to NotificationCompat.PRIORITY_DEFAULT,
                Importance.HIGH to NotificationCompat.PRIORITY_HIGH,
                Importance.MAX to NotificationCompat.PRIORITY_MAX)
            b.priority = impmap[m.importance] ?: NotificationCompat.PRIORITY_DEFAULT
            
            if (! m.icon.isEmpty) {
                val bin = m.icon.toByteArray()
                b.setSmallIcon(IconCompat.createWithData(bin, 0, bin.size))
            } else {
                b.setSmallIcon(R.drawable.ic_service_notification)
            }
            
            if (m.timestamp != 0L) {
                b.setWhen(m.timestamp)
            }
            
            b.setShowWhen(m.showTimestamp)
            b.setOngoing(m.ongoing)
            b.setOnlyAlertOnce(m.alertOnce)
            
            for ((index, action) in m.actionsList.withIndex()) {
                b.addAction(0, action, PendingIntent.getBroadcast(app, 0, Intent(
                    Intent.ACTION_DEFAULT, Uri.parse(
                        ConnectionHandler.gson.toJsonTree(mapOf(
                            Pair(PendingIntentReceiver.NID, id),
                            Pair(PendingIntentReceiver.ACTION, index),
                            Pair(PendingIntentReceiver.THREAD, Thread.currentThread().id))).toString()),
                    app, PendingIntentReceiver::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT))
            }
            
            
            b.setContentIntent(PendingIntent.getBroadcast(app, 0, Intent(
                Intent.ACTION_DEFAULT, Uri.parse(
                    ConnectionHandler.gson.toJsonTree(mapOf(
                        Pair(PendingIntentReceiver.NID, id),
                        Pair(PendingIntentReceiver.THREAD, Thread.currentThread().id))).toString()),
                app, PendingIntentReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT))
            
            
            b.setDeleteIntent(PendingIntent.getBroadcast(app, 0, Intent(
                Intent.ACTION_DEFAULT, Uri.parse(
                    ConnectionHandler.gson.toJsonTree(mapOf(
                        Pair(PendingIntentReceiver.NID, id),
                        Pair(PendingIntentReceiver.THREAD, Thread.currentThread().id),
                        Pair(PendingIntentReceiver.DISMISSED, true))).toString()),
                app, PendingIntentReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT))
            
            NotificationManagerCompat.from(app).notify(Thread.currentThread().id.toString(), id, b.build())
            notifications.add(id)
            ret.id = id
        } catch (e: java.lang.Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.id = -1
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }
    
    
    fun cancel(m: CancelNotificationRequest) {
        val ret = CancelNotificationResponse.newBuilder()
        try {
            if (! notifications.remove(m.id)) {
                ret.success = false
            } else {
                NotificationManagerCompat.from(app).cancel(Thread.currentThread().id.toString(), m.id)
                ret.success = true
            }
        } catch (e: java.lang.Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }
    
    
    
    
    
}