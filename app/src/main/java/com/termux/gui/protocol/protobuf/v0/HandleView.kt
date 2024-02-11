package com.termux.gui.protocol.protobuf.v0

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.hardware.HardwareBuffer
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.*
import androidx.core.view.setPadding
import androidx.core.widget.NestedScrollView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.termux.gui.*
import com.termux.gui.protocol.protobuf.ProtoUtils
import com.termux.gui.protocol.protobuf.v0.GUIProt0.*
import com.termux.gui.protocol.shared.v0.DataClasses
import com.termux.gui.protocol.shared.v0.GUIWebViewClient
import com.termux.gui.protocol.shared.v0.V0Shared
import com.termux.gui.views.HardwareBufferSurfaceView
import java.io.OutputStream
import java.util.concurrent.LinkedBlockingQueue

class HandleView(val v: V0Proto, val main: OutputStream, val activities: MutableMap<Int, DataClasses.ActivityState>,
                 val overlays: MutableMap<Int, DataClasses.Overlay>,
                 val eventQueue: LinkedBlockingQueue<Event>,
                 val buffers: MutableMap<Int, DataClasses.SharedBuffer>,
                 val hardwareBuffers: MutableMap<Int, HardwareBuffer>,
                 val logger: V0Proto.ProtoLogger
) {
    
    private val handler = ProtoUtils.Companion.ViewHandler(main, activities, overlays)
    

    fun showCursor(m: ShowCursorRequest) {
        handler.handleView(m.v, ShowCursorResponse.newBuilder(), { ret, v: EditText -> 
            v.isCursorVisible = m.show
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun linearParams(m: SetLinearLayoutParamsRequest) {
        handler.handleView(m.v, SetLinearLayoutParamsResponse.newBuilder(), { ret, v: android.view.View ->
            val p = v.layoutParams as? LinearLayout.LayoutParams
            if (p != null) {
                if (m.weight >= 0) {
                    p.weight = m.weight
                }
                v.layoutParams = p
                if (m.position != 0) {
                    val parent = v.parent as? LinearLayout
                    parent?.removeView(v)
                    parent?.addView(v, m.position-1)
                }
                ret.success = true
            } else {
                ret.success = false
            }
        }, {
            it.success = false
        })
    }

    fun gridParams(m: SetGridLayoutParamsRequest) {
        handler.handleView(m.v, SetGridLayoutParamsResponse.newBuilder(), { ret, v: android.view.View ->
            val p = v.layoutParams as? GridLayout.LayoutParams
            if (p != null) {
                fun align(a: SetGridLayoutParamsRequest.Alignment): GridLayout.Alignment {
                    return when (a) {
                        SetGridLayoutParamsRequest.Alignment.CENTER -> GridLayout.CENTER
                        SetGridLayoutParamsRequest.Alignment.TOP -> GridLayout.TOP
                        SetGridLayoutParamsRequest.Alignment.BOTTOM -> GridLayout.BOTTOM
                        SetGridLayoutParamsRequest.Alignment.LEFT -> GridLayout.LEFT
                        SetGridLayoutParamsRequest.Alignment.RIGHT -> GridLayout.RIGHT
                        SetGridLayoutParamsRequest.Alignment.BASELINE -> GridLayout.BASELINE
                        SetGridLayoutParamsRequest.Alignment.FILL -> GridLayout.FILL
                        SetGridLayoutParamsRequest.Alignment.UNRECOGNIZED -> GridLayout.CENTER
                    }
                }
                p.rowSpec = GridLayout.spec(m.row, m.rowSize, align(m.rowAlign))
                p.columnSpec = GridLayout.spec(m.col, m.colSize, align(m.colAlign))
                v.layoutParams = p
                ret.success = true
            } else {
                ret.success = false
                ret.code = Error.INTERNAL_ERROR
            }
        }, {
            it.success = false
        })
    }

    fun location(m: SetViewLocationRequest) {
        handler.handleView(m.v, SetViewLocationResponse.newBuilder(), { ret, v: android.view.View, c ->
            v.x = ProtoUtils.unitToPX(m.unit, m.x, c.resources.displayMetrics)
            v.y = ProtoUtils.unitToPX(m.unit, m.y, c.resources.displayMetrics)
            if (m.top) {
                v.bringToFront()
            }
            ret.success = true
        }, {
            it.success = false
        })
    }
    
    fun relative(m: SetRelativeLayoutParamsRequest) {
        ProtoUtils.write(SetRelativeLayoutParamsResponse.newBuilder().setSuccess(false), main)
    }

    fun visible(m: SetVisibilityRequest) {
        handler.handleView(m.v, SetVisibilityResponse.newBuilder(), { ret, v: android.view.View ->
            ret.success = true
            when (m.vis!!) {
                Visibility.visible -> v.visibility = android.view.View.VISIBLE
                Visibility.hidden -> v.visibility = android.view.View.INVISIBLE
                Visibility.gone -> v.visibility = android.view.View.GONE
                Visibility.UNRECOGNIZED -> ret.success = false
            }
        }, {
            it.success = false
        })
    }

    fun setWidth(m: SetWidthRequest) {
        handler.handleView(m.v, SetWidthResponse.newBuilder(), { ret, v: android.view.View, c ->
            val p = v.layoutParams
            if (p == null) {
                ret.success = false
                ret.code = Error.INTERNAL_ERROR
                return@handleView
            }
            when (m.s.valueCase!!) {
                ViewSize.ValueCase.SIZE -> {
                    p.width = ProtoUtils.unitToPX(m.s.size.unit, m.s.size.value, c.resources.displayMetrics).toInt()
                }
                ViewSize.ValueCase.CONSTANT -> {
                    when (m.s.constant!!) {
                        ViewSize.Constant.MATCH_PARENT -> p.width = LayoutParams.MATCH_PARENT
                        ViewSize.Constant.WRAP_CONTENT -> p.width = LayoutParams.WRAP_CONTENT
                        ViewSize.Constant.UNRECOGNIZED -> {
                            ret.success = false
                            ret.code = Error.INVALID_ENUM
                            return@handleView
                        }
                    }
                }
                ViewSize.ValueCase.VALUE_NOT_SET -> {
                    ret.success = false
                    ret.code = Error.INVALID_ENUM
                    return@handleView
                }
            }
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun setHeight(m: SetHeightRequest) {
        handler.handleView(m.v, SetHeightResponse.newBuilder(), { ret, v: android.view.View, c ->
            val p = v.layoutParams
            if (p == null) {
                ret.success = false
                ret.code = Error.INTERNAL_ERROR
                return@handleView
            }
            when (m.s.valueCase!!) {
                ViewSize.ValueCase.SIZE -> {
                    p.height = ProtoUtils.unitToPX(m.s.size.unit, m.s.size.value, c.resources.displayMetrics).toInt()
                }
                ViewSize.ValueCase.CONSTANT -> {
                    when (m.s.constant!!) {
                        ViewSize.Constant.MATCH_PARENT -> p.height = LayoutParams.MATCH_PARENT
                        ViewSize.Constant.WRAP_CONTENT -> p.height = LayoutParams.WRAP_CONTENT
                        ViewSize.Constant.UNRECOGNIZED -> {
                            ret.success = false
                            ret.code = Error.INVALID_ENUM
                            return@handleView
                        }
                    }
                }
                ViewSize.ValueCase.VALUE_NOT_SET -> {
                    ret.success = false
                    ret.code = Error.INVALID_ENUM
                    return@handleView
                }
            }
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun getDimensions(m: GetDimensionsRequest) {
        handler.handleView(m.v, GetDimensionsResponse.newBuilder(), { ret, v: android.view.View, c ->
            ret.width = ProtoUtils.pxToUnit(m.unit, v.width.toFloat(), c.resources.displayMetrics)
            ret.height = ProtoUtils.pxToUnit(m.unit, v.height.toFloat(), c.resources.displayMetrics)
        }, {
            it.width = -1.0f
            it.height = -1.0f
        })
    }

    fun delete(m: DeleteViewRequest) {
        handler.handleView(m.v, DeleteViewResponse.newBuilder(), { ret, v: android.view.View, _, usedIds ->
            Util.removeViewRecursive(v, usedIds)
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun deleteChildren(m: DeleteChildrenRequest) {
        handler.handleView(m.v, DeleteChildrenResponse.newBuilder(), { ret, v: ViewGroup, _, usedIds ->
            while (v.childCount > 0) {
                Util.removeViewRecursive(v.getChildAt(0), usedIds)
            }
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun margin(m: SetMarginRequest) {
        handler.handleView(m.v, SetMarginResponse.newBuilder(), { ret, v: android.view.View, c, _ ->
            val p = v.layoutParams as? ViewGroup.MarginLayoutParams
            if (p == null) {
                ret.success = false
                ret.code = Error.INTERNAL_ERROR
                return@handleView
            }
            val px = ProtoUtils.unitToPX(m.s.unit, m.s.value, c.resources.displayMetrics).toInt()
            when (m.dir!!) {
                Direction.ALL -> p.setMargins(px, px, px, px)
                Direction.TOP -> p.topMargin = px
                Direction.LEFT -> p.leftMargin = px
                Direction.BOTTOM -> p.bottomMargin = px
                Direction.RIGHT -> p.rightMargin = px
                Direction.UNRECOGNIZED -> {
                    ret.success = false
                    ret.code = Error.INVALID_ENUM
                    return@handleView
                }
            }
            v.layoutParams = p
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun padding(m: SetPaddingRequest) {
        handler.handleView(m.v, SetPaddingResponse.newBuilder(), { ret, v: android.view.View, c, _ ->
            val px = ProtoUtils.unitToPX(m.s.unit, m.s.value, c.resources.displayMetrics).toInt()
            when (m.dir!!) {
                Direction.ALL -> v.setPadding(px)
                Direction.TOP -> v.setPadding(v.paddingLeft, px, v.paddingRight, v.paddingBottom)
                Direction.LEFT -> v.setPadding(px, v.paddingTop, v.paddingRight, v.paddingBottom)
                Direction.BOTTOM -> v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, px)
                Direction.RIGHT -> v.setPadding(v.paddingLeft, v.paddingTop, px, v.paddingBottom)
                Direction.UNRECOGNIZED -> {
                    ret.success = false
                    ret.code = Error.INVALID_ENUM
                    return@handleView
                }
            }
            ret.success = true
        }, {
            it.success = false
        })
    }
    
    fun backgroundColor(m: SetBackgroundColorRequest) {
        handler.handleView(m.v, SetBackgroundColorResponse.newBuilder(), { ret, v: android.view.View, _, _ ->
            v.backgroundTintList = ColorStateList.valueOf(m.color)
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun textColor(m: SetTextColorRequest) {
        handler.handleView(m.v, SetTextColorResponse.newBuilder(), { ret, v: TextView, _, _ ->
            v.setTextColor(m.color)
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun progress(m: SetProgressRequest) {
        handler.handleView(m.v, SetProgressResponse.newBuilder(), { ret, v: ProgressBar, _, _ ->
            if (m.progress < 0 || m.progress > 100) {
                ret.success = false
                ret.code = Error.INTERNAL_ERROR
                return@handleView
            }
            v.progress = m.progress
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun refreshing(m: SetRefreshingRequest) {
        handler.handleView(m.v, SetRefreshingResponse.newBuilder(), { ret, v: SwipeRefreshLayout, _, _ ->
            v.isRefreshing = m.refreshing
            ret.success = true
        }, {
            it.success = false
        })
    }


    fun setText(m: SetTextRequest) {
        handler.handleView(m.v, SetTextResponse.newBuilder(), { ret, v: TextView, _, _ ->
            v.text = m.text
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun gravity(m: SetGravityRequest) {
        handler.handleView(m.v, SetGravityResponse.newBuilder(), { ret, v: TextView, _, _ ->
            val grav = when (m.horizontal!!) {
                SetGravityRequest.Gravity.CENTER -> Gravity.CENTER
                SetGravityRequest.Gravity.LEFTTOP -> Gravity.START
                SetGravityRequest.Gravity.RIGHTBOTTOM -> Gravity.END
                SetGravityRequest.Gravity.UNRECOGNIZED -> {
                    ret.success = false
                    ret.code = Error.INVALID_ENUM
                    return@handleView
                }
            } or when (m.vertical!!) {
                SetGravityRequest.Gravity.CENTER -> Gravity.CENTER
                SetGravityRequest.Gravity.LEFTTOP -> Gravity.TOP
                SetGravityRequest.Gravity.RIGHTBOTTOM -> Gravity.BOTTOM
                SetGravityRequest.Gravity.UNRECOGNIZED -> {
                    ret.success = false
                    ret.code = Error.INVALID_ENUM
                    return@handleView
                }
            }
            v.gravity = grav
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun textSize(m: SetTextSizeRequest) {
        handler.handleView(m.v, SetTextSizeResponse.newBuilder(), { ret, v: TextView, c, _ ->
            v.textSize = ProtoUtils.unitToPX(m.s.unit, m.s.value, c.resources.displayMetrics)
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun getText(m: GetTextRequest) {
        handler.handleView(m.v, GetTextResponse.newBuilder(), { ret, v: TextView, _, _ ->
            ret.text = v.text.toString()
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun focus(m: RequestFocusRequest) {
        handler.handleView(m.v, RequestFocusResponse.newBuilder(), { ret, v: android.view.View, c, _ ->
            v.requestFocus()
            if (m.forcesoft) {
                val im = c.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                im.showSoftInput(v, 0)
            }
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun getScroll(m: GetScrollPositionRequest) {
        handler.handleView(m.v, GetScrollPositionResponse.newBuilder(), { ret, v: android.view.View, c, _ ->
            if (v is NestedScrollView || v is HorizontalScrollView) {
                ret.x = ProtoUtils.pxToUnit(m.unit, v.scrollX.toFloat(), c.resources.displayMetrics)
                ret.y = ProtoUtils.pxToUnit(m.unit, v.scrollY.toFloat(), c.resources.displayMetrics)
            } else {
                ret.x = -1f
                ret.y = -1f
            }
        }, {
            it.x = -1f
            it.y = -1f
        })
    }

    fun setScroll(m: SetScrollPositionRequest) {
        handler.handleView(m.v, SetScrollPositionResponse.newBuilder(), { ret, v: android.view.View, c, _ ->
            if (v is NestedScrollView || v is HorizontalScrollView) {
                val x = ProtoUtils.unitToPX(m.x.unit, m.x.value, c.resources.displayMetrics).toInt()
                val y = ProtoUtils.unitToPX(m.y.unit, m.y.value, c.resources.displayMetrics).toInt()
                if (m.smooth) {
                    when (v) {
                        is NestedScrollView -> {
                            v.smoothScrollTo(x, y)
                        }
                        is HorizontalScrollView -> {
                            v.smoothScrollTo(x, y)
                        }
                        else -> {
                            ret.success = false
                            ret.code = Error.INVALID_VIEW_TYPE
                            return@handleView
                        }
                    }
                } else {
                    v.scrollTo(x, y)
                }
                ret.success = true
            } else {
                ret.success = false
                ret.code = Error.INVALID_VIEW_TYPE
            }
        }, {
            it.success = false
        })
    }

    fun setList(m: SetListRequest) {
        handler.handleView(m.v, SetListResponse.newBuilder(), { ret, v: android.view.View, c, _ ->
            when (v) {
                is Spinner -> {
                    v.adapter = ArrayAdapter(c, R.layout.spinner_text, m.listList)
                }
                is TabLayout -> {
                    v.removeAllTabs()
                    for (tab in m.listList) {
                        val t = v.newTab()
                        t.text = tab
                        v.addTab(t)
                    }
                }
                else -> {
                    ret.success = false
                    ret.code = Error.INVALID_VIEW_TYPE
                    return@handleView
                }
            }
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun setImage(m: SetImageRequest) {
        handler.handleView(m.v, SetImageResponse.newBuilder(), { ret, v: ImageView, _, _ ->
            val bin = m.image.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(bin, 0, bin.size)
            v.setImageBitmap(bitmap)
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun setBuffer(m: SetBufferRequest) {
        handler.handleView(m.v, SetBufferResponse.newBuilder(), { ret, v: ImageView, _, _ ->
            val buffer = buffers[m.buffer]
            if (buffer != null) {
                v.setImageBitmap(buffer.btm)
                ret.success = true
            } else {
                ret.success = false
                ret.code = Error.BUFFER_NOT_FOUND
            }
        }, {
            it.success = false
        })
    }


    fun refreshImageView(m: RefreshImageViewRequest) {
        handler.handleView(m.v, RefreshImageViewResponse.newBuilder(), { ret, v: ImageView, _, _ ->
            v.invalidate()
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun selectTab(m: SelectTabRequest) {
        handler.handleView(m.v, SelectTabResponse.newBuilder(), { ret, v: TabLayout, _, _ ->
            val t = v.getTabAt(m.tab)
            if (t != null) {
                v.selectTab(t)
                ret.success = true
            } else {
                ret.success = false
                ret.code = Error.INTERNAL_ERROR
            }
        }, {
            it.success = false
        })
    }

    fun selectItem(m: SelectItemRequest) {
        handler.handleView(m.v, SelectItemResponse.newBuilder(), { ret, v: Spinner, _, _ ->
            v.setSelection(m.item)
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun setClickable(m: SetClickableRequest) {
        handler.handleView(m.v, SetClickableResponse.newBuilder(), { ret, v: android.view.View, _, _ ->
            v.isClickable = m.clickable
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun setChecked(m: SetCheckedRequest) {
        handler.handleView(m.v, SetCheckedResponse.newBuilder(), { ret, v: CompoundButton, _, _ ->
            v.isChecked = m.checked
            ret.success = true
        }, {
            it.success = false
        })
    }


    @SuppressLint("SetJavaScriptEnabled")
    fun allowJS(m: AllowJavascriptRequest) {
        handler.handleView(m.v, AllowJavascriptResponse.newBuilder(), { ret, v: WebView, c, _ ->
            if (m.allow) {
                if (! Settings.instance.javascript) {
                    val data = Thread.currentThread().id.toString()
                    val r = GUIWebViewJavascriptDialog.Companion.Request()
                    GUIWebViewJavascriptDialog.requestMap[data] = r
                    val i = Intent(c, GUIWebViewJavascriptDialog::class.java)
                    i.data = Uri.parse(data)
                    c.startActivity(i)
                    synchronized(r.monitor) {
                        while (r.allow == null)
                            r.monitor.wait()
                    }
                    GUIWebViewJavascriptDialog.requestMap.remove(data)
                    if (r.allow == true) {
                        v.settings.javaScriptEnabled = true
                        ret.allowed = true
                        ret.success = true
                    } else {
                        ret.allowed = false
                        ret.success = true
                    }
                } else {
                    v.settings.javaScriptEnabled = true
                    ret.allowed = true
                    ret.success = true
                }
            } else {
                v.settings.javaScriptEnabled = false
                ret.allowed = false
                ret.success = true
            }
        }, {
            it.success = false
        })
    }
    
    fun allowContent(m: AllowContentURIRequest) {
        handler.handleView(m.v, AllowContentURIResponse.newBuilder(), { ret, v: WebView, _, _ ->
            v.settings.allowContentAccess = m.allow
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun setData(m: SetDataRequest) {
        handler.handleView(m.v, SetDataResponse.newBuilder(), { ret, v: WebView, _, _ ->
            val encoding = if (m.base64) "base64" else null
            v.loadData(m.data, m.mime, encoding)
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun loadURI(m: LoadURIRequest) {
        handler.handleView(m.v, LoadURIResponse.newBuilder(), { ret, v: WebView, _, _ ->
            v.loadUrl(m.uri)
            ret.success = true
        }, {
            it.success = false
        })
    }

    @SuppressLint("WebViewApiAvailability")
    fun allowNavigation(m: AllowNavigationRequest) {
        handler.handleView(m.v, AllowNavigationResponse.newBuilder(), { ret, v: WebView, _, _ ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (v.webViewClient as GUIWebViewClient).allowNavigation = m.allow
                ret.success = true
            } else {
                ret.success = false
                ret.code = Error.ANDROID_VERSION_TOO_LOW
            }
        }, {
            it.success = false
        })
    }

    fun goBack(m: GoBackRequest) {
        handler.handleView(m.v, GoBackResponse.newBuilder(), { ret, v: WebView, _, _ ->
            v.goBack()
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun goForward(m: GoForwardRequest) {
        handler.handleView(m.v, GoForwardResponse.newBuilder(), { ret, v: WebView, _, _ ->
            v.goForward()
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun evaluateJS(m: EvaluateJSRequest) {
        handler.handleView(m.v, EvaluateJSResponse.newBuilder(), { ret, v: WebView, _, _ ->
            if (v.settings.javaScriptEnabled) {
                v.evaluateJavascript(m.code, null)
                ret.success = true
            } else {
                ret.success = false
                ret.code = Error.INTERNAL_ERROR
            }
        }, {
            it.success = false
        })
    }


    fun sendClickEvent(m: SendClickEventRequest) {
        handler.handleView(m.v, SendClickEventResponse.newBuilder(), { ret, v: android.view.View, _, _ ->
            ProtoUtils.setClickListener(v, m.v.aid, m.send, eventQueue)
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun sendLongClickEvent(m: SendLongClickEventRequest) {
        handler.handleView(m.v, SendLongClickEventResponse.newBuilder(), { ret, v: android.view.View, _, _ ->
            ProtoUtils.setLongClickListener(v, m.v.aid, m.send, eventQueue)
            ret.success = true
        }, {
            it.success = false
        })
    }


    fun sendFocusChangeEvent(m: SendFocusChangeEventRequest) {
        handler.handleView(m.v, SendFocusChangeEventResponse.newBuilder(), { ret, v: android.view.View, _, _ ->
            ProtoUtils.setFocusChangeListener(v, m.v.aid, m.send, eventQueue)
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun sendTouchEvent(m: SendTouchEventRequest) {
        handler.handleView(m.v, SendTouchEventResponse.newBuilder(), { ret, v: android.view.View, _, _ ->
            ProtoUtils.setTouchListenerProto(v, m.v.aid, m.send, eventQueue)
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun sendTextEvent(m: SendTextEventRequest) {
        handler.handleView(m.v, SendTextEventResponse.newBuilder(), { ret, v: TextView, _, _ ->
            ProtoUtils.setTextWatcher(v, m.v.aid, m.send, eventQueue)
            ret.success = true
        }, {
            it.success = false
        })
    }

    fun sendOverlayTouch(m: SendOverlayTouchEventRequest) {
        val ret = SendOverlayTouchEventResponse.newBuilder()
        try {
            val o = overlays[m.aid]
            if (o != null) {
                o.sendTouch = m.send
                ret.success = true
            } else {
                ret.success = false
            }
        } catch (e: java.lang.Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
        }
        ProtoUtils.write(ret, main)
    }
    
    
    fun setSurfaceBuffer(m: SurfaceViewSetBufferRequest) {
        // setBuffer is synchronized with the rendering and so can be called from the connection thread
        handler.handleViewConnectionThread(m.v, SurfaceViewSetBufferResponse.newBuilder(), { ret, v: HardwareBufferSurfaceView, _, _ ->
            val b = hardwareBuffers[m.buffer]
            if (b != null) {
                v.setBuffer(b)
                ret.success = true
            } else {
                ret.success = false
                ret.code = Error.BUFFER_NOT_FOUND
            }
        }, {
            it.success = false
        })
    }
    
    fun surfaceConfig(m: SurfaceViewConfigRequest) {
        handler.handleView(m.v, SurfaceViewSetBufferResponse.newBuilder(), { ret, v: HardwareBufferSurfaceView, _, _ ->
            synchronized(v.RENDER_LOCK) {
                v.config.backgroundColor = m.backgroundColor
                when (m.xMismatch) {
                    SurfaceViewConfigRequest.OnDimensionMismatch.STICK_TOPLEFT -> v.config.x = HardwareBufferSurfaceView.Config.OnDimensionMismatch.STICK_TOPLEFT
                    SurfaceViewConfigRequest.OnDimensionMismatch.CENTER_AXIS -> v.config.x = HardwareBufferSurfaceView.Config.OnDimensionMismatch.CENTER_AXIS
                    else -> {
                        ret.success = false
                        ret.code = Error.INVALID_ENUM
                    }
                }
                when (m.yMismatch) {
                    SurfaceViewConfigRequest.OnDimensionMismatch.STICK_TOPLEFT -> v.config.y = HardwareBufferSurfaceView.Config.OnDimensionMismatch.STICK_TOPLEFT
                    SurfaceViewConfigRequest.OnDimensionMismatch.CENTER_AXIS -> v.config.y = HardwareBufferSurfaceView.Config.OnDimensionMismatch.CENTER_AXIS
                    else -> {
                        ret.success = false
                        ret.code = Error.INVALID_ENUM
                    }
                }
                v.setFrameRate(m.framerate)
                ret.success = true
            }
        }, {
            it.success = false
        })
    }
    
    
}