package com.termux.gui.protocol.protobuf.v0

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.LocalSocket
import android.os.Build
import android.view.WindowManager
import com.termux.gui.GUIActivity
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
            while (! Thread.currentThread().isInterrupted) {
                val m = GUIProt0.Method.parseDelimitedFrom(input)
                when (m.methodCase) {
                    GUIProt0.Method.MethodCase.NEWACTIVITY -> newActivity(m.newActivity)
                    GUIProt0.Method.MethodCase.FINISHACTIVITY -> TODO()
                    GUIProt0.Method.MethodCase.FINISHTASK -> TODO()
                    GUIProt0.Method.MethodCase.BRINGTASKTOFRONT -> TODO()
                    GUIProt0.Method.MethodCase.MOVETASKTOBACK -> TODO()
                    GUIProt0.Method.MethodCase.SETTHEME -> TODO()
                    GUIProt0.Method.MethodCase.SETTASKDESCRIPTION -> TODO()
                    GUIProt0.Method.MethodCase.METHOD_NOT_SET -> TODO()
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
        eventQueue.offer(GUIProt0.Event.newBuilder().setPip(GUIProt0.PiPChangedEvent.newBuilder().setPip(isInPictureInPictureMode)).build())
    }

    override fun onUserLeaveHint(a: GUIActivity) {
        eventQueue.offer(GUIProt0.Event.newBuilder().setUserLeaveHint(GUIProt0.UserLeaveHintEvent.newBuilder().setAid(a.intent?.dataString)).build())
    }


}