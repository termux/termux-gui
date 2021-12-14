package com.termux.gui

import android.app.ActivityManager
import android.app.Application
import android.content.ComponentName

class App : Application() {
    companion object {
        var APP: App? = null
    }
    override fun onCreate() {
        super.onCreate()
        APP = this
        // clean up any old task stacks
        getSystemService(ActivityManager::class.java).let {
            for (t in it.appTasks) {
                if (t.taskInfo.baseIntent.component == ComponentName(this, GUIActivity::class.java))
                    t.finishAndRemoveTask()
            }
        }
        Thread.setDefaultUncaughtExceptionHandler { _, e -> e.printStackTrace() }
    }
    
    
}