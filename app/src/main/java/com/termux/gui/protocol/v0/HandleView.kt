package com.termux.gui.protocol.v0

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.termux.gui.ConnectionHandler
import com.termux.gui.Util
import java.io.DataOutputStream
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class HandleView {
    companion object {
        fun handleView(m: ConnectionHandler.Message, activities: MutableMap<String, V0.ActivityState>, widgets: MutableMap<Int, V0.WidgetRepresentation>,
                       overlays: MutableMap<String, V0.Overlay>, rand: Random, out: DataOutputStream,
                       app: Context, eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) {
            operator fun Regex.contains(text: String?): Boolean = if (text == null) false else this.matches(text)
            when (m.method) {
                in Regex("create.*") -> {
                    if (m.params != null) {
                        Create.handleCreateMessage(m, activities, widgets, overlays, rand, out, app, eventQueue)
                    }
                    return
                }
                "showCursor" -> {
                    val aid = m.params?.get("aid")?.asString
                    val id = m.params?.get("id")?.asInt
                    val show = m.params?.get("show")?.asBoolean ?: true
                    val a = activities[aid]
                    val o = overlays[aid]
                    if (a != null && id != null) {
                        V0.runOnUIThreadActivityStarted(a) {
                            it.findViewReimplemented<EditText>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.isCursorVisible = show
                        }
                    }
                    if (o != null && id != null) {
                        Util.runOnUIThreadBlocking {
                            o.root.findViewReimplemented<EditText>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.isCursorVisible = show
                        }
                    }
                    return
                }
                "deleteView" -> {
                    val aid = m.params?.get("aid")?.asString
                    val id = m.params?.get("id")?.asInt
                    val a = activities[aid]
                    val o = overlays[aid]
                    if (a != null && id != null) {
                        V0.runOnUIThreadActivityStarted(a) {
                            
                            Util.removeViewRecursive(it.findViewReimplemented(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt), it.usedIds, it.recyclerviews)
                        }
                    }
                    if (o != null && id != null) {
                        Util.runOnUIThreadBlocking {
                            Util.removeViewRecursive(o.root.findViewReimplemented(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt), o.usedIds, o.recyclerviews)
                        }
                    }
                    return
                }
                "deleteChildren" -> {
                    val aid = m.params?.get("aid")?.asString
                    val id = m.params?.get("id")?.asInt
                    val a = activities[aid]
                    val o = overlays[aid]
                    if (a != null && id != null) {
                        V0.runOnUIThreadActivityStarted(a) {
                            val v = it.findViewReimplemented<ViewGroup>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                            if (v != null) {
                                while (v.childCount > 0) {
                                    Util.removeViewRecursive(v.getChildAt(0), it.usedIds, it.recyclerviews)
                                }
                            }
                        }
                    }
                    if (o != null && id != null) {
                        Util.runOnUIThreadBlocking {
                            val v = o.root.findViewReimplemented<ViewGroup>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                            if (v != null) {
                                while (v.childCount > 0) {
                                    Util.removeViewRecursive(v.getChildAt(0), o.usedIds, o.recyclerviews)
                                }
                            }
                        }
                    }
                    return
                }
                "setTextSize" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asString
                        val id = m.params?.get("id")?.asInt
                        val size = m.params?.get("size")?.asInt
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null && size != null && size > 0) {
                            if (a != null) {
                                V0.runOnUIThreadActivityStarted(a) {
                                    val tv = it.findViewReimplemented<TextView>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                                    tv?.setTextSize(TypedValue.COMPLEX_UNIT_SP, size.toFloat())
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    o.root.findViewReimplemented<TextView>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.setTextSize(TypedValue.COMPLEX_UNIT_SP, size.toFloat())
                                }
                            }
                        }
                    }
                    return
                }
                "setImage" -> {
                    val aid = m.params?.get("aid")?.asString
                    val a = activities[aid]
                    val id = m.params?.get("id")?.asInt
                    val img = m.params?.get("img")?.asString
                    val o = overlays[aid]
                    if (img != null && id != null) {
                        if (a != null) {
                            V0.runOnUIThreadActivityStarted(a) {
                                val bin = Base64.decode(img, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(bin, 0, bin.size)
                                it.findViewReimplemented<ImageView>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.setImageBitmap(bitmap)
                            }
                        }
                        if (o != null) {
                            val bin = Base64.decode(img, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bin, 0, bin.size)
                            Util.runOnUIThreadBlocking {
                                o.root.findViewReimplemented<ImageView>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.setImageBitmap(bitmap)
                            }
                        }
                    }
                    return
                }
                "setMargin" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asString
                        val id = m.params?.get("id")?.asInt
                        val margin = m.params?.get("margin")?.asInt
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null && margin != null) {
                            if (a != null) {
                                V0.runOnUIThreadActivityStarted(a) {
                                    val mar = Util.toPX(it, margin)
                                    val v = it.findViewReimplemented<View>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
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
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    val mar = Util.toPX(app, margin)
                                    val v = o.root.findViewReimplemented<View>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
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
                    }
                    return
                }
                "setLinearLayoutParams" -> {
                    val aid = m.params?.get("aid")?.asString
                    val a = activities[aid]
                    val id = m.params?.get("id")?.asInt
                    val weight = m.params?.get("weight")?.asInt
                    val o = overlays[aid]
                    if (id != null && weight != null) {
                        if (a != null) {
                            V0.runOnUIThreadActivityStarted(a) {
                                val v = it.findViewReimplemented<View>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                                val p = v?.layoutParams as? LinearLayout.LayoutParams
                                if (p != null) {
                                    p.weight = weight.toFloat()
                                    v.layoutParams = p
                                }
                            }
                        }
                        if (o != null) {
                            Util.runOnUIThreadBlocking {
                                val v = o.root.findViewReimplemented<View>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                                val p = v?.layoutParams as? LinearLayout.LayoutParams
                                if (p != null) {
                                    p.weight = weight.toFloat()
                                    v.layoutParams = p
                                }
                            }
                        }
                    }
                    return
                }
                "setWidth" -> {
                    setDimension(m, activities, overlays, app)
                    return
                }
                "setHeight" -> {
                    setDimension(m, activities, overlays, app)
                    return
                }
                "setText" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asString
                        val id = m.params?.get("id")?.asInt
                        val text = m.params?.get("text")?.asString
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null) {
                            if (a != null) {
                                V0.runOnUIThreadActivityStarted(a) {
                                    val tv = it.findViewReimplemented<TextView>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                                    tv?.text = text
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    o.root.findViewReimplemented<TextView>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.text = text
                                }
                            }
                        }
                    }
                    return
                }
                "getText" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asString
                        val id = m.params?.get("id")?.asInt
                        var text: String? = null
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null) {
                            if (a != null) {
                                V0.runOnUIThreadActivityStartedBlocking(a) {
                                    text = it.findViewReimplemented<TextView>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.text?.toString()
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    text = o.root.findViewReimplemented<TextView>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.text?.toString()
                                }
                            }
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(text ?: ""))
                    } else {
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(""))
                    }
                    return
                }
            }
        }

        private fun setDimension(m: ConnectionHandler.Message, activities: MutableMap<String, V0.ActivityState>, overlays: MutableMap<String, V0.Overlay>, app: Context) {
            val aid = m.params?.get("aid")?.asString
            val a = activities[aid]
            val id = m.params?.get("id")?.asInt
            val width = m.params?.get("width")
            val height = m.params?.get("height")
            val o = overlays[aid]
            val el: JsonElement? = width ?: height
            fun set(pa: ViewGroup.LayoutParams, p: JsonPrimitive) {
                if (p.isString) {
                    if (p.asString == "MATCH_PARENT") {
                        if (width != null) {
                            pa.width = ViewGroup.LayoutParams.MATCH_PARENT
                        } else {
                            pa.height = ViewGroup.LayoutParams.MATCH_PARENT
                        }
                    }
                    if (p.asString == "WRAP_CONTENT") {
                        if (width != null) {
                            pa.width = ViewGroup.LayoutParams.WRAP_CONTENT
                        } else {
                            pa.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        }
                    }
                }
                if (p.isNumber) {
                    if (width != null) {
                        pa.width = Util.toPX(app, p.asInt)
                    } else {
                        pa.height = Util.toPX(app, p.asInt)
                    }
                }
            }
            if (id != null && el?.isJsonPrimitive == true) {
                val p = el.asJsonPrimitive
                if (a != null) {
                    V0.runOnUIThreadActivityStarted(a) {
                        val v = it.findViewReimplemented<View>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        val pa = v?.layoutParams
                        if (pa != null) {
                            set(pa, p)
                            v.layoutParams = pa
                        }
                    }
                }
                if (o != null) {
                    Util.runOnUIThreadBlocking {
                        val v = o.root.findViewReimplemented<View>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)
                        val pa = v?.layoutParams
                        if (pa != null) {
                            set(pa, p)
                            v.layoutParams = pa
                        }
                        v?.layoutParams = pa
                    }
                }
            }
        }
    }
}