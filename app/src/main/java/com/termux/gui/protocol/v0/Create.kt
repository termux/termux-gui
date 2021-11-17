package com.termux.gui.protocol.v0

import android.app.PendingIntent
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.widget.*
import androidx.core.widget.NestedScrollView
import com.termux.gui.*
import java.io.DataOutputStream
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.collections.HashMap

class Create {
    companion object {
        fun handleCreateMessage(m: ConnectionHandler.Message, activities: MutableMap<String, V0.ActivityState>,
                                widgets: MutableMap<Int, V0.WidgetRepresentation>, overlays: MutableMap<String, V0.Overlay>, rand: Random, out: DataOutputStream,
                                app: Context, eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) {
            val aid = m.params?.get("aid")?.asString
            val parent = m.params?.get("parent")?.asInt
            val a = activities[aid]
            val o = overlays[aid]
            val wid = m.params?.get("wid")?.asInt
            val w = widgets[wid]
            if (aid != null) {
                if (a != null) {
                    if (m.method == "createTextView") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = TextView(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            v.text = m.params?.get("text")?.asString
                            v.freezesText = true
                            Util.setViewActivity(it, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createEditText") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = if (m.params?.get("blockinput")?.asBoolean == true) getCustomEditText(it, aid, eventQueue) else EditText(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            if (m.params?.get("singleline")?.asBoolean == true) {
                                println("singleline")
                                v.inputType = EditorInfo.TYPE_CLASS_TEXT
                            }
                            if (m.params?.get("line")?.asBoolean == false) {
                                v.setBackgroundResource(android.R.color.transparent)
                            }
                            if (m.params?.get("blockinput")?.asBoolean == true) {
                                val args = HashMap<String, Any>()
                                args["id"] = v.id
                                args["aid"] = aid
                                
                            }
                            v.setText(m.params?.get("text")?.asString, TextView.BufferType.EDITABLE)
                            Util.setViewActivity(it, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createLinearLayout") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = LinearLayout(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            v.orientation = if (m.params?.get("vertical")?.asBoolean != false) {
                                LinearLayout.VERTICAL
                            } else {
                                LinearLayout.HORIZONTAL
                            }
                            Util.setViewActivity(it, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createButton") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = Button(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            v.text = m.params?.get("text")?.asString
                            v.freezesText = true
                            Util.setClickListener(v, aid, true, eventQueue)
                            Util.setViewActivity(it, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createImageView") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = ImageView(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            Util.setViewActivity(it, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createSpace") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = Space(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            Util.setViewActivity(it, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createFrameLayout") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = FrameLayout(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            Util.setViewActivity(it, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createCheckbox") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = CheckBox(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            v.text = m.params?.get("text")?.asString
                            v.isChecked = m.params?.get("checked")?.asBoolean
                                    ?: false
                            v.freezesText = true
                            Util.setClickListener(v, aid, true, eventQueue)
                            Util.setViewActivity(it, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createNestedScrollView") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = NestedScrollView(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            Util.setViewActivity(it, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createRadioGroup") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = RadioGroup(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            val args = HashMap<String, Any>()
                            args["aid"] = aid
                            args["id"] = v.id
                            v.setOnCheckedChangeListener { _, checked ->
                                args["selected"] = checked
                                eventQueue.offer(ConnectionHandler.Event("selected", ConnectionHandler.gson.toJsonTree(args)))
                            }
                            Util.setViewActivity(it, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createRadioButton") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = RadioButton(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            Util.setViewActivity(it, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createSpinner") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = Spinner(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            v.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                                val args = HashMap<String, Any?>()
                                init {
                                    args["aid"] = aid
                                    args["id"] = id
                                }
                                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                    args["selected"] = (view as? TextView)?.text?.toString()
                                    eventQueue.offer(ConnectionHandler.Event("itemselected", ConnectionHandler.gson.toJsonTree(args)))
                                }
                                override fun onNothingSelected(parent: AdapterView<*>?) {
                                    args["selected"] = null
                                    eventQueue.offer(ConnectionHandler.Event("itemselected", ConnectionHandler.gson.toJsonTree(args)))
                                }
                            }
                            Util.setViewActivity(it, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                }
                if (o != null) {
                    if (m.method == "createTextView") {
                        val v = TextView(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            v.text = m.params?.get("text")?.asString
                            v.freezesText = true
                            V0.setViewOverlay(o, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createEditText") {
                        val v = if (m.params?.get("blockinput")?.asBoolean == true) getCustomEditText(app, aid, eventQueue) else EditText(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            if (m.params?.get("line")?.asBoolean == false) {
                                v.setBackgroundResource(android.R.color.transparent)
                            }
                            v.setText(m.params?.get("text")?.asString, TextView.BufferType.EDITABLE)
                            V0.setViewOverlay(o, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createLinearLayout") {
                        val v = LinearLayout(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            v.orientation = if (m.params?.get("vertical")?.asBoolean != false) {
                                LinearLayout.VERTICAL
                            } else {
                                LinearLayout.HORIZONTAL
                            }
                            V0.setViewOverlay(o, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createButton") {
                        val v = Button(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            v.text = m.params?.get("text")?.asString
                            v.freezesText = true
                            Util.setClickListener(v, aid, true, eventQueue)
                            V0.setViewOverlay(o, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createImageView") {
                        val v = ImageView(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            V0.setViewOverlay(o, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createSpace") {
                        val v = Space(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            V0.setViewOverlay(o, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createFrameLayout") {
                        val v = FrameLayout(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            V0.setViewOverlay(o, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createCheckbox") {
                        val v = CheckBox(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            v.text = m.params?.get("text")?.asString
                            v.isChecked = m.params?.get("checked")?.asBoolean
                                    ?: false
                            v.freezesText = true
                            Util.setClickListener(v, aid, true, eventQueue)
                            V0.setViewOverlay(o, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createNestedScrollView") {
                        val v = NestedScrollView(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            V0.setViewOverlay(o, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createRadioGroup") {
                        val v = RadioGroup(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            val args = HashMap<String, Any>()
                            args["aid"] = aid
                            args["id"] = v.id
                            v.setOnCheckedChangeListener { _, checked ->
                                args["selected"] = checked
                                eventQueue.offer(ConnectionHandler.Event("selected", ConnectionHandler.gson.toJsonTree(args)))
                            }
                            V0.setViewOverlay(o, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createRadioButton") {
                        val v = RadioButton(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            V0.setViewOverlay(o, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createSpinner") {
                        val v = Spinner(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            v.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                                val args = HashMap<String, Any?>()
                                init {
                                    args["aid"] = aid
                                    args["id"] = id
                                }
                                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                    args["selected"] = (view as? TextView)?.text?.toString()
                                    eventQueue.offer(ConnectionHandler.Event("itemselected", ConnectionHandler.gson.toJsonTree(args)))
                                }
                                override fun onNothingSelected(parent: AdapterView<*>?) {
                                    args["selected"] = null
                                    eventQueue.offer(ConnectionHandler.Event("itemselected", ConnectionHandler.gson.toJsonTree(args)))
                                }
                            }
                            V0.setViewOverlay(o, v, parent, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                }
            }
            
            // TODO rework
            if (wid != null && w != null) {
                if (m.method == "createLinearLayout") {
                    val v = RemoteViews(app.packageName, R.layout.remote_linearlayout)
                    val id = V0.generateWidgetViewID(rand, w)
                    //v.setInt(R.id.remoteview, "setId", id)
                    if (m.params?.get("vertical")?.asBoolean == false) {
                        v.setInt(id, "setOrientation", LinearLayout.HORIZONTAL)
                    }
                    V0.setViewWidget(w, v, parent, R.id.remoteview)
                    Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                    return
                }
                if (m.method == "createTextView") {
                    val v = RemoteViews(app.packageName, R.layout.remote_textview)
                    val id = V0.generateWidgetViewID(rand, w)
                    //v.setInt(R.id.remoteview, "setId", id)
                    v.setTextViewText(R.id.remoteview, m.params?.get("text")?.asString)
                    V0.setViewWidget(w, v, parent, R.id.remoteview)
                    Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                    return
                }
                if (m.method == "createButton") {
                    val v = RemoteViews(app.packageName, R.layout.remote_button)
                    val id = V0.generateWidgetViewID(rand, w)
                    //v.setInt(R.id.remoteview, "setId", id)
                    val i = Intent(app, WidgetButtonReceiver::class.java)
                    i.action = app.packageName + ".button"
                    i.data = Uri.parse("$wid:$id")
                    v.setOnClickPendingIntent(id, PendingIntent.getBroadcast(app, 0, i, PendingIntent.FLAG_IMMUTABLE))
                    v.setString(R.id.remoteview, "setText", m.params?.get("text")?.asString)
                    V0.setViewWidget(w, v, parent, R.id.remoteview)
                    Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                    return
                }
                if (m.method == "createFrameLayout") {

                    return
                }
                if (m.method == "createImageView") {

                    return
                }
            }
        }

        private fun getCustomEditText(it : Context, aid: String, eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>): EditText {
            return object : androidx.appcompat.widget.AppCompatEditText(it) {
                override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection {
                    return InputWrapper(super.onCreateInputConnection(outAttrs), true)
                }

                override fun onTextContextMenuItem(id: Int): Boolean {
                    val consumed: Boolean = super.onTextContextMenuItem(id)
                    val args = HashMap<String, Any>()
                    args["aid"] = aid
                    args["id"] = getId()
                    when (id) {
                        android.R.id.cut -> {
                            val t = text
                            if (t != null) {
                                val selStart = selectionStart
                                val selEnd = selectionEnd
                                val min = 0.coerceAtLeast(selStart.coerceAtMost(selEnd))
                                val max = 0.coerceAtLeast(selStart.coerceAtLeast(selEnd))
                                args["text"] = t.subSequence(min, max)
                                eventQueue.offer(ConnectionHandler.Event("cut", ConnectionHandler.gson.toJsonTree(args)))
                            }
                        }
                        android.R.id.paste -> {
                            val clip = it.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            if (clip.hasPrimaryClip() && clip.primaryClipDescription!!.hasMimeType(MIMETYPE_TEXT_PLAIN)) {
                                val item = clip.primaryClip!!.getItemAt(0)
                                args["text"] = item.text
                                eventQueue.offer(ConnectionHandler.Event("paste", ConnectionHandler.gson.toJsonTree(args)))
                            }
                        }
                    }
                    return consumed
                }

                private inner class InputWrapper(c: InputConnection?, mutable: Boolean) : InputConnectionWrapper(c, mutable) {
                    private val args = HashMap<String, Any>()

                    init {
                        args["aid"] = aid
                    }

                    override fun sendKeyEvent(e: KeyEvent?): Boolean {
                        if (!args.containsKey("id")) {
                            args["id"] = id
                        }
                        if (e != null) {
                            args["key"] = e.unicodeChar
                            args["shift"] = e.isShiftPressed
                            args["ctrl"] = e.isCtrlPressed
                            args["caps"] = e.isCapsLockOn
                            args["alt"] = e.isAltPressed
                            args["fn"] = e.isFunctionPressed
                            args["meta"] = e.isMetaPressed
                            args["num"] = e.isNumLockOn
                            args["code"] = e.keyCode
                            eventQueue.offer(ConnectionHandler.Event("input", ConnectionHandler.gson.toJsonTree(args)))
                        }
                        return true
                    }
                }
            }
        }


    }
}