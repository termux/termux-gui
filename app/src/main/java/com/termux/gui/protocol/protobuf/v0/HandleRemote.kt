package com.termux.gui.protocol.protobuf.v0

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.*
import com.termux.gui.GUIWidget
import com.termux.gui.R
import com.termux.gui.Util
import com.termux.gui.protocol.protobuf.ProtoUtils
import com.termux.gui.protocol.protobuf.v0.GUIProt0.*
import com.termux.gui.protocol.shared.v0.DataClasses
import com.termux.gui.protocol.shared.v0.V0Shared
import java.io.OutputStream
import java.util.*

class HandleRemote(val main: OutputStream, val remoteviews: MutableMap<Int, DataClasses.RemoteLayoutRepresentation>,
                   val rand: Random, val app: Context, val logger: V0Proto.ProtoLogger) {
    
    private val handler = ProtoUtils.Companion.RemoteHandler(main, remoteviews)
    
    fun createLayout(m: CreateRemoteLayoutRequest) {
        val ret = CreateRemoteLayoutResponse.newBuilder()
        try {
            val id = Util.generateIndex(rand, remoteviews.keys)
            remoteviews[id] = DataClasses.RemoteLayoutRepresentation(RemoteViews(app.packageName, R.layout.remote_view_root))
            ret.rid = id
        } catch (e: java.lang.Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.rid = -1
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }

    fun deleteLayout(m: DeleteRemoteLayoutRequest) {
        val ret = DeleteRemoteLayoutResponse.newBuilder()
        try {
            ret.success = remoteviews.remove(m.rid) != null
        } catch (e: java.lang.Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }

    fun frame(m: AddRemoteFrameLayoutRequest) {
        handler.handleRemote(m.rid, AddRemoteFrameLayoutResponse.newBuilder(), { ret, r ->
            ret.id = V0Shared.addRemoteView(FrameLayout::class, r, m.parent, app)
        }) {
            it.id = -1
        }
    }

    fun linear(m: AddRemoteLinearLayoutRequest) {
        handler.handleRemote(m.rid, AddRemoteLinearLayoutResponse.newBuilder(), { ret, r ->
            ret.id = V0Shared.addRemoteView(LinearLayout::class, r, m.parent, app)
        }) {
            it.id = -1
        }
    }

    fun text(m: AddRemoteTextViewRequest) {
        handler.handleRemote(m.rid, AddRemoteTextViewResponse.newBuilder(), { ret, r ->
            ret.id = V0Shared.addRemoteView(TextView::class, r, m.parent, app)
        }) {
            it.id = -1
        }
    }

    fun button(m: AddRemoteButtonRequest) {
        handler.handleRemote(m.rid, AddRemoteButtonResponse.newBuilder(), { ret, r ->
            ret.id = V0Shared.addRemoteView(Button::class, r, m.parent, app)
        }) {
            it.id = -1
        }
    }

    fun image(m: AddRemoteImageViewRequest) {
        handler.handleRemote(m.rid, AddRemoteImageViewResponse.newBuilder(), { ret, r ->
            ret.id = V0Shared.addRemoteView(ImageView::class, r, m.parent, app)
        }) {
            it.id = -1
        }
    }

    fun progress(m: AddRemoteProgressBarRequest) {
        handler.handleRemote(m.rid, AddRemoteProgressBarResponse.newBuilder(), { ret, r ->
            ret.id = V0Shared.addRemoteView(ProgressBar::class, r, m.parent, app)
        }) {
            it.id = -1
        }
    }




    fun backgroundColor(m: SetRemoteBackgroundColorRequest) {
        handler.handleRemote(m.rid, SetRemoteBackgroundColorResponse.newBuilder(), { ret, r ->
            r.root?.setInt(m.id, "setBackgroundColor", m.color)
            ret.success = true
        }) {
            it.success = false
        }
    }

    fun setProgress(m: SetRemoteProgressBarRequest) {
        handler.handleRemote(m.rid, SetRemoteProgressBarResponse.newBuilder(), { ret, r ->
            r.root?.setProgressBar(m.id, m.max, m.progress, false)
            ret.success = true
        }) {
            it.success = false
        }
    }

    fun setText(m: SetRemoteTextRequest) {
        handler.handleRemote(m.rid, SetRemoteTextResponse.newBuilder(), { ret, r ->
            r.root?.setTextViewText(m.id, m.text)
            ret.success = true
        }) {
            it.success = false
        }
    }

    fun setTextSize(m: SetRemoteTextSizeRequest) {
        handler.handleRemote(m.rid, SetRemoteTextSizeResponse.newBuilder(), { ret, r ->
            r.root?.setTextViewTextSize(m.id, ProtoUtils.unitToTypedValue(m.s.unit), m.s.value)
            ret.success = true
        }) {
            it.success = false
        }
    }

    fun setTextColor(m: SetRemoteTextColorRequest) {
        handler.handleRemote(m.rid, SetRemoteTextColorResponse.newBuilder(), { ret, r ->
            r.root?.setTextColor(m.id, m.color)
            ret.success = true
        }) {
            it.success = false
        }
    }

    fun setVisibility(m: SetRemoteVisibilityRequest) {
        handler.handleRemote(m.rid, SetRemoteVisibilityResponse.newBuilder(), { ret, r ->
            r.root?.setViewVisibility(m.id, when (m.v!!) {
                Visibility.visible -> android.view.View.VISIBLE
                Visibility.hidden -> android.view.View.INVISIBLE
                Visibility.gone -> android.view.View.GONE
                Visibility.UNRECOGNIZED -> {
                    ret.success = false
                    ret.code = Error.INVALID_ENUM
                    return@handleRemote
                }
            })
            ret.success = true
        }) {
            it.success = false
        }
    }

    fun setPadding(m: SetRemotePaddingRequest) {
        handler.handleRemote(m.rid, SetRemotePaddingResponse.newBuilder(), { ret, r ->
            r.root?.setViewPadding(m.id,
                ProtoUtils.unitToPX(m.left.unit, m.left.value, app.resources.displayMetrics).toInt(),
                ProtoUtils.unitToPX(m.top.unit, m.top.value, app.resources.displayMetrics).toInt(),
                ProtoUtils.unitToPX(m.right.unit, m.right.value, app.resources.displayMetrics).toInt(),
                ProtoUtils.unitToPX(m.bottom.unit, m.bottom.value, app.resources.displayMetrics).toInt(),)
            ret.success = true
        }) {
            it.success = false
        }
    }

    fun setImage(m: SetRemoteImageRequest) {
        handler.handleRemote(m.rid, SetRemoteImageResponse.newBuilder(), { ret, r ->
            val bin = m.image.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(bin, 0, bin.size)
            r.root?.setImageViewBitmap(m.id, bitmap)
            ret.success = true
        }) {
            it.success = false
        }
    }

    @SuppressLint("ApplySharedPref")
    fun setWidget(m: SetWidgetLayoutRequest) {
        handler.handleRemote(m.rid, SetWidgetLayoutResponse.newBuilder(), { ret, r ->
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
            val w = GUIWidget.getIDMappingPrefs(app)?.getInt(m.wid, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
            val p = GUIWidget.getIDMappingPrefs(app)
            if (p != null && w != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val l = p.getInt("$w-layout", 1)
                if (l == 1) {
                    p.edit().putInt("$w-layout", 2).commit()
                } else {
                    p.edit().putInt("$w-layout", 1).commit()
                }
                val layout = R.layout::class.java
                val id = try {
                    layout.getDeclaredField("remote_view_root_real$l").getInt(null)
                } catch (_:Exception) {
                    null
                }
                if (id != null) {
                    val rvReal = RemoteViews(app.packageName, id)
                    rvReal.addView(R.id.root_real, r.root)
                    Util.runOnUIThreadBlocking {
                        AppWidgetManager.getInstance(app).updateAppWidget(w, rvReal)
                    }
                    ret.success = true
                    ret.code = Error.OK
                }
            }
        }) {
            it.success = false
        }
    }
    
    
    
    
    
    
    
}