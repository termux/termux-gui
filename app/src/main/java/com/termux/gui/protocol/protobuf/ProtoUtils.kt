package com.termux.gui.protocol.protobuf

import com.google.protobuf.MessageLite
import com.termux.gui.GUIActivity
import com.termux.gui.protocol.shared.v0.DataClasses
import java.io.OutputStream

class ProtoUtils {
    companion object {
        fun <T : MessageLite> ViewActionOrFail(main: OutputStream, activities: Map<String, DataClasses.ActivityState>, 
                                               overlays: Map<String, DataClasses.Overlay>, aid: String, ifActivity: (a: GUIActivity) -> T,
                                               ifOverlay: (o: DataClasses.Overlay) -> T, ifFail: () -> T) {
            val s = activities[aid]
            if (s != null) {
                val a = s.a
                if (a != null) {
                    ifActivity(a)
                } else {
                    ifFail()
                }
            } else {
                val o = overlays[aid]
                if (o != null) {
                    ifOverlay(o)
                } else {
                    ifFail()
                }
            }.writeDelimitedTo(main)
        }
        
        
        
        
        
        
        
        
        
    }
}