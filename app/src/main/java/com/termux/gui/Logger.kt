package com.termux.gui

import android.util.Log

class Logger {
    companion object {
        fun log(level: Int, tag: String?, msg: String) {
            if (Settings.instance.loglevel >= level) {
                Log.i(tag, msg)
            }
        }
    }
}