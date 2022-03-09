package com.termux.gui.protocol.json.v0

import android.graphics.Bitmap
import android.net.LocalSocket
import android.os.Build
import android.os.SharedMemory
import android.widget.ImageView
import com.termux.gui.ConnectionHandler
import com.termux.gui.Util
import java.io.DataOutputStream
import java.io.FileDescriptor
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import java.util.*

class HandleBuffer {
    companion object {
        fun handleBuffer(m: ConnectionHandler.Message, activities: MutableMap<String, V0.ActivityState>, overlays: MutableMap<String, V0.Overlay>,
                         rand: Random, out: DataOutputStream, buffers: MutableMap<Int, V0.SharedBuffer>, main: LocalSocket): Boolean {
            when (m.method) {
                "addBuffer" -> {
                    val format = m.params?.get("format")?.asString
                    val w = m.params?.get("w")?.asInt
                    val h = m.params?.get("h")?.asInt
                    if (w != null && h != null && format == "ARGB888" && w > 0 && h > 0) {
                        val bid = V0.generateBufferID(rand, buffers)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            println("creating buffer on API 27+")
                            val shm = SharedMemory.create(bid.toString(), w * h * 4)
                            val b = V0.SharedBuffer(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888, true), shm, shm.mapReadOnly(), null)
                            try {
                                // this is a dirty trick to get the FileDescriptor of a SharedMemory object without using JNI or a higher API version.
                                // this could break anytime, though it is still marked as public but discouraged, so that is unlikely, also given the implementation of the class.
                                // noinspection DiscouragedPrivateApi
                                val getFd = SharedMemory::class.java.getDeclaredMethod("getFd")
                                val fdint = getFd.invoke(shm) as? Int
                                if (fdint == null) {
                                    println("fd empty or not a Int")
                                    shm.close()
                                    SharedMemory.unmap(b.buff)
                                    b.btm.recycle()
                                    Util.sendMessage(out, ConnectionHandler.gson.toJson(-1))
                                    return true
                                }
                                val fdesc = FileDescriptor()
                                val setInt = FileDescriptor::class.java.getDeclaredMethod("setInt$", Int::class.java)
                                setInt(fdesc, fdint)
                                Util.sendMessageFd(out, ConnectionHandler.gson.toJson(bid), main, fdesc)
                                buffers[bid] = b
                            } catch (e: Exception) {
                                SharedMemory.unmap(b.buff)
                                b.shm?.close()
                                b.btm.recycle()
                                if (e is NoSuchMethodException || e is IllegalArgumentException || e is IllegalAccessException ||
                                        e is InstantiationException || e is InvocationTargetException) {
                                    println("reflection exception")
                                    e.printStackTrace()
                                    Util.sendMessage(out, ConnectionHandler.gson.toJson(-1))
                                } else {
                                    throw e
                                }
                            }
                        } else {
                            println("creating buffer on API 26-")
                            val fdint = ConnectionHandler.create_ashmem(w * h * 4)
                            if (fdint == -1) {
                                println("could not create ashmem with NDK")
                                Util.sendMessage(out, ConnectionHandler.gson.toJson(-1))
                                return true
                            }
                            val buff: ByteBuffer? = ConnectionHandler.map_ashmem(fdint, w * h * 4)
                            if (buff == null) {
                                println("could not map ashmem with NDK")
                                ConnectionHandler.destroy_ashmem(fdint)
                                Util.sendMessage(out, ConnectionHandler.gson.toJson(-1))
                                return true
                            }
                            try {
                                val fdesc = FileDescriptor()
                                val setInt = FileDescriptor::class.java.getDeclaredMethod("setInt$", Int::class.java)
                                setInt(fdesc, fdint)
                                Util.sendMessageFd(out, ConnectionHandler.gson.toJson(bid), main, fdesc)
                                buffers[bid] = V0.SharedBuffer(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888), null, buff, fdint)
                            } catch (e: Exception) {
                                ConnectionHandler.unmap_ashmem(buff)
                                ConnectionHandler.destroy_ashmem(fdint)
                                if (e is NoSuchMethodException || e is IllegalArgumentException || e is IllegalAccessException ||
                                        e is InstantiationException || e is InvocationTargetException) {
                                    println("reflection exception")
                                    e.printStackTrace()
                                    Util.sendMessage(out, ConnectionHandler.gson.toJson(-1))
                                } else {
                                    throw e
                                }
                            }
                        }
                    } else {
                        println("invalid parameters")
                        Util.sendMessage(out, ConnectionHandler.gson.toJson(-1))
                    }
                    return true
                }
                "deleteBuffer" -> {
                    val bid = m.params?.get("bid")?.asInt
                    val buffer = buffers[bid]
                    if (buffer != null) {
                        buffers.remove(bid)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            SharedMemory.unmap(buffer.buff)
                            buffer.shm?.close()
                        } else {
                            val fd = buffer.fd
                            if (fd != null) {
                                ConnectionHandler.unmap_ashmem(buffer.buff)
                                ConnectionHandler.destroy_ashmem(fd)
                            }
                        }
                    }
                    return true
                }
                "blitBuffer" -> {
                    val buffer = buffers[m.params?.get("bid")?.asInt]
                    if (buffer != null) {
                        buffer.btm.copyPixelsFromBuffer(buffer.buff)
                        buffer.buff.position(0)
                    }
                    return true
                }
                "setBuffer" -> {
                    val aid = m.params?.get("aid")?.asString
                    val a = activities[aid]
                    val id = m.params?.get("id")?.asInt
                    val buffer = buffers[m.params?.get("bid")?.asInt]
                    val o = overlays[aid]
                    if (buffer != null && id != null) {
                        if (a != null) {
                            V0.runOnUIThreadActivityStarted(a) {
                                it.findViewReimplemented<ImageView>(id)?.setImageBitmap(buffer.btm)
                            }
                        }
                        if (o != null) {
                            Util.runOnUIThreadBlocking {
                                o.root.findViewReimplemented<ImageView>(id)?.setImageBitmap(buffer.btm)
                            }
                        }
                    }
                    return true
                }
                "refreshImageView" -> {
                    val aid = m.params?.get("aid")?.asString
                    val a = activities[aid]
                    val id = m.params?.get("id")?.asInt
                    val o = overlays[aid]
                    if (id != null) {
                        if (a != null) {
                            V0.runOnUIThreadActivityStarted(a) {
                                it.findViewReimplemented<ImageView>(id)?.invalidate()
                            }
                        }
                        if (o != null) {
                            Util.runOnUIThreadBlocking {
                                o.root.findViewReimplemented<ImageView>(id)?.invalidate()
                            }
                        }
                    }
                    return true
                }
            }
            return false
        }
    }
}