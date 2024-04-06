package com.termux.gui.protocol.protobuf.v0

import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.termux.gui.App
import com.termux.gui.GUIActivity
import com.termux.gui.R
import com.termux.gui.Util
import com.termux.gui.protocol.protobuf.ProtoUtils
import com.termux.gui.protocol.shared.v0.DataClasses
import com.termux.gui.protocol.shared.v0.V0Shared
import java.io.OutputStream
import java.lang.Exception
import com.termux.gui.protocol.protobuf.v0.GUIProt0.*

class HandleActivity(val v: V0Proto, val main: OutputStream, val activities: MutableMap<Int, DataClasses.ActivityState>,
                     val wm: WindowManager, val overlays: MutableMap<Int, DataClasses.Overlay>, val logger: V0Proto.ProtoLogger) {
    
    
    
    fun newActivity(m: NewActivityRequest) {
        var pip = false
        var dialog = false
        var overlay = false
        var lockscreen = false
        var canceloutside = false
        
        val ret = NewActivityResponse.newBuilder()
        try {
            when (m.type!!) {
                NewActivityRequest.ActivityType.normal -> {}
                NewActivityRequest.ActivityType.dialog -> dialog = true
                NewActivityRequest.ActivityType.dialogCancelOutside -> {
                    dialog = true
                    canceloutside = true
                }
                NewActivityRequest.ActivityType.pip -> pip = true
                NewActivityRequest.ActivityType.lockscreen -> lockscreen = true
                NewActivityRequest.ActivityType.overlay -> overlay = true
                NewActivityRequest.ActivityType.UNRECOGNIZED -> {
                    ProtoUtils.write(ret.setAid(-1).setTid(-1).build(), main)
                    return
                }
            }
            if (overlay) {
                ret.aid = v.generateOverlay()
                ret.tid = -1
            } else {
                val a = v.newActivity(if (m.tid == -1) null else m.tid, pip, dialog, lockscreen, canceloutside, m.interceptBackButton)
                val aid = a?.aid
                if (a != null && aid != null) {
                    ret.aid = aid
                    ret.tid = a.taskId
                } else {
                    ret.aid = -1
                    ret.tid = -1
                }
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.aid = -1
            ret.tid = -1
        }
        ProtoUtils.write(ret, main)
    }
    
    fun finishActivity(m: FinishActivityRequest) {
        val ret = FinishActivityResponse.newBuilder()
        
        try {
            val aid = m.aid
            val a = activities[aid]
            val o = overlays[aid]
            if (a != null) {
                if(V0Shared.runOnUIThreadActivityStartedBlocking(a) {
                        it.finish()
                        ret.success = true
                }) {
                    ret.success = false
                    ret.code = Error.INVALID_ACTIVITY
                }
            } else {
                if (o != null) {
                    wm.removeView(o.root)
                    overlays.remove(aid)
                    ret.success = true
                } else {
                    ret.success = false
                    ret.code = Error.INVALID_ACTIVITY
                }
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        
        ProtoUtils.write(ret, main)
    }

    fun moveTaskToBack(m: MoveTaskToBackRequest) {
        val ret = MoveTaskToBackResponse.newBuilder()
        try {
            Util.runOnUIThreadBlocking {
                val a = activities[m.aid]?.a
                if (a != null) {
                    ret.success = a.moveTaskToBack(true)
                } else {
                    ret.success = false
                    ret.code = Error.INVALID_ACTIVITY
                }
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }

    fun setTheme(m: SetThemeRequest) {
        val ret = SetThemeResponse.newBuilder()
        try {
            if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    it.theme = GUIActivity.GUITheme(m.statusBarColor, m.colorPrimary, m.windowBackground, m.textColor, m.colorAccent)
                    ret.success = true
            }) {
                ret.success = false
                ret.code = Error.INVALID_ACTIVITY
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }

    @Suppress("DEPRECATION")
    fun setTaskDescription(m: SetTaskDescriptionRequest) {
        val ret = SetTaskDescriptionResponse.newBuilder()
        try {
            if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    val t = it.theme
                    val prim = t?.colorPrimary ?: (0xFF000000).toInt()
                    if (m.img.isEmpty) {
                        it.setTaskDescription(ActivityManager.TaskDescription(m.label, BitmapFactory.decodeResource(it.resources, R.mipmap.ic_launcher_round), prim))
                    } else {
                        val bin = m.img.toByteArray()
                        it.setTaskDescription(ActivityManager.TaskDescription(m.label, BitmapFactory.decodeByteArray(bin, 0, bin.size), prim))
                    }
                    ret.success = true
                }) {
                ret.success = false
                ret.code = Error.INVALID_ACTIVITY
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }

    fun setPiPParams(m: SetPiPParamsRequest) {
        val ret = SetPiPParamsResponse.newBuilder()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    val rat = Rational(m.num, m.den)
                        .coerceAtMost(Rational(239, 100))
                        .coerceAtLeast(Rational(100, 239))
                    it.setPictureInPictureParams(PictureInPictureParams.Builder().setAspectRatio(rat).build())
                    ret.success = true
                }) {
                ret.success = false
                ret.code = Error.INVALID_ACTIVITY
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }

    @Suppress("DEPRECATION")
    fun setInputMode(m: SetInputModeRequest) {
        val ret = SetInputModeResponse.newBuilder()
        try {
            if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    when (m.mode!!) {
                        SetInputModeRequest.InputMode.pan -> {
                            it.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
                            ret.success = true
                        }
                        SetInputModeRequest.InputMode.resize -> {
                            it.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                            ret.success = true
                        }
                        SetInputModeRequest.InputMode.UNRECOGNIZED -> ret.success = false
                    }
                }) {
                ret.success = false
                ret.code = Error.INVALID_ACTIVITY
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }

    @Suppress("DEPRECATION")
    fun setPiPMode(m: SetPiPModeRequest) {
        val ret = SetPiPModeResponse.newBuilder()
        try {
            if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    if (m.pip) {
                        it.enterPictureInPictureMode()
                    } else {
                        it.moveTaskToBack(true)
                    }
                    ret.success = true
                }) {
                ret.success = false
                ret.code = Error.INVALID_ACTIVITY
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }

    fun setPiPModeAuto(m: SetPiPModeAutoRequest) {
        val ret = SetPiPModeAutoResponse.newBuilder()
        try {
            if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    it.data.autopip = m.pip
                    ret.success = true
                }) {
                ret.success = false
                ret.code = Error.INVALID_ACTIVITY
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }

    fun keepScreenOn(m: KeepScreenOnRequest) {
        val ret = KeepScreenOnResponse.newBuilder()
        try {
            if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    if (m.on) {
                        it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        it.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    ret.success = true
                }) {
                ret.success = false
                ret.code = Error.INVALID_ACTIVITY
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }


    fun setOrientation(m: SetOrientationRequest) {
        val ret = SetOrientationResponse.newBuilder()
        try {
            if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    ret.success = true
                    it.requestedOrientation = when (m.orientation) {
                        Orientation.behind -> ActivityInfo.SCREEN_ORIENTATION_BEHIND
                        Orientation.fullSensor -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                        Orientation.fullUser -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                        Orientation.landscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        Orientation.locked -> ActivityInfo.SCREEN_ORIENTATION_LOCKED
                        Orientation.nosensor -> ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
                        Orientation.portrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        Orientation.reverseLandscape -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        Orientation.reversePortrait -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                        Orientation.sensor -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                        Orientation.sensorLandscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        Orientation.sensorPortrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                        Orientation.user -> ActivityInfo.SCREEN_ORIENTATION_USER
                        Orientation.userLandscape -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                        Orientation.userPortrait -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                        else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }) {
                ret.success = false
                ret.code = Error.INVALID_ACTIVITY
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }


    fun setPosition(m: SetPositionRequest) {
        val ret = SetPositionResponse.newBuilder()
        try {
            val o = overlays[m.aid]
            if (o == null) {
                ret.success = false
                ret.code = Error.INVALID_ACTIVITY
            } else {
                Util.runOnUIThreadBlocking {
                    val p = o.root.layoutParams as WindowManager.LayoutParams
                    p.x = m.x
                    p.y = m.y
                    wm.updateViewLayout(o.root, p)
                    ret.success = true
                }
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }

    fun getConfiguration(m: GetConfigurationRequest) {
        val ret = GetConfigurationResponse.newBuilder()
        try {
            if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    val c: Configuration? = it.resources?.configuration
                    if (c == null) {
                        ret.success = false
                        ret.code = Error.INTERNAL_ERROR
                    } else {
                        ret.setConfiguration(V0Proto.configMessage(it, c))
                        ret.success = true
                    }
                }) {
                ret.success = false
                ret.code = Error.INVALID_ACTIVITY
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }


    fun requestUnlock(m: RequestUnlockRequest) {
        val ret = RequestUnlockResponse.newBuilder()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            ret.success = false
            ret.code = Error.ANDROID_VERSION_TOO_LOW
            ProtoUtils.write(ret, main)
            return
        }
        try {
            if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    val kg = App.APP?.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    if (kg != null) {
                        kg.requestDismissKeyguard(it, null)
                        ret.success = true
                    } else {
                        ret.success = false
                        ret.code = Error.INTERNAL_ERROR
                    }
                }) {
                ret.success = false
                ret.code = Error.INVALID_ACTIVITY
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }

    fun hideSoftKeyboard(m: HideSoftKeyboardRequest) {
        val ret = HideSoftKeyboardResponse.newBuilder()
        try {
            val a = activities[m.aid]
            val o = overlays[m.aid]
            if (a != null) {
                V0Shared.runOnUIThreadActivityStarted(a) {
                    val im = it.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    if (im != null) {
                        im.hideSoftInputFromWindow(it.findViewById<View>(R.id.root).windowToken, 0)
                        ret.success = true
                    } else {
                        ret.success = false
                        ret.code = Error.INTERNAL_ERROR
                    }
                }
            }
            if (o != null) {
                Util.runOnUIThreadBlocking {
                    val im = App.APP?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    if (im != null) {
                        im.hideSoftInputFromWindow(o.root.windowToken, 0)
                        ret.success = true
                    } else {
                        ret.success = false
                        ret.code = Error.INTERNAL_ERROR
                    }
                }
            }
            if (a == null && o == null) {
                ret.success = false
                ret.code = Error.INVALID_ACTIVITY
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }

    fun interceptBackButton(m: InterceptBackButtonRequest) {
        val ret = InterceptBackButtonResponse.newBuilder()
        try {
            if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    it.data.backEvent = m.intercept
                    ret.success = true
                }) {
                ret.success = false
                ret.code = Error.INVALID_ACTIVITY
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }

    fun setSecure(m: SetSecureFlagRequest) {
        val ret = SetSecureFlagResponse.newBuilder()
        try {
            if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    it.setSecure(m.secure)
                    ret.success = true
                }) {
                ret.success = false
                ret.code = Error.INVALID_ACTIVITY
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }
    
    fun interceptVolume(m: InterceptVolumeButtonRequest) {
        val ret = InterceptVolumeButtonResponse.newBuilder()
        try {
            if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    it.data.volumeDown = m.interceptDown
                    it.data.volumeUp = m.interceptUp
                    ret.success = true
                }) {
                ret.success = false
                ret.code = Error.INVALID_ACTIVITY
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }
    
    fun configInsets(m: ConfigureInsetsRequest) {
        val ret = ConfigureInsetsResponse.newBuilder()
        try {
            if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    val c = WindowInsetsControllerCompat(it.window, it.window.decorView)
                    c.systemBarsBehavior = when (m.behaviour) {
                        ConfigureInsetsRequest.BarBehaviour.BAR_BEHAVIOUR_DEFAULT -> WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
                        ConfigureInsetsRequest.BarBehaviour.BAR_BEHAVIOUR_TRANSIENT -> WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        else -> WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
                    }
                    when (m.shown) {
                        ConfigureInsetsRequest.Bars.BOTH_BARS -> c.show(WindowInsetsCompat.Type.systemBars())
                        ConfigureInsetsRequest.Bars.NAVIGATION_BAR -> {
                            c.show(WindowInsetsCompat.Type.navigationBars())
                            c.hide(WindowInsetsCompat.Type.statusBars())
                        }
                        ConfigureInsetsRequest.Bars.STATUS_BAR -> {
                            c.show(WindowInsetsCompat.Type.statusBars())
                            c.hide(WindowInsetsCompat.Type.navigationBars())
                        }
                        ConfigureInsetsRequest.Bars.NO_BAR -> c.hide(WindowInsetsCompat.Type.systemBars())
                        else -> {}
                    }
                    if (m.underCutout) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            it.window.addFlags(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            it.window.addFlags(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS)
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            it.window.clearFlags(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            it.window.clearFlags(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS)
                        }
                    }
                    if (m.shown == ConfigureInsetsRequest.Bars.BOTH_BARS && ! m.underCutout) {
                        WindowCompat.setDecorFitsSystemWindows(it.window, true)
                    } else {
                        WindowCompat.setDecorFitsSystemWindows(it.window, false)
                    }
                    ret.success = true
                }) {
                ret.success = false
                ret.code = Error.INVALID_ACTIVITY
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }
    
    
    

}