#include <cerrno>
#include <jni.h>
#include <android/log.h>
#include <android/hardware_buffer.h>
#include <android/hardware_buffer_jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/api-level.h>

#include <EGL/egl.h>
#include <EGL/eglext.h>

#include <csignal>

#include <atomic>


static std::atomic<PFNEGLCREATEIMAGEKHRPROC> createImage{nullptr};
static std::atomic<PFNEGLGETNATIVECLIENTBUFFERANDROIDPROC> getClientBuffer{nullptr};

extern "C"
JNIEXPORT jlong JNICALL
Java_com_termux_gui_hbuffers_HBuffers_00024Companion_nativeHardwareBufferToEGLImageKHR(JNIEnv *env,
                                                                                       jobject thiz,
                                                                                       jlong dispJ,
                                                                                       jobject bJ) {
    auto disp = (EGLDisplay) dispJ;
    AHardwareBuffer* b = AHardwareBuffer_fromHardwareBuffer(env, bJ);
    if (createImage.load() == nullptr) {
        createImage = (PFNEGLCREATEIMAGEKHRPROC) eglGetProcAddress("eglCreateImageKHR");
        if (createImage.load() == nullptr) {
            jclass nullPointerException = env->FindClass("java/lang/NullPointerException");
            if (nullPointerException == nullptr) {
                __android_log_print( ANDROID_LOG_ERROR,"nativeHardwareBufferToEGLImageKHR", "Could not find class NullPointerException\n");
            } else {
                env->ThrowNew(nullPointerException, "Could not get eglCreateImageKHR address");
            }
            return reinterpret_cast<jlong>(EGL_NO_IMAGE_KHR);
        }
    }
    if (getClientBuffer.load() == nullptr) {
        getClientBuffer = (PFNEGLGETNATIVECLIENTBUFFERANDROIDPROC) eglGetProcAddress("eglGetNativeClientBufferANDROID");
        if (getClientBuffer.load() == nullptr) {
            jclass nullPointerException = env->FindClass("java/lang/NullPointerException");
            if (nullPointerException == nullptr) {
                __android_log_print( ANDROID_LOG_ERROR,"nativeHardwareBufferToEGLImageKHR", "Could not find class NullPointerException\n");
            } else {
                env->ThrowNew(nullPointerException, "Could not get eglGetNativeClientBufferANDROID address");
            }
            return reinterpret_cast<jlong>(EGL_NO_IMAGE_KHR);
        }
    }
    EGLClientBuffer cb = getClientBuffer.load()(b);
    if (cb == nullptr) {
        return reinterpret_cast<jlong>(EGL_NO_IMAGE_KHR);
    }
    EGLint attribs[] = {EGL_IMAGE_PRESERVED_KHR, EGL_TRUE, EGL_NONE};
    EGLImageKHR img = createImage.load()(disp, EGL_NO_CONTEXT, EGL_NATIVE_BUFFER_ANDROID, cb, attribs);
    return reinterpret_cast<jlong>(img);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_termux_gui_hbuffers_HBuffers_00024Companion_sendHardwareBuffer(JNIEnv *env, jobject thiz,
                                                                        jint fd, jobject bJ) {
    AHardwareBuffer* b = AHardwareBuffer_fromHardwareBuffer(env, bJ);
    // Ignore sigpipe as a precaution, AHardwareBuffer_sendHandleToUnixSocket doesn't use the NO_SIGNAL flag for sendmsg
    signal(SIGPIPE, SIG_IGN);
    return AHardwareBuffer_sendHandleToUnixSocket(b, fd);
}