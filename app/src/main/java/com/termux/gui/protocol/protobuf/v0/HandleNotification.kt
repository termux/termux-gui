package com.termux.gui.protocol.protobuf.v0

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.termux.gui.ConnectionHandler
import com.termux.gui.PendingIntentReceiver
import com.termux.gui.R
import com.termux.gui.Util
import com.termux.gui.protocol.protobuf.ProtoUtils
import com.termux.gui.protocol.protobuf.v0.GUIProt0.*
import com.termux.gui.protocol.shared.v0.DataClasses
import java.io.OutputStream
import java.util.*

class HandleNotification(val main: OutputStream, val remoteviews: MutableMap<Int, DataClasses.RemoteLayoutRepresentation>,
                         val rand: Random, val app: Context, val notifications: MutableSet<Int>) {
    
    
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
        }
        ProtoUtils.write(ret, main)
    }
    
    
    @SuppressLint("LaunchActivityFromNotification")
    fun create(m: CreateNotificationRequest) {
        val ret = CreateNotificationResponse.newBuilder()
        try {
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
                CreateNotificationRequest.TypeCase.CUSTOM -> TODO()
                CreateNotificationRequest.TypeCase.TYPE_NOT_SET -> {
                    ret.id = -1
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
        }
        ProtoUtils.write(ret, main)
    }
    
    
    
    
    
}