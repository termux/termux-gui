package com.termux.gui.externaltest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.LocalServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class TestConnectReceiver : BroadcastReceiver() {
    companion object {
        const val sockname = "com.termux.gui test"
        const val esockname = "com.termux.gui teste"
    }
    
    override fun onReceive(c: Context, intent: Intent) {
        val e = Executors.newCachedThreadPool()
        val append = getResultExtras(true).getString("append", "")
        val f = e.submit {
            val main = LocalServerSocket(sockname+append)
            val event = LocalServerSocket(esockname+append)
            val m = main.accept()
            event.accept()
            m.outputStream.write(1)
            val read = m.inputStream.read()
            if (read != -1)
                throw RuntimeException("Connection accepted: $read")
        }
        try {
            f.get(2, TimeUnit.SECONDS)
            setResult(0, "no exception", null)
        } catch (e: TimeoutException) {
            setResult(1, "no connection", null)
        }
        catch (e: Exception) {
            setResult(0, "other exception: "+e.javaClass.canonicalName+": "+e.message, null)
        }
        
    }
}