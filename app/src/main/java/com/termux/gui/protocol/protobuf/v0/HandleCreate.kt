package com.termux.gui.protocol.protobuf.v0

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.widget.NestedScrollView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.termux.gui.ConnectionHandler
import com.termux.gui.Util
import com.termux.gui.protocol.json.v0.Create
import com.termux.gui.protocol.protobuf.ProtoUtils
import com.termux.gui.protocol.protobuf.v0.GUIProt0.*
import com.termux.gui.protocol.shared.v0.DataClasses
import com.termux.gui.protocol.shared.v0.V0Shared
import com.termux.gui.views.SnappingHorizontalScrollView
import com.termux.gui.views.SnappingNestedScrollView
import java.io.OutputStream
import java.lang.Exception
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class HandleCreate(val v: V0Proto, val main: OutputStream, val activities: MutableMap<Int, DataClasses.ActivityState>,
                   val wm: WindowManager, val overlays: MutableMap<Int, DataClasses.Overlay>, val rand: Random,
                   val eventQueue: LinkedBlockingQueue<Event>) {
    
    private val create: ProtoUtils.Companion.ViewCreator = ProtoUtils.Companion.ViewCreator(
        v,
        main,
        activities,
        overlays,
        rand
    )
    
    fun linear(m: CreateLinearLayoutRequest) {
        create.createView<LinearLayout>(m) {
            it.orientation = if (m.horizontal) {
                LinearLayout.HORIZONTAL
            } else {
                LinearLayout.VERTICAL
            }
        }
    }

    fun frame(m: CreateFrameLayoutRequest) {
        create.createView<FrameLayout>(m) {}
    }

    fun swipeRefresh(m: CreateSwipeRefreshLayoutRequest) {
        create.createView<SwipeRefreshLayout>(m) {}
    }
    
    
    fun text(m: CreateTextViewRequest) {
        create.createView<TextView>(m) {
            it.text = m.text
            it.freezesText = true
            it.setTextIsSelectable(m.selectableText)
            if (m.clickableLinks) {
                it.movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    fun edit(m: CreateEditTextRequest) {
        create.createView<EditText>(m) {
            when (m.type) {
                CreateEditTextRequest.Type.text -> it.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_NORMAL
                CreateEditTextRequest.Type.textMultiLine -> it.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE or EditorInfo.TYPE_TEXT_VARIATION_NORMAL
                CreateEditTextRequest.Type.phone -> it.inputType = EditorInfo.TYPE_CLASS_PHONE
                CreateEditTextRequest.Type.date -> it.inputType = EditorInfo.TYPE_CLASS_DATETIME or EditorInfo.TYPE_DATETIME_VARIATION_DATE
                CreateEditTextRequest.Type.time -> it.inputType = EditorInfo.TYPE_CLASS_DATETIME or EditorInfo.TYPE_DATETIME_VARIATION_TIME
                CreateEditTextRequest.Type.datetime -> it.inputType = EditorInfo.TYPE_CLASS_DATETIME or EditorInfo.TYPE_DATETIME_VARIATION_NORMAL
                CreateEditTextRequest.Type.number -> it.inputType = EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_NORMAL
                CreateEditTextRequest.Type.numberDecimal -> it.inputType = EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_FLAG_DECIMAL or EditorInfo.TYPE_NUMBER_VARIATION_NORMAL
                CreateEditTextRequest.Type.numberPassword -> it.inputType = EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD
                CreateEditTextRequest.Type.numberSigned -> it.inputType = EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_FLAG_SIGNED or EditorInfo.TYPE_NUMBER_VARIATION_NORMAL
                CreateEditTextRequest.Type.numberDecimalSigned -> it.inputType = EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_FLAG_DECIMAL or EditorInfo.TYPE_NUMBER_FLAG_SIGNED or EditorInfo.TYPE_NUMBER_VARIATION_NORMAL
                CreateEditTextRequest.Type.textEmailAddress -> it.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                CreateEditTextRequest.Type.textPassword -> it.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
                else -> it.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_NORMAL
            }
            if (m.noline) {
                it.setBackgroundResource(android.R.color.transparent)
            }
            it.setText(m.txt, TextView.BufferType.EDITABLE)
        }
    }


    fun button(m: CreateButtonRequest) {
        create.createView<Button>(m) {
            it.text = m.text
            it.freezesText = true
            it.isAllCaps = m.allcaps
            ProtoUtils.setClickListener(it, m.data.aid, true, eventQueue)
        }
    }

    fun image(m: CreateImageViewRequest) {
        create.createView<ImageView>(m) {}
    }

    fun space(m: CreateSpaceRequest) {
        create.createView<Space>(m) {}
    }

    fun nestedScroll(m: CreateNestedScrollViewRequest) {
        val init = { it: NestedScrollView ->
            it.isFillViewport = m.fillViewport
            if (m.nobar) {
                it.scrollBarSize = 0
            }
        }
        if (m.snapping) {
            create.createView<SnappingNestedScrollView>(m, init)
        } else {
            create.createView(m, init)
        }
    }

    fun horizontalScroll(m: CreateHorizontalScrollViewRequest) {
        val init = { it: HorizontalScrollView ->
            it.isFillViewport = m.fillViewPort
            if (m.nobar) {
                it.scrollBarSize = 0
            }
        }
        if (m.snapping) {
            create.createView<SnappingHorizontalScrollView>(m, init)
        } else {
            create.createView(m, init)
        }
    }
    
    
    
    
    
    
    
}