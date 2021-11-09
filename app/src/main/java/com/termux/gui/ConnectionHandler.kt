package com.termux.gui

import android.app.*
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SharedMemory
import android.util.Base64
import android.util.Rational
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.core.widget.NestedScrollView
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.FileDescriptor
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.collections.HashMap
import kotlin.math.roundToInt

class ConnectionHandler(private val request: GUIService.ConnectionRequest, service: Context) : Runnable {
    class Message {
        var method: String? = null
        var params: HashMap<String, JsonElement>? = null
    }
    
    @Suppress("unused")
    class Event(var type: String, var value: JsonElement?)
    companion object {
        val gson = Gson()
        val INVALID_METHOD: Event = Event("invalidMethod", gson.toJsonTree("invalid method"))


        fun sendMessage(w: DataOutputStream, ret: String) {
            val bytes = ret.toByteArray(UTF_8)
            w.writeInt(bytes.size)
            w.write(bytes)
            w.flush()
        }
    }
    
    private var activityID = 0
    private val app: Context = service.applicationContext
    private val rand = Random()
    var eventQueue = LinkedBlockingQueue<Event>(10000)
    private var eventWorker: Thread? = null
    
    data class SharedBuffer(val btm: Bitmap, val shm: SharedMemory, val buff: ByteBuffer)
    data class WidgetRepresentation(val usedIds: TreeSet<Int> = TreeSet(), var root: RemoteViews?, var theme: GUIActivity.GUITheme?)
    data class ActivityState(var a: GUIActivity?, @Volatile var saved: Boolean = false, val queued: LinkedBlockingQueue<(activity: GUIActivity) -> Unit> = LinkedBlockingQueue<(activity: GUIActivity) -> Unit>(10000))
    
