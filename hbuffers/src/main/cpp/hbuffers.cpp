#include <cerrno>
#include <jni.h>
#include <android/log.h>
#include <android/hardware_buffer.h>
#include <android/hardware_buffer_jni.h>


#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>


thread_local EGLDisplay d = EGL_NO_DISPLAY;
thread_local EGLContext c = EGL_NO_CONTEXT;



extern "C"
JNIEXPORT jboolean JNICALL
Java_com_termux_gui_hbuffers_HBuffers_00024Companion_nativeRenderBuffer(JNIEnv *env, jobject thiz,
                                                            jobject surface, jobject buffer) {
    if (d == EGL_NO_DISPLAY) {
        d = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        if (d == EGL_NO_DISPLAY)
            return false;
        if (! eglInitialize(d, nullptr, nullptr))
            return false;
        const EGLint attrib[] = {
                
                EGL_BLUE_SIZE, 8,
                EGL_GREEN_SIZE, 8,
                EGL_RED_SIZE, 8,
                EGL_NONE
        };
        EGLConfig conf;
        EGLint configs = 0;
        eglChooseConfig(d, attrib, &conf, 1, &configs);
        if (configs == 0) {
            eglTerminate(d);
            return false;
        }
        
        //c = eglCreateContext(d, )
    }
    
    
    
    
    
    return true;
}



extern "C"
JNIEXPORT void JNICALL
Java_com_termux_gui_hbuffers_HBuffers_00024Companion_nativeCleanup(JNIEnv *env, jobject thiz) {
    
    
    
    if (d != EGL_NO_DISPLAY) {
        eglTerminate(d);
        d = EGL_NO_DISPLAY;
    }
    
    
    
    
}