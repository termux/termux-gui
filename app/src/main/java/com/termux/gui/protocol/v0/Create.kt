package com.termux.gui.protocol.v0

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.widget.NestedScrollView
import com.termux.gui.ConnectionHandler
import com.termux.gui.R
import com.termux.gui.Util
import com.termux.gui.WidgetButtonReceiver
import java.io.DataOutputStream
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

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
                            val v = EditText(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            if (m.params?.get("singleline")?.asBoolean == true) {
                                println("singleline")
                                v.inputType = EditorInfo.TYPE_CLASS_TEXT
                            }
                            if (m.params?.get("line")?.asBoolean == false) {
                                v.setBackgroundResource(android.R.color.transparent)
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
                            val map = HashMap<String, Any>()
                            map["id"] = v.id
                            map["aid"] = aid
                            v.setOnClickListener { eventQueue.offer(ConnectionHandler.Event("click", ConnectionHandler.gson.toJsonTree(map))) }
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
                            val map = HashMap<String, Any>()
                            map["id"] = v.id
                            map["aid"] = aid
                            v.setOnClickListener {
                                map["set"] = v.isChecked
                                eventQueue.offer(ConnectionHandler.Event("click", ConnectionHandler.gson.toJsonTree(map)))
                            }
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
                        val v = EditText(app)
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
                            val map = HashMap<String, Any>()
                            map["id"] = v.id
                            map["aid"] = aid
                            v.setOnClickListener { eventQueue.offer(ConnectionHandler.Event("click", ConnectionHandler.gson.toJsonTree(map))) }
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
                            val map = HashMap<String, Any>()
                            map["id"] = v.id
                            map["aid"] = aid
                            v.setOnClickListener {
                                map["set"] = v.isChecked
                                eventQueue.offer(ConnectionHandler.Event("click", ConnectionHandler.gson.toJsonTree(map)))
                            }
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
        
        
    }
}