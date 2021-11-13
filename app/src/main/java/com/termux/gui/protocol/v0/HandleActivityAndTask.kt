package com.termux.gui.protocol.v0

import android.app.ActivityManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import com.termux.gui.ConnectionHandler
import com.termux.gui.GUIActivity
import com.termux.gui.R
import com.termux.gui.Util
import java.util.*

class HandleActivityAndTask {
    companion object {
        @Suppress("DEPRECATION")
        fun handleActivityTaskMessage(m: ConnectionHandler.Message, activities: MutableMap<String, V0.ActivityState>, tasks: LinkedList<ActivityManager.AppTask>,
                                      widgets: MutableMap<Int, V0.WidgetRepresentation>, overlays: MutableMap<String, V0.Overlay>, app: Context, wm: WindowManager) {
            when (m.method) {
                "finishTask" -> {
                    tasks.find { t -> Util.getTaskInfo(tasks, t)?.let { it1 -> Util.getTaskId(it1) } == m.params?.get("tid")?.asInt }?.finishAndRemoveTask()
                    return
                }
                "bringTaskToFront" -> {
                    tasks.find { t -> Util.getTaskInfo(tasks, t)?.let { it1 -> Util.getTaskId(it1) } == m.params?.get("tid")?.asInt }?.moveToFront()
                    return
                }
                "moveTaskToBack" -> {
                    val aid = m.params?.get("aid")?.asString
                    val a = activities[aid]
                    if (a != null) {
                        V0.runOnUIThreadActivityStarted(a) {
                            it.moveTaskToBack(true)
                        }
                    }
                    return
                }
                "setTheme" -> {
                    val aid = m.params?.get("aid")?.asString
                    val a = activities[aid]
                    val s = m.params?.get("statusBarColor")?.asInt
                    val t = m.params?.get("textColor")?.asInt
                    val b = m.params?.get("windowBackground")?.asInt
                    val p = m.params?.get("colorPrimary")?.asInt
                    val ac = m.params?.get("colorAccent")?.asInt
                    val wid = m.params?.get("wid")?.asInt
                    val w = widgets[wid]
                    val o = overlays[aid]
                    if ((a != null || w != null || o != null) && s != null && t != null && b != null && p != null && ac != null) {
                        if (a != null) {
                            V0.runOnUIThreadActivityStarted(a) {
                                it.theme = GUIActivity.GUITheme(s, p, b, t, ac)
                            }
                        }
                        if (w != null) {
                            w.theme = GUIActivity.GUITheme(s, p, b, t, ac)
                        }
                        if (o != null) {
                            o.theme = GUIActivity.GUITheme(s, p, b, t, ac)
                        }
                    }
                    return
                }
                "setTaskDescription" -> {
                    val aid = m.params?.get("aid")?.asString
                    val a = activities[aid]
                    val img = m.params?.get("img")?.asString
                    if (a != null) {
                        V0.runOnUIThreadActivityStarted(a) {
                            val t = it.theme
                            val prim = t?.colorPrimary ?: (0xFF000000).toInt()
                            if (img != null) {
                                if (img == "default") {
                                    it.setTaskDescription(ActivityManager.TaskDescription(m.params?.get("label")?.asString, BitmapFactory.decodeResource(it.resources, R.mipmap.ic_launcher_round), prim))
                                } else {
                                    val bin = Base64.decode(img, Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(bin, 0, bin.size)
                                    it.setTaskDescription(ActivityManager.TaskDescription(m.params?.get("label")?.asString, bitmap, prim))
                                }
                            } else {
                                it.setTaskDescription(ActivityManager.TaskDescription(m.params?.get("label")?.asString, null, prim))
                            }
                        }
                    }
                    return
                }
                "setPiPParams" -> {
                    val aid = m.params?.get("aid")?.asString
                    val num = m.params?.get("num")?.asInt
                    val den = m.params?.get("den")?.asInt
                    val a = activities[aid]
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && num != null && den != null && a != null) {
                        val rat = Rational(num, den).coerceAtMost(Rational(239, 100)).coerceAtLeast(Rational(100, 239))
                        V0.runOnUIThreadActivityStarted(a) {
                            it.setPictureInPictureParams(PictureInPictureParams.Builder().setAspectRatio(rat).build())
                        }
                    }
                    return
                }
                "setInputMode" -> {
                    val aid = m.params?.get("aid")?.asString
                    val mode = m.params?.get("mode")?.asString
                    val a = activities[aid]
                    if (a != null && mode != null) {
                        V0.runOnUIThreadActivityStarted(a) {
                            when (mode) {
                                "resize" -> {
                                    it.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                                }
                                "pan" -> {
                                    it.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
                                }
                            }
                        }
                    }
                    return
                }
                "setPiPMode" -> {
                    val aid = m.params?.get("aid")?.asString
                    val pip = m.params?.get("pip")?.asBoolean ?: false
                    val a = activities[aid]
                    if (a != null) {
                        V0.runOnUIThreadActivityStarted(a) {
                            if (pip) {
                                it.enterPictureInPictureMode()
                            } else {
                                it.moveTaskToBack(true)
                            }
                        }
                    }
                    return
                }
                "setPiPModeAuto" -> {
                    val aid = m.params?.get("aid")?.asString
                    val pip = m.params?.get("pip")?.asBoolean ?: false
                    val a = activities[aid]
                    if (a != null) {
                        V0.runOnUIThreadActivityStarted(a) {
                            it.data.autopip = pip
                        }
                    }
                    return
                }
                "toast" -> {
                    val text = m.params?.get("text")?.asString ?: ""
                    val long = m.params?.get("long")?.asBoolean ?: false
                    Util.runOnUIThreadBlocking {
                        Toast.makeText(app, text, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                "keepScreenOn" -> {
                    val aid = m.params?.get("aid")?.asString
                    val on = m.params?.get("on")?.asBoolean ?: false
                    val a = activities[aid]
                    if (a != null) {
                        V0.runOnUIThreadActivityStarted(a) {
                            if (on) {
                                it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            } else {
                                it.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            }
                        }
                    }
                    return
                }
                "setOrientation" -> {
                    val aid = m.params?.get("aid")?.asString
                    val orientation = m.params?.get("orientation")?.asString
                    val a = activities[aid]
                    if (a != null && orientation != null) {
                        V0.runOnUIThreadActivityStarted(a) {
                            it.requestedOrientation = when (orientation) {
                                "behind" -> ActivityInfo.SCREEN_ORIENTATION_BEHIND
                                "fullSensor" -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                                "fullUser" -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                                "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                "locked" -> ActivityInfo.SCREEN_ORIENTATION_LOCKED
                                "nosensor" -> ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
                                "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                "reverseLandscape" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                                "reversePortrait" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                                "sensorLandscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                "sensorPortrait" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                "user" -> ActivityInfo.SCREEN_ORIENTATION_USER
                                "userLandscape" -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                                "userPortrait" -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                                else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                        }
                    }
                    return
                }
                "setPosition" -> {
                    val aid = m.params?.get("aid")?.asString
                    val x = m.params?.get("x")?.asInt
                    val y = m.params?.get("y")?.asInt
                    val o = overlays[aid]
                    if (o != null && x != null && y != null) {
                        Util.runOnUIThreadBlocking {
                            val p = o.root.layoutParams as WindowManager.LayoutParams
                            p.x = x
                            p.y = y
                            wm.updateViewLayout(o.root, p)
                        }
                    }
                }
            }
        }
    }
}