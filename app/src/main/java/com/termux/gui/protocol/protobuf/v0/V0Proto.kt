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
import com.termux.gui.GUIActivity
import com.termux.gui.R
import com.termux.gui.Util
import com.termux.gui.protocol.shared.v0.DataClasses
import com.termux.gui.protocol.shared.v0.V0Shared
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import com.termux.gui.protocol.protobuf.v0.GUIProt0.*

class V0Proto(app: Context, private val eventQueue: LinkedBlockingQueue<Event>) : V0Shared(app) {
    


    fun handleConnection(main: LocalSocket) {
        val am = app.getSystemService(ActivityManager::class.java)
        val wm = app.getSystemService(WindowManager::class.java)
        withSystemListenersAndCleanup(am, wm) {
            val input = main.inputStream
            val out = main.outputStream
            val handleActivity = HandleActivity(this, out, activities, wm, overlays)
            val handleGlobal = HandleGlobal(out,  tasks)
            val handleCreate = HandleCreate(this, out, activities, wm, overlays, rand, eventQueue)
            while (! Thread.currentThread().isInterrupted) {
                val m = Method.parseDelimitedFrom(input)
                when (m.methodCase) {
                    Method.MethodCase.NEWACTIVITY -> handleActivity.newActivity(m.newActivity)
                    Method.MethodCase.FINISHACTIVITY -> handleActivity.finishActivity(m.finishActivity)
                    Method.MethodCase.FINISHTASK -> handleGlobal.finishTask(m.finishTask)
                    Method.MethodCase.BRINGTASKTOFRONT -> handleGlobal.bringTaskToFront(m.bringTaskToFront)
                    Method.MethodCase.MOVETASKTOBACK -> handleActivity.moveTaskToBack(m.moveTaskToBack)
                    Method.MethodCase.SETTHEME -> handleActivity.setTheme(m.setTheme)
                    Method.MethodCase.SETTASKDESCRIPTION -> handleActivity.setTaskDescription(m.setTaskDescription)
                    Method.MethodCase.SETPIPPARAMS -> handleActivity.setPiPParams(m.setPiPParams)
                    Method.MethodCase.SETINPUTMODE -> handleActivity.setInputMode(m.setInputMode)
                    Method.MethodCase.SETPIPMODE -> handleActivity.setPiPMode(m.setPiPMode)
                    Method.MethodCase.SETPIPMODEAUTO -> handleActivity.setPiPModeAuto(m.setPiPModeAuto)
                    Method.MethodCase.TOAST -> handleGlobal.toast(m.toast)
                    Method.MethodCase.KEEPSCREENON -> handleActivity.keepScreenOn(m.keepScreenOn)
                    Method.MethodCase.SETORIENTATION -> handleActivity.setOrientation(m.setOrientation)
                    Method.MethodCase.SETPOSITION -> handleActivity.setPosition(m.setPosition)
                    Method.MethodCase.GETCONFIGURATION -> handleActivity.getConfiguration(m.getConfiguration)
                    Method.MethodCase.TURNSCREENON -> handleGlobal.turnScreenOn(m.turnScreenOn)
                    Method.MethodCase.ISLOCKED -> handleGlobal.isLocked(m.isLocked)
                    Method.MethodCase.REQUESTUNLOCK -> handleActivity.requestUnlock(m.requestUnlock)
                    Method.MethodCase.HIDESOFTKEYBOARD -> handleActivity.hideSoftKeyboard(m.hideSoftKeyboard)
                    Method.MethodCase.INTERCEPTBACKBUTTON -> handleActivity.interceptBackButton(m.interceptBackButton)
                    Method.MethodCase.VERSION -> handleGlobal.version()
                    
                    Method.MethodCase.CREATELINEARLAYOUT -> handleCreate.linear(m.createLinearLayout)
                    Method.MethodCase.CREATEFRAMELAYOUT -> handleCreate.frame(m.createFrameLayout)
                    Method.MethodCase.CREATESWIPEREFRESHLAYOUT -> handleCreate.swipeRefresh(m.createSwipeRefreshLayout)
                    Method.MethodCase.CREATETEXTVIEW -> handleCreate.text(m.createTextView)
                    Method.MethodCase.CREATEEDITTEXT -> handleCreate.edit(m.createEditText)
                    Method.MethodCase.CREATEBUTTON -> handleCreate.button(m.createButton)
                    Method.MethodCase.CREATEIMAGEVIEW -> handleCreate.image(m.createImageView)
                    Method.MethodCase.CREATESPACE -> handleCreate.space(m.createSpace)
                    Method.MethodCase.CREATENESTEDSCROLLVIEW -> handleCreate.nestedScroll(m.createNestedScrollView)
                    Method.MethodCase.CREATEHORIZONTALSCROLLVIEW -> handleCreate.horizontalScroll(m.createHorizontalScrollView)
                    Method.MethodCase.CREATERADIOGROUP -> handleCreate.radioGroup(m.createRadioGroup)
                    Method.MethodCase.CREATERADIOBUTTON -> handleCreate.radio(m.createRadioButton)
                    Method.MethodCase.CREATECHECKBOX -> handleCreate.checkbox(m.createCheckbox)
                    Method.MethodCase.CREATETOGGLEBUTTON -> handleCreate.toggleButton(m.createToggleButton)
                    Method.MethodCase.CREATESWITCH -> handleCreate.switch(m.createSwitch)
                    Method.MethodCase.CREATESPINNER -> handleCreate.spinner(m.createSpinner)
                    Method.MethodCase.CREATEPROGRESSBAR -> handleCreate.progressBar(m.createProgressBar)
                    Method.MethodCase.CREATETABLAYOUT -> handleCreate.tab(m.createTabLayout)
                    Method.MethodCase.CREATEWEBVIEW -> handleCreate.webView(m.createWebView)
                    
                    Method.MethodCase.SHOWCURSOR -> TODO()
                    Method.MethodCase.SETLINEARLAYOUT -> TODO()
                    Method.MethodCase.SETGRIDLAYOUT -> TODO()
                    Method.MethodCase.SETLOCATION -> TODO()
                    Method.MethodCase.SETRELATIVE -> TODO()
                    Method.MethodCase.SETVISIBILITY -> TODO()
                    Method.MethodCase.SETWIDTH -> TODO()
                    Method.MethodCase.SETHEIGHT -> TODO()
                    Method.MethodCase.GETDIMENSIONS -> TODO()
                    Method.MethodCase.DELETEVIEW -> TODO()
                    Method.MethodCase.DELETECHILDREN -> TODO()
                    Method.MethodCase.SETMARGIN -> TODO()
                    Method.MethodCase.SETPADDING -> TODO()
                    Method.MethodCase.SETBACKGROUNDCOLOR -> TODO()
                    Method.MethodCase.SETTEXTCOLOR -> TODO()
                    Method.MethodCase.SETPROGRESS -> TODO()
                    Method.MethodCase.SETREFRESHING -> TODO()
                    Method.MethodCase.SETTEXT -> TODO()
                    Method.MethodCase.SETGRAVITY -> TODO()
                    Method.MethodCase.SETTEXTSIZE -> TODO()
                    Method.MethodCase.GETTEXT -> TODO()
                    Method.MethodCase.REQUESTFOCUS -> TODO()
                    Method.MethodCase.GETSCROLLPOSITION -> TODO()
                    Method.MethodCase.SETSCROLLPOSITION -> TODO()
                    Method.MethodCase.SETLIST -> TODO()
                    Method.MethodCase.SETIMAGE -> TODO()
                    Method.MethodCase.ADDBUFFER -> TODO()
                    Method.MethodCase.DELETEBUFFER -> TODO()
                    Method.MethodCase.BLITBUFFER -> TODO()
                    Method.MethodCase.SETBUFFER -> TODO()
                    Method.MethodCase.REFRESHIMAGEVIEW -> TODO()
                    Method.MethodCase.SELECTTAB -> TODO()
                    Method.MethodCase.SELECTITEM -> TODO()
                    Method.MethodCase.SETCLICKABLE -> TODO()
                    
                    Method.MethodCase.CREATEREMOTELAYOUT -> TODO()
                    Method.MethodCase.DELETEREMOTELAYOUT -> TODO()
                    Method.MethodCase.ADDREMOTEFRAMELAYOUT -> TODO()
                    Method.MethodCase.ADDREMOTELINEARLAYOUT -> TODO()
                    Method.MethodCase.ADDREMOTETEXTVIEW -> TODO()
                    Method.MethodCase.ADDREMOTEBUTTON -> TODO()
                    Method.MethodCase.ADDREMOTEIMAGEVIEW -> TODO()
                    Method.MethodCase.ADDREMOTEPROGRESSBAR -> TODO()
                    Method.MethodCase.SETREMOTEBACKGROUNDCOLOR -> TODO()
                    Method.MethodCase.SETREMOTEPROGRESSBAR -> TODO()
                    Method.MethodCase.SETREMOTETEXT -> TODO()
                    Method.MethodCase.SETREMOTETEXTSIZE -> TODO()
                    Method.MethodCase.SETREMOTETEXTCOLOR -> TODO()
                    Method.MethodCase.SETREMOTEVISIBILITY -> TODO()
                    Method.MethodCase.SETREMOTEPADDING -> TODO()
                    Method.MethodCase.SETREMOTEIMAGE -> TODO()
                    Method.MethodCase.SETWIDGETLAYOUT -> TODO()
                    
                    Method.MethodCase.ALLOWJS -> TODO()
                    Method.MethodCase.ALLOWCONTENT -> TODO()
                    Method.MethodCase.SETDATA -> TODO()
                    Method.MethodCase.LOADURI -> TODO()
                    Method.MethodCase.ALLOWNAVIGATION -> TODO()
                    Method.MethodCase.GOBACK -> TODO()
                    Method.MethodCase.GOFORWARD -> TODO()
                    Method.MethodCase.EVALUATEJS -> TODO()
                    
                    Method.MethodCase.CREATECHANNEL -> TODO()
                    Method.MethodCase.CREATENOTIFICATION -> TODO()
                    Method.MethodCase.CANCELNOTIFICATION -> TODO()
                    
                    Method.MethodCase.SENDCLICKEVENT -> TODO()
                    Method.MethodCase.SENDLONGCLICKEVENT -> TODO()
                    Method.MethodCase.SENDFOCUSCHANGEEVENT -> TODO()
                    Method.MethodCase.SENDTOUCHEVENT -> TODO()
                    Method.MethodCase.SENDGESTUREEVENT -> TODO()
                    Method.MethodCase.SENDTEXTEVENT -> TODO()
                    Method.MethodCase.SENDOVERLAYTOUCH -> TODO()
                    Method.MethodCase.METHOD_NOT_SET -> { return@withSystemListenersAndCleanup } // terminate the connection when nothing is in the oneof
                    null -> { return@withSystemListenersAndCleanup } // terminate the connection when nothing is in the oneof
                }
            }
        }
    }

    override fun onActivityCreated(state: DataClasses.ActivityState) {
        state.a?.aid?.let { eventQueue.offer(Event.newBuilder().setCreate(CreateEvent.newBuilder().setAid(it)).build()) }
    }

    override fun onActivityStarted(state: DataClasses.ActivityState) {
        state.a?.aid?.let { eventQueue.offer(Event.newBuilder().setStart(StartEvent.newBuilder().setAid(it)).build()) } 
    }

    override fun onActivityResumed(state: DataClasses.ActivityState) {
        state.a?.aid?.let { eventQueue.offer(Event.newBuilder().setResume(ResumeEvent.newBuilder().setAid(it)).build()) }
    }

    override fun onActivityPaused(state: DataClasses.ActivityState) {
        state.a?.aid?.let { eventQueue.offer(Event.newBuilder().setPause(PauseEvent.newBuilder().setAid(it).setFinishing(state.a?.isFinishing ?: false)).build()) }
    }

    override fun onActivityStopped(state: DataClasses.ActivityState) {
        state.a?.aid?.let { eventQueue.offer(Event.newBuilder().setStop(StopEvent.newBuilder().setAid(it).setFinishing(state.a?.isFinishing ?: false)).build()) }
    }

    override fun onActivityDestroyed(state: DataClasses.ActivityState) {
        state.a?.aid?.let {
            eventQueue.offer(Event.newBuilder().setDestroy(DestroyEvent.newBuilder().setAid(it).setFinishing(state.a?.isFinishing ?: false)).build())
        }
    }

    override fun onAirplaneModeChanged(c: Context, i: Intent) {
        eventQueue.offer(Event.newBuilder().setAirplane(AirplaneEvent.newBuilder().setActive(i.getBooleanExtra("state", false))).build())
    }

    override fun onLocaleChanged(c: Context, i: Intent) {
        eventQueue.offer(Event.newBuilder().setLocale(LocaleEvent.newBuilder().setLocale(c.resources.configuration.locales.get(0).language)).build())
    }

    override fun onScreenOff(c: Context, i: Intent) {
        eventQueue.offer(Event.newBuilder().setScreenOff(ScreenOffEvent.newBuilder()).build())
    }

    override fun onScreenOn(c: Context, i: Intent) {
        eventQueue.offer(Event.newBuilder().setScreenOn(ScreenOnEvent.newBuilder()).build())
    }

    override fun onTimezoneChanged(c: Context, i: Intent) {
        eventQueue.offer(Event.newBuilder().setTimezone(TimezoneEvent.newBuilder().setTz(TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT, c.resources.configuration.locales.get(0)))).build())
    }
    
    override fun onBackButton(a: GUIActivity) {
        a.aid?.let {
            eventQueue.offer(Event.newBuilder().setBack(BackButtonEvent.newBuilder().setAid(it)).build())
        }
    }

    override fun onRemoteButton(rid: Int, id: Int) {
        eventQueue.offer(Event.newBuilder().setRemoteClick(RemoteClickEvent.newBuilder().setRid(rid).setId(id)).build())
    }

    override fun onNotification(nid: Int) {
        eventQueue.offer(Event.newBuilder().setNotification(NotificationEvent.newBuilder().setId(nid)).build())
    }

    override fun onNotificationDismissed(nid: Int) {
        eventQueue.offer(Event.newBuilder().setNotificationDismissed(NotificationDismissedEvent.newBuilder().setId(nid)).build())
    }

    override fun onNotificationAction(nid: Int, action: Int) {
        eventQueue.offer(Event.newBuilder().setNotificationAction(NotificationActionEvent.newBuilder().setId(nid).setAction(action)).build())
    }

    override fun onConfigurationChanged(a: GUIActivity, newConfig: Configuration) {
        val e = ConfigEvent.newBuilder()
        e.setConfiguration(configMessage(a, newConfig))
        eventQueue.offer(Event.newBuilder().setConfig(e).build())
    }

    override fun onPictureInPictureModeChanged(a: GUIActivity, isInPictureInPictureMode: Boolean) {
        a.aid?.let {
            eventQueue.offer(Event.newBuilder().setPip(PiPChangedEvent.newBuilder().setPip(isInPictureInPictureMode).setAid(it)).build())
        }
    }

    override fun onUserLeaveHint(a: GUIActivity) {
        a.aid?.let {
            eventQueue.offer(Event.newBuilder().setUserLeaveHint(UserLeaveHintEvent.newBuilder().setAid(it)).build())
        }
    }

    @Suppress("DEPRECATION")
    fun generateOverlay(): Int {
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
            return -1
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
                                    eventQueue.offer(Event.newBuilder().setOverlayScale(OverlayScaleEvent.newBuilder()
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
                                MotionEvent.ACTION_DOWN -> TouchEvent.Action.down
                                MotionEvent.ACTION_UP -> TouchEvent.Action.up
                                MotionEvent.ACTION_MOVE -> TouchEvent.Action.move
                                else -> null
                            }
                            if (map["action"] != null) {
                                eventQueue.offer(Event.newBuilder().setTouch(TouchEvent.newBuilder()
                                    .setV(View.newBuilder().setAid(aid).setId(-1))
                                    .setAction(action)
                                    .addTouches(TouchEvent.Touch.newBuilder().addPointers(TouchEvent.Touch.Pointer.newBuilder().setX(
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
                return -1
            }
        }
    }
    
    companion object {
        fun configMessage(a: GUIActivity, config: Configuration): GUIProt0.Configuration.Builder {
            val c = GUIProt0.Configuration.newBuilder()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                c.darkMode = config.isNightModeActive
            }
            val l = config.locales.get(0)
            c.country = l.country
            c.language = l.language
            c.orientation = when (config.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> Orientation.landscape
                Configuration.ORIENTATION_PORTRAIT -> Orientation.portrait
                else -> Orientation.portrait
            }
            c.keyboardHidden = when (config.keyboardHidden) {
                Configuration.KEYBOARDHIDDEN_NO -> false
                Configuration.KEYBOARDHIDDEN_YES -> true
                else -> true
            }
            c.screenWidth = config.screenWidthDp
            c.screenHeight = config.screenHeightDp
            c.fontscale = config.fontScale
            c.density = a.resources.displayMetrics.density
            return c
        }
    }
    
}