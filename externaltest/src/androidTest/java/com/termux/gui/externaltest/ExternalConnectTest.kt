package com.termux.gui.externaltest

import android.content.ComponentName
import android.content.Intent
import android.net.LocalServerSocket
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ExternalConnectTest {
    companion object {
        const val sockname = "com.termux.gui test"
        const val esockname = "com.termux.gui teste"
    }


    /**
     * Tests if the GUIReceiver is inaccessible from other apps. Passes if no connection on the sockets is registered.
     * Additionally a logcat entry will show that the permission for the broadcast is denied.
     */
    @Test
    fun nontermuxFail() {
        val c = InstrumentationRegistry.getInstrumentation().context
        
        val e = Executors.newCachedThreadPool()
        val f = e.submit {
            val main = LocalServerSocket(sockname)
            val event = LocalServerSocket(esockname)
            val m = main.accept()
            event.accept()
            m.outputStream.write(1)
            val read = m.inputStream.read()
            if (read != -1)
                throw RuntimeException("Connection accepted: $read")
        }
        val i = Intent()
        i.component = ComponentName.unflattenFromString("com.termux.gui/.GUIReceiver")
        i.putExtra("mainSocket", sockname)
        i.putExtra("eventSocket", esockname)
        c.sendBroadcast(i)
        try {
            f.get(2, TimeUnit.SECONDS)
            throw RuntimeException("Connection accepted")
        } catch (e: TimeoutException) {}
        catch (e: Exception) {
            throw e
        }
    }
}