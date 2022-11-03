package com.termux.gui.protocol.protobuf.v0

import android.content.Context
import android.os.Build
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.ConsoleMessage
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.NestedScrollView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.google.protobuf.MessageLite
import com.termux.gui.App
import com.termux.gui.ConnectionHandler
import com.termux.gui.Util
import com.termux.gui.protocol.json.v0.Create
import com.termux.gui.protocol.protobuf.ProtoUtils
import com.termux.gui.protocol.protobuf.v0.GUIProt0.*
import com.termux.gui.protocol.shared.v0.*
import com.termux.gui.views.HorizontalProgressBar
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
            when (m.type!!) {
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
                CreateEditTextRequest.Type.UNRECOGNIZED -> it.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_NORMAL
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

    fun radioGroup(m: CreateRadioGroupRequest) {
        create.createView<RadioGroup>(m) {
            ProtoUtils.setCheckedListener(it, m.data.aid, true, eventQueue)
        }
    }

    fun radio(m: CreateRadioButtonRequest) {
        create.createView<RadioButton>(m) {
            it.text = m.text
            it.freezesText = true
            it.isChecked = m.checked
        }
    }

    fun checkbox(m: CreateCheckboxRequest) {
        create.createView<CheckBox>(m) {
            it.text = m.text
            it.freezesText = true
            it.isChecked = m.checked
            ProtoUtils.setClickListener(it, m.data.aid, true, eventQueue)
        }
    }

    fun toggleButton(m: CreateToggleButtonRequest) {
        create.createView<ToggleButton>(m) {
            it.freezesText = true
            it.isChecked = m.checked
            ProtoUtils.setClickListener(it, m.data.aid, true, eventQueue)
        }
    }

    fun switch(m: CreateSwitchRequest) {
        create.createView<SwitchCompat>(m) {
            it.text = m.text
            it.freezesText = true
            it.isChecked = m.checked
            ProtoUtils.setClickListener(it, m.data.aid, true, eventQueue)
        }
    }
    
    fun spinner(m: CreateSpinnerRequest) {
        create.createView<Spinner>(m) {
            ProtoUtils.setSpinnerListener(it, m.data.aid, eventQueue)
        }
    }

    fun progressBar(m: CreateProgressBarRequest) {
        create.createView<HorizontalProgressBar>(m) {}
    }

    fun tab(m: CreateTabLayoutRequest) {
        create.createView<TabLayout>(m) {
            ProtoUtils.setTabSelectedListener(it, m.data.aid, true, eventQueue)
        }
    }

    @Suppress("DEPRECATION")
    fun webView(m: CreateWebViewRequest) {
        create.createView<WebView>(m) {
            val settings = it.settings
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            settings.domStorageEnabled = true
            settings.setGeolocationEnabled(false)
            settings.mediaPlaybackRequiresUserGesture = false
            settings.saveFormData = false
            settings.savePassword = false

            val l = object: WebEventListener {
                override fun onNavigation(url: String) {
                    eventQueue.offer(Event.newBuilder().setWebNavigation(WebViewNavigationEvent.newBuilder().setUrl(url).
                    setV(GUIProt0.View.newBuilder().setAid(m.data.aid).setId(it.id))).build())
                }

                override fun onHTTPError(url: String, code: Int) {
                    eventQueue.offer(Event.newBuilder().setWebHTTPError(WebViewHTTPErrorEvent.newBuilder().setUrl(url).setCode(code).
                    setV(GUIProt0.View.newBuilder().setAid(m.data.aid).setId(it.id))).build())
                }

                override fun onReceivedError(url: String, description: String, code: Int) {
                    eventQueue.offer(Event.newBuilder().setWebError(WebViewErrorEvent.newBuilder().setUrl(url).
                    setV(GUIProt0.View.newBuilder().setAid(m.data.aid).setId(it.id))).build())
                }

                override fun onRenderProcessGone(v: WebView) {
                    eventQueue.offer(Event.newBuilder().setWebDestroyed(WebViewDestroyedEvent.newBuilder().
                    setV(GUIProt0.View.newBuilder().setAid(m.data.aid).setId(it.id))).build())
                }

                override fun onProgressChanged(progress: Int) {
                    eventQueue.offer(Event.newBuilder().setWebProgress(WebViewProgressEvent.newBuilder().setProgress(progress).
                    setV(GUIProt0.View.newBuilder().setAid(m.data.aid).setId(it.id))).build())
                }

                override fun onConsoleMessage(msg: ConsoleMessage) {
                    eventQueue.offer(Event.newBuilder().setWebConsoleMessage(WebViewConsoleMessageEvent.newBuilder().setMessage(msg.message()).
                    setV(GUIProt0.View.newBuilder().setAid(m.data.aid).setId(it.id))).build())
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true)
            }
            it.webViewClient = GUIWebViewClient(l)
            it.webChromeClient = GUIWebChromeClient(l)
        }
    }
    
    
    
    
}