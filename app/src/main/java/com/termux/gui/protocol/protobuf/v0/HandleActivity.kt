package com.termux.gui.protocol.protobuf.v0

import android.app.ActivityManager
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.util.Rational
import android.view.WindowManager
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
                     val wm: WindowManager, val overlays: MutableMap<Int, DataClasses.Overlay>) {
    
    
    
    fun newActivity(m: NewActivityRequest) {
        var pip = false
        var dialog = false
        var overlay = false
        var lockscreen = false
        var canceloutside = false
        
        val ret = NewActivityResponse.newBuilder()
        try {
            when (m.type) {
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
                    ret.setAid(-1).setTid(-1).build().writeDelimitedTo(main)
                    return
                }
                null -> {}
            }
            if (overlay) {
                ret.aid = v.generateOverlay()
                ret.tid = -1
            } else {
                val a = v.newActivity(m.tid, pip, dialog, lockscreen, canceloutside, m.interceptBackButton)
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
                }) ret.success = false
            } else {
                if (o != null) {
                    wm.removeView(o.root)
                    overlays.remove(aid)
                    ret.success = true
                } else {
                    ret.success = false
                }
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
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
                }
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
        }
        ProtoUtils.write(ret, main)
    }

    fun setTheme(m: SetThemeRequest) {
        val ret = SetThemeResponse.newBuilder()
        try {
            if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    it.theme = GUIActivity.GUITheme(m.statusBarColor, m.colorPrimary, m.windowBackground, m.textColor, m.colorAccent)
                    ret.success = true
            }) ret.success = false
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
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
                }) ret.success = false
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
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
                }) ret.success = false
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
        }
        ProtoUtils.write(ret, main)
    }

    @Suppress("DEPRECATION")
    fun setInputMode(m: SetInputModeRequest) {
        val ret = SetInputModeResponse.newBuilder()
        try {
            if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    when (m.mode) {
                        SetInputModeRequest.InputMode.pan -> {
                            it.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
                            ret.success = true
                        }
                        SetInputModeRequest.InputMode.resize -> {
                            it.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                            ret.success = true
                        }
                        else -> ret.success = false
                    }
                }) ret.success = false
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
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
                }) ret.success = false
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
        }
        ProtoUtils.write(ret, main)
    }

    fun setPiPModeAuto(m: SetPiPModeAutoRequest) {
        val ret = SetPiPModeAutoResponse.newBuilder()
        try {
            if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    it.data.autopip = m.pip
                    ret.success = true
                }) ret.success = false
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
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
                }) ret.success = false
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
        }
        ProtoUtils.write(ret, main)
    }


    fun setOrientation(m: SetOrientationRequest) {
        val ret = SetOrientationResponse.newBuilder()
        try {
            if (V0Shared.runOnUIThreadActivityStartedBlocking(activities[m.aid]) {
                    ret.success = true
                    it.requestedOrientation = when (m.orientation) {
                        SetOrientationRequest.Orientation.behind -> ActivityInfo.SCREEN_ORIENTATION_BEHIND
                        SetOrientationRequest.Orientation.fullSensor -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                        SetOrientationRequest.Orientation.fullUser -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                        SetOrientationRequest.Orientation.landscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        SetOrientationRequest.Orientation.locked -> ActivityInfo.SCREEN_ORIENTATION_LOCKED
                        SetOrientationRequest.Orientation.nosensor -> ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
                        SetOrientationRequest.Orientation.portrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        SetOrientationRequest.Orientation.reverseLandscape -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        SetOrientationRequest.Orientation.reversePortrait -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                        SetOrientationRequest.Orientation.sensor -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                        SetOrientationRequest.Orientation.sensorLandscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        SetOrientationRequest.Orientation.sensorPortrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                        SetOrientationRequest.Orientation.user -> ActivityInfo.SCREEN_ORIENTATION_USER
                        SetOrientationRequest.Orientation.userLandscape -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                        SetOrientationRequest.Orientation.userPortrait -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                        else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }) ret.success = false
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
        }
        ProtoUtils.write(ret, main)
    }


    fun setPosition(m: SetPositionRequest) {
        val ret = SetPositionResponse.newBuilder()
        try {
            val o = overlays[m.aid]
            if (o == null) {
                ret.success = false
            } else {
                Util.runOnUIThreadBlocking {
                    val p = o.root.layoutParams as WindowManager.LayoutParams
                    p.x = m.x
                    p.y = m.y
                    wm.updateViewLayout(o.root, p)
                }
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
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
                    } else {
                        val build = GUIProt0.Configuration.newBuilder()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            build.darkMode = c.isNightModeActive
                        }
                        val l = c.locales.get(0)
                        build.country = l.country
                        build.language = l.language
                        
                        
                        ret.setConfiguration(build)
                        ret.success = true
                    }
                }) ret.success = false
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
        }
        ProtoUtils.write(ret, main)
    }
    
    
    
}