package com.termux.gui

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import java.util.*

class ExternalSocketTest {
    companion object {
        const val sockname = "com.termux.gui test"
        const val esockname = "com.termux.gui teste"
    }

    /**
     * Tests if the plugin doesn't connect to sockets that don't have the same uid. Passes if it doesn't connect.
     */
    @Test
    fun nontermuxSocketFail() {
        val c = InstrumentationRegistry.getInstrumentation().context
        
        val r = Random()
        val append = r.nextLong().toString()
        var rec = false
        
        val starttest = Intent()
        starttest.component = ComponentName.unflattenFromString("com.termux.gui.externaltest/.RestartReceiver")
        c.sendBroadcast(starttest)
        starttest.component = ComponentName.unflattenFromString("com.termux.gui.externaltest/.TestConnectReceiver")
        val extras = Bundle()
        extras.putString("append", append)
        c.sendOrderedBroadcast(starttest, null, object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (resultCode == 0) {
                    throw RuntimeException("failed: $resultData")
                } else {
                    rec = true
                }
            }

        }, Handler(c.mainLooper), 0, "receiver not found", extras)
        
        val i = Intent()
        i.component = ComponentName.unflattenFromString("com.termux.gui/.GUIReceiver")
        i.putExtra("mainSocket", sockname+append)
        i.putExtra("eventSocket", esockname+append)
        c.sendBroadcast(i)
        
        while (! rec) {
            Thread.sleep(1)
        }
    }
    
}