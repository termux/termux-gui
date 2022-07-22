package com.termux.gui.protocol.shared.v0

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.SharedMemory
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import com.google.gson.JsonObject
import com.termux.gui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.reflect.KClass


abstract class V0Shared(protected val app: Context) : GUIActivity.Listener {
    
    
    private var activityID = 0
    protected val rand = Random()

    protected val tasks = LinkedList<ActivityManager.AppTask>()
    protected val activities: MutableMap<String, DataClasses.ActivityState> = Collections.synchronizedMap(HashMap<String, DataClasses.ActivityState>())
    protected val buffers: MutableMap<Int, DataClasses.SharedBuffer> = HashMap()
    protected val remoteviews: MutableMap<Int, DataClasses.RemoteLayoutRepresentation> = HashMap()
    protected val overlays: MutableMap<String, DataClasses.Overlay> = Collections.synchronizedMap(HashMap<String, DataClasses.Overlay>())
    
    protected fun withSystemListenersAndCleanup(am: ActivityManager, wm: WindowManager, clos: () -> Unit) {
        val lifecycleCallbacks = LifecycleListener(this, activities, tasks, am)
        App.APP?.registerActivityLifecycleCallbacks(lifecycleCallbacks)
        val sysrec = SystemBroadcastReceiver(this)
        val filter = IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_LOCALE_CHANGED)
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        App.APP?.registerReceiver(sysrec, filter)
        WidgetButtonReceiver.threadCallbacks[Thread.currentThread().id] = fun(m: JsonObject) {
            val rid = m[WidgetButtonReceiver.RID].asInt
            val id = m[WidgetButtonReceiver.ID].asInt
            if (rid != -1 && id != -1) {
                onWidgetButton(rid, id)
            }
        }
        try {
            clos()
        } finally {
            Logger.log(1, TAG, "cleanup V0")
            WidgetButtonReceiver.threadCallbacks.remove(Thread.currentThread().id)
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
        }
    }
    
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
    
    abstract fun onWidgetButton(rid: Int, id: Int)
    
    protected fun generateActivityID(): String {
        val aid = Thread.currentThread().id.toString()+"-"+activityID.toString()
        activityID++
        return aid
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

        fun runOnUIThreadActivityStartedBlocking(a: DataClasses.ActivityState, r: (activity: GUIActivity) -> Unit) : Boolean {
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
            return ! stopped
        }
        
        // this can only be used in API 31, where you can set the ID of the root element of the Layout in a RemoteViews
        // this could be used to make unlimited amounts of views in remote layouts, but isn't compatible with earlier API versions
        /*
        fun generateWidgetViewID(rand: Random, w: DataClasses.RemoteLayoutRepresentation): Int {
            return Util.generateViewIDRaw(rand, w.usedIds)
        }
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