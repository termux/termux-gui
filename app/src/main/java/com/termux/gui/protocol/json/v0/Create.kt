package com.termux.gui.protocol.json.v0

import android.app.PendingIntent
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.NestedScrollView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.termux.gui.ConnectionHandler
import com.termux.gui.R
import com.termux.gui.Util
import com.termux.gui.WidgetButtonReceiver
import com.termux.gui.views.SnappingHorizontalScrollView
import com.termux.gui.views.SnappingNestedScrollView
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
                            v.setTextIsSelectable(m.params?.get("selectableText")?.asBoolean ?: false)
                            if (m.params?.get("clickableLinks")?.asBoolean == true) {
                                v.movementMethod = LinkMovementMethod.getInstance()
                            }
                            Util.setViewActivity(it, v, parent)
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
                            when (m.params?.get("type")?.asString) {
                                "text" -> v.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_NORMAL
                                "textMultiLine" -> v.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE or EditorInfo.TYPE_TEXT_VARIATION_NORMAL
                                "phone" -> v.inputType = EditorInfo.TYPE_CLASS_PHONE
                                "date" -> v.inputType = EditorInfo.TYPE_CLASS_DATETIME or EditorInfo.TYPE_DATETIME_VARIATION_DATE
                                "time" -> v.inputType = EditorInfo.TYPE_CLASS_DATETIME or EditorInfo.TYPE_DATETIME_VARIATION_TIME
                                "datetime" -> v.inputType = EditorInfo.TYPE_CLASS_DATETIME or EditorInfo.TYPE_DATETIME_VARIATION_NORMAL
                                "number" -> v.inputType = EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_NORMAL
                                "numberDecimal" -> v.inputType = EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_FLAG_DECIMAL or EditorInfo.TYPE_NUMBER_VARIATION_NORMAL
                                "numberPassword" -> v.inputType = EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD
                                "numberSigned" -> v.inputType = EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_FLAG_SIGNED or EditorInfo.TYPE_NUMBER_VARIATION_NORMAL
                                "numberDecimalSigned" -> v.inputType = EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_FLAG_DECIMAL or EditorInfo.TYPE_NUMBER_FLAG_SIGNED or EditorInfo.TYPE_NUMBER_VARIATION_NORMAL
                                "textEmailAddress" -> v.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                                "textPassword" -> v.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
                            }
                            if (m.params?.get("singleline")?.asBoolean == true) {
                                //println("singleline")
                                v.inputType = EditorInfo.TYPE_CLASS_TEXT
                            }
                            if (m.params?.get("line")?.asBoolean == false) {
                                v.setBackgroundResource(android.R.color.transparent)
                            }
                            if (m.params?.get("blockinput")?.asBoolean == true) {
                                v.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_FILTER or EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                            }
                            v.setText(m.params?.get("text")?.asString, TextView.BufferType.EDITABLE)
                            Util.setViewActivity(it, v, parent)
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
                            Util.setViewActivity(it, v, parent)
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
                            Util.setViewActivity(it, v, parent)
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
                            Util.setViewActivity(it, v, parent)
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
                            Util.setViewActivity(it, v, parent)
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
                            Util.setViewActivity(it, v, parent)
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
                            v.isChecked = m.params?.get("checked")?.asBoolean ?: false
                            v.freezesText = true
                            Util.setClickListener(v, aid, true, eventQueue)
                            Util.setViewActivity(it, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createNestedScrollView") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = if (m.params?.get("snapping")?.asBoolean == true) SnappingNestedScrollView(it) else NestedScrollView(it)
                            if (m.params?.get("fillviewport")?.asBoolean == true) {
                                v.isFillViewport = true
                            }
                            if (m.params?.get("nobar")?.asBoolean == true) {
                                v.scrollBarSize = 0
                            }
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            Util.setViewActivity(it, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createHorizontalScrollView") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = if (m.params?.get("snapping")?.asBoolean == true) SnappingHorizontalScrollView(it) else HorizontalScrollView(it)
                            if (m.params?.get("fillviewport")?.asBoolean == true) {
                                v.isFillViewport = true
                            }
                            if (m.params?.get("nobar")?.asBoolean == true) {
                                v.scrollBarSize = 0
                            }
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            Util.setViewActivity(it, v, parent)
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
                            Util.setCheckedListener(v, aid, eventQueue)
                            Util.setViewActivity(it, v, parent)
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
                            v.text = m.params?.get("text")?.asString
                            v.freezesText = true
                            v.isChecked = m.params?.get("checked")?.asBoolean ?: false
                            Util.setViewActivity(it, v, parent)
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
                            Util.setSpinnerListener(v, aid, eventQueue)
                            Util.setViewActivity(it, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createToggleButton") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = ToggleButton(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            v.freezesText = true
                            v.isChecked = m.params?.get("checked")?.asBoolean ?: false
                            Util.setClickListener(v, aid, true, eventQueue)
                            Util.setViewActivity(it, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createSwitch") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = SwitchCompat(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            v.text = m.params?.get("text")?.asString
                            v.freezesText = true
                            v.isChecked = m.params?.get("checked")?.asBoolean ?: false
                            Util.setClickListener(v, aid, true, eventQueue)
                            Util.setViewActivity(it, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createProgressBar") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = ProgressBar(it, null, android.R.attr.progressBarStyleHorizontal)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            Util.setViewActivity(it, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createSwipeRefreshLayout") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = SwipeRefreshLayout(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            Util.setRefreshListener(v, aid, eventQueue)
                            Util.setViewActivity(it, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createTabLayout") {
                        var id = -1
                        V0.runOnUIThreadActivityStartedBlocking(a) {
                            val v = TabLayout(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            Util.setTabSelectedListener(v, aid, eventQueue)
                            Util.setViewActivity(it, v, parent)
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
                            v.setTextIsSelectable(m.params?.get("selectableText")?.asBoolean ?: false)
                            if (m.params?.get("clickableLinks")?.asBoolean == true) {
                                v.movementMethod = LinkMovementMethod.getInstance()
                            }
                            V0.setViewOverlay(o, v, parent)
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
                            V0.setViewOverlay(o, v, parent)
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
                            V0.setViewOverlay(o, v, parent)
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
                            V0.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createImageView") {
                        val v = ImageView(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            V0.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createSpace") {
                        val v = Space(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            V0.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createFrameLayout") {
                        val v = FrameLayout(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            V0.setViewOverlay(o, v, parent)
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
                            v.isChecked = m.params?.get("checked")?.asBoolean ?: false
                            v.freezesText = true
                            Util.setClickListener(v, aid, true, eventQueue)
                            V0.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createNestedScrollView") {
                        val v = NestedScrollView(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            if (m.params?.get("fillviewport")?.asBoolean == true) {
                                v.isFillViewport = true
                            }
                            if (m.params?.get("nobar")?.asBoolean == true) {
                                v.scrollBarSize = 0
                            }
                            V0.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createHorizontalScrollView") {
                        val v = HorizontalScrollView(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            if (m.params?.get("fillviewport")?.asBoolean == true) {
                                v.isFillViewport = true
                            }
                            if (m.params?.get("nobar")?.asBoolean == true) {
                                v.scrollBarSize = 0
                            }
                            V0.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createRadioGroup") {
                        val v = RadioGroup(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            Util.setCheckedListener(v, aid, eventQueue)
                            V0.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createRadioButton") {
                        val v = RadioButton(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            v.text = m.params?.get("text")?.asString
                            v.freezesText = true
                            v.isChecked = m.params?.get("checked")?.asBoolean ?: false
                            V0.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createSpinner") {
                        val v = Spinner(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            Util.setSpinnerListener(v, aid, eventQueue)
                            V0.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createToggleButton") {
                        var id = -1
                        Util.runOnUIThreadBlocking {
                            val v = ToggleButton(app)
                            id = Util.generateViewIDRaw(rand, o.usedIds)
                            v.id = id
                            v.freezesText = true
                            v.isChecked = m.params?.get("checked")?.asBoolean ?: false
                            Util.setClickListener(v, aid, true, eventQueue)
                            V0.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createSwitch") {
                        var id = -1
                        Util.runOnUIThreadBlocking {
                            val v = SwitchCompat(app)
                            id = Util.generateViewIDRaw(rand, o.usedIds)
                            v.id = id
                            v.text = m.params?.get("text")?.asString
                            v.freezesText = true
                            v.isChecked = m.params?.get("checked")?.asBoolean ?: false
                            Util.setClickListener(v, aid, true, eventQueue)
                            V0.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createProgressBar") {
                        var id = -1
                        Util.runOnUIThreadBlocking {
                            val v = ProgressBar(app, null, android.R.attr.progressBarStyleHorizontal)
                            id = Util.generateViewIDRaw(rand, o.usedIds)
                            v.id = id
                            V0.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createSwipeRefreshLayout") {
                        var id = -1
                        Util.runOnUIThreadBlocking {
                            val v = SwipeRefreshLayout(app)
                            id = Util.generateViewIDRaw(rand, o.usedIds)
                            v.id = id
                            Util.setRefreshListener(v, aid, eventQueue)
                            V0.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createTabLayout") {
                        var id = -1
                        Util.runOnUIThreadBlocking {
                            val v = TabLayout(app)
                            id = Util.generateViewIDRaw(rand, o.usedIds)
                            v.id = id
                            Util.setTabSelectedListener(v, aid, eventQueue)
                            V0.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createGridLayout") {
                        var id = -1
                        Util.runOnUIThreadBlocking {
                            val v = GridLayout(app)
                            val rows: Int = m.params!!["rows"]!!.asInt
                            val cols: Int = m.params!!["cols"]!!.asInt
                            v.rowCount = rows
                            v.columnCount = cols
                            id = Util.generateViewIDRaw(rand, o.usedIds)
                            v.id = id
                            V0.setViewOverlay(o, v, parent)
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
                override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
                    return InputWrapper(super.onCreateInputConnection(outAttrs), true)
                }

                override fun isSuggestionsEnabled(): Boolean {
                    return false
                }
                
                override fun onTextContextMenuItem(id: Int): Boolean {
                    val consumed: Boolean = super.onTextContextMenuItem(id)
                    val args = HashMap<String, Any>()
                    args["aid"] = aid
                    args["id"] = getId()
                    println("contextmenu")
                    when (id) {
                        android.R.id.cut -> {
                            println("cut")
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
                            println("paste")
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

                    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                        println(text)
                        return super.commitText(text, newCursorPosition)
                    }

                    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                        println("delete $beforeLength $afterLength")
                        return super.deleteSurroundingText(beforeLength, afterLength)
                    }

                    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
                        println(text)
                        return true
                        //return super.setComposingText(text, newCursorPosition)
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