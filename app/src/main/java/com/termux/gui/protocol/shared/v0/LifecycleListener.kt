package com.termux.gui.protocol.shared.v0

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.os.Bundle
import com.termux.gui.GUIActivity
import com.termux.gui.Util
import java.util.*

class LifecycleListener(private val v0: V0Shared, private val activities: MutableMap<Int, DataClasses.ActivityState>,
                        private val tasks: MutableList<ActivityManager.AppTask>, private val am: ActivityManager, private val connectionID: Long) : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(a: Activity, savedInstanceState: Bundle?) {
        //println("create")
        if (a is GUIActivity && a.connection == connectionID) {
            val f = activities[a.aid]
            if (f != null) {
                if (tasks.find { Util.getTaskInfo(tasks, it)?.let { it1 -> Util.getTaskId(it1) } == a.taskId } == null) {
                    for (t in am.appTasks) {
                        if (Util.getTaskInfo(tasks, t)?.let { it1 -> Util.getTaskId(it1) } == a.taskId) {
                            //println("task added")
                            tasks.add(t)
                            break
                        }
                    }
                }
                a.listener = v0
                f.a = a
                v0.onActivityCreated(f)
            }
        }
    }
    override fun onActivityStarted(a: Activity) {
        //println("start")
        if (a is GUIActivity && a.connection == connectionID) {
            val f = activities[a.aid]
            if (f != null) {
                f.saved = false
                for (r in f.queued) {
                    //println("running queued")
                    try {
                        r(a)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                v0.onActivityStarted(f)
            }
        }
    }
    override fun onActivityResumed(a: Activity) {
        //println("resume")
        try {
            if (a is GUIActivity && a.connection == connectionID) {
                val f = activities[a.aid]
                if (f != null) {
                    v0.onActivityResumed(f)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    override fun onActivityPaused(a: Activity) {
        //println("pause")
        try {
            if (a is GUIActivity && a.connection == connectionID) {
                val f = activities[a.aid]
                if (f != null) {
                    v0.onActivityPaused(f)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    override fun onActivityStopped(a: Activity) {
        //println("stop")
        try {
            if (a is GUIActivity && a.connection == connectionID) {
                val f = activities[a.aid]
                if (f != null) {
                    v0.onActivityStopped(f)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
    override fun onActivitySaveInstanceState(a: Activity, outState: Bundle) {
        //println("saveInstanceState")
        try {
            if (a is GUIActivity && a.connection == connectionID) {
                val f = activities[a.aid]
                if (f != null) {
                    f.saved = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    override fun onActivityDestroyed(a: Activity) {
        try {
            if (a is GUIActivity && a.connection == connectionID) {
                val aid = a.aid
                val f = activities[aid]
                if (f != null) {
                    v0.onActivityDestroyed(f)
                }
                if (a.isFinishing) {
                    //println("finishing")
                    if (f != null) {
                        activities.remove(aid)
                    }
                } else {
                    f?.a = null
                }
            }
            //println("destroy")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}