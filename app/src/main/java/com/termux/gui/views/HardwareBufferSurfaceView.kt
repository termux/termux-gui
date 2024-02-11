package com.termux.gui.views

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.HardwareBuffer
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLObjectHandle
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Build
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.termux.gui.hbuffers.HBuffers
import com.termux.gui.protocol.shared.v0.RawInputConnection
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

/**
 * A SurfaceView that can draw a shared HardwareBuffer to the Surface.
 */
class HardwareBufferSurfaceView(c: Context) : SurfaceView(c), Choreographer.FrameCallback {

    /**
     * Configuration of the View.
     */
    class Config {
        /**
         * Behaviour on buffer dimension mismatch with the Surface.
         */
        enum class OnDimensionMismatch {
            /**
             * Center the buffer on the axis.
             */
            CENTER_AXIS,

            /**
             * Attach the buffer at the top/left side of the Surface.
             */
            STICK_TOPLEFT
        }
        var x: OnDimensionMismatch = OnDimensionMismatch.CENTER_AXIS
        var y: OnDimensionMismatch = OnDimensionMismatch.CENTER_AXIS

        /**
         * Color usedin case the buffer is too small.
         */
        var backgroundColor: Int = 0xff000000.toInt()
    }
    
    
    interface SurfaceChangedListener {
        fun onSurfaceChanged(width: Int, height: Int)
    }

    interface FrameCallbackListener {
        fun onSurfaceFrame(timestamp: Long)
    }

    /**
     * EGLImageKHR wrapper.
     */
    @Suppress("EqualsOrHashCode")
    class EGLImageKHR(handle: Long) : EGLObjectHandle(handle) {
        companion object {
            private external fun nativeEglDestroyImageKHR(disp: Long, img: Long): Boolean
            private external fun nativeEGLImageTargetTexture2DOES(img: Long)
            
            
             fun eglImageTargetTexture2DOES(img: EGLImageKHR) {
                 nativeEGLImageTargetTexture2DOES(img.nativeHandle)
             }
            
            fun eglDestroyImageKHR(d: EGLDisplay, img: EGLImageKHR): Boolean {
                return nativeEglDestroyImageKHR(d.nativeHandle, img.nativeHandle)
            }

            
            
            val EGL_NO_IMAGE_KHR: EGLImageKHR = EGLImageKHR(0)
        }

        override fun equals(other: Any?): Boolean {
            if (other is EGLImageKHR && other.nativeHandle == nativeHandle) {
                return true
            }
            return false
        }

    }
    
    
    companion object {
        private var vertexCode: String = ""
        private var fragmentCode: String = ""
        
        private fun checkEGLError(msg: String): Boolean {
            val err = EGL14.eglGetError()
            if (err != EGL14.EGL_SUCCESS) {
                println("%s: %x".format(msg, err))
                return true
            }
            return false
        }
        private fun logGLESError(msg: String) {
            val err = GLES20.glGetError()
            if (err != GLES20.GL_NO_ERROR) {
                println("%s: %x".format(msg, err))
            }
        }
        
    }

    /**
     * Lock for modifying the View, so the main Thread and the connection Thread can draw.
     */
    val RENDER_LOCK = Object()
    
    private var keyListener: OnKeyListener? = null
    var surfaceChangedListener: SurfaceChangedListener? = null
    var frameCallback: FrameCallbackListener? = null
    var config: Config = Config()
    
    private var buffer: HardwareBuffer? = null
    private var bufferImage: EGLImageKHR = EGLImageKHR.EGL_NO_IMAGE_KHR
    private var disp = EGL14.EGL_NO_DISPLAY
    private var eglConfig: EGLConfig? = null
    private var gl: EGLContext = EGL14.EGL_NO_CONTEXT
    private var surface: Surface? = null
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var surfaceChanged = false
    private val posBuffer = ByteBuffer.allocateDirect(8*4)
    private var posI = -1
    private val tposBuffer = ByteBuffer.allocateDirect(8*4)
    private var tposI = -1
    private var prog = -1
    
    
    
