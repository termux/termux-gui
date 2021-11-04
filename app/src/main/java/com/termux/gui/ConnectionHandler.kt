package com.termux.gui

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.net.Uri
import android.os.*
import android.util.Base64
import android.util.Rational
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.*
import java.io.*
import java.lang.IllegalArgumentException
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import kotlin.collections.HashMap
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
    data class SharedBuffer(val btm: Bitmap, val shm: SharedMemory, val buff: ByteBuffer)
    
    
    
    @Suppress("DEPRECATION")
    private fun newActivityJSON(tasks: LinkedList<ActivityManager.AppTask>, activities: MutableMap<String, GUIFragment>, ptid: Int?, flags: Int, pip: Boolean, dialog: Boolean): String {
        //println("ptid: $ptid")
        val i = Intent(app, GUIActivity::class.java)
        if (ptid == null) {
            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val aid = Thread.currentThread().id.toString()+"-"+activityID.toString()
        i.data = Uri.parse(aid)
        activityID++
        
        activities[aid] = GUIFragment()
        i.putExtra("pip", pip)
        if (pip) {
            i.flags = i.flags or Intent.FLAG_ACTIVITY_NO_ANIMATION
        } else {
            // pip overwrites dialog
            if (dialog) {
                i.setClass(app, GUIActivityDialog::class.java)
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
            if (activities[aid]?.isAdded == true) {
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
    
    private fun sendMessage(w: DataOutputStream, ret: String) {
        w.writeInt(ret.length)
        w.write(ret.toByteArray(UTF_8))
        w.flush()
    }

    private fun sendMessageFd(w: DataOutputStream, ret: String, s: LocalSocket, fd: FileDescriptor) {
        w.writeInt(ret.length)
        s.setFileDescriptorsForSend(arrayOf(fd))
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

    private fun generateBufferID(buffers: MutableMap<Int, SharedBuffer>): Int {
        var id = rand.nextInt(Integer.MAX_VALUE)
        while (buffers.contains(id)) {
            id = rand.nextInt(Integer.MAX_VALUE)
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
    
    
    private fun getFragmentActivityBlocking(f: GUIFragment?, activities: MutableMap<String, GUIFragment>) : GUIActivity? {
        if (f == null) {
            return null
        }
        var a = f.activity
        if (a == null) {
            if (! activities.containsValue(f)) {
                return null // activity got destroyed
            }
            Thread.sleep(0, 100)
            a = f.activity
        }
        return a as GUIActivity
    }

    private fun removeViewRecursive(f: GUIFragment, v: View?) {
        if (v is ViewGroup) {
            removeViewRecursive(f, v)
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
        val buffers: MutableMap<Int, SharedBuffer> = HashMap()
        
        fun toPX(a: GUIActivity, dip: Int): Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip.toFloat(), a.resources.displayMetrics).roundToInt()
        }
        
        
        
        var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
        
        
        
        try {
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
                    
                    lifecycleCallbacks = object: Application.ActivityLifecycleCallbacks {
                        override fun onActivityCreated(a: Activity, savedInstanceState: Bundle?) {
                            println("create")
                            if (a is GUIActivity) {
                                val f = activities[a.intent?.dataString]
                                if (f != null) {
                                    for (task in am.appTasks) {
                                        val info = getTaskInfo(task)
                                        if (a.taskId == info?.let { getTaskId(it) }) {
                                            if (tasks.find { getTaskInfo(task)?.let { getTaskId(it) } == getTaskId(task.taskInfo)} == null) {
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
                            try {
                                if (a is GUIActivity) {
                                    val f = activities[a.intent?.dataString]
                                    if (f != null) {
                                        val map = HashMap<String, Any?>()
                                        map["finishing"] = a.isFinishing
                                        map["aid"] = a.intent?.dataString
                                        sendMessage(eventOut, gson.toJson(Event("stop", gson.toJsonTree(map))))
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            
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
                    App.APP?.registerActivityLifecycleCallbacks(lifecycleCallbacks)
                    
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
                                                m.params?.get("flags")?.asInt ?: 0, m.params?.get("pip")?.asBoolean ?: false,
                                                m.params?.get("dialog")?.asBoolean ?: false))
                                        continue
                                    }
                                    "finishActivity" -> {
                                        getFragmentActivityBlocking(activities[m.params?.get("aid")?.asString], activities)?.finish()
                                    }
                                    "finishTask" -> {
                                        tasks.find { t -> getTaskInfo(t)?.let { it1 -> getTaskId(it1) } == m.params?.get("tid")?.asInt }?.finishAndRemoveTask()
                                    }
                                    "bringTaskToFront" -> {
                                        tasks.find { t -> getTaskInfo(t)?.let { it1 -> getTaskId(it1) } == m.params?.get("tid")?.asInt }?.moveToFront()
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
                                    "setTaskDescription" -> {
                                        val aid = m.params?.get("aid")?.asString
                                        val a = activities[aid]
                                        val ac = getFragmentActivityBlocking(a, activities)
                                        val img = m.params?.get("img")?.asString
                                        if (ac != null && a != null) {
                                            runOnUIThreadBlocking {
                                                val t = a.theme
                                                val prim = t?.colorPrimary ?: (0xFF000000).toInt()
                                                if (img != null) {
                                                    val bin = Base64.decode(img, Base64.DEFAULT)
                                                    val bitmap = BitmapFactory.decodeByteArray(bin, 0, bin.size)
                                                    ac.setTaskDescription(ActivityManager.TaskDescription("Termux:GUI Test app", bitmap, prim))
                                                } else {
                                                    ac.setTaskDescription(ActivityManager.TaskDescription("Termux:GUI Test app", null, prim))
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
                                        val ac = getFragmentActivityBlocking(a, activities)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && ac != null && num != null && den != null) {
                                            val rat = Rational(num,den).coerceAtMost(Rational(239,100)).coerceAtLeast(Rational(100, 239))
                                            ac.setPictureInPictureParams(PictureInPictureParams.Builder().setAspectRatio(rat).build())
                                        }
                                        continue
                                    }
                                    // View and Layout Methods
                                    in Regex("create.*") -> {
                                        if (m.params != null) {
                                            val aid = m.params?.get("aid")?.asString
                                            val parent = m.params?.get("parent")?.asInt
                                            val a = activities[aid]
                                            val ac = getFragmentActivityBlocking(a, activities)
                                            if (ac != null && a != null && aid != null) {
                                                if (m.method == "createTextView") {
                                                    val v = TextView(ac)
                                                    v.id = generateViewID(a)
                                                    v.text = m.params?.get("text")?.asString
                                                    setView(a, v, parent)
                                                    sendMessage(out, gson.toJson(v.id))
                                                    continue
                                                }
                                                if (m.method == "createEditText") {
                                                    val v = EditText(ac)
                                                    v.id = generateViewID(a)
                                                    v.setText(m.params?.get("text")?.asString, TextView.BufferType.EDITABLE)
                                                    setView(a, v, parent)
                                                    sendMessage(out, gson.toJson(v.id))
                                                    continue
                                                }
                                                if (m.method == "createLinearLayout") {
                                                    val v = LinearLayout(ac)
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
                                                if (m.method == "createButton") {
                                                    val v = Button(ac)
                                                    v.id = generateViewID(a)
                                                    v.text = m.params?.get("text")?.asString
                                                    val map = HashMap<String, Any>()
                                                    map["id"] = v.id
                                                    map["aid"] = aid
                                                    v.setOnClickListener { sendMessage(eventOut, gson.toJson(Event("click", gson.toJsonTree(map)))) }
                                                    setView(a, v, parent)
                                                    sendMessage(out, gson.toJson(v.id))
                                                    continue
                                                }
                                                if (m.method == "createImageView") {
                                                    val v = ImageView(ac)
                                                    v.id = generateViewID(a)
                                                    setView(a, v, parent)
                                                    sendMessage(out, gson.toJson(v.id))
                                                    continue
                                                }
                                                if (m.method == "createSpace") {
                                                    val v = Space(ac)
                                                    v.id = generateViewID(a)
                                                    setView(a, v, parent)
                                                    sendMessage(out, gson.toJson(v.id))
                                                    continue
                                                }
                                                if (m.method == "createCheckbox") {
                                                    val v = CheckBox(ac)
                                                    v.id = generateViewID(a)
                                                    v.text = m.params?.get("text")?.asString
                                                    val map = HashMap<String, Any>()
                                                    map["id"] = v.id
                                                    map["aid"] = aid
                                                    v.setOnClickListener {
                                                        map["set"] = v.isChecked
                                                        sendMessage(eventOut, gson.toJson(Event("click", gson.toJsonTree(map))))
                                                    }
                                                    setView(a, v, parent)
                                                    sendMessage(out, gson.toJson(v.id))
                                                    continue
                                                }
                                            }
                                        }
                                    }
                                    "deleteView" -> {
                                        val aid = m.params?.get("aid")?.asString
                                        val id = m.params?.get("id")?.asInt
                                        val a = activities[aid]
                                        if (a != null && id != null) {
                                            runOnUIThreadBlocking {
                                                removeViewRecursive(a, a.view?.findViewById(id))
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
                                                runOnUIThreadBlocking {
                                                    val tv = a.view?.findViewById<TextView>(id)
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
                                            runOnUIThreadBlocking {
                                                val bin = Base64.decode(img, Base64.DEFAULT)
                                                val bitmap = BitmapFactory.decodeByteArray(bin, 0, bin.size)
                                                a.view?.findViewById<ImageView>(id)?.setImageBitmap(bitmap)
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
                                            val ac = getFragmentActivityBlocking(a, activities)
                                            if (id != null && a != null && margin != null && ac != null) {
                                                runOnUIThreadBlocking {
                                                    val mar = toPX(ac, margin)
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
                                    "setLinearLayoutParams" -> {
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
                                            runOnUIThreadBlocking {
                                                a.view?.findViewById<ImageView>(id)?.setImageBitmap(buffer.btm)
                                            }
                                        }
                                        continue
                                    }
                                    "refreshImageView" -> {
                                        val aid = m.params?.get("aid")?.asString
                                        val a = activities[aid]
                                        val id = m.params?.get("id")?.asInt
                                        if (a != null && id != null) {
                                            runOnUIThreadBlocking {
                                                a.view?.findViewById<ImageView>(id)?.invalidate()
                                            }
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