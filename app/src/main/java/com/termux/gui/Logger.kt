package com.termux.gui

import android.util.Log

/**
 * Global logging that's configured with the log level from the settings.
 */
class Logger {
    companion object {
        fun log(level: Int, tag: String?, msg: String) {
            if (Settings.instance.loglevel >= level) {
                Log.i(tag, msg)
            }
        }
    }
}