    @Suppress("DEPRECATION")
    private fun newActivityJSON(tasks: LinkedList<ActivityManager.AppTask>, activities: MutableMap<String, ActivityState>, ptid: Int?, pip: Boolean, dialog: Boolean, lockscreen: Boolean, overlay: Boolean): String {
        //println("ptid: $ptid")
        val i = Intent(app, GUIActivity::class.java)
        if (ptid == null) {
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        }
        val aid = Thread.currentThread().id.toString()+"-"+activityID.toString()
        i.data = Uri.parse(aid)
        activityID++
        
        activities[aid] = ActivityState(null)
        i.putExtra("pip", pip)
        when {
            pip -> {
                i.flags = i.flags or Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            dialog -> { // pip overrides dialog
                i.setClass(app, GUIActivityDialog::class.java)
            }
            lockscreen -> { // dialog overrides lockscreen
                i.setClass(app, GUIActivityLockscreen::class.java)
            }
            overlay -> {

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
                return gson.toJson(arrayOf("-1",-1))
            }
            task.startActivity(app, i, null)
        }
        while (true) {
            if (activities[aid]?.a != null) {
                break
            }
            Thread.sleep(1)
        }
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
    
    

    private fun sendMessageFd(w: DataOutputStream, ret: String, s: LocalSocket, fd: FileDescriptor) {
        val bytes = ret.toByteArray(UTF_8)
        w.writeInt(bytes.size)
        s.setFileDescriptorsForSend(arrayOf(fd))
        w.write(bytes)
        w.flush()
    }
    
    private fun generateViewID(a: GUIActivity): Int {
        var id = rand.nextInt(Integer.MAX_VALUE)
        while (a.usedIds.contains(id)) {
            id = rand.nextInt(Integer.MAX_VALUE)
        }
        a.usedIds.add(id)
        return id
    }

    private fun generateWidgetViewID(w: WidgetRepresentation): Int {
        var id = rand.nextInt(Integer.MAX_VALUE)
        synchronized(w.usedIds) {
            while (w.usedIds.contains(id)) {
                id = rand.nextInt(Integer.MAX_VALUE)
            }
            w.usedIds.add(id)
        }
        return id
    }

    private fun generateBufferID(buffers: MutableMap<Int, SharedBuffer>): Int {
        var id = rand.nextInt(Integer.MAX_VALUE)
        while (buffers.contains(id)) {
            id = rand.nextInt(Integer.MAX_VALUE)
        }
        return id
    }
    
    

    private fun runOnUIThreadActivityStarted(a: ActivityState, r: (activity: GUIActivity) -> Unit) {
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

    private fun runOnUIThreadActivityStartedBlocking(a: ActivityState, r: (activity: GUIActivity) -> Unit) : Boolean {
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
    
    
    
    fun setView(a: GUIActivity, v: View, parent: Int?) {
        val t = a.theme
        if (t != null) {
            if (v is TextView) {
                v.setTextColor(t.textColor)
            }
        }
        if (parent == null) {
            if (t != null) {
                v.setBackgroundColor(t.windowBackground)
            }
            val fl = a.findViewById(R.id.root) as FrameLayout
            fl.removeAllViews()
            fl.addView(v)
        } else {
            val g = a.findViewById<ViewGroup>(parent)
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


    private fun setViewWidget(w: WidgetRepresentation, v: RemoteViews, parent: Int?, id: Int) {
        if (parent == null) {
            if (w.theme != null) {
                v.setInt(id, "setBackgroundColor", w.theme!!.windowBackground)
            } else {
                v.setInt(id, "setBackgroundColor", R.color.widget_background)
            }
            w.root = v
        } else {
            w.root?.addView(parent, v)
        }
    }
    
    

    private fun removeViewRecursive(v: View?) {
        if (v is ViewGroup) {
            removeViewRecursive(v)
        } else {
            if (v != null) {
                val p = v.parent
                if (p is ViewGroup) {
                    p.removeView(v)
                }
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
        val activities = Collections.synchronizedMap(HashMap<String,ActivityState>())
        val buffers: MutableMap<Int, SharedBuffer> = HashMap()
        val widgets: MutableMap<Int,WidgetRepresentation> = HashMap()
        
        
        
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
        fun toPX(a: GUIActivity, dip: Int): Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip.toFloat(), a.resources.displayMetrics).roundToInt()
        }
        
        
        
        var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
        
        
        
        
        try {
            main.use {
                event.use {
                    main.connect(LocalSocketAddress(request.mainSocket))
                    event.connect(LocalSocketAddress(request.eventSocket))
                    // check if it is a termux program that wants to connect to the plugin
                    if (main.peerCredentials.uid != app.applicationInfo.uid || event.peerCredentials.uid != app.applicationInfo.uid) {
                        return
                    }
                    
                    var protocol = -1
                    while (protocol == -1) {
                        protocol = main.inputStream.read()
                        Thread.sleep(1)
                    }
                    val pversion = (protocol and 0xf0) shr 4
                    val ptype = protocol and 0x0f
                    if (pversion != 0 && ptype != 1) {
                        main.outputStream.write(1)
                        return
                    }
                    main.outputStream.write(0)
                    main.outputStream.flush()
                    val inp = DataInputStream(main.inputStream)
                    var msgbytes = ByteArray(0)
                    val out = DataOutputStream(main.outputStream)
                    

                    eventWorker = Thread {
                        val eventOut = DataOutputStream(event.outputStream)
                        while (! Thread.currentThread().isInterrupted) {
                            try {
                                sendMessage(eventOut, gson.toJson(eventQueue.take()))
                            } catch (ignored: Exception) {}
                        }
                    }
                    eventWorker!!.start()
                    lifecycleCallbacks = object: Application.ActivityLifecycleCallbacks {
                        override fun onActivityCreated(a: Activity, savedInstanceState: Bundle?) {
                            println("create")
                            if (a is GUIActivity) {
                                val f = activities[a.intent?.dataString]
                                if (f != null) {
                                    if (tasks.find { getTaskInfo(it)?.let { it1 -> getTaskId(it1) } == a.taskId } == null) {
                                        for (t in am.appTasks) {
                                            if (getTaskInfo(t)?.let { it1 -> getTaskId(it1) } == a.taskId) {
                                                println("task added")
                                                tasks.add(t)
                                                break
                                            }
                                        }
                                    }
                                    f.a = a
                                }
                            }
                        }
                        override fun onActivityStarted(a: Activity) {
                            println("start")
                            if (a is GUIActivity) {
                                val f = activities[a.intent?.dataString]
                                if (f != null) {
                                    f.saved = false
                                    for (r in f.queued) {
                                        println("running queued")
                                        try {
                                            r(a)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
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
                            try {
                                if (a is GUIActivity) {
                                    val f = activities[a.intent?.dataString]
                                    if (f != null) {
                                        val map = HashMap<String, Any?>()
                                        map["finishing"] = a.isFinishing
                                        map["aid"] = a.intent?.dataString
                                        eventQueue.add(Event("stop", gson.toJsonTree(map)))
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            
                        }
                        override fun onActivitySaveInstanceState(a: Activity, outState: Bundle) {
                            println("saveInstanceState")
                            try {
                                if (a is GUIActivity) {
                                    val aid = a.intent?.dataString
                                    val f = activities[aid]
                                    if (f != null) {
                                        f.saved = true
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        override fun onActivityDestroyed(a: Activity) {
                            try {
                                if (a is GUIActivity) {
                                    val aid = a.intent?.dataString
                                    val f = activities[aid]
                                    if (a.isFinishing) {
                                        println("finishing")
                                        if (f != null) {
                                            activities.remove(aid)
                                            val map = HashMap<String, Any?>()
                                            map["finishing"] = a.isFinishing
                                            map["aid"] = a.intent?.dataString
                                            eventQueue.add(Event("destroy", gson.toJsonTree(map)))
                                        }
                                    } else {
                                        f?.a = null
                                    }
                                }
                                println("destroy")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    App.APP?.registerActivityLifecycleCallbacks(lifecycleCallbacks)
                    println("listening")
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
                                println(m?.method)
                                when (m?.method) {
                                    // Activity and Task methods
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
                                        sendMessage(out, newActivityJSON(tasks, activities, m.params?.get("tid")?.asInt,
                                                m.params?.get("pip")?.asBoolean ?: false, m.params?.get("dialog")?.asBoolean ?: false,
                                                m.params?.get("lockscreen")?.asBoolean ?: false, m.params?.get("overlay")?.asBoolean ?: false))
                                        continue
                                    }
                                    "finishActivity" -> {
                                        val aid = m.params?.get("aid")?.asString
                                        val a = activities[aid]
                                        if (a != null) {
                                            runOnUIThreadActivityStarted(a) {
                                                it.finish()
                                            }
                                        }
                                        continue
                                    }
                                    "finishTask" -> {
                                        tasks.find { t -> getTaskInfo(t)?.let { it1 -> getTaskId(it1) } == m.params?.get("tid")?.asInt }?.finishAndRemoveTask()
                                        continue
                                    }
                                    "bringTaskToFront" -> {
                                        tasks.find { t -> getTaskInfo(t)?.let { it1 -> getTaskId(it1) } == m.params?.get("tid")?.asInt }?.moveToFront()
                                        continue
                                    }
                                    "setTheme" -> {
                                        val aid = m.params?.get("aid")?.asString
                                        val a = activities[aid]
                                        val s = m.params?.get("statusBarColor")?.asInt
                                        val t = m.params?.get("textColor")?.asInt
                                        val b = m.params?.get("windowBackground")?.asInt
                                        val p = m.params?.get("colorPrimary")?.asInt
                                        val ac = m.params?.get("colorAccent")?.asInt
                                        val wid = m.params?.get("wid")?.asInt
                                        val w = widgets[wid]
                                        if ((a != null || w != null) && s != null && t != null && b != null && p != null && ac != null) {
                                            if (a != null) {
                                                runOnUIThreadActivityStarted(a) {
                                                    it.theme = GUIActivity.GUITheme(s, p, b, t, ac)
                                                }
                                            }
                                            if (w != null) {
                                                w.theme = GUIActivity.GUITheme(s, p, b, t, ac)
                                            }
                                        }
                                        continue
                                    }
                                    "setTaskDescription" -> {
                                        val aid = m.params?.get("aid")?.asString
                                        val a = activities[aid]
                                        val img = m.params?.get("img")?.asString
                                        if (a != null) {
                                            runOnUIThreadActivityStarted(a) {
                                                val t = it.theme
                                                val prim = t?.colorPrimary ?: (0xFF000000).toInt()
                                                if (img != null) {
                                                    if (img == "default") {
                                                        it.setTaskDescription(ActivityManager.TaskDescription(m.params?.get("label")?.asString, BitmapFactory.decodeResource(it.resources, R.mipmap.ic_launcher_round), prim))
                                                    } else {
                                                        val bin = Base64.decode(img, Base64.DEFAULT)
                                                        val bitmap = BitmapFactory.decodeByteArray(bin, 0, bin.size)
                                                        it.setTaskDescription(ActivityManager.TaskDescription(m.params?.get("label")?.asString, bitmap, prim))
                                                    }
                                                } else {
                                                    it.setTaskDescription(ActivityManager.TaskDescription(m.params?.get("label")?.asString, null, prim))
                                                }
                                            }
                                        }
                                        continue
                                    }
                                    "setPiPParams" -> {
                                        val aid = m.params?.get("aid")?.asString
                                        val num = m.params?.get("num")?.asInt
                                        val den = m.params?.get("den")?.asInt
                                        val a = activities[aid]
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && num != null && den != null && a != null) {
                                            val rat = Rational(num,den).coerceAtMost(Rational(239,100)).coerceAtLeast(Rational(100, 239))
                                            runOnUIThreadActivityStarted(a) {
                                                it.setPictureInPictureParams(PictureInPictureParams.Builder().setAspectRatio(rat).build())
                                            }
                                        }
                                        continue
                                    }
                                    "setInputMode" -> {
                                        val aid = m.params?.get("aid")?.asString
                                        val mode = m.params?.get("mode")?.asString
                                        val a = activities[aid]
                                        if (a != null && mode != null) {
                                            runOnUIThreadActivityStarted(a) {
                                                when (mode) {
                                                    "resize" -> {
                                                        it.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                                                    }
                                                    "pan" -> {
                                                        it.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
                                                    }
                                                }
                                            }
                                        }
                                        continue
                                    }
                                    // View and Layout Methods
                                    in Regex("create.*") -> {
                                        if (m.params != null) {
                                            val aid = m.params?.get("aid")?.asString
                                            val parent = m.params?.get("parent")?.asInt
                                            val a = activities[aid]
                                            val wid = m.params?.get("wid")?.asInt
                                            val w = widgets[wid]
                                            if (a != null && aid != null) {
                                                if (m.method == "createTextView") {
                                                    var id = -1
                                                    runOnUIThreadActivityStartedBlocking(a) {
                                                        val v = TextView(it)
                                                        id = generateViewID(it)
                                                        v.id = id
                                                        v.text = m.params?.get("text")?.asString
                                                        v.freezesText = true
                                                        setView(it, v, parent)
                                                    }
                                                    sendMessage(out, gson.toJson(id))
                                                    continue
                                                }
                                                if (m.method == "createEditText") {
                                                    var id = -1
                                                    runOnUIThreadActivityStartedBlocking(a) {
                                                        val v = EditText(it)
                                                        id = generateViewID(it)
                                                        v.id = id
                                                        v.setText(m.params?.get("text")?.asString, TextView.BufferType.EDITABLE)
                                                        setView(it, v, parent)
                                                    }
                                                    sendMessage(out, gson.toJson(id))
                                                    continue
                                                }
                                                if (m.method == "createLinearLayout") {
                                                    var id = -1
                                                    runOnUIThreadActivityStartedBlocking(a) {
                                                        val v = LinearLayout(it)
                                                        id = generateViewID(it)
                                                        v.id = id
                                                        v.orientation = if (m.params?.get("vertical")?.asBoolean != false) {
                                                            LinearLayout.VERTICAL
                                                        } else {
                                                            LinearLayout.HORIZONTAL
                                                        }
                                                        setView(it, v, parent)
                                                    }
                                                    sendMessage(out, gson.toJson(id))
                                                    continue
                                                }
                                                if (m.method == "createButton") {
                                                    var id = -1
                                                    runOnUIThreadActivityStartedBlocking(a) {
                                                        val v = Button(it)
                                                        id = generateViewID(it)
                                                        v.id = id
                                                        v.text = m.params?.get("text")?.asString
                                                        v.freezesText = true
                                                        val map = HashMap<String, Any>()
                                                        map["id"] = v.id
                                                        map["aid"] = aid
                                                        v.setOnClickListener { eventQueue.offer(Event("click", gson.toJsonTree(map))) }
                                                        setView(it, v, parent)
                                                    }
                                                    sendMessage(out, gson.toJson(id))
                                                    continue
                                                }
                                                if (m.method == "createImageView") {
                                                    var id = -1
                                                    runOnUIThreadActivityStartedBlocking(a) {
                                                        val v = ImageView(it)
                                                        id = generateViewID(it)
                                                        v.id = id
                                                        setView(it, v, parent)
                                                    }
                                                    sendMessage(out, gson.toJson(id))
                                                    continue
                                                }
                                                if (m.method == "createSpace") {
                                                    var id = -1
                                                    runOnUIThreadActivityStartedBlocking(a) {
                                                        val v = Space(it)
                                                        id = generateViewID(it)
                                                        v.id = id
                                                        setView(it, v, parent)
                                                    }
                                                    sendMessage(out, gson.toJson(id))
                                                    continue
                                                }
                                                if (m.method == "createFrameLayout") {
                                                    var id = -1
                                                    runOnUIThreadActivityStartedBlocking(a) {
                                                        val v = FrameLayout(it)
                                                        id = generateViewID(it)
                                                        v.id = id
                                                        setView(it, v, parent)
                                                    }
                                                    sendMessage(out, gson.toJson(id))
                                                    continue
                                                }
                                                if (m.method == "createCheckbox") {
                                                    var id = -1
                                                    runOnUIThreadActivityStartedBlocking(a) {
                                                        val v = CheckBox(it)
                                                        id = generateViewID(it)
                                                        v.id = id
                                                        v.text = m.params?.get("text")?.asString
                                                        v.isChecked = m.params?.get("checked")?.asBoolean
                                                                ?: false
                                                        v.freezesText = true
                                                        val map = HashMap<String, Any>()
                                                        map["id"] = v.id
                                                        map["aid"] = aid
                                                        v.setOnClickListener {
                                                            map["set"] = v.isChecked
                                                            eventQueue.offer(Event("click", gson.toJsonTree(map)))
                                                        }
                                                        setView(it, v, parent)
                                                    }
                                                    sendMessage(out, gson.toJson(id))
                                                    continue
                                                }
                                                if (m.method == "createNestedScrollView") {
                                                    var id = -1
                                                    runOnUIThreadActivityStartedBlocking(a) {
                                                        val v = NestedScrollView(it)
                                                        id = generateViewID(it)
                                                        v.id = id
                                                        setView(it, v, parent)
                                                    }
                                                    sendMessage(out, gson.toJson(id))
                                                    continue
                                                }
                                            }
                                            if (wid != null && w != null) {
                                                if (m.method == "createLinearLayout") {
                                                    val v = RemoteViews(app.packageName, R.layout.remote_linearlayout)
                                                    val id = generateWidgetViewID(w)
                                                    //v.setInt(R.id.remoteview, "setId", id)
                                                    if (m.params?.get("vertical")?.asBoolean == false) {
                                                        v.setInt(id, "setOrientation", LinearLayout.HORIZONTAL)
                                                    }
                                                    setViewWidget(w, v, parent, R.id.remoteview)
                                                    sendMessage(out, gson.toJson(id))
                                                    continue
                                                }
                                                if (m.method == "createTextView") {
                                                    val v = RemoteViews(app.packageName, R.layout.remote_textview)
                                                    val id = generateWidgetViewID(w)
                                                    //v.setInt(R.id.remoteview, "setId", id)
                                                    v.setTextViewText(R.id.remoteview, m.params?.get("text")?.asString)
                                                    setViewWidget(w, v, parent, R.id.remoteview)
                                                    sendMessage(out, gson.toJson(id))
                                                    continue
                                                }
                                                if (m.method == "createButton") {
                                                    val v = RemoteViews(app.packageName, R.layout.remote_button)
                                                    val id = generateWidgetViewID(w)
                                                    //v.setInt(R.id.remoteview, "setId", id)
                                                    val i = Intent(app, WidgetButtonReceiver::class.java)
                                                    i.action = app.packageName+".button"
                                                    i.data = Uri.parse("$wid:$id")
                                                    v.setOnClickPendingIntent(id, PendingIntent.getBroadcast(app, 0, i, PendingIntent.FLAG_IMMUTABLE))
                                                    v.setString(R.id.remoteview, "setText", m.params?.get("text")?.asString)
                                                    setViewWidget(w, v, parent, R.id.remoteview)
                                                    sendMessage(out, gson.toJson(id))
                                                    continue
                                                }
                                                if (m.method == "createFrameLayout") {

                                                    continue
                                                }
                                                if (m.method == "createImageView") {
                                                    
                                                    continue
                                                }
                                            }
                                        }
                                        continue
                                    }
                                    "deleteView" -> {
                                        val aid = m.params?.get("aid")?.asString
                                        val id = m.params?.get("id")?.asInt
                                        val a = activities[aid]
                                        if (a != null && id != null) {
                                            runOnUIThreadActivityStarted(a) {
                                                removeViewRecursive(it.findViewById(id))
                                            }
                                        }
                                        continue
                                    }
                                    "setTextSize" -> {
                                        if (m.params != null) {
                                            val aid = m.params?.get("aid")?.asString
                                            val id = m.params?.get("id")?.asInt
                                            val size = m.params?.get("size")?.asInt
                                            val a = activities[aid]
                                            if (a != null && id != null && size != null && size > 0) {
                                                runOnUIThreadActivityStarted(a) {
                                                    val tv = it.findViewById<TextView>(id)
                                                    tv?.setTextSize(TypedValue.COMPLEX_UNIT_SP, size.toFloat())
                                                }
                                            }
                                        }
                                        continue
                                    }
                                    "setImage" -> {
                                        val aid = m.params?.get("aid")?.asString
                                        val a = activities[aid]
                                        val id = m.params?.get("id")?.asInt
                                        val img = m.params?.get("img")?.asString
                                        if (img != null && a != null && id != null) {
                                            runOnUIThreadActivityStarted(a) {
                                                val bin = Base64.decode(img, Base64.DEFAULT)
                                                val bitmap = BitmapFactory.decodeByteArray(bin, 0, bin.size)
                                                it.findViewById<ImageView>(id)?.setImageBitmap(bitmap)
                                            }
                                        }
                                        continue
                                    }
                                    "setMargin" -> {
                                        if (m.params != null) {
                                            val aid = m.params?.get("aid")?.asString
                                            val id = m.params?.get("id")?.asInt
                                            val margin = m.params?.get("margin")?.asInt
                                            val a = activities[aid]
                                            if (id != null && margin != null && a != null) {
                                                runOnUIThreadActivityStarted(a) {
                                                    val mar = toPX(it, margin)
                                                    val v = it.findViewById<View>(id)
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
                                    "setLinearLayoutParams" -> {
                                        val aid = m.params?.get("aid")?.asString
                                        val a = activities[aid]
                                        val id = m.params?.get("id")?.asInt
                                        val weight = m.params?.get("weight")?.asInt
                                        if (a != null && id != null && weight != null) {
                                            runOnUIThreadActivityStarted(a) {
                                                val v = it.findViewById<View>(id)
                                                val p = v?.layoutParams as? LinearLayout.LayoutParams
                                                if (p != null) {
                                                    p.weight = weight.toFloat()
                                                    v.layoutParams = p
                                                }
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
                                                runOnUIThreadActivityStarted(a) {
                                                    val tv = it.findViewById<TextView>(id)
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
                                                runOnUIThreadActivityStartedBlocking(a) {
                                                    text = it.findViewById<TextView>(id)?.text?.toString()
                                                }
                                            }
                                            sendMessage(out, gson.toJson(text ?: ""))
                                        } else {
                                            sendMessage(out, gson.toJson(""))
                                        }
                                        continue
                                    }
                                    "addBuffer" -> {
                                        val format = m.params?.get("format")?.asString
                                        val w = m.params?.get("w")?.asInt
                                        val h = m.params?.get("h")?.asInt
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && w != null && h != null && format == "ARGB888" && w > 0 && h > 0) {
                                            val bid = generateBufferID(buffers)
                                            val shm = SharedMemory.create(bid.toString(), w*h*4)
                                            val b = SharedBuffer(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888, true), shm, shm.mapReadOnly())
                                            try {
                                                // this is a dirty trick to get the FileDescriptor of a SharedMemory object without using JNI or a higher API version.
                                                // this could break anytime, though it is still marked as public but discouraged, so that is unlikely, also given the implementation of the class.
                                                // noinspection DiscouragedPrivateApi
                                                val getFd = SharedMemory::class.java.getDeclaredMethod("getFd")
                                                val fdint = getFd.invoke(shm) as? Int
                                                if (fdint == null) {
                                                    println("fd empty or not a Int")
                                                    b.shm.close()
                                                    SharedMemory.unmap(b.buff)
                                                    b.btm.recycle()
                                                    sendMessage(out, gson.toJson(-1))
                                                    continue
                                                }
                                                val fd = ParcelFileDescriptor.fromFd(fdint)
                                                fd.use {
                                                    sendMessageFd(out, gson.toJson(bid), main, fd.fileDescriptor)
                                                }
                                                buffers[bid] = b
                                            } catch (e: Exception) {
                                                SharedMemory.unmap(b.buff)
                                                b.shm.close()
                                                b.btm.recycle()
                                                if (e is NoSuchMethodException || e is IllegalArgumentException || e is IllegalAccessException ||
                                                        e is InstantiationException || e is InvocationTargetException) {
                                                    println("reflection exception")
                                                    e.printStackTrace()
                                                    sendMessage(out, gson.toJson(-1))
                                                } else {
                                                    throw e
                                                }
                                            }
                                        } else {
                                            println("invalid parameters or api version")
                                            sendMessage(out, gson.toJson(-1))
                                        }
                                        continue
                                    }
                                    "deleteBuffer" -> {
                                        val bid = m.params?.get("bid")?.asInt
                                        val buffer = buffers[bid]
                                        if (buffer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                                            buffers.remove(bid)
                                            SharedMemory.unmap(buffer.buff)
                                            buffer.shm.close()
                                            buffer.btm.recycle() // this frees the bitmap memory
                                        }
                                        continue
                                    }
                                    "blitBuffer" -> {
                                        val buffer = buffers[m.params?.get("bid")?.asInt]
                                        if (buffer != null) {
                                            buffer.btm.copyPixelsFromBuffer(buffer.buff)
                                            buffer.buff.position(0)
                                        }
                                        continue
                                    }
                                    "setBuffer" -> {
                                        val aid = m.params?.get("aid")?.asString
                                        val a = activities[aid]
                                        val id = m.params?.get("id")?.asInt
                                        val buffer = buffers[m.params?.get("bid")?.asInt]
                                        if (buffer != null && a != null && id != null) {
                                            runOnUIThreadActivityStarted(a) {
                                                it.findViewById<ImageView>(id)?.setImageBitmap(buffer.btm)
                                            }
                                        }
                                        continue
                                    }
                                    "refreshImageView" -> {
                                        val aid = m.params?.get("aid")?.asString
                                        val a = activities[aid]
                                        val id = m.params?.get("id")?.asInt
                                        if (a != null && id != null) {
                                            runOnUIThreadActivityStarted(a) {
                                                it.findViewById<ImageView>(id)?.invalidate()
                                            }
                                        }
                                        continue
                                    }
                                    "bindWidget" -> {
                                        val wid = m.params?.get("wid")?.asInt
                                        if (wid != null && ! widgets.containsKey(wid)) {
                                            widgets[wid] = WidgetRepresentation(TreeSet(), null, null)
                                        }
                                        continue
                                    }
                                    "blitWidget" -> {
                                        val wid = m.params?.get("wid")?.asInt
                                        val root = widgets[wid]?.root
                                        if (wid != null && widgets.containsKey(wid) && root != null) {
                                            println("updated widget")
                                            app.getSystemService(AppWidgetManager::class.java).updateAppWidget(wid, root)
                                        }
                                        continue
                                    }
                                    "clearWidget" -> {
                                        val wid = m.params?.get("wid")?.asInt
                                        val v = widgets[wid]
                                        if (v != null) {
                                            v.root = null
                                            v.usedIds.clear()
                                        }
                                        continue
                                    }
                                }
                                eventQueue.offer(INVALID_METHOD)
                            }
                            0 -> {
                                
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (e is JsonSyntaxException) {
                println("program send invalid json")
                return
            }
            if (e is EOFException) {
                println("connection closed by program")
                return
            }
            e.printStackTrace()
        } finally {
            eventWorker?.interrupt()
            println("cleanup")
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
                    b.shm.close()
                    b.btm.recycle() // this frees the bitmap memory
                }
            }
            
        }
    }

}