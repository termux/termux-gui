package com.termux.gui

import android.content.Context
import android.net.LocalSocket
import android.net.LocalSocketAddress
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import com.termux.gui.Util.Companion.sendMessage
import com.termux.gui.protocol.json.v0.V0Json
import com.termux.gui.protocol.protobuf.v0.GUIProt0
import com.termux.gui.protocol.protobuf.v0.V0Proto
import kotlinx.coroutines.Runnable
import java.io.DataOutputStream
import java.io.EOFException
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

/**
 * Handles one connection. This should only be run in a handler Thread.
 * Checks if the connection comes from a Termux program and terminates the connection if not.
 * Handles the protocol negotiation, then delegates the connection further to the correct handler for the protocol type and version.
 */
class ConnectionHandler(private val request: GUIService.ConnectionRequest, private val service: GUIService) : Runnable {
    class Message {
        var method: String? = null
        var params: HashMap<String, JsonElement>? = null
    }
    
    @Suppress("unused")
    class Event(var type: String, var value: JsonElement?)
    companion object {
        private val TAG: String? = ConnectionHandler::class.java.canonicalName
        val gson = Gson()
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
    private var eventQueueJson = LinkedBlockingQueue<Event>(10000)
    private var eventQueueProto = LinkedBlockingQueue<GUIProt0.Event>(10000)
    private var eventWorker: Thread? = null
    
    
    
    override fun run() {
        Logger.log(4, TAG, "Socket address: " + request.mainSocket)
        Logger.log(4, TAG, "Event socket address: " + request.eventSocket)
        
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
                    Logger.log(1, TAG, "requested type: $ptype")
                    Logger.log(1, TAG, "requested version: $pversion")
                    if (ptype != 1 && ptype != 0) {
                        main.outputStream.write(1)
                        return
                    }
                    if (ptype == 1 && pversion != 0) {
                        main.outputStream.write(1)
                        return
                    }
                    if (ptype == 0 && pversion != 0) {
                        main.outputStream.write(1)
                        return
                    }
                    main.outputStream.write(0)
                    main.outputStream.flush()

                    Logger.log(1, TAG, "connection accepted")

                    eventWorker = Thread {
                        when (ptype) {
                            0 -> {
                                while (! Thread.currentThread().isInterrupted) {
                                    try {
                                        Util.sendProto(event.outputStream, eventQueueProto.take())
                                    } catch (ignored: Exception) {}
                                }
                            }
                            1 -> {
                                val eventOut = DataOutputStream(event.outputStream)
                                while (! Thread.currentThread().isInterrupted) {
                                    try {
                                        sendMessage(eventOut, gson.toJson(eventQueueJson.take()))
                                    } catch (ignored: Exception) {}
                                }
                            }
                        }
                    }
                    eventWorker!!.start()
                    Logger.log(1, TAG, "listening")
                    when (ptype) {
                        0 -> {
                            when (pversion) {
                                0 -> V0Proto(app, eventQueueProto).handleConnection(main)
                            }
                        }
                        1 -> {
                            when (pversion) {
                                0 -> V0Json(app, eventQueueJson).handleConnection(main)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            service.destroywatch.remove(watch)
            if (e is JsonSyntaxException) {
                Logger.log(1, TAG, "program send invalid json")
                return
            }
            if (e is EOFException) {
                Logger.log(1, TAG, "connection closed by program")
                return
            }
            e.printStackTrace()
        } finally {
            eventWorker?.interrupt()
            Logger.log(1, TAG, "cleanup")
        }
    }

}