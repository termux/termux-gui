package com.termux.gui.protocol.json.v0

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.*
import androidx.core.widget.NestedScrollView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.termux.gui.*
import com.termux.gui.protocol.shared.v0.DataClasses
import com.termux.gui.protocol.shared.v0.GUIWebViewClient
import com.termux.gui.protocol.shared.v0.V0Shared
import java.io.DataOutputStream
import java.util.*
import java.util.concurrent.LinkedBlockingQueue


class HandleView {
    companion object {
        @SuppressLint("WebViewApiAvailability")
        fun handleView(m: ConnectionHandler.Message, activities: MutableMap<Int, DataClasses.ActivityState>, overlays: MutableMap<Int, DataClasses.Overlay>,
                       rand: Random, out: DataOutputStream, app: Context,
                       eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) : Boolean {
            operator fun Regex.contains(text: String?): Boolean = if (text == null) false else this.matches(text)
            when (m.method) {
                in Regex("create.*") -> {
                    if (m.params != null) {
                        Create.handleCreateMessage(m, activities, overlays, rand, out, app, eventQueue)
                    }
                    return true
                }
                "showCursor" -> {
                    val aid = m.params?.get("aid")?.asInt
                    val id = m.params?.get("id")?.asInt
                    val show = m.params?.get("show")?.asBoolean ?: true
                    val a = activities[aid]
                    val o = overlays[aid]
                    if (a != null && id != null) {
                        V0Shared.runOnUIThreadActivityStarted(a) {
                            it.findViewReimplemented<EditText>(id)?.isCursorVisible = show
                        }
                    }
                    if (o != null && id != null) {
                        Util.runOnUIThreadBlocking {
                            o.root.findViewReimplemented<EditText>(id)?.isCursorVisible = show
                        }
                    }
                    return true
                }
                "deleteView" -> {
                    val aid = m.params?.get("aid")?.asInt
                    val id = m.params?.get("id")?.asInt
                    val a = activities[aid]
                    val o = overlays[aid]
                    if (a != null && id != null) {
                        V0Shared.runOnUIThreadActivityStarted(a) {
                            Util.removeViewRecursive(it.findViewReimplemented(id), it.usedIds)
                        }
                    }
                    if (o != null && id != null) {
                        Util.runOnUIThreadBlocking {
                            Util.removeViewRecursive(o.root.findViewReimplemented(id), o.usedIds)
                        }
                    }
                    return true
                }
                "deleteChildren" -> {
                    val aid = m.params?.get("aid")?.asInt
                    val id = m.params?.get("id")?.asInt
                    val a = activities[aid]
                    val o = overlays[aid]
                    if (a != null && id != null) {
                        V0Shared.runOnUIThreadActivityStarted(a) {
                            val v = it.findViewReimplemented<ViewGroup>(id)
                            if (v != null) {
                                while (v.childCount > 0) {
                                    Util.removeViewRecursive(v.getChildAt(0), it.usedIds)
                                }
                            }
                        }
                    }
                    if (o != null && id != null) {
                        Util.runOnUIThreadBlocking {
                            val v = o.root.findViewReimplemented<ViewGroup>(id)
                            if (v != null) {
                                while (v.childCount > 0) {
                                    Util.removeViewRecursive(v.getChildAt(0), o.usedIds)
                                }
                            }
                        }
                    }
                    return true
                }
                "setTextSize" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val size = m.params?.get("size")?.asInt
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null && size != null && size > 0) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) {
                                    val tv = it.findViewReimplemented<TextView>(id)
                                    tv?.setTextSize(TypedValue.COMPLEX_UNIT_SP, size.toFloat())
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    o.root.findViewReimplemented<TextView>(id)?.setTextSize(TypedValue.COMPLEX_UNIT_SP, size.toFloat())
                                }
                            }
                        }
                    }
                    return true
                }
                "setImage" -> {
                    val aid = m.params?.get("aid")?.asInt
                    val a = activities[aid]
                    val id = m.params?.get("id")?.asInt
                    val img = m.params?.get("img")?.asString
                    val o = overlays[aid]
                    if (img != null && id != null) {
                        if (a != null) {
                            V0Shared.runOnUIThreadActivityStarted(a) {
                                val bin = Base64.decode(img, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(bin, 0, bin.size)
                                it.findViewReimplemented<ImageView>(id)?.setImageBitmap(bitmap)
                            }
                        }
                        if (o != null) {
                            val bin = Base64.decode(img, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bin, 0, bin.size)
                            Util.runOnUIThreadBlocking {
                                o.root.findViewReimplemented<ImageView>(id)?.setImageBitmap(bitmap)
                            }
                        }
                    }
                    return true
                }
                "setMargin" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val margin = m.params?.get("margin")?.asInt
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null && margin != null) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) {
                                    val mar = Util.toPX(it, margin)
                                    val v = it.findViewReimplemented<View>(id)
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
                                    val v = o.root.findViewReimplemented<View>(id)
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
                    return true
                }
                "setLinearLayoutParams" -> {
                    val aid = m.params?.get("aid")?.asInt
                    val a = activities[aid]
                    val id = m.params?.get("id")?.asInt
                    var weight = m.params?.get("weight")
                    if (weight is JsonNull)
                        weight = null
                    val o = overlays[aid]
                    var position = m.params?.get("position")
                    if (position is JsonNull)
                        position = null
                    if (id != null && (weight != null || position != null)) {
                        if (a != null) {
                            V0Shared.runOnUIThreadActivityStarted(a) {
                                val v = it.findViewReimplemented<View>(id)
                                val p = v?.layoutParams as? LinearLayout.LayoutParams
                                if (p != null) {
                                    if (weight != null) {
                                        p.weight = weight.asInt.toFloat()
                                    }
                                    v.layoutParams = p
                                    if (position != null) {
                                        val parent = v.parent as? LinearLayout
                                        parent?.removeView(v)
                                        parent?.addView(v, position.asInt)
                                    }
                                }
                            }
                        }
                        if (o != null) {
                            Util.runOnUIThreadBlocking {
                                val v = o.root.findViewReimplemented<View>(id)
                                val p = v?.layoutParams as? LinearLayout.LayoutParams
                                if (p != null) {
                                    if (weight != null) {
                                        p.weight = weight.asInt.toFloat()
                                    }
                                    v.layoutParams = p
                                    if (position != null) {
                                        val parent = v.parent as? LinearLayout
                                        parent?.removeView(v)
                                        parent?.addView(v, position.asInt)
                                    }
                                }
                            }
                        }
                    }
                    return true
                }
                "setGridLayoutParams" -> {
                    val aid = m.params?.get("aid")?.asInt
                    val a = activities[aid]
                    val id = m.params?.get("id")?.asInt
                    val row = m.params?.get("row")?.asInt
                    val col = m.params?.get("col")?.asInt
                    val rowsize = m.params?.get("rowsize")?.asInt
                    val colsize = m.params?.get("colsize")?.asInt
                    val alignmentrow = m.params?.get("alignmentrow")?.asString
                    val alignmentcol = m.params?.get("alignmentcol")?.asString
                    val align: Map<String, GridLayout.Alignment> = mapOf("top" to GridLayout.TOP, "bottom" to GridLayout.BOTTOM, "left" to GridLayout.LEFT, "right" to GridLayout.RIGHT, "center" to GridLayout.CENTER, "baseline" to GridLayout.BASELINE, "fill" to GridLayout.FILL)
                    val o = overlays[aid]
                    if (id != null && row != null && col != null && rowsize != null && colsize != null) {
                        if (a != null) {
                            V0Shared.runOnUIThreadActivityStarted(a) {
                                val v = it.findViewReimplemented<View>(id)
                                val p = v?.layoutParams as? GridLayout.LayoutParams
                                if (p != null) {
                                    p.rowSpec = GridLayout.spec(row, rowsize, align[alignmentrow] ?: GridLayout.CENTER)
                                    p.columnSpec = GridLayout.spec(col, colsize, align[alignmentcol] ?: GridLayout.CENTER)
                                }
                                v?.layoutParams = p
                            }
                        }
                        if (o != null) {
                            Util.runOnUIThreadBlocking {
                                val v = o.root.findViewReimplemented<View>(id)
                                val p = v?.layoutParams as? GridLayout.LayoutParams
                                if (p != null) {
                                    p.rowSpec = GridLayout.spec(row, rowsize, align[alignmentrow] ?: GridLayout.CENTER)
                                    p.columnSpec = GridLayout.spec(col, colsize, align[alignmentcol] ?: GridLayout.CENTER)
                                }
                                v?.layoutParams = p
                            }
                        }
                    }
                    return true
                }
                "setViewLocation" -> {
                    val aid = m.params?.get("aid")?.asInt
                    val a = activities[aid]
                    val id = m.params?.get("id")?.asInt
                    val x = m.params?.get("x")?.asInt
                    val y = m.params?.get("y")?.asInt
                    val dp = m.params?.get("dp")?.asBoolean
                    val top = m.params?.get("top")?.asBoolean
                    val o = overlays[aid]
                    if (id != null) {
                        if (a != null) {
                            V0Shared.runOnUIThreadActivityStarted(a) {
                                val v = it.findViewReimplemented<View>(id)
                                if (x != null && y != null) {
                                    v?.x = if (dp == true) Util.toPX(it, x).toFloat() else x.toFloat()
                                    v?.y = if (dp == true) Util.toPX(it, y).toFloat() else y.toFloat()
                                }
                                if (top == true) {
                                    v?.bringToFront()
                                }
                            }
                        }
                        if (o != null) {
                            Util.runOnUIThreadBlocking {
                                val v = o.root.findViewReimplemented<View>(id)
                                if (x != null && y != null) {
                                    v?.x = if (dp == true) Util.toPX(o.context, x).toFloat() else x.toFloat()
                                    v?.y = if (dp == true) Util.toPX(o.context, y).toFloat() else y.toFloat()
                                }
                                if (top == true) {
                                    v?.bringToFront()
                                }
                            }
                        }
                    }
                    return true
                }
                "setWidth" -> {
                    setDimension(m, activities, overlays, app)
                    return true
                }
                "setHeight" -> {
                    setDimension(m, activities, overlays, app)
                    return true
                }
                "getDimensions" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) {
                                    val v = it.findViewReimplemented<View>(id)
                                    if (v != null) {
                                        Util.sendMessage(out, ConnectionHandler.gson.toJson(arrayOf(v.width, v.height)))
                                    } else {
                                        Util.sendMessage(out, ConnectionHandler.gson.toJson(arrayOf(0, 0)))
                                    }
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    val v = o.root.findViewReimplemented<View>(id)
                                    if (v != null) {
                                        Util.sendMessage(out, ConnectionHandler.gson.toJson(arrayOf(v.width, v.height)))
                                    } else {
                                        Util.sendMessage(out, ConnectionHandler.gson.toJson(arrayOf(0, 0)))
                                    }
                                }
                            }
                        }
                    }
                    return true
                }
                "setText" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val text = m.params?.get("text")?.asString
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) {
                                    val tv = it.findViewReimplemented<TextView>(id)
                                    tv?.text = text
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    o.root.findViewReimplemented<TextView>(id)?.text = text
                                }
                            }
                        }
                    }
                    return true
                }
                "setGravity" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val horizontal = m.params?.get("horizontal")?.asInt
                        val vertical = m.params?.get("vertical")?.asInt
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null && horizontal != null && vertical != null) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) {
                                    val tv = it.findViewReimplemented<TextView>(id)
                                    var grav = when (horizontal) {
                                        0 -> Gravity.START
                                        2 -> Gravity.END
                                        else -> Gravity.CENTER_HORIZONTAL
                                    }
                                    grav = grav or when (vertical) {
                                        0 -> Gravity.TOP
                                        2 -> Gravity.BOTTOM
                                        else -> Gravity.CENTER_VERTICAL
                                    }
                                    tv?.gravity = grav
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    val tv = o.root.findViewReimplemented<TextView>(id)
                                    var grav = when (horizontal) {
                                        0 -> Gravity.START
                                        2 -> Gravity.END
                                        else -> Gravity.CENTER_HORIZONTAL
                                    }
                                    grav = grav or when (vertical) {
                                        0 -> Gravity.TOP
                                        2 -> Gravity.BOTTOM
                                        else -> Gravity.CENTER_VERTICAL
                                    }
                                    tv?.gravity = grav
                                }
                            }
                        }
                    }
                    return true
                }
                "setBackgroundColor" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val color = m.params?.get("color")?.asInt
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null && color != null) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) {
                                    it.findViewReimplemented<View>(id)?.backgroundTintList = ColorStateList.valueOf(color)
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    o.root.findViewReimplemented<View>(id)?.backgroundTintList = ColorStateList.valueOf(color)
                                }
                            }
                        }
                    }
                    return true
                }
                "setTextColor" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val color = m.params?.get("color")?.asInt
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null && color != null) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) {
                                    it.findViewReimplemented<TextView>(id)?.setTextColor(color)
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    o.root.findViewReimplemented<TextView>(id)?.setTextColor(color)
                                }
                            }
                        }
                    }
                    return true
                }
                "setProgress" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val progress = m.params?.get("progress")?.asInt
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null && progress != null) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) {
                                    it.findViewReimplemented<ProgressBar>(id)?.progress = progress
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    o.root.findViewReimplemented<ProgressBar>(id)?.progress = progress
                                }
                            }
                        }
                    }
                    return true
                }
                "setRefreshing" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val refresh = m.params?.get("refresh")?.asBoolean
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null && refresh != null) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) {
                                    it.findViewReimplemented<SwipeRefreshLayout>(id)?.isRefreshing = refresh
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    o.root.findViewReimplemented<SwipeRefreshLayout>(id)?.isRefreshing = refresh
                                }
                            }
                        }
                    }
                    return true
                }
                "getText" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        var text: String? = null
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStartedBlocking(a) {
                                    text = it.findViewReimplemented<TextView>(id)?.text?.toString()
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    text = o.root.findViewReimplemented<TextView>(id)?.text?.toString()
                                }
                            }
                        }
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(text ?: ""))
                    } else {
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(""))
                    }
                    return true
                }
                "setChecked" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) {
                                    it.findViewReimplemented<CompoundButton>(id)?.isChecked = m.params?.get("checked")?.asBoolean ?: false
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    o.root.findViewReimplemented<CompoundButton>(id)?.isChecked = m.params?.get("checked")?.asBoolean ?: false
                                }
                            }
                        }
                    }
                    return true
                }
                "requestFocus" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) {
                                    val v = it.findViewReimplemented<View>(id)
                                    if (v != null) {
                                        v.requestFocus()
                                        if (m.params?.get("forcesoft")?.asBoolean == true) {
                                            val im = it.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                            im?.showSoftInput(v, 0)
                                        }
                                    }
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    val v = o.root.findViewReimplemented<View>(id)
                                    if (v != null) {
                                        v.requestFocus()
                                        if (m.params?.get("forcesoft")?.asBoolean == true) {
                                            val im = App.APP?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                            im?.showSoftInput(v, 0)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return true
                }
                "setScrollPosition" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val x = m.params?.get("x")?.asInt
                        val y = m.params?.get("y")?.asInt
                        val soft = m.params?.get("soft")?.asBoolean
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null && x != null && y != null) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) {
                                    val v = it.findViewReimplemented<View>(id)
                                    if (v is NestedScrollView || v is HorizontalScrollView) {
                                        if (soft == true) {
                                            if (v is NestedScrollView) {
                                                v.smoothScrollTo(x, y)
                                            } else {
                                                if (v is HorizontalScrollView) {
                                                    v.smoothScrollTo(x, y)
                                                } else {
                                                    v.scrollTo(x, y)
                                                }
                                            }
                                        } else {
                                            v.scrollTo(x, y)
                                        }
                                    }
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    val v = o.root.findViewReimplemented<View>(id)
                                    if (v is NestedScrollView || v is HorizontalScrollView) {
                                        if (soft == true) {
                                            if (v is NestedScrollView) {
                                                v.smoothScrollTo(x, y)
                                            } else {
                                                if (v is HorizontalScrollView) {
                                                    v.smoothScrollTo(x, y)
                                                } else {
                                                    v.scrollTo(x, y)
                                                }
                                            }
                                        } else {
                                            v.scrollTo(x, y)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return true
                }
                "getScrollPosition" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) {
                                    val v = it.findViewReimplemented<View>(id)
                                    if (v is NestedScrollView || v is HorizontalScrollView) {
                                        Util.sendMessage(out, ConnectionHandler.gson.toJson(arrayOf(v.scrollX, v.scrollY)))
                                    } else {
                                        Util.sendMessage(out, ConnectionHandler.gson.toJson(arrayOf(0, 0)))
                                    }
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    val v = o.root.findViewReimplemented<View>(id)
                                    if (v is NestedScrollView || v is HorizontalScrollView) {
                                        Util.sendMessage(out, ConnectionHandler.gson.toJson(arrayOf(v.scrollX, v.scrollY)))
                                    } else {
                                        Util.sendMessage(out, ConnectionHandler.gson.toJson(arrayOf(0, 0)))
                                    }
                                }
                            }
                        }
                    }
                    return true
                }
                "setList" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val list = m.params?.get("list")?.asJsonArray
                        val options = LinkedList<String>()
                        if (list != null) {
                            for (a in list) {
                                if (! a.isJsonPrimitive || ! a.asJsonPrimitive.isString) {
                                    return true
                                } else {
                                    options.add(a.asString)
                                }
                            }
                        } else {
                            return true
                        }
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStartedBlocking(a) {
                                    val v = it.findViewReimplemented<View>(id)
                                    if (v is Spinner)
                                        v.adapter = ArrayAdapter(it, R.layout.spinner_text, options)
                                    if (v is TabLayout) {
                                        v.removeAllTabs()
                                        for (tab in options) {
                                            val t = v.newTab()
                                            t.text = tab
                                            v.addTab(t)
                                        }
                                    }
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    val v = o.root.findViewReimplemented<View>(id)
                                    if (v is Spinner)
                                        v.adapter = ArrayAdapter(app, R.layout.spinner_text, options)
                                    if (v is TabLayout) {
                                        v.removeAllTabs()
                                        for (tab in options) {
                                            val t = TabLayout.Tab()
                                            t.text = tab
                                            v.addTab(t)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return true
                }
                "setVisibility" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val vis = m.params?.get("vis")?.asInt
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null && vis != null && vis >= 0 && vis <= 2) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) {
                                    it.findViewReimplemented<View>(id)?.visibility = when (vis) {
                                        0 -> View.GONE
                                        1 -> View.INVISIBLE
                                        2 -> View.VISIBLE
                                        else -> {View.VISIBLE}
                                    }
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    o.root.findViewReimplemented<View>(id)?.visibility = when (vis) {
                                        0 -> View.GONE
                                        1 -> View.INVISIBLE
                                        2 -> View.VISIBLE
                                        else -> {View.VISIBLE}
                                    }
                                }
                            }
                        }
                    }
                    return true
                }
                "setClickable" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val clickable = m.params?.get("clickable")?.asBoolean
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null && clickable != null) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) {
                                    it.findViewReimplemented<View>(id)?.isClickable = clickable
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    o.root.findViewReimplemented<View>(id)?.isClickable = clickable
                                }
                            }
                        }
                    }
                    return true
                }
                "selectTab" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val tab = m.params?.get("tab")?.asInt
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null && tab != null) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) { it ->
                                    it.findViewReimplemented<TabLayout>(id)?.let { it.selectTab(it.getTabAt(tab)) }
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    o.root.findViewReimplemented<TabLayout>(id)?.let { it.selectTab(it.getTabAt(tab)) }
                                }
                            }
                        }
                    }
                    return true
                }
                "selectItem" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val item = m.params?.get("item")?.asInt
                        val a = activities[aid]
                        val o = overlays[aid]
                        if (id != null && item != null) {
                            if (a != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) {
                                    it.findViewReimplemented<Spinner>(id)?.setSelection(item)
                                }
                            }
                            if (o != null) {
                                Util.runOnUIThreadBlocking {
                                    o.root.findViewReimplemented<Spinner>(id)?.setSelection(item)
                                }
                            }
                        }
                    }
                    return true
                }
                "allowJavascript" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val a = activities[aid]
                        val allow = m.params?.get("allow")?.asBoolean
                        if (a != null && id != null && allow != null) {
                            if (allow == true) {
                                if (! Settings.instance.javascript) {
                                    val data = Thread.currentThread().id.toString()
                                    val r = GUIWebViewJavascriptDialog.Companion.Request()
                                    GUIWebViewJavascriptDialog.requestMap[data] = r
                                    val i = Intent(app, GUIWebViewJavascriptDialog::class.java)
                                    i.data = Uri.parse(data)
                                    V0Shared.runOnUIThreadActivityStarted(a) {
                                        it.startActivity(i)
                                    }
                                    synchronized(r.monitor) {
                                        while (r.allow == null)
                                            r.monitor.wait()
                                    }
                                    GUIWebViewJavascriptDialog.requestMap.remove(data)
                                    if (r.allow == true) {
                                        V0Shared.runOnUIThreadActivityStarted(a) { it ->
                                            //noinspection SetJavaScriptEnabled
                                            it.findViewReimplemented<WebView>(id)
                                                ?.let { it.settings.javaScriptEnabled = true }
                                        }
                                        Util.sendMessage(out, ConnectionHandler.gson.toJson(true))
                                    } else {
                                        Util.sendMessage(out, ConnectionHandler.gson.toJson(false))
                                    }
                                } else {
                                    V0Shared.runOnUIThreadActivityStarted(a) { it ->
                                        //noinspection SetJavaScriptEnabled
                                        it.findViewReimplemented<WebView>(id)
                                            ?.let { it.settings.javaScriptEnabled = true }
                                    }
                                    Util.sendMessage(out, ConnectionHandler.gson.toJson(true))
                                }
                            } else {
                                V0Shared.runOnUIThreadActivityStarted(a) { it ->
                                    it.findViewReimplemented<WebView>(id)?.let { it.settings.javaScriptEnabled = false }
                                }
                                Util.sendMessage(out, ConnectionHandler.gson.toJson(false))
                            }
                        }
                    }
                    return true
                }
                "allowContentURI" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val a = activities[aid]
                        val allow = m.params?.get("allow")?.asBoolean
                        if (a != null && id != null && allow != null) {
                            V0Shared.runOnUIThreadActivityStarted(a) { it ->
                                it.findViewReimplemented<WebView>(id)?.let { it.settings.allowContentAccess = allow }
                            }
                        }
                    }
                    return true
                }
                "setData" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val a = activities[aid]
                        val doc = m.params?.get("doc")?.asString
                        if (a != null && id != null && doc != null) {
                            V0Shared.runOnUIThreadActivityStarted(a) {
                                it.findViewReimplemented<WebView>(id)?.loadData(doc, null, null)
                            }
                        }
                    }
                    return true
                }
                "loadURI" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val a = activities[aid]
                        val uri = m.params?.get("uri")?.asString
                        if (a != null && id != null && uri != null) {
                            V0Shared.runOnUIThreadActivityStarted(a) {
                                it.findViewReimplemented<WebView>(id)?.loadUrl(uri)
                            }
                        }
                    }
                    return true
                }
                "allowNavigation" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (m.params != null) {
                            val aid = m.params?.get("aid")?.asInt
                            val id = m.params?.get("id")?.asInt
                            val a = activities[aid]
                            val allow = m.params?.get("allow")?.asBoolean
                            if (a != null && id != null && allow != null) {
                                V0Shared.runOnUIThreadActivityStarted(a) { it ->
                                    it.findViewReimplemented<WebView>(id)?.let { (it.webViewClient as GUIWebViewClient).allowNavigation = allow }
                                }
                            }
                        }
                    }
                    return true
                }
                "goBack" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val a = activities[aid]
                        if (a != null && id != null) {
                            V0Shared.runOnUIThreadActivityStarted(a) {
                                it.findViewReimplemented<WebView>(id)?.goBack()
                            }
                        }
                    }
                    return true
                }
                "goForward" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val a = activities[aid]
                        if (a != null && id != null) {
                            V0Shared.runOnUIThreadActivityStarted(a) {
                                it.findViewReimplemented<WebView>(id)?.goForward()
                            }
                        }
                    }
                    return true
                }
                "evaluateJS" -> {
                    if (m.params != null) {
                        val aid = m.params?.get("aid")?.asInt
                        val id = m.params?.get("id")?.asInt
                        val a = activities[aid]
                        val code = m.params?.get("code")?.asString
                        if (a != null && id != null && code != null) {
                            V0Shared.runOnUIThreadActivityStarted(a) {
                                val w = it.findViewReimplemented<WebView>(id)
                                if (w != null && w.settings.javaScriptEnabled)
                                    w.evaluateJavascript(code, null)
                            }
                        }
                    }
                    return true
                }
            }
            return false
        }

        private fun setDimension(m: ConnectionHandler.Message, activities: MutableMap<Int, DataClasses.ActivityState>, overlays: MutableMap<Int, DataClasses.Overlay>, app: Context) {
            val aid = m.params?.get("aid")?.asInt
            val a = activities[aid]
            val id = m.params?.get("id")?.asInt
            val width = m.params?.get("width")
            val height = m.params?.get("height")
            val px = m.params?.get("px")?.asBoolean
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
                        pa.width = if (px == true) p.asInt else Util.toPX(app, p.asInt)
                    } else {
                        pa.height = if (px == true) p.asInt else Util.toPX(app, p.asInt)
                    }
                }
            }
            if (id != null && el?.isJsonPrimitive == true) {
                val p = el.asJsonPrimitive
                if (a != null) {
                    V0Shared.runOnUIThreadActivityStarted(a) {
                        val v = it.findViewReimplemented<View>(id)
                        val pa = v?.layoutParams
                        if (pa != null) {
                            set(pa, p)
                            v.layoutParams = pa
                        }
                    }
                }
                if (o != null) {
                    Util.runOnUIThreadBlocking {
                        val v = o.root.findViewReimplemented<View>(id)
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