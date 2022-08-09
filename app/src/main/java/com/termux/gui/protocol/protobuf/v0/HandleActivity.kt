package com.termux.gui.protocol.protobuf.v0

import java.io.OutputStream

class HandleActivity {
    companion object {
        fun newActivity(m: GUIProt0.NewActivityRequest, main: OutputStream, v: V0Proto) {
            var pip = false
            var dialog = false
            var overlay = false
            var lockscreen = false
            var canceloutside = false
            
            val ret = GUIProt0.NewActivityResponse.newBuilder()
            
            when (m.type) {
                GUIProt0.NewActivityRequest.ActivityType.normal -> {}
                GUIProt0.NewActivityRequest.ActivityType.dialog -> dialog = true
                GUIProt0.NewActivityRequest.ActivityType.dialogCancelOutside -> {
                    dialog = true
                    canceloutside = true
                }
                GUIProt0.NewActivityRequest.ActivityType.pip -> pip = true
                GUIProt0.NewActivityRequest.ActivityType.lockscreen -> lockscreen = true
                GUIProt0.NewActivityRequest.ActivityType.overlay -> overlay = true
                GUIProt0.NewActivityRequest.ActivityType.UNRECOGNIZED -> {
                    ret.setAid("-1").setTid(-1).build().writeDelimitedTo(main)
                    return
                }
                null -> {}
            }
            if (overlay) {
                ret.aid = v.generateOverlay()
                ret.tid = -1
            } else {
                val a = v.newActivity(m.tid, pip, dialog, lockscreen, canceloutside, m.interceptBackButton)
                if (a != null) {
                    ret.aid = a.aid
                    ret.tid = a.taskId
                } else {
                    ret.aid = "-1"
                    ret.tid = -1;
                }
            }
            ret.build().writeDelimitedTo(main)
        }
        
        
        
        
        
    }
}