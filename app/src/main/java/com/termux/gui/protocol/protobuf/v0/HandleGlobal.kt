package com.termux.gui.protocol.protobuf.v0

import android.app.ActivityManager
import android.util.Log
import android.widget.Toast
import com.termux.gui.App
import com.termux.gui.Util
import com.termux.gui.protocol.protobuf.ProtoUtils
import java.io.OutputStream
import java.util.*
import com.termux.gui.protocol.protobuf.v0.GUIProt0.*

class HandleGlobal(val main: OutputStream, val tasks: LinkedList<ActivityManager.AppTask>) {
    
    fun finishTask(m: FinishTaskRequest) {
        val ret = FinishTaskResponse.newBuilder()
        try {
            val t = tasks.find { t -> Util.getTaskInfo(tasks, t)?.let { it1 -> Util.getTaskId(it1) } == m.tid }
            ret.success = if (t != null) {
                t.finishAndRemoveTask()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
        }
        ProtoUtils.write(ret, main)
    }

    fun bringTaskToFront(m: BringTaskToFrontRequest) {
        val ret = BringTaskToFrontResponse.newBuilder()
        try {
            val t = tasks.find { t -> Util.getTaskInfo(tasks, t)?.let { it1 -> Util.getTaskId(it1) } == m.tid }
            ret.success = if (t != null) {
                t.moveToFront()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
        }
        ProtoUtils.write(ret, main)
    }


    fun toast(m: ToastRequest) {
        val ret = ToastResponse.newBuilder()
        try {
            Toast.makeText(App.APP!!.applicationContext, m.text, if (m.long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
            ret.success = true
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
        }
        ProtoUtils.write(ret, main)
    }
    
    
}