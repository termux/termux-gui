package com.termux.gui.protocol.protobuf.v0

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.net.LocalSocket
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.WindowManager
import android.widget.Toast
import com.termux.gui.ConnectionHandler
import com.termux.gui.GUIActivity
import com.termux.gui.R
import com.termux.gui.Util
import com.termux.gui.protocol.shared.v0.DataClasses
import com.termux.gui.protocol.shared.v0.V0Shared
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class V0Proto(app: Context, private val eventQueue: LinkedBlockingQueue<GUIProt0.Event>) : V0Shared(app) {
    


    fun handleConnection(main: LocalSocket) {
        val am = app.getSystemService(ActivityManager::class.java)
        val wm = app.getSystemService(WindowManager::class.java)
        withSystemListenersAndCleanup(am, wm) {
            val input = main.inputStream
            val out = main.outputStream
            while (! Thread.currentThread().isInterrupted) {
                val m = GUIProt0.Method.parseDelimitedFrom(input)
                when (m.methodCase) {
                    GUIProt0.Method.MethodCase.NEWACTIVITY -> HandleActivity.newActivity(m.newActivity, out, this)
                    GUIProt0.Method.MethodCase.FINISHACTIVITY -> TODO()
                    GUIProt0.Method.MethodCase.FINISHTASK -> TODO()
                    GUIProt0.Method.MethodCase.BRINGTASKTOFRONT -> TODO()
                    GUIProt0.Method.MethodCase.MOVETASKTOBACK -> TODO()
                    GUIProt0.Method.MethodCase.SETTHEME -> TODO()
                    GUIProt0.Method.MethodCase.SETTASKDESCRIPTION -> TODO()
                    GUIProt0.Method.MethodCase.SETPIPPARAMS -> TODO()
                    GUIProt0.Method.MethodCase.SETINPUTMODE -> TODO()
                    GUIProt0.Method.MethodCase.SETPIPMODE -> TODO()
                    GUIProt0.Method.MethodCase.SETPIPMODEAUTO -> TODO()
                    GUIProt0.Method.MethodCase.TOAST -> TODO()
                    GUIProt0.Method.MethodCase.KEEPSCREENON -> TODO()
                    GUIProt0.Method.MethodCase.SETORIENTATION -> TODO()
                    GUIProt0.Method.MethodCase.SETPOSITION -> TODO()
                    GUIProt0.Method.MethodCase.GETCONFIGURATION -> TODO()
                    GUIProt0.Method.MethodCase.TURNSCREENON -> TODO()
                    GUIProt0.Method.MethodCase.ISLOCKED -> TODO()
                    GUIProt0.Method.MethodCase.REQUESTUNLOCK -> TODO()
                    GUIProt0.Method.MethodCase.HIDESOFTKEYBOARD -> TODO()
                    GUIProt0.Method.MethodCase.INTERCEPTBACKBUTTON -> TODO()
                    GUIProt0.Method.MethodCase.VERSION -> TODO()
                    GUIProt0.Method.MethodCase.CREATELINEARLAYOUT -> TODO()
                    GUIProt0.Method.MethodCase.CREATEFRAMELAYOUT -> TODO()
                    GUIProt0.Method.MethodCase.CREATESWIPEREFRESHLAYOUT -> TODO()
                    GUIProt0.Method.MethodCase.CREATETEXTVIEW -> TODO()
                    GUIProt0.Method.MethodCase.CREATEEDITTEXT -> TODO()
                    GUIProt0.Method.MethodCase.CREATEBUTTON -> TODO()
                    GUIProt0.Method.MethodCase.CREATEIMAGEVIEW -> TODO()
                    GUIProt0.Method.MethodCase.CREATESPACE -> TODO()
                    GUIProt0.Method.MethodCase.CREATENESTEDSCROLLVIEW -> TODO()
                    GUIProt0.Method.MethodCase.CREATEHORIZONTALSCROLLVIEW -> TODO()
                    GUIProt0.Method.MethodCase.CREATERADIOGROUP -> TODO()
                    GUIProt0.Method.MethodCase.CREATERADIOBUTTON -> TODO()
                    GUIProt0.Method.MethodCase.CREATECHECKBOX -> TODO()
                    GUIProt0.Method.MethodCase.CREATETOGGLEBUTTON -> TODO()
                    GUIProt0.Method.MethodCase.CREATESWITCH -> TODO()
                    GUIProt0.Method.MethodCase.CREATESPINNER -> TODO()
                    GUIProt0.Method.MethodCase.CREATEPROGRESSBAR -> TODO()
                    GUIProt0.Method.MethodCase.CREATETABLAYOUT -> TODO()
                    GUIProt0.Method.MethodCase.CREATEWEBVIEW -> TODO()
                    GUIProt0.Method.MethodCase.SHOWCURSOR -> TODO()
                    GUIProt0.Method.MethodCase.SETLINEARLAYOUT -> TODO()
                    GUIProt0.Method.MethodCase.SETGRIDLAYOUT -> TODO()
                    GUIProt0.Method.MethodCase.SETLOCATION -> TODO()
                    GUIProt0.Method.MethodCase.SETRELATIVE -> TODO()
                    GUIProt0.Method.MethodCase.SETVISIBILITY -> TODO()
                    GUIProt0.Method.MethodCase.SETWIDTH -> TODO()
                    GUIProt0.Method.MethodCase.SETHEIGHT -> TODO()
                    GUIProt0.Method.MethodCase.GETDIMENSIONS -> TODO()
                    GUIProt0.Method.MethodCase.DELETEVIEW -> TODO()
                    GUIProt0.Method.MethodCase.DELETECHILDREN -> TODO()
                    GUIProt0.Method.MethodCase.SETMARGIN -> TODO()
                    GUIProt0.Method.MethodCase.SETPADDING -> TODO()
                    GUIProt0.Method.MethodCase.SETBACKGROUNDCOLOR -> TODO()
                    GUIProt0.Method.MethodCase.SETTEXTCOLOR -> TODO()
                    GUIProt0.Method.MethodCase.SETPROGRESS -> TODO()
                    GUIProt0.Method.MethodCase.SETREFRESHING -> TODO()
                    GUIProt0.Method.MethodCase.SETTEXT -> TODO()
                    GUIProt0.Method.MethodCase.SETGRAVITY -> TODO()
                    GUIProt0.Method.MethodCase.SETTEXTSIZE -> TODO()
                    GUIProt0.Method.MethodCase.GETTEXT -> TODO()
                    GUIProt0.Method.MethodCase.REQUESTFOCUS -> TODO()
                    GUIProt0.Method.MethodCase.GETSCROLLPOSITION -> TODO()
                    GUIProt0.Method.MethodCase.SETSCROLLPOSITION -> TODO()
                    GUIProt0.Method.MethodCase.SETLIST -> TODO()
                    GUIProt0.Method.MethodCase.SETIMAGE -> TODO()
                    GUIProt0.Method.MethodCase.ADDBUFFER -> TODO()
                    GUIProt0.Method.MethodCase.DELETEBUFFER -> TODO()
                    GUIProt0.Method.MethodCase.BLITBUFFER -> TODO()
                    GUIProt0.Method.MethodCase.SETBUFFER -> TODO()
                    GUIProt0.Method.MethodCase.REFRESHIMAGEVIEW -> TODO()
                    GUIProt0.Method.MethodCase.SELECTTAB -> TODO()
                    GUIProt0.Method.MethodCase.SETCLICKABLE -> TODO()
                    GUIProt0.Method.MethodCase.CREATEREMOTELAYOUT -> TODO()
                    GUIProt0.Method.MethodCase.DELETEREMOTELAYOUT -> TODO()
                    GUIProt0.Method.MethodCase.ADDREMOTEFRAMELAYOUT -> TODO()
                    GUIProt0.Method.MethodCase.ADDREMOTELINEARLAYOUT -> TODO()
                    GUIProt0.Method.MethodCase.ADDREMOTETEXTVIEW -> TODO()
                    GUIProt0.Method.MethodCase.ADDREMOTEBUTTON -> TODO()
                    GUIProt0.Method.MethodCase.ADDREMOTEIMAGEVIEW -> TODO()
                    GUIProt0.Method.MethodCase.ADDREMOTEPROGRESSBAR -> TODO()
                    GUIProt0.Method.MethodCase.SETREMOTEBACKGROUNDCOLOR -> TODO()
                    GUIProt0.Method.MethodCase.SETREMOTEPROGRESSBAR -> TODO()
                    GUIProt0.Method.MethodCase.SETREMOTETEXT -> TODO()
                    GUIProt0.Method.MethodCase.SETREMOTETEXTSIZE -> TODO()
                    GUIProt0.Method.MethodCase.SETREMOTETEXTCOLOR -> TODO()
                    GUIProt0.Method.MethodCase.SETREMOTEVISIBILITY -> TODO()
                    GUIProt0.Method.MethodCase.SETREMOTEPADDING -> TODO()
                    GUIProt0.Method.MethodCase.SETREMOTEIMAGE -> TODO()
                    GUIProt0.Method.MethodCase.SETWIDGETLAYOUT -> TODO()
                    GUIProt0.Method.MethodCase.ALLOWJS -> TODO()
                    GUIProt0.Method.MethodCase.ALLOWCONTENT -> TODO()
                    GUIProt0.Method.MethodCase.SETDATA -> TODO()
                    GUIProt0.Method.MethodCase.LOADURI -> TODO()
                    GUIProt0.Method.MethodCase.ALLOWNAVIGATION -> TODO()
                    GUIProt0.Method.MethodCase.GOBACK -> TODO()
                    GUIProt0.Method.MethodCase.GOFORWARD -> TODO()
                    GUIProt0.Method.MethodCase.EVALUATEJS -> TODO()
                    GUIProt0.Method.MethodCase.CREATECHANNEL -> TODO()
                    GUIProt0.Method.MethodCase.CREATENOTIFICATION -> TODO()
                    GUIProt0.Method.MethodCase.CANCELNOTIFICATION -> TODO()
                    GUIProt0.Method.MethodCase.SENDCLICKEVENT -> TODO()
                    GUIProt0.Method.MethodCase.SENDLONGCLICKEVENT -> TODO()
                    GUIProt0.Method.MethodCase.SENDFOCUSCHANGEEVENT -> TODO()
                    GUIProt0.Method.MethodCase.SENDTOUCHEVENT -> TODO()
                    GUIProt0.Method.MethodCase.SENDGESTUREEVENT -> TODO()
                    GUIProt0.Method.MethodCase.SENDTEXTEVENT -> TODO()
                    GUIProt0.Method.MethodCase.SENDOVERLAYTOUCH -> TODO()
                    GUIProt0.Method.MethodCase.METHOD_NOT_SET -> { return@withSystemListenersAndCleanup } // terminate the connection when nothing is in the oneof
                    null -> { return@withSystemListenersAndCleanup } // terminate the connection when nothing is in the oneof
                }
            }
        }
    }

    override fun onActivityCreated(state: DataClasses.ActivityState) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setCreate(GUIProt0.CreateEvent.newBuilder().setAid(state.a?.aid)).build())
    }

    override fun onActivityStarted(state: DataClasses.ActivityState) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setStart(GUIProt0.StartEvent.newBuilder().setAid(state.a?.aid)).build())
    }

    override fun onActivityResumed(state: DataClasses.ActivityState) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setResume(GUIProt0.ResumeEvent.newBuilder().setAid(state.a?.aid)).build())
    }

    override fun onActivityPaused(state: DataClasses.ActivityState) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setPause(GUIProt0.PauseEvent.newBuilder().setAid(state.a?.aid).setFinishing(state.a?.isFinishing ?: false)).build())
    }

    override fun onActivityStopped(state: DataClasses.ActivityState) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setStop(GUIProt0.StopEvent.newBuilder().setAid(state.a?.aid).setFinishing(state.a?.isFinishing ?: false)).build())
    }

    override fun onActivityDestroyed(state: DataClasses.ActivityState) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setDestroy(GUIProt0.DestroyEvent.newBuilder().setAid(state.a?.aid).setFinishing(state.a?.isFinishing ?: false)).build())
    }

    override fun onAirplaneModeChanged(c: Context, i: Intent) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setAirplane(GUIProt0.AirplaneEvent.newBuilder().setActive(i.getBooleanExtra("state", false))).build())
    }

    override fun onLocaleChanged(c: Context, i: Intent) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setLocale(GUIProt0.LocaleEvent.newBuilder().setLocale(c.resources.configuration.locales.get(0).language)).build())
    }

    override fun onScreenOff(c: Context, i: Intent) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setScreenOff(GUIProt0.ScreenOffEvent.newBuilder()).build())
    }

    override fun onScreenOn(c: Context, i: Intent) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setScreenOn(GUIProt0.ScreenOnEvent.newBuilder()).build())
    }

    override fun onTimezoneChanged(c: Context, i: Intent) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setTimezone(GUIProt0.TimezoneEvent.newBuilder().setTz(TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT, c.resources.configuration.locales.get(0)))).build())
    }
    
    override fun onBackButton(a: GUIActivity) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setBack(GUIProt0.BackButtonEvent.newBuilder().setAid(a.aid)).build())
    }

    override fun onRemoteButton(rid: Int, id: Int) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setRemoteClick(GUIProt0.RemoteClickEvent.newBuilder().setRid(rid).setId(id)).build())
    }

    override fun onNotification(nid: Int) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setNotification(GUIProt0.NotificationEvent.newBuilder().setId(nid)).build())
    }

    override fun onNotificationAction(nid: Int, action: Int) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setNotificationAction(GUIProt0.NotificationActionEvent.newBuilder().setId(nid).setAction(action)).build())
    }

    override fun onConfigurationChanged(a: GUIActivity, newConfig: Configuration) {
        val e = GUIProt0.ConfigEvent.newBuilder()
        val c = GUIProt0.Configuration.newBuilder()
        e.aid = a.aid
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            c.darkMode = newConfig.isNightModeActive
        }
        val l = newConfig.locales.get(0)
        c.country = l.country
        c.language = l.language
        c.orientation = when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> GUIProt0.Configuration.Orientation.landscape
            Configuration.ORIENTATION_PORTRAIT -> GUIProt0.Configuration.Orientation.portrait
            else -> GUIProt0.Configuration.Orientation.portrait
        }
        c.keyboardHidden = when (newConfig.keyboardHidden) {
            Configuration.KEYBOARDHIDDEN_NO -> false
            Configuration.KEYBOARDHIDDEN_YES -> true
            else -> true
        }
        c.screenWidth = newConfig.screenWidthDp
        c.screenHeight = newConfig.screenHeightDp
        c.fontscale = newConfig.fontScale
        c.density = a.resources.displayMetrics.density
        e.setConfiguration(c)
        eventQueue.offer(GUIProt0.Event.newBuilder().setConfig(e).build())
    }

    override fun onPictureInPictureModeChanged(a: GUIActivity, isInPictureInPictureMode: Boolean) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setPip(GUIProt0.PiPChangedEvent.newBuilder().setPip(isInPictureInPictureMode).setAid(a.aid)).build())
    }

    override fun onUserLeaveHint(a: GUIActivity) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setUserLeaveHint(GUIProt0.UserLeaveHintEvent.newBuilder().setAid(a.aid)).build())
    }

    @Suppress("DEPRECATION")
    fun generateOverlay(): String {
        val wm = app.getSystemService(WindowManager::class.java)
        if (!Settings.canDrawOverlays(app)) {
            try {
                val a = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                a.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                a.data = Uri.parse(app.packageName)
                app.startActivity(a)
            } catch (ignored: Exception) {
                Util.runOnUIThreadBlocking {
                    Toast.makeText(app, R.string.overlay_settings, Toast.LENGTH_LONG).show()
                }
            }
            return "-1"
        } else {
            val aid = generateActivityID()
            val o = DataClasses.Overlay(app)
            overlays[aid] = o
            try {
                Util.runOnUIThreadBlocking {
                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
                        0,
                        PixelFormat.RGBA_8888
                    )
                    params.x = 0
                    params.y = 0
                    params.gravity = Gravity.START or Gravity.TOP
                    params.flags =
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    val scale = ScaleGestureDetector(
                        app,
                        object : ScaleGestureDetector.OnScaleGestureListener {
                            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                                if (o.sendTouch && detector != null) {
                                    eventQueue.offer(GUIProt0.Event.newBuilder().setOverlayScale(GUIProt0.OverlayScaleEvent.newBuilder()
                                        .setAid(aid)
                                        .setSpan(detector.currentSpan)
                                    ).build())
                                }
                                return true
                            }

                            override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
                                return true
                            }

                            override fun onScaleEnd(detector: ScaleGestureDetector?) {}
                        })
                    o.root.interceptListener = { e ->
                        if (o.sendTouch) {
                            val map = HashMap<String, Any?>()
                            map["x"] = e.rawX
                            map["y"] = e.rawY
                            val action = when (e.action) {
                                MotionEvent.ACTION_DOWN -> GUIProt0.TouchEvent.Action.down
                                MotionEvent.ACTION_UP -> GUIProt0.TouchEvent.Action.up
                                MotionEvent.ACTION_MOVE -> GUIProt0.TouchEvent.Action.move
                                else -> null
                            }
                            if (map["action"] != null) {
                                eventQueue.offer(GUIProt0.Event.newBuilder().setTouch(GUIProt0.TouchEvent.newBuilder()
                                    .setV(GUIProt0.View.newBuilder().setAid(aid).setId(-1))
                                    .setAction(action)
                                    .addTouches(GUIProt0.TouchEvent.Touch.newBuilder().addPointers(GUIProt0.TouchEvent.Touch.Pointer.newBuilder().setX(
                                        e.rawX.toInt()
                                    ).setY(e.rawY.toInt()).setId(0)))
                                    .setTime(System.currentTimeMillis())
                                    .setIndex(0)
                                ).build())
                            }
                        }
                        if (o.root.inside(e)) {
                            scale.onTouchEvent(e)
                            params.flags = 0
                            wm.updateViewLayout(o.root, params)
                        } else {
                            scale.onTouchEvent(e)
                            params.flags =
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            wm.updateViewLayout(o.root, params)
                        }
                    }
                    wm.addView(o.root, params)
                }
                return aid
            } catch (e: Exception) {
                e.printStackTrace()
                overlays.remove(aid)
                return "-1"
            }
        }
    }
    
}