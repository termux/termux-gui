package com.termux.gui

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.*
import java.io.*
import java.lang.IllegalArgumentException
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.roundToInt

class ConnectionHandler(private val request: GUIService.ConnectionRequest, service: Context) : Runnable {
    class Message {
        var method: String? = null
        var params: HashMap<String, JsonElement>? = null
    }

    class Event(var type: String, var value: JsonElement?)
    companion object {
        private val gson = Gson()
        var INVALID_METHOD: Event = Event("invalidMethod", gson.toJsonTree("invalid method"))
    }
    
    private var activityID = 0
    private val app: Context = service.applicationContext
    private var am = app.getSystemService(ActivityManager::class.java)
    private val rand = Random()
    
    
    @Suppress("DEPRECATION")
    private fun newActivityJSON(tasks: LinkedList<ActivityManager.AppTask>, activities: MutableMap<String, GUIFragment>, ptid: Int?, flags: Int): String {
        //println("ptid: $ptid")
        val i = Intent(app, GUIActivity::class.java)
        if (ptid == null) {
            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val aid = Thread.currentThread().id.toString()+"-"+activityID.toString()
        i.data = Uri.parse(aid)
        activityID++
        
        activities[aid] = GUIFragment()
        
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
                return gson.toJson(arrayOf("-1",-1))
            }
            task.startActivity(app, i, null)
        }
        while (true) {
            if (activities[aid]?.isAdded == true) {
                break
            }
            Thread.sleep(1)
        }
        /*
        // search for the activity
        var found = false
        val t1 = System.currentTimeMillis()
        searchActivity@ while (abs(t1 - System.currentTimeMillis()) < 1000) {
            for (aref in GUIActivity.newActivities) {
                val a = aref.get()
                if (a != null) {
                    val intent = a.startIntent
                    if (intent != null && intent.dataString == aid) {
                        //activities.add(a)
                        GUIActivity.newActivities.remove(aref)
                        a.handler = WeakReference(this)
                        //println("activity found")
                        if (ptid == null) {
                            for (t in am.appTasks) {
                                if (a.taskId == if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            t.taskInfo?.taskId
                                        } else {
                                            t.taskInfo?.id
                                        }) {
                                    //println("task found")
                                    found = true
                                    tasks.add(t)
                                    break
                                }
                            }
                            if (! found) {
                                gson.toJson(arrayOf("-1",-1))
                            }
                        }
                        found = true
                        break@searchActivity
                    }
                }
            }
            Thread.sleep(1)
        }
        if (! found) {
            //println("could not find activity")
            task?.finishAndRemoveTask()
            return if (ptid == null) {
                gson.toJson(arrayOf("-1",-1))
            } else {
                gson.toJson("-1")
            }
        } else {
            return if (ptid == null) {
                gson.toJson(arrayOf(aid, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    task?.taskInfo?.taskId ?: 0
                } else {
                    task?.taskInfo?.id ?: 0
                }))
            } else {
                gson.toJson(aid)
            }
        }
        */
        return if (ptid == null) {
            gson.toJson(arrayOf(aid, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                task?.taskInfo?.taskId ?: 0
            } else {
                task?.taskInfo?.id ?: 0
            }))
        } else {
            gson.toJson(aid)
        }
    }
    
    private fun sendMessage(w: DataOutputStream, ret: String) {
        w.writeInt(ret.length)
        w.write(ret.toByteArray(UTF_8))
        w.flush()
    }
    
    private fun generateViewID(a: GUIFragment): Int {
        var id = rand.nextInt(Integer.MAX_VALUE)
        synchronized(a.usedIds) {
            while (a.usedIds.contains(id)) {
                id = rand.nextInt(Integer.MAX_VALUE)
            }
            a.usedIds.add(id)
        }
        return id
    }
    
    private fun runOnUIThreadBlocking(r: Runnable) {
        var e: Exception? = null
        runBlocking(Dispatchers.Main) {
            launch {
                try {
                    r.run()
                } catch (ex: java.lang.Exception) {
                    e = ex
                }
            }
        }
        val ex = e
        if (ex != null) {
            throw ex
        }
    }
    
    private fun setView(a: GUIFragment, v: View, parent: Int?) {
        val t = a.theme
        if (t != null) {
            if (v is TextView) {
                v.setTextColor(t.textColor)
            }
        }
        runOnUIThreadBlocking {
            if (parent == null) {
                if (t != null) {
                    v.setBackgroundColor(t.windowBackground)
                }
                val fl = a.view as FrameLayout
                fl.removeAllViews()
                fl.addView(v)
            } else {
                val g = a.view?.findViewById<ViewGroup>(parent)
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
    }

    @Suppress("DEPRECATION")
    override fun run() {
        println("Socket address: " + request.mainSocket)
        
        val main = LocalSocket(LocalSocket.SOCKET_STREAM)
        val event = LocalSocket(LocalSocket.SOCKET_STREAM)
        
        val am = app.getSystemService(ActivityManager::class.java)
        
        
        val tasks = LinkedList<ActivityManager.AppTask>()
        fun getTaskInfo(task: ActivityManager.AppTask): ActivityManager.RecentTaskInfo? {
            try {
                return task.taskInfo
            } catch (e: IllegalArgumentException) {
                tasks.remove(task)
            }
            return null
        }
        fun getTaskId(info: ActivityManager.RecentTaskInfo): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                info.taskId
            } else {
                info.id
            }
        }
        val activities = Collections.synchronizedMap(HashMap<String,GUIFragment>())
        
        
        fun toPX(a: GUIFragment, dip: Int): Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip.toFloat(), a.requireActivity().resources.displayMetrics).roundToInt()
        }
        
        
        
        val lifecycleCallbacks = object: Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, savedInstanceState: Bundle?) {
                println("create")
                if (a is GUIActivity) {
                    val f = activities[a.intent?.dataString]
                    if (f != null) {
                        for (task in am.appTasks) {
                            val info = getTaskInfo(task)
                            if (a.taskId == info?.let { getTaskId(it) }) {
                                if (tasks.find { t -> getTaskInfo(task)?.let { getTaskId(it) } == getTaskId(task.taskInfo)} == null) {
                                    tasks.add(task)
                                    println("task added")
                                }
                                break
                            }
                        }
                    }
                }
            }
            override fun onActivityStarted(a: Activity) {
                println("start")
                if (a is GUIActivity) {
                    val f = activities[a.intent?.dataString]
                    if (f != null) {
                        if (a.supportFragmentManager.fragments.find { f2 -> f2 == f } == null) {
                            a.supportFragmentManager.beginTransaction().add(R.id.root, f).commitNow()
                            println("fragment attached")
                        }
                        // to reset the window background color
                        f.theme = f.theme
                    }
                }
            }
            override fun onActivityResumed(a: Activity) {
                println("resume")
            }
            override fun onActivityPaused(a: Activity) {
                println("pause")
            }
            override fun onActivityStopped(a: Activity) {
                println("stop")
            }
            override fun onActivitySaveInstanceState(a: Activity, outState: Bundle) {
                println("saveInstanceState")
            }
            override fun onActivityDestroyed(a: Activity) {
                if (a is GUIActivity) {
                    val aid = a.intent?.dataString
                    val f = activities[aid]
                    if (a.isFinishing) {
                        println("finishing")
                        if (f != null) {
                            activities.remove(aid)
                        }
                    }
                }
                println("destroy")
            }
        }
        
        
        
        try {
            App.APP?.registerActivityLifecycleCallbacks(lifecycleCallbacks)
            main.use {
                event.use {
                    main.connect(LocalSocketAddress(request.mainSocket))
                    event.connect(LocalSocketAddress(request.eventSocket))
                    var protocol = -1
                    while (protocol == -1) {
                        protocol = main.inputStream.read()
                        Thread.sleep(1)
                    }
                    val pversion = (protocol and 0xf0) shr 4
                    val ptype = protocol and 0x0f
                    if (ptype != 0 && ptype != 1) {
                        main.outputStream.write(1)
                        return
                    }
                    main.outputStream.write(0)
                    main.outputStream.flush()
                    val inp = DataInputStream(main.inputStream)
                    var msgbytes = ByteArray(0)
                    val out = DataOutputStream(main.outputStream)
                    val eventOut = DataOutputStream(event.outputStream)
                    
                    // to use regex in the when clause
                    operator fun Regex.contains(text: String?): Boolean = if (text == null) false else this.matches(text)
                    while (! Thread.currentThread().isInterrupted) {
                        val len = inp.readInt()
                        // resize the buffer if needed
                        if (len > msgbytes.size) {
                            msgbytes = ByteArray(len)
                        }
                        
                        inp.readFully(msgbytes, 0, len)
                        when (ptype) {
                            1 -> {
                                val msg = msgbytes.decodeToString(0, len, true)
                                val m = gson.fromJson(msg, Message::class.java)
                                println(m.method)
                                when (m?.method) {
                                    "newActivity" -> {
                                        for (t in tasks) {
                                            try {
                                                if (t.taskInfo == null) {
                                                    tasks.remove(t)
                                                }
                                            } catch (e: IllegalArgumentException) {
                                                tasks.remove(t)
                                            }
                                        }
                                        sendMessage(out, newActivityJSON(tasks, activities, m.params?.get("tid")?.asInt, m.params?.get("flags")?.asInt ?: 0))
                                        continue
                                    }
                                    in Regex("create.*") -> {
                                        if (m.params != null) {
                                            val aid = m.params?.get("aid")?.asString
                                            val parent = m.params?.get("parent")?.asInt
                                            val a = activities[aid]
                                            if (a != null) {
                                                if (m.method == "createTextView") {
                                                    val v = TextView(a.requireActivity())
                                                    v.id = generateViewID(a)
                                                    v.text = m.params?.get("text")?.asString
                                                    setView(a, v, parent)
                                                    sendMessage(out, gson.toJson(v.id))
                                                    continue
                                                }
                                                if (m.method == "createEditText") {
                                                    val v = EditText(a.requireActivity())
                                                    v.id = generateViewID(a)
                                                    v.setText(m.params?.get("text")?.asString, TextView.BufferType.EDITABLE)
                                                    setView(a, v, parent)
                                                    sendMessage(out, gson.toJson(v.id))
                                                    continue
                                                }
                                                if (m.method == "createLinearLayout") {
                                                    val v = LinearLayout(a.requireActivity())
                                                    v.id = generateViewID(a)
                                                    v.orientation = if (m.params?.get("vertical")?.asBoolean != false) {
                                                        LinearLayout.VERTICAL
                                                    } else {
                                                        LinearLayout.HORIZONTAL
                                                    }
                                                    setView(a, v, parent)
                                                    sendMessage(out, gson.toJson(v.id))
                                                    continue
                                                }
                                            }
                                        }
                                    }
                                    "setMargin" -> {
                                        if (m.params != null) {
                                            val aid = m.params?.get("aid")?.asString
                                            val id = m.params?.get("id")?.asInt
                                            val margin = m.params?.get("margin")?.asInt
                                            val a = activities[aid]
                                            if (id != null && a != null && margin != null) {
                                                runOnUIThreadBlocking {
                                                    val mar = toPX(a, margin)
                                                    val v = a.view?.findViewById<View>(id)
                                                    val p = v?.layoutParams as? ViewGroup.MarginLayoutParams
                                                    when (m.params?.get("dir")?.asString) {
                                                        "top" -> p?.topMargin = mar
                                                        "bottom" -> p?.bottomMargin = mar
                                                        "left" -> p?.marginStart = mar
                                                        "right" -> p?.marginEnd = mar
                                                        else -> p?.setMargins(mar, mar, mar, mar)
                                                    }
                                                    v?.layoutParams = p
                                                }
                                            }
                                        }
                                        continue
                                    }
                                    "setLayoutWeight" -> {
                                        val aid = m.params?.get("aid")?.asString
                                        val a = activities[aid]
                                        val id = m.params?.get("id")?.asInt
                                        val weight = m.params?.get("weight")?.asInt
                                        if (a != null && id != null && weight != null) {
                                            runOnUIThreadBlocking {
                                                val v = a.view?.findViewById<View>(id)
                                                val p = v?.layoutParams as? LinearLayout.LayoutParams
                                                if (p != null) {
                                                    p.weight = weight.toFloat()
                                                    v.layoutParams = p
                                                }
                                            }
                                        }
                                    }
                                    "setTheme" -> {
                                        val aid = m.params?.get("aid")?.asString
                                        val a = activities[aid]
                                        val s = m.params?.get("statusBarColor")?.asInt
                                        val t = m.params?.get("textColor")?.asInt
                                        val b = m.params?.get("windowBackground")?.asInt
                                        val p = m.params?.get("colorPrimary")?.asInt
                                        val ac = m.params?.get("colorAccent")?.asInt
                                        if (a != null && s != null && t != null && b != null && p != null && ac != null) {
                                            runOnUIThreadBlocking {
                                                a.theme = GUIActivity.GUITheme(s, p, b, t, ac)
                                            }
                                        }
                                        continue
                                    }
                                    "setTaskIcon" -> {
                                        val aid = m.params?.get("aid")?.asString
                                        val a = activities[aid]
                                        val ac = a?.requireActivity() as? GUIActivity
                                        val img = m.params?.get("img")?.asString
                                        if (ac != null && img != null) {
                                            runOnUIThreadBlocking {
                                                val t = a.theme
                                                val prim = t?.colorPrimary ?: (0xFF000000).toInt()
                                                val bin = Base64.decode(img, Base64.DEFAULT)
                                                val bitmap = BitmapFactory.decodeByteArray(bin, 0, bin.size)
                                                ac.setTaskDescription(ActivityManager.TaskDescription("Termux:GUI Test app", bitmap, prim))
                                            }
                                        }
                                        continue
                                    }
                                    "setText" -> {
                                        if (m.params != null) {
                                            val aid = m.params?.get("aid")?.asString
                                            val id = m.params?.get("id")?.asInt
                                            val text = m.params?.get("text")?.asString
                                            val a = activities[aid]
                                            if (a != null && id != null) {
                                                runOnUIThreadBlocking {
                                                    val tv = a.view?.findViewById<TextView>(id)
                                                    tv?.text = text
                                                }
                                            }
                                        }
                                        continue
                                    }
                                    "getText" -> {
                                        if (m.params != null) {
                                            val aid = m.params?.get("aid")?.asString
                                            val id = m.params?.get("id")?.asInt
                                            var text: String? = null
                                            val a = activities[aid]
                                            if (a != null && id != null) {
                                                runOnUIThreadBlocking {
                                                    text = a.view?.findViewById<TextView>(id)?.text?.toString()
                                                }
                                            }
                                            sendMessage(out, gson.toJson(text ?: ""))
                                        }
                                        continue
                                    }
                                }
                                
                            }
                            0 -> {
                                
                            }
                        }
                    }
                }
            }
        } catch (e: EOFException) {
            println("connection closed by program")
        } catch (e: JsonSyntaxException) {
            println("program send invalid json")
        }  catch (e: Exception) {
            e.printStackTrace()
        } finally {
            println("cleanup")
            App.APP?.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
            for (t in tasks) {
                try {
                    t.finishAndRemoveTask()
                } catch (ignored: Exception) {}
            }
        }
    }

}