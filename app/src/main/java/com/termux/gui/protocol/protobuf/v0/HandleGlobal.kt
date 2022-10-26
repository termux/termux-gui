package com.termux.gui.protocol.protobuf.v0

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import com.termux.gui.App
import com.termux.gui.BuildConfig
import com.termux.gui.ConnectionHandler
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
    
    @Suppress("DEPRECATION")
    fun turnScreenOn(m: TurnScreenOnRequest) {
        val ret = TurnScreenOnResponse.newBuilder()
        try {
            val pm = App.APP?.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null) {
                val lock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE, "com.termux.gui:wake")
                lock.acquire(0)
                lock.release()
            }
            ret.success = true
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            ret.success = false
        }
        ProtoUtils.write(ret, main)
    }

    fun isLocked(m: IsLockedRequest) {
        val ret = IsLockedResponse.newBuilder()
        ret.locked = IsLockedResponse.Locked.UNKNOWN
        try {
            val kg = App.APP?.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (kg != null) {
                if (! kg.isKeyguardLocked) {
                    ret.locked = IsLockedResponse.Locked.UNLOCKED
                } else {
                    ret.locked = IsLockedResponse.Locked.LOCKED
                }
            }
        } catch (e: Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
        }
        ProtoUtils.write(ret, main)
    }

    fun version(m: GetVersionRequest) {
        val ret = GetVersionResponse.newBuilder()
        ret.versionCode = BuildConfig.VERSION_CODE
        ProtoUtils.write(ret, main)
    }
    
}