    fun setBuffer(b: HardwareBuffer) {
        synchronized(RENDER_LOCK) {
            buffer = b
            if (disp != EGL14.EGL_NO_DISPLAY) {
                if (bufferImage != EGLImageKHR.EGL_NO_IMAGE_KHR) {
                    EGLImageKHR.eglDestroyImageKHR(disp, bufferImage)
                    bufferImage = EGLImageKHR.EGL_NO_IMAGE_KHR
                }
                render()
            }
        }
    }
    
    fun setFrameRate(rate: Float): Boolean {
        val s = surface
        synchronized(RENDER_LOCK) {
            if (s != null && rate > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    s.setFrameRate(rate, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
                }
                return true
            } else {
                return false
            }
        }
    }
    
    
    fun getBuffer(): HardwareBuffer? {
        return buffer
    }
    
    
    
    private fun render() {
        synchronized(RENDER_LOCK) {
            initEGL()
            if (buffer != null && surface != null) {
                if (eglSurface == EGL14.EGL_NO_SURFACE) {
                    eglSurface = EGL14.eglCreateWindowSurface(disp, eglConfig, surface, null, 0)
                    if (eglSurface == EGL14.EGL_NO_SURFACE) {
                        val err = EGL14.eglGetError()
                        deinitEGL()
                        throw RuntimeException("Could create EGLSurface: 0x%x".format(err))
                    }
                }
                
                initGLES()
                if (!EGL14.eglMakeCurrent(disp, eglSurface, eglSurface, gl)) {
                    val err = EGL14.eglGetError()
                    if (err == EGL14.EGL_CONTEXT_LOST) {
                        EGL14.eglDestroyContext(disp, gl)
                        gl = EGL14.EGL_NO_CONTEXT
                        prog = -1
                        posI = -1
                        initEGL()
                        initGLES()
                    } else {
                        throw RuntimeException("Could not make GLES2 context current: 0x%x".format(err))
                    }
                }
                if (surfaceChanged) {
                    surfaceChanged = false
                    GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
                    logGLESError("viewport")
                }
                
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) throw RuntimeException("HardwareBufferSurfaceView used on Android < 8.0")
                val bWidth = buffer!!.width
                val bHeight = buffer!!.height

                val vertexArray = floatArrayOf(
                    -1f, -1f, // bottom left vertex
                    -1f, 1f, // top left vertex
                    1f, -1f, // bottom right vertex
                    1f, 1f // top right vertex
                )
                val textureArray = floatArrayOf(
                    0f, 0f, // bottom left vertex
                    0f, 1f, // top left vertex
                    1f, 0f, // bottom right vertex
                    1f, 1f // top right vertex
                )
                if (bWidth != surfaceWidth) {
                    when (config.x) {
                        Config.OnDimensionMismatch.CENTER_AXIS -> {
                            val marginPX = (bWidth.toFloat() - surfaceWidth.toFloat())/2f
                            val marginNDC = marginPX * (2f / surfaceWidth.toFloat())
                            vertexArray[0] -= marginNDC
                            vertexArray[2] -= marginNDC
                            vertexArray[4] += marginNDC
                            vertexArray[6] += marginNDC
                        }
                        Config.OnDimensionMismatch.STICK_TOPLEFT -> {
                            val ratio = bWidth.toFloat() / surfaceWidth.toFloat()
                            if (ratio < 1f) {
                                // scale the presentation rectangle down to fit the buffer
                                vertexArray[4] *= ratio
                                vertexArray[6] *= ratio
                            }
                            if (ratio > 1f) {
                                // scale the texture coordinates down to show a  slice of the buffer
                                textureArray[4] = 1f / ratio
                                textureArray[6] = 1f / ratio
                            }
                        }
                    }
                }
                if (bHeight != surfaceHeight) {
                    when (config.y) {
                        Config.OnDimensionMismatch.CENTER_AXIS -> {
                            val marginPX = (bHeight.toFloat() - surfaceHeight.toFloat())/2f
                            val marginNDC = marginPX * (2f / surfaceHeight.toFloat())
                            vertexArray[1] -= marginNDC
                            vertexArray[3] += marginNDC
                            vertexArray[5] -= marginNDC
                            vertexArray[7] += marginNDC
                        }
                        Config.OnDimensionMismatch.STICK_TOPLEFT -> {
                            val ratio = bHeight.toFloat() / surfaceHeight.toFloat()
                            if (ratio < 1f) {
                                // scale the presentation rectangle down to fit the buffer
                                vertexArray[1] *= ratio
                                vertexArray[5] *= ratio
                            }
                            if (ratio > 1f) {
                                // scale the texture coordinates down to show a  slice of the buffer
                                textureArray[1] = 1f - 1f / ratio
                                textureArray[5] = 1f - 1f / ratio
                            }
                        }
                    }
                }
                
                
                val fPos = posBuffer.asFloatBuffer()
                val fTex = tposBuffer.asFloatBuffer()
                for (i in vertexArray.indices) {
                    fPos.put(vertexArray[i])
                    fTex.put(textureArray[i])
                }
                
                GLES20.glVertexAttribPointer(posI, 2, GLES20.GL_FLOAT, false, 0, posBuffer)
                logGLESError("vertexAttribPointer")
                GLES20.glVertexAttribPointer(tposI, 2, GLES20.GL_FLOAT, false, 0, tposBuffer)
                logGLESError("vertexAttribPointer")
                
                GLES20.glClearColor(
                    (config.backgroundColor and 0xff).toFloat()/255f,
                    ((config.backgroundColor ushr 8) and 0xff).toFloat()/255f,
                    ((config.backgroundColor ushr 16) and 0xff).toFloat()/255f,
                    ((config.backgroundColor ushr 24) and 0xff).toFloat()/255f)
                logGLESError("glClearColor")
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                logGLESError("glClear")
                
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
                logGLESError("drawArrays")
                
                EGL14.eglSwapBuffers(disp, eglSurface)
                checkEGLError("swap buffers")
                
                EGL14.eglMakeCurrent(disp, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            }
        }
    }
    
    private fun createBufferImage() {
        if (bufferImage == EGLImageKHR.EGL_NO_IMAGE_KHR && buffer != null && disp != EGL14.EGL_NO_DISPLAY) {
            bufferImage = EGLImageKHR(HBuffers.hardwareBufferToEGLImageKHR(disp, buffer!!))
            if (bufferImage == EGLImageKHR.EGL_NO_IMAGE_KHR) {
                deinitEGL()
                throw RuntimeException("Could create EGLImageKHR: "+EGL14.eglGetError())
            }
        }
    }
    
    private fun initGLES() {
        if (eglConfig == null)
            initEGL()
        if (gl == EGL14.EGL_NO_CONTEXT) {
            if (!EGL14.eglBindAPI(EGL14.EGL_OPENGL_ES_API)) {
                val err = EGL14.eglGetError()
                deinitEGL()
                throw RuntimeException("Could not bind GLES: 0x%x".format(err))
            }
            gl = EGL14.eglCreateContext(
                disp, eglConfig, EGL14.EGL_NO_CONTEXT, intArrayOf(
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
                ), 0
            )
            if (gl == EGL14.EGL_NO_CONTEXT) {
                val err = EGL14.eglGetError()
                deinitEGL()
                throw RuntimeException("Could not create GLES2 context: 0x%x".format(err))
            }
            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                if (!EGL14.eglMakeCurrent(disp, eglSurface, eglSurface, gl)) {
                    val err = EGL14.eglGetError()
                    deinitEGL()
                    throw RuntimeException("Could not make GLES2 context current: 0x%x".format(err))
                }
            }
        }
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            if (!EGL14.eglMakeCurrent(disp, eglSurface, eglSurface, gl)) {
                val err = EGL14.eglGetError()
                deinitEGL()
                throw RuntimeException("Could not make GLES2 context current: 0x%x".format(err))
            }
            val exts = GLES20.glGetString(GLES20.GL_EXTENSIONS)
            logGLESError("get gles2 extensions")
            if (exts == null) {
                deinitEGL()
                throw RuntimeException("Could not query GLES2 extensions")
            }
            if (!exts.contains("GL_OES_EGL_image_external")) {
                deinitEGL()
                throw RuntimeException("GLES2 doesn't have the GL_OES_EGL_image_external extension")
            }
            if (buffer != null && bufferImage == EGLImageKHR.EGL_NO_IMAGE_KHR) {
                createBufferImage()
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE1)
                logGLESError("bind external texture")
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
                logGLESError("glActiveTexture")
                EGLImageKHR.eglImageTargetTexture2DOES(bufferImage)
                checkEGLError("eglImageTargetTexture2DOES")
                GLES20.glTexParameteri(
                    GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST
                )
                logGLESError("set min filter")
                GLES20.glTexParameteri(
                    GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_NEAREST
                )
                logGLESError("set mag filter")
            }
            if (prog == -1) {
                val frag = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
                logGLESError("create shader")
                GLES20.glShaderSource(frag, fragmentCode)
                logGLESError("shader source")
                GLES20.glCompileShader(frag)
                logGLESError("compile shader")
                val vert = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
                logGLESError("create shader")
                GLES20.glShaderSource(vert, vertexCode)
                logGLESError("shader source")
                GLES20.glCompileShader(vert)
                logGLESError("compile shader")
                prog = GLES20.glCreateProgram()
                logGLESError("create program")
                GLES20.glAttachShader(prog, vert)
                logGLESError("attach vertex shader")
                GLES20.glAttachShader(prog, frag)
                logGLESError("attach fragment shader shader")
                GLES20.glLinkProgram(prog)
                val err: Int = GLES20.glGetError()
                val status = intArrayOf(0)
                GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, status, 0)
                if (err != GLES20.GL_NO_ERROR || status[0] != GLES20.GL_TRUE) {
                    println("GLES error link program: $err")
                    println(GLES20.glGetProgramInfoLog(prog))
                    println(GLES20.glGetShaderInfoLog(vert))
                    println(GLES20.glGetShaderInfoLog(frag))
                }
                GLES20.glUseProgram(prog)
                logGLESError("use program")
            }
            if (prog != -1 && bufferImage != EGLImageKHR.EGL_NO_IMAGE_KHR) {
                val posS = GLES20.glGetUniformLocation(prog, "hbSampler")
                logGLESError("glGetUniformLocation")
                GLES20.glUniform1i(posS, 1)
            }
            if (posI == -1) {
                posI = GLES20.glGetAttribLocation(prog, "pos")
                logGLESError("get attrib location")
                GLES20.glEnableVertexAttribArray(posI)
                logGLESError("enableVertexAttribArray")
            }
            if (tposI == -1) {
                tposI = GLES20.glGetAttribLocation(prog, "tpos")
                logGLESError("get attrib location")
                GLES20.glEnableVertexAttribArray(tposI)
                logGLESError("enableVertexAttribArray")
                GLES20.glVertexAttribPointer(tposI, 2, GLES20.GL_FLOAT, false, 0, tposBuffer)
                logGLESError("vertexAttribPointer")
            }
            EGL14.eglMakeCurrent(disp, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        }
    }
    
    
    
    private val callback = object: SurfaceHolder.Callback, SurfaceHolder.Callback2 {
        override fun surfaceCreated(holder: SurfaceHolder) {
            synchronized(RENDER_LOCK) {
                surface = holder.surface
                if (eglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(disp, eglSurface)
                    eglSurface = EGL14.EGL_NO_SURFACE
                }
            }
        }
        
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            synchronized(RENDER_LOCK) {
                surfaceChangedListener?.onSurfaceChanged(width, height)
                surfaceWidth = width
                surfaceHeight = height
                surfaceChanged = true
                render()
            }
        }
        
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            synchronized(RENDER_LOCK) {
                surface = null
                if (eglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(disp, eglSurface)
                    eglSurface = EGL14.EGL_NO_SURFACE
                }
            }
        }
        
        override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
            render()
        }

    }
    
    override fun onAttachedToWindow() {
        synchronized(RENDER_LOCK) {
            initEGL()
        }
        super.onAttachedToWindow()
        Choreographer.getInstance().postFrameCallback(this)
    }
    
    override fun onDetachedFromWindow() {
        synchronized(RENDER_LOCK) {
            deinitEGL()
        }
        super.onDetachedFromWindow()
        Choreographer.getInstance().removeFrameCallback(this)
    }
    
    fun finalize() {
        deinitEGL()
    }
    
    
    private fun deinitEGL() {
        if (disp != EGL14.EGL_NO_DISPLAY) {
            if (gl != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(disp, gl)
                gl = EGL14.EGL_NO_CONTEXT
                EGL14.eglMakeCurrent(disp, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                posI = -1
                prog = -1
            }
            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(disp, eglSurface)
                eglSurface = EGL14.EGL_NO_SURFACE
            }
            if (bufferImage != EGLImageKHR.EGL_NO_IMAGE_KHR) {
                EGLImageKHR.eglDestroyImageKHR(disp, bufferImage)
                bufferImage = EGLImageKHR.EGL_NO_IMAGE_KHR
            }
            
            EGL14.eglTerminate(disp)
            disp = EGL14.EGL_NO_DISPLAY
        }
    }
    
    private fun initEGL() {
        if (disp == EGL14.EGL_NO_DISPLAY) {
            disp = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (disp == EGL14.EGL_NO_DISPLAY) {
                throw RuntimeException("Could not get EGL display: 0x%x".format(EGL14.eglGetError()))
            }
            // Android bug: minor version is only updated when using 2 separate arrays with offset 0
            val major = IntArray(1)
            val minor = IntArray(1)
            if (!EGL14.eglInitialize(disp, major, 0, minor, 0)) {
                throw RuntimeException("Could not initialize EGL display: 0x%x".format(EGL14.eglGetError()))
            }
            if (major[0] != 1 || minor[0] < 2) {
                deinitEGL()
                throw RuntimeException("EGL version less than 1.2: " + major[0] + "." + minor[0])
            }
            val eglExts = EGL14.eglQueryString(disp, EGL14.EGL_EXTENSIONS)
            if (!eglExts.contains("EGL_KHR_image_base")) {
                deinitEGL()
                throw RuntimeException("EGL extension EGL_KHR_image_base not found")
            }
            if (!eglExts.contains("EGL_ANDROID_image_native_buffer")) {
                deinitEGL()
                throw RuntimeException("EGL extension EGL_ANDROID_image_native_buffer not found")
            }
            if (!eglExts.contains("EGL_ANDROID_get_native_client_buffer")) {
                deinitEGL()
                throw RuntimeException("EGL extension EGL_ANDROID_get_native_client_buffer not found")
            }
            val configs: Array<EGLConfig?> = Array(1) { null }
            val numConfigs = IntArray(1)
            if (!EGL14.eglChooseConfig(disp, intArrayOf(
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_COLOR_BUFFER_TYPE, EGL14.EGL_RGB_BUFFER,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_NONE
            ), 0, configs, 0, 1, numConfigs, 0)) {
                deinitEGL()
                throw RuntimeException("Could not get EGL configs: 0x%x".format(EGL14.eglGetError()))
            }
            if (numConfigs[0] == 0) {
                deinitEGL()
                throw RuntimeException("No appropriate EGL configuration")
            }
            eglConfig = configs[0]
        }
        if (! EGL14.eglBindAPI(EGL14.EGL_OPENGL_ES_API)) {
            checkEGLError("eglBindAPI")
        }
        initGLES()
    }
    
    
    init {
        posBuffer.order(ByteOrder.nativeOrder())
        tposBuffer.order(ByteOrder.nativeOrder())
        synchronized(this.javaClass) {
            if (vertexCode == "") {
                val assets = context.assets
                vertexCode = assets.open("SurfaceShader.vert").use {
                    it.bufferedReader(Charset.defaultCharset()).readText()
                }
                fragmentCode = assets.open("SurfaceShader.frag").use {
                    it.bufferedReader(Charset.defaultCharset()).readText()
                }
            }
        }
        
        holder.setFormat(PixelFormat.RGBA_8888)
        setZOrderOnTop(true)
        
        initEGL()
        holder.addCallback(callback)
        
    }
    
    
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        val l = keyListener
        return if (l != null) {
            outAttrs.inputType = EditorInfo.TYPE_NULL
            RawInputConnection(l)
        } else {
            super.onCreateInputConnection(outAttrs)
        }
    }
    
    override fun setOnKeyListener(l: OnKeyListener?) {
        keyListener = l
        super.setOnKeyListener(l)
    }

    override fun doFrame(frameTimeNanos: Long) {
        frameCallback?.onSurfaceFrame(frameTimeNanos)
        if (isAttachedToWindow) Choreographer.getInstance().postFrameCallback(this)
    }

}