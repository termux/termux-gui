package com.termux.gui.protocol.protobuf.v0

import android.util.Log
import android.view.WindowManager
import com.termux.gui.GUIActivity
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
    
    
}