package com.termux.gui

import android.app.ActivityManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class GUIService : Service() {
    class ConnectionRequest(val mainSocket: String?, val eventSocket: String?)

    private val pool = ThreadPoolExecutor(0, 30, 1, TimeUnit.SECONDS, ArrayBlockingQueue(100))
    private val requestWatcher: Thread
    private val notification: Notification
        get() {
            NotificationManagerCompat.from(this).createNotificationChannel(
                    NotificationChannelCompat.Builder("service", NotificationManagerCompat.IMPORTANCE_LOW)
                            .setName("Service").setLightsEnabled(false).setVibrationEnabled(false).build())
            val b = NotificationCompat.Builder(this, "service")
            b.setSmallIcon(R.drawable.ic_service_notification)
            b.setContentTitle("Termux:GUI")
            b.setOnlyAlertOnce(true)
            b.setOngoing(true)
            b.setShowWhen(false)
            b.setAllowSystemGeneratedContextualActions(false)
            b.setSilent(true)
            b.priority = NotificationCompat.PRIORITY_LOW
            b.addAction(NotificationCompat.Action.Builder(0, "Close all",
                    PendingIntent.getBroadcast(this, 0, Intent(this, ServiceShutdownReceiver::class.java), PendingIntent.FLAG_IMMUTABLE)).build())
            b.setContentText("Connections: " + pool.activeCount)
            return b.build()
        }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification)
        if (! requestWatcher.isAlive) {
            requestWatcher.start()
            // clean up any old task stacks
            getSystemService(ActivityManager::class.java).let {
                for (t in it.appTasks) {
                    t.finishAndRemoveTask()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        requestWatcher.interrupt()
        pool.shutdownNow()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        const val NOTIFICATION_ID = 100
        val requests = ConcurrentLinkedQueue<ConnectionRequest>()
    }

    init {
        requestWatcher = Thread (fun() {
            var noRequests = 0
            var lastConnections = 0
            while (!Thread.currentThread().isInterrupted) {
                val r = requests.poll()
                val count = pool.activeCount
                if (count != 0) {
                    noRequests = 0
                }
                if (lastConnections != pool.activeCount) {
                    lastConnections = count
                    NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
                }
                if (r != null) {
                    println("new connection")
                    noRequests = 0
                    pool.submit(ConnectionHandler(r, this))
                } else {
                    try {
                        Thread.sleep(1)
                        noRequests++
                        if (noRequests > 3000) {
                            stopSelf()
                            return
                        }
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }
            }
        })
    }
}