package com.termux.gui

import android.app.ActivityManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.LocalServerSocket
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.*
import kotlin.collections.HashSet

class GUIService : Service() {
    class ConnectionRequest(val mainSocket: String?, val eventSocket: String?)
    
    companion object {
        /*
        const val PASSIVE_SOCKET = "com.termux.gui://passiveconnect"
        private val pas = LocalServerSocket(PASSIVE_SOCKET)
        private val passiveRequestHandler: Thread
        init {
            passiveRequestHandler = Thread(fun() {
                try {
                    @Suppress("DEPRECATION")
                    while (!Thread.currentThread().isInterrupted) {
                        val c = pas.accept()
                        if (c.peerCredentials.uid != App.APP!!.applicationInfo.uid) {
                            c.shutdownInput()
                            c.shutdownOutput()
                            c.close()
                            continue
                        }
                        val r = BufferedReader(InputStreamReader(c.inputStream))
                        val main = r.readLine()
                        val event = r.readLine()
                        requests.add(ConnectionRequest(main, event))
                        val i = Intent(App.APP, GUIReceiver::class.java)
                        i.putExtra("mainSocket", main)
                        i.putExtra("eventSocket", event)
                        App.APP!!.sendBroadcast(i)
                        c.outputStream.write(0)
                        c.outputStream.flush()
                    }
                } catch (_: InterruptedException) {}
                println("PassiveRequestHandler exited")
            })
        }
         */
        const val NOTIFICATION_ID = 100
        val requests = ConcurrentLinkedQueue<ConnectionRequest>()
    }
    
    
    private val settings = Settings()
    
    
    val destroywatch: MutableSet<Runnable> = Collections.synchronizedSet(HashSet<Runnable>())
    private val pool = ThreadPoolExecutor(30, 30, 1, TimeUnit.SECONDS, ArrayBlockingQueue(10))
    private val requestWatcher: Thread
    private val settingsWatcher: Thread
    
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
                    if (t.taskInfo.baseIntent.component == ComponentName(this, GUIActivity::class.java))
                        t.finishAndRemoveTask()
                }
            }
            requestWatcher.start()
        }
        if (! settingsWatcher.isAlive) {
            settingsWatcher.start()
        }
        /*
        if (! passiveRequestHandler.isAlive) {
            passiveRequestHandler.start()
        }
         */
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        for (r in destroywatch) {
            try {
                r.run()
            } catch (ignored: Exception) {}
        }
        requestWatcher.interrupt()
        settingsWatcher.interrupt()
        
        
        
        pool.shutdownNow()
        println("service destroyed")
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
                    //NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
                }
                if (settings.background && count == 0) {
                    stopForeground(true)
                    
                }
                if (r != null) {
                    println("new connection")
                    noRequests = 0
                    try {
                        pool.submit(ConnectionHandler(r, this))
                    } catch (ignored: RejectedExecutionException) {}
                } else {
                    try {
                        Thread.sleep(1)
                        if (settings.timeout >= 0) {
                            noRequests++
                            if (noRequests > settings.timeout * 1000 && count == 0) {
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
        settingsWatcher = Thread(fun() {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    settings.load(this)
                    synchronized(Settings.MODIFIED_WAIT) {
                        while (!Settings.modified)
                            Settings.MODIFIED_WAIT.wait()
                    }
                }
            } catch (_: InterruptedException) {}
        })
    }
}