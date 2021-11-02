package com.termux.gui

import android.app.ActivityManager
import android.app.Application

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
                t.finishAndRemoveTask()
            }
        }
    }
    
    
}