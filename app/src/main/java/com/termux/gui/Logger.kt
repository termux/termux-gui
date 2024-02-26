package com.termux.gui

import android.util.Log
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter

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

        fun writeLog(msg: String) {
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
            val file = File(path + "/log.txt")
            if (!file.exists()) {
                        file.createNewFile()
                    }
                    val fileWriter = FileWriter(file, true)
                    val bufWriter = BufferedWriter(fileWriter)
                    bufWriter.write(msg)
                    bufWriter.newLine()
                    bufWriter.close()
                    fileWriter.close()
}
        
    }
}
