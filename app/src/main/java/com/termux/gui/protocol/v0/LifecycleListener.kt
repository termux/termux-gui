package com.termux.gui.protocol.v0

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.os.Bundle
import com.termux.gui.ConnectionHandler
import com.termux.gui.GUIActivity
import com.termux.gui.Util
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class LifecycleListener(private val eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>, private val activities: MutableMap<String, V0.ActivityState>,
                        private val tasks: LinkedList<ActivityManager.AppTask>, private val am: ActivityManager) : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(a: Activity, savedInstanceState: Bundle?) {
        println("create")
        if (a is GUIActivity) {
            val f = activities[a.intent?.dataString]
            if (f != null) {
                if (tasks.find { Util.getTaskInfo(tasks, it)?.let { it1 -> Util.getTaskId(it1) } == a.taskId } == null) {
                    for (t in am.appTasks) {
                        if (Util.getTaskInfo(tasks, t)?.let { it1 -> Util.getTaskId(it1) } == a.taskId) {
                            println("task added")
                            tasks.add(t)
                            break
                        }
                    }
                }
                a.eventQueue = eventQueue
                f.a = a
                val map = HashMap<String, Any?>()
                map["finishing"] = a.isFinishing
                map["aid"] = a.intent?.dataString
                eventQueue.offer(ConnectionHandler.Event("create", ConnectionHandler.gson.toJsonTree(map)))
            }
        }
    }
    override fun onActivityStarted(a: Activity) {
        println("start")
        if (a is GUIActivity) {
            val f = activities[a.intent?.dataString]
            if (f != null) {
                f.saved = false
                for (r in f.queued) {
                    println("running queued")
                    try {
                        r(a)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val map = HashMap<String, Any?>()
                map["finishing"] = a.isFinishing
                map["aid"] = a.intent?.dataString
                eventQueue.offer(ConnectionHandler.Event("start", ConnectionHandler.gson.toJsonTree(map)))
            }
        }
    }
    override fun onActivityResumed(a: Activity) {
        println("resume")
        try {
            if (a is GUIActivity) {
                val f = activities[a.intent?.dataString]
                if (f != null) {
                    val map = HashMap<String, Any?>()
                    map["finishing"] = a.isFinishing
                    map["aid"] = a.intent?.dataString
                    eventQueue.add(ConnectionHandler.Event("resume", ConnectionHandler.gson.toJsonTree(map)))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    override fun onActivityPaused(a: Activity) {
        println("pause")
        try {
            if (a is GUIActivity) {
                val f = activities[a.intent?.dataString]
                if (f != null) {
                    val map = HashMap<String, Any?>()
                    map["finishing"] = a.isFinishing
                    map["aid"] = a.intent?.dataString
                    eventQueue.add(ConnectionHandler.Event("pause", ConnectionHandler.gson.toJsonTree(map)))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    override fun onActivityStopped(a: Activity) {
        println("stop")
        try {
            if (a is GUIActivity) {
                val f = activities[a.intent?.dataString]
                if (f != null) {
                    val map = HashMap<String, Any?>()
                    map["finishing"] = a.isFinishing
                    map["aid"] = a.intent?.dataString
                    eventQueue.add(ConnectionHandler.Event("stop", ConnectionHandler.gson.toJsonTree(map)))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
    override fun onActivitySaveInstanceState(a: Activity, outState: Bundle) {
        println("saveInstanceState")
        try {
            if (a is GUIActivity) {
                val aid = a.intent?.dataString
                val f = activities[aid]
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
            if (a is GUIActivity) {
                val aid = a.intent?.dataString
                val f = activities[aid]
                if (f != null) {
                    val map = HashMap<String, Any?>()
                    map["finishing"] = a.isFinishing
                    map["aid"] = a.intent?.dataString
                    eventQueue.add(ConnectionHandler.Event("destroy", ConnectionHandler.gson.toJsonTree(map)))
                }
                if (a.isFinishing) {
                    println("finishing")
                    if (f != null) {
                        activities.remove(aid)
                    }
                } else {
                    f?.a = null
                }
            }
            println("destroy")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}