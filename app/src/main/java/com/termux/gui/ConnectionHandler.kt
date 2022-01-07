package com.termux.gui

import android.content.Context
import android.net.LocalSocket
import android.net.LocalSocketAddress
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import com.termux.gui.Util.Companion.sendMessage
import com.termux.gui.protocol.v0.V0
import kotlinx.coroutines.Runnable
import java.io.DataOutputStream
import java.io.EOFException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class ConnectionHandler(private val request: GUIService.ConnectionRequest, val service: GUIService) : Runnable {
    class Message {
        var method: String? = null
        var params: HashMap<String, JsonElement>? = null
    }
    
    @Suppress("unused")
    class Event(var type: String, var value: JsonElement?)
    companion object {
        val gson = Gson()
        val INVALID_METHOD: Event = Event("invalidMethod", gson.toJsonTree("invalid method"))
        init {
            System.loadLibrary("gui")
        }
        // methods to use ashmem pre API 27
        external fun create_ashmem(size: Int): Int
        external fun destroy_ashmem(fd: Int)
        external fun map_ashmem(fd: Int, size: Int): ByteBuffer?
        external fun unmap_ashmem(buff: ByteBuffer) // DO NOT use the ByteBuffer after this, clear the reference before so it isn't even possible!
    }
    
    
    private val app: Context = service.applicationContext
    private var eventQueue = LinkedBlockingQueue<Event>(10000)
    private var eventWorker: Thread? = null
    
    
    
    override fun run() {
        //println("Socket address: " + request.mainSocket)
        
        val main = LocalSocket(LocalSocket.SOCKET_STREAM)
        val event = LocalSocket(LocalSocket.SOCKET_STREAM)
        
        val watch = Runnable {
            main.shutdownInput()
            main.shutdownOutput()
            event.shutdownInput()
            event.shutdownOutput()
            main.close()
            event.close()
        }
        service.destroywatch.add(watch)
        try {
            main.use {
                event.use {
                    main.connect(LocalSocketAddress(request.mainSocket))
                    event.connect(LocalSocketAddress(request.eventSocket))
                    // check if it is a termux program that wants to connect to the plugin
                    if (main.peerCredentials.uid != app.applicationInfo.uid || event.peerCredentials.uid != app.applicationInfo.uid) {
                        return
                    }
                    
                    var protocol = -1
                    while (protocol == -1) {
                        protocol = main.inputStream.read()
                        Thread.sleep(1)
                    }
                    val pversion = (protocol and 0xf0) shr 4
                    val ptype = protocol and 0x0f
                    if (pversion != 0 && ptype != 1) {
                        main.outputStream.write(1)
                        return
                    }
                    main.outputStream.write(0)
                    main.outputStream.flush()
                    

                    eventWorker = Thread {
                        val eventOut = DataOutputStream(event.outputStream)
                        while (! Thread.currentThread().isInterrupted) {
                            try {
                                sendMessage(eventOut, gson.toJson(eventQueue.take()))
                            } catch (ignored: Exception) {}
                        }
                    }
                    eventWorker!!.start()
                    //println("listening")
                    when (pversion) {
                        0 -> V0(app).handleConnection(ptype, main, eventQueue)
                    }
                }
            }
        } catch (e: Exception) {
            service.destroywatch.remove(watch)
            if (e is JsonSyntaxException) {
                println("program send invalid json")
                return
            }
            if (e is EOFException) {
                println("connection closed by program")
                return
            }
            e.printStackTrace()
        } finally {
            eventWorker?.interrupt()
            println("cleanup")
        }
    }

}