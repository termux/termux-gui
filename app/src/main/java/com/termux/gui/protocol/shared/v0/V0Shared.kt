package com.termux.gui.protocol.shared.v0

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.HardwareBuffer
import android.net.Uri
import android.os.Build
import android.os.SharedMemory
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import com.google.gson.JsonObject
import com.termux.gui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.collections.HashSet
import kotlin.reflect.KClass

/**
 * Code that can be shared for all Protocol implementations.
 */
abstract class V0Shared(protected val app: Context) : GUIActivity.Listener {
    
    
    private var activityID = 0
    protected val rand = Random()
    
    /*
    State of the connection, list of active objects.
     */
    protected val tasks: MutableList<ActivityManager.AppTask> = Collections.synchronizedList(LinkedList())
    protected val hardwareBuffers: MutableMap<Int, HardwareBuffer> = Collections.synchronizedMap(HashMap())
    protected val activities: MutableMap<Int, DataClasses.ActivityState> = Collections.synchronizedMap(HashMap())
    protected val buffers: MutableMap<Int, DataClasses.SharedBuffer> = HashMap()
    protected val remoteviews: MutableMap<Int, DataClasses.RemoteLayoutRepresentation> = HashMap()
    protected val overlays: MutableMap<Int, DataClasses.Overlay> = Collections.synchronizedMap(HashMap())
    protected val notifications: MutableSet<Int> = Collections.synchronizedSet(HashSet())

    /**
     * Runs a closure with dynamic Intent listeners and cleanup of all Intent listeners and State if the closure returns.
     */
    protected fun withSystemListenersAndCleanup(am: ActivityManager, wm: WindowManager, clos: () -> Unit) {
        val lifecycleCallbacks = LifecycleListener(this, activities, tasks, am, Util.connectionID())
        App.APP?.registerActivityLifecycleCallbacks(lifecycleCallbacks)
        val sysrec = SystemBroadcastReceiver(this)
        val filter = IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_LOCALE_CHANGED)
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        App.APP?.registerReceiver(sysrec, filter)
        PendingIntentReceiver.threadCallbacks[Thread.currentThread().id] = fun(m: JsonObject) {
            val rid = m[PendingIntentReceiver.RID]?.asInt
            val id = m[PendingIntentReceiver.ID]?.asInt
            val action = m[PendingIntentReceiver.ACTION]?.asInt
            val nid = m[PendingIntentReceiver.NID]?.asInt
            val dismissed = m[PendingIntentReceiver.DISMISSED]?.asBoolean
            if (rid != null && id != null && rid != -1 && id != -1) {
                onRemoteButton(rid, id)
                return
            }
            if (nid != null) {
                if (action == null) {
                    if (dismissed == true)
                        onNotificationDismissed(nid)
                    else
                        onNotification(nid)
                } else {
                    onNotificationAction(nid, action)
                }
            }
        }
        try {
            clos()
        } finally {
            Logger.log(1, TAG, "cleanup V0")
            PendingIntentReceiver.threadCallbacks.remove(Thread.currentThread().id)
            for (o in overlays.values) {
                wm.removeView(o.root)
            }
            App.APP?.unregisterReceiver(sysrec)
            App.APP?.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
            for (t in tasks) {
                try {
                    t.finishAndRemoveTask()
                } catch (ignored: Exception) {}
            }
            for (a in activities.values) {
                try {
                    a.a?.finishAndRemoveTask()
                } catch (ignored: Exception) {}
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                for (b in buffers.values) {
                    SharedMemory.unmap(b.buff)
                    b.shm?.close()
                }
            } else {
                for (b in buffers.values) {
                    val fd = b.fd
                    if (fd != null) {
                        ConnectionHandler.unmap_ashmem(b.buff)
                        ConnectionHandler.destroy_ashmem(fd)
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                for (b in hardwareBuffers.values) {
                    b.close()
                }
            } 
            val not = NotificationManagerCompat.from(app)
            for (id in notifications) {
                not.cancel(Thread.currentThread().id.toString(), id)
            }
            GUIWebViewJavascriptDialog.requestMap.remove(Thread.currentThread().id.toString())
        }
    }
    
    /*
    Listeners for events the protocol implementations have to provide.
     */
    
    abstract fun onActivityCreated(state: DataClasses.ActivityState)
    abstract fun onActivityStarted(state: DataClasses.ActivityState)
    abstract fun onActivityResumed(state: DataClasses.ActivityState)
    abstract fun onActivityPaused(state: DataClasses.ActivityState)
    abstract fun onActivityStopped(state: DataClasses.ActivityState)
    abstract fun onActivityDestroyed(state: DataClasses.ActivityState)
    
    abstract fun onAirplaneModeChanged(c: Context, i: Intent)
    abstract fun onLocaleChanged(c: Context, i: Intent)
    abstract fun onScreenOff(c: Context, i: Intent)
    abstract fun onScreenOn(c: Context, i: Intent)
    abstract fun onTimezoneChanged(c: Context, i: Intent)
    
    abstract fun onRemoteButton(rid: Int, id: Int)
    abstract fun onNotification(nid: Int)
    abstract fun onNotificationDismissed(nid: Int)
    abstract fun onNotificationAction(nid: Int, action: Int)

    /**
     * Generate a new Activity id in the connection.
     * Since each connection has its own id sequence, predictable ids aren't an issue.
     */
    protected fun generateActivityID(): Int {
        // to make the aid unique even on integer overflow
        while (activities.containsKey(activityID)) {
            activityID++
            if (activityID < 0) activityID = 0
        }
        val aid = activityID
        activityID++
        if (activityID < 0) activityID = 0
        return aid
    }

    /**
     * Creates a new Activity with the specified configuration.
     */
    @Suppress("DEPRECATION")
    fun newActivity(ptid: Int?, pip: Boolean, dialog: Boolean, lockscreen: Boolean, canceloutside: Boolean, interceptBackButton: Boolean): GUIActivity? {
        //println("ptid: $ptid")
        val i = Intent(app, GUIActivity::class.java)
        if (ptid == null) {
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        }
        val aid = generateActivityID()
        i.data = Uri.parse(Util.activityIDData(aid))

        activities[aid] = DataClasses.ActivityState(Util.connectionID())
        i.putExtra(GUIActivity.PIP_KEY, pip)
        i.putExtra(GUIActivity.INTERCEPT_KEY, interceptBackButton)
        when {
            pip -> {
                i.flags = i.flags or Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            dialog -> { // pip overrides dialog
                i.setClass(app, GUIActivityDialog::class.java)
                i.putExtra(GUIActivityDialog.CANCELOUTSIDE_KEY, canceloutside)
            }
            lockscreen -> { // dialog overrides lockscreen
                i.setClass(app, GUIActivityLockscreen::class.java)
            }
        }

        var task: ActivityManager.AppTask? = null
        if (ptid == null) {
            app.startActivity(i)
        } else {
            for (t in tasks) {
                if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        t.taskInfo?.taskId == ptid
                    } else {
                        t.taskInfo?.id == ptid
                    }) {
                    task = t
                    break
                }
            }
            if (task == null) {
                return null
            }
            task.startActivity(app, i, null)
        }
        while (true) {
            if (activities[aid]?.a != null) {
                break
            }
            Thread.sleep(1)
        }
        return activities[aid]?.a
    }
    
    
    companion object {
        private val TAG: String? = V0Shared::class.java.canonicalName
        fun setViewOverlay(o: DataClasses.Overlay, v: View, parent: Int?) {
            val t = o.theme
            if (v is TextView) {
                if (t != null) {
                    v.setTextColor(t.textColor)
                } else {
                    v.setTextColor((0xffffffff).toInt())
                }
            }
            if (parent == null) {
                if (t != null) {
                    v.setBackgroundColor(t.windowBackground)
                } else {
                    v.setBackgroundColor((0xff000000).toInt())
                }
                val fl = o.root
                println("removing views")
                fl.removeAllViews()
                println("adding view")
                fl.addView(v)
            } else {
                val g = o.root.findViewReimplemented<ViewGroup>(parent)
                if (g is LinearLayout) {
                    val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1F)
                    if (g.orientation == LinearLayout.VERTICAL) {
                        p.width = LinearLayout.LayoutParams.MATCH_PARENT
                    } else {
                        p.height = LinearLayout.LayoutParams.MATCH_PARENT
                    }
                    v.layoutParams = p
                }
                g?.addView(v)
            }
        }

        /**
         * Run a closure when the Activity is started, queueing it if it is stopped.
         */
        fun runOnUIThreadActivityStarted(a: DataClasses.ActivityState, r: (activity: GUIActivity) -> Unit) {
            var e: Exception? = null
            var stopped = false
            runBlocking(Dispatchers.Main) {
                launch {
                    val ac = a.a
                    if (a.saved || ac == null) {
                        stopped = true
                        return@launch
                    }
                    try {
                        r(ac)
                    } catch (ex: java.lang.Exception) {
                        e = ex
                    }
                }
            }
            val ex = e
            if (ex != null) {
                throw ex
            }
            if (stopped) {
                println("queueing Runnable")
                a.queued.offer(r)
            }
        }

        /**
         * Returns true if the activity is stopped or not found and the runnable wasn't run.
         */
        fun runOnUIThreadActivityStartedBlocking(a: DataClasses.ActivityState?, r: (activity: GUIActivity) -> Unit) : Boolean {
            if (a == null) return true
            var e: Exception? = null
            var stopped = false
            runBlocking(Dispatchers.Main) {
                launch {
                    val ac = a.a
                    if (a.saved || ac == null) {
                        stopped = true
                        return@launch
                    }
                    try {
                        r(ac)
                    } catch (ex: java.lang.Exception) {
                        e = ex
                    }
                }
            }
            val ex = e
            if (ex != null) {
                throw ex
            }
            return stopped
        }
        
        // this can only be used in API 31, where you can set the ID of the root element of the Layout in a RemoteViews
        // this could be used to make unlimited amounts of views in remote layouts, but isn't compatible with earlier API versions
        /*
        fun generateWidgetViewID(rand: Random, w: DataClasses.RemoteLayoutRepresentation): Int {
            return Util.generateViewIDRaw(rand, w.usedIds)
        }
         */
        
        /*
        Get layouts through reflection, because a good RemoteViews API was only introduced in API 31.
         */

        fun getReflectedLayout(name: String): Pair<Int?,Int?> {
            val layout = R.layout::class.java
            val id = R.id::class.java
            return try {
                Pair(layout.getDeclaredField(name).getInt(null), id.getDeclaredField(name).getInt(null))
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(null, null)
            }
        }
        
        
        fun getRemoteLayout(c: KClass<*>, index: Int, postfix: String): Pair<Int?,Int?> {
            return getReflectedLayout("remote_" + c.simpleName?.lowercase() + postfix + index)
        }
        
        fun addRemoteView(c: KClass<*>, rv: DataClasses.RemoteLayoutRepresentation?, parent: Int?, app: Context, postfix: String = ""): Int {
            if (rv == null) {
                return -1
            }
            val p = parent ?: R.id.root
            val count = rv.viewCount[c] ?: 0
            val ids = getRemoteLayout(c, count+1, postfix)
            if (ids.first != null && ids.second != null) {
                rv.root?.addView(p, RemoteViews(app.packageName, ids.first!!))
                rv.viewCount[c] = count + 1
            }
            return ids.second ?: -1
        }
        
        fun generateBufferID(rand: Random, buffers: MutableMap<Int, DataClasses.SharedBuffer>): Int {
            var id = rand.nextInt(Integer.MAX_VALUE)
            while (buffers.contains(id)) {
                id = rand.nextInt(Integer.MAX_VALUE)
            }
            return id
        }
    }
}