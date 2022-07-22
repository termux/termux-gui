package com.termux.gui.protocol.json.v0

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.NestedScrollView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.termux.gui.ConnectionHandler
import com.termux.gui.Util
import com.termux.gui.protocol.shared.v0.DataClasses
import com.termux.gui.protocol.shared.v0.V0Shared
import com.termux.gui.views.SnappingHorizontalScrollView
import com.termux.gui.views.SnappingNestedScrollView
import com.termux.gui.views.WrappedEditText
import java.io.DataOutputStream
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class Create {
    companion object {
        fun handleCreateMessage(m: ConnectionHandler.Message, activities: MutableMap<String, DataClasses.ActivityState>,
                                overlays: MutableMap<String, DataClasses.Overlay>, rand: Random, out: DataOutputStream, app: Context,
                                eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) {
            val aid = m.params?.get("aid")?.asString
            val parent = m.params?.get("parent")?.asInt
            val a = activities[aid]
            val o = overlays[aid]
            if (aid != null) {
                if (a != null) {
                    if (m.method == "createTextView") {
                        var id = -1
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
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
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
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
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
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
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
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
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
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
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
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
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
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
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
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
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
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
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
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
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
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
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
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
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
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
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
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
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
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
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
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
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
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
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
                            val v = TabLayout(it)
                            id = Util.generateViewID(rand, it)
                            v.id = id
                            Util.setTabSelectedListener(v, aid, eventQueue)
                            Util.setViewActivity(it, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createGridLayout") {
                        var id = -1
                        V0Shared.runOnUIThreadActivityStartedBlocking(a) {
                            val v = GridLayout(app)
                            val rows: Int = m.params!!["rows"]!!.asInt
                            val cols: Int = m.params!!["cols"]!!.asInt
                            v.rowCount = rows
                            v.columnCount = cols
                            id = Util.generateViewID(rand, it)
                            v.id = id
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
                            V0Shared.setViewOverlay(o, v, parent)
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
                            V0Shared.setViewOverlay(o, v, parent)
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
                            V0Shared.setViewOverlay(o, v, parent)
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
                            V0Shared.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createImageView") {
                        val v = ImageView(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            V0Shared.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createSpace") {
                        val v = Space(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            V0Shared.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                    if (m.method == "createFrameLayout") {
                        val v = FrameLayout(app)
                        val id = Util.generateViewIDRaw(rand, o.usedIds)
                        Util.runOnUIThreadBlocking {
                            v.id = id
                            V0Shared.setViewOverlay(o, v, parent)
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
                            V0Shared.setViewOverlay(o, v, parent)
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
                            V0Shared.setViewOverlay(o, v, parent)
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
                            V0Shared.setViewOverlay(o, v, parent)
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
                            V0Shared.setViewOverlay(o, v, parent)
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
                            V0Shared.setViewOverlay(o, v, parent)
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
                            V0Shared.setViewOverlay(o, v, parent)
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
                            V0Shared.setViewOverlay(o, v, parent)
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
                            V0Shared.setViewOverlay(o, v, parent)
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
                            V0Shared.setViewOverlay(o, v, parent)
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
                            V0Shared.setViewOverlay(o, v, parent)
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
                            V0Shared.setViewOverlay(o, v, parent)
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
                            V0Shared.setViewOverlay(o, v, parent)
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(id))
                        return
                    }
                }
            }
        }

        private fun getCustomEditText(it : Context, aid: String, eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>): EditText {
            return WrappedEditText(it, aid, eventQueue)
        }
    }
}