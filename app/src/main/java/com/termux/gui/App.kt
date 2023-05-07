package com.termux.gui

import android.app.ActivityManager
import android.app.Application
import android.content.ComponentName

/**
 * Makes the Application object globally available enable the handler Threads to register Activity Lifecycle listeners.
 * Also cleans up any leftover Tasks stacks from e.g. a crash and sets the default Exception handler to just print
 * (so exceptions in handler threads don't bring down the whole app).
 */
class App : Application() {
    companion object {
        var APP: App? = null
    }
    override fun onCreate() {
        super.onCreate()
        APP = this
        Settings.instance.load(this)
        // clean up any old task stacks
        getSystemService(ActivityManager::class.java).let {
            for (t in it.appTasks) {
                if (t.taskInfo.baseIntent.component == ComponentName(this, GUIActivity::class.java) ||
                        t.taskInfo.baseIntent.component == ComponentName(this, GUIActivityDialog::class.java) ||
                        t.taskInfo.baseIntent.component == ComponentName(this, GUIActivityLockscreen::class.java))
                    t.finishAndRemoveTask()
            }
        }
        Thread.setDefaultUncaughtExceptionHandler { _, e -> e.printStackTrace() }
    }
    
    
}