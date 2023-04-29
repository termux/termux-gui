package com.termux.gui.protocol.protobuf.v0

import android.graphics.Bitmap
import android.hardware.HardwareBuffer
import android.net.LocalSocket
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SharedMemory
import android.util.Log
import com.termux.gui.ConnectionHandler
import com.termux.gui.Util
import com.termux.gui.hbuffers.HBuffers
import com.termux.gui.protocol.protobuf.ProtoUtils
import com.termux.gui.protocol.shared.v0.DataClasses
import com.termux.gui.protocol.protobuf.v0.GUIProt0.*
import com.termux.gui.protocol.shared.v0.V0Shared
import java.io.DataOutputStream
import java.io.FileDescriptor
import java.io.OutputStream
import java.lang.RuntimeException
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import java.util.Collections
import java.util.HashMap
import java.util.Random


class HandleBuffer(val buffers: MutableMap<Int, DataClasses.SharedBuffer>, val hardwareBuffers: MutableMap<Int, HardwareBuffer>,
                   val main: OutputStream, val rand: Random, val sock: LocalSocket, val logger: V0Proto.ProtoLogger) {
    
    fun addBuffer(m: AddBufferRequest) {
        main.flush()
        val out = DataOutputStream(main)
        try {
            val f = m.f!!
            val w = m.width
            val h = m.height
            if (w <= 0 || h <= 0) {
                out.writeInt(-1)
                out.flush()
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
                ret.code = Error.BUFFER_NOT_FOUND
            }
        } catch (e: java.lang.Exception) {
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
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
                ret.code = Error.BUFFER_NOT_FOUND
            }
        } catch (e: java.lang.Exception) {
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }
    
    
    fun createHardwareBuffer(m: CreateHardwareBufferRequest) {
        main.flush()
        val out = DataOutputStream(main)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            out.writeInt(-1)
            out.flush()
        } else {
            val k = Util.generateIndex(rand, hardwareBuffers.keys)
            var b: HardwareBuffer? = null
            @Suppress("KotlinConstantConditions")
            try {
                b = HardwareBuffer.create(
                    m.width,
                    m.height,
                    when (m.format) {
                        CreateHardwareBufferRequest.Format.RGBA8888 -> HardwareBuffer.RGBA_8888
                        CreateHardwareBufferRequest.Format.RGBX8888 -> HardwareBuffer.RGBX_8888
                        CreateHardwareBufferRequest.Format.RGB888 -> HardwareBuffer.RGB_888
                        CreateHardwareBufferRequest.Format.RGB565 -> HardwareBuffer.RGB_565
                        else -> HardwareBuffer.RGB_888
                    },
                    1,
                    HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT or when (m.cpuRead) {
                        CreateHardwareBufferRequest.CPUUsage.never -> 0
                        CreateHardwareBufferRequest.CPUUsage.rarely -> HardwareBuffer.USAGE_CPU_READ_RARELY
                        CreateHardwareBufferRequest.CPUUsage.often -> HardwareBuffer.USAGE_CPU_READ_OFTEN
                        else -> HardwareBuffer.USAGE_CPU_READ_RARELY
                    } or when (m.cpuWrite) {
                        CreateHardwareBufferRequest.CPUUsage.never -> 0
                        CreateHardwareBufferRequest.CPUUsage.rarely -> HardwareBuffer.USAGE_CPU_WRITE_RARELY
                        CreateHardwareBufferRequest.CPUUsage.often -> HardwareBuffer.USAGE_CPU_WRITE_OFTEN
                        else -> HardwareBuffer.USAGE_CPU_WRITE_RARELY
                    }
                )
                hardwareBuffers[k] = b
            }  catch (e: Exception) {
                hardwareBuffers.remove(k)
                b?.close()
                out.writeInt(-1)
                out.flush()
                return
            }
            out.writeInt(k)
            out.flush()
            ParcelFileDescriptor.dup(sock.fileDescriptor).use {
                if (HBuffers.sendHardwareBuffer(it.fd, b) != 0) {
                    throw RuntimeException("Hardware buffer send error, connection in undefined state, closing")
                }
            }
            out.flush()
        }
    }
    
    
    fun destroyHardwareBuffer(m: DestroyHardwareBufferRequest) {
        val ret = DestroyHardwareBufferResponse.newBuilder()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            ret.success = false
            ret.code = Error.ANDROID_VERSION_TOO_LOW
            ProtoUtils.write(ret, main)
            return
        }
        try {
            val b: HardwareBuffer? = hardwareBuffers.remove(m.buffer)
            if (b != null) {
                b.close()
                ret.success = true
            } else {
                ret.success = false
                ret.code = Error.BUFFER_NOT_FOUND
            }
        } catch (e: java.lang.Exception) {
            ret.success = false
            ret.code = Error.INTERNAL_ERROR
        }
        ProtoUtils.write(ret, main)
    }
    
    
}