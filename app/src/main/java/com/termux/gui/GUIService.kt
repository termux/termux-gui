package com.termux.gui

import android.app.ActivityManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.*
import java.util.concurrent.*

/**
 * The foreground service that prevents the process from being killed when no Activity is shown.
 * It receives the connection requests gathered by the GUIReceiver and hands them to executor Threads and ConnectionHandler
 */
class GUIService : Service() {
    class ConnectionRequest(val mainSocket: String?, val eventSocket: String?)
    
    companion object {
        private val TAG: String? = GUIService::class.java.canonicalName
        const val NOTIFICATION_ID = 100
        val requests = ConcurrentLinkedQueue<ConnectionRequest>()
    }
    
    
    
    
    val destroywatch: MutableSet<Runnable> = Collections.synchronizedSet(HashSet())
    private val pool = ThreadPoolExecutor(30, 30, 1, TimeUnit.SECONDS, ArrayBlockingQueue(10))
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
            // clean up any old task stacks
            getSystemService(ActivityManager::class.java).let {
                for (t in it.appTasks) {
                    if (t.taskInfo.baseIntent.component == ComponentName(this, GUIActivity::class.java) ||
                            t.taskInfo.baseIntent.component == ComponentName(this, GUIActivityDialog::class.java) ||
                            t.taskInfo.baseIntent.component == ComponentName(this, GUIActivityLockscreen::class.java))
                        t.finishAndRemoveTask()
                }
            }
            requestWatcher.start()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // This is needed because the Android LocalSocket implementation doesn't close itself when the Thread is interrupted.
        // The Runnables close the connections, making the handler Threads throw an exception to terminate them.
        for (r in destroywatch) {
            try {
                r.run()
            } catch (ignored: Exception) {}
        }
        requestWatcher.interrupt()
        
        pool.shutdownNow()
        Logger.log(1, TAG, "service destroyed")
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
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
                    startForeground(NOTIFICATION_ID, notification)
                }
                if (Settings.instance.background && count == 0) {
                    stopForeground(true)
                }
                if (r != null) {
                    Logger.log(1, TAG, "new connection")
                    noRequests = 0
                    try {
                        pool.submit(ConnectionHandler(r, this))
                    } catch (ignored: RejectedExecutionException) {}
                } else {
                    try {
                        Thread.sleep(1)
                        if (Settings.instance.timeout >= 0) {
                            noRequests++
                            if (noRequests > Settings.instance.timeout * 1000 && count == 0) {
                                stopSelf()
                                return
                            }
                        } else {
                            noRequests = 0
                        }
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }
            }
        })
    }
}