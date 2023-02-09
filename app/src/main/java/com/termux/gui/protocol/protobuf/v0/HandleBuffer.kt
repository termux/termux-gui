package com.termux.gui.protocol.protobuf.v0

import android.graphics.Bitmap
import android.net.LocalSocket
import android.os.Build
import android.os.SharedMemory
import android.util.Log
import com.termux.gui.ConnectionHandler
import com.termux.gui.protocol.protobuf.ProtoUtils
import com.termux.gui.protocol.shared.v0.DataClasses
import com.termux.gui.protocol.protobuf.v0.GUIProt0.*
import com.termux.gui.protocol.shared.v0.V0Shared
import java.io.DataOutputStream
import java.io.FileDescriptor
import java.io.OutputStream
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import java.util.Random


class HandleBuffer(val buffers: MutableMap<Int, DataClasses.SharedBuffer>, val main: OutputStream, val rand: Random, val sock: LocalSocket) {
    
    fun addBuffer(m: AddBufferRequest) {
        main.flush()
        val out = DataOutputStream(main)
        try {
            val f = m.f!!
            val w = m.width
            val h = m.height
            if (w <= 0 || h <= 0) {
                out.writeInt(-1)
                return
            }
            when (f) {
                AddBufferRequest.Format.ARGB8888 -> {
                    val bid = V0Shared.generateBufferID(rand, buffers)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        val shm = SharedMemory.create(bid.toString(), w * h * 4)
                        val b = DataClasses.SharedBuffer(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888, true), shm, shm.mapReadOnly(), null)
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
                                out.writeInt(-1)
                                out.flush()
                                return
                            }
                            val fdesc = FileDescriptor()
                            val setInt = FileDescriptor::class.java.getDeclaredMethod("setInt$", Int::class.java)
                            setInt(fdesc, fdint)
                            out.writeInt(bid)
                            ProtoUtils.sendBufferFD(out, sock, fdesc)
                            buffers[bid] = b
                        } catch (e: Exception) {
                            SharedMemory.unmap(b.buff)
                            b.shm?.close()
                            b.btm.recycle()
                            if (e is NoSuchMethodException || e is IllegalArgumentException || e is IllegalAccessException ||
                                e is InstantiationException || e is InvocationTargetException
                            ) {
                                println("reflection exception")
                                e.printStackTrace()
                                out.writeInt(-1)
                                out.flush()
                                return
                            } else {
                                throw e
                            }
                        }
                    } else {
                        println("creating buffer on API 26-")
                        val fdint = ConnectionHandler.create_ashmem(w * h * 4)
                        if (fdint == -1) {
                            println("could not create ashmem with NDK")
                            out.writeInt(-1)
                            out.flush()
                            return
                        }
                        val buff: ByteBuffer? = ConnectionHandler.map_ashmem(fdint, w * h * 4)
                        if (buff == null) {
                            println("could not map ashmem with NDK")
                            ConnectionHandler.destroy_ashmem(fdint)
                            out.writeInt(-1)
                            out.flush()
                            return
                        }
                        try {
                            val fdesc = FileDescriptor()
                            val setInt = FileDescriptor::class.java.getDeclaredMethod("setInt$", Int::class.java)
                            setInt(fdesc, fdint)
                            out.writeInt(bid)
                            ProtoUtils.sendBufferFD(out, sock, fdesc)
                            buffers[bid] = DataClasses.SharedBuffer(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888), null, buff, fdint)
                        } catch (e: Exception) {
                            ConnectionHandler.unmap_ashmem(buff)
                            ConnectionHandler.destroy_ashmem(fdint)
                            if (e is NoSuchMethodException || e is IllegalArgumentException || e is IllegalAccessException ||
                                e is InstantiationException || e is InvocationTargetException) {
                                println("reflection exception")
                                e.printStackTrace()
                                out.writeInt(-1)
                                out.flush()
                                return
                            } else {
                                throw e
                            }
                        }
                    }
                }
                AddBufferRequest.Format.UNRECOGNIZED -> {
                    out.writeInt(-1)
                    out.flush()
                    return
                }
            }
        } catch (e: java.lang.Exception) {
            Log.d(this.javaClass.name, "Exception: ", e)
            out.writeInt(-1)
            out.flush()
        }
    }
    
    fun deleteBuffer(m: DeleteBufferRequest) {
        val ret = DeleteBufferResponse.newBuilder()
        try {
            val buffer = buffers[m.buffer]
            if (buffer != null) {
                buffers.remove(m.buffer)
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
                ret.success = true
            } else {
                ret.success = false
            }
        } catch (e: java.lang.Exception) {
            ret.success = false
        }
        ProtoUtils.write(ret, main)
    }

    fun blitBuffer(m: BlitBufferRequest) {
        val ret = BlitBufferResponse.newBuilder()
        try {
            val buffer = buffers[m.buffer]
            if (buffer != null) {
                buffer.btm.copyPixelsFromBuffer(buffer.buff)
                buffer.buff.position(0)
                ret.success = true
            } else {
                ret.success = false
            }
        } catch (e: java.lang.Exception) {
            ret.success = false
        }
        ProtoUtils.write(ret, main)
    }
    
}