#include <linux/ashmem.h>
#include <cerrno>
#include <jni.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <linux/ashmem.h>
#include <android/log.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <atomic>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

extern "C"
JNIEXPORT jint JNICALL
Java_com_termux_gui_ConnectionHandler_00024Companion_create_1ashmem(JNIEnv *env, jobject thiz, jint size) {
    __android_log_print( ANDROID_LOG_DEBUG,"create_ashmem", "creating ashmem\n");
    int fd = open("/dev/ashmem", O_RDWR);
    if (fd == -1) {
        __android_log_print( ANDROID_LOG_DEBUG,"create_ashmem", "could not open /dev/ashmem: %d\n", errno);
        return -1;
    }
    if (ioctl(fd, ASHMEM_SET_SIZE, (size_t) size) == -1) {
        __android_log_print( ANDROID_LOG_DEBUG,"create_ashmem", "could not set ashmem size: %d\n", errno);
        close(fd);
        return -1;
    }
    __android_log_print( ANDROID_LOG_DEBUG,"create_ashmem", "ashmem created successfully\n");
    return fd;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_termux_gui_ConnectionHandler_00024Companion_destroy_1ashmem(JNIEnv *env, jobject thiz, jint fd) {
    __android_log_print( ANDROID_LOG_DEBUG,"destroy_ashmem", "closing ashmem fd\n");
    close(fd);
}


extern "C"
JNIEXPORT jobject JNICALL
Java_com_termux_gui_ConnectionHandler_00024Companion_map_1ashmem(JNIEnv *env, jobject thiz, jint fd, jint size) {
    __android_log_print( ANDROID_LOG_DEBUG,"map_ashmem", "mapping ashmem\n");
    void* mem = mmap(nullptr, size, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (mem == MAP_FAILED) {
        __android_log_print( ANDROID_LOG_DEBUG,"map_ashmem", "could not map ashmem: %d\n", errno);
        return nullptr;
    }
    return env->NewDirectByteBuffer(mem, size);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_termux_gui_ConnectionHandler_00024Companion_unmap_1ashmem(JNIEnv *env, jobject thiz, jobject buff) {
    __android_log_print( ANDROID_LOG_DEBUG,"unmap_ashmem", "unmapping ashmem\n");
    void* adr = env->GetDirectBufferAddress(buff);
    jlong cap = env->GetDirectBufferCapacity(buff);
    if (adr != nullptr && cap != -1) {
        __android_log_print( ANDROID_LOG_DEBUG,"unmap_ashmem", "ashmem unmapped\n");
        munmap(adr, cap);
    } else {
        __android_log_print( ANDROID_LOG_DEBUG,"unmap_ashmem", "could not unmap ashmem, could not get direct buffer address or capacity\n");
    }
}


static std::atomic<PFNEGLDESTROYIMAGEKHRPROC> destroyImage{nullptr};

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_termux_gui_views_HardwareBufferSurfaceView_00024EGLImageKHR_00024Companion_nativeEglDestroyImageKHR(
        JNIEnv *env, jobject thiz, jlong disp, jlong img) {
    if (destroyImage.load() == nullptr) {
        destroyImage = (PFNEGLDESTROYIMAGEKHRPROC) eglGetProcAddress("eglDestroyImageKHR");
        if (destroyImage.load() == nullptr) {
            jclass nullPointerException = env->FindClass("java/lang/NullPointerException");
            if (nullPointerException == nullptr) {
                __android_log_print( ANDROID_LOG_ERROR,"nativeEglDestroyImageKHR", "Could not find class NullPointerException\n");
            } else {
                env->ThrowNew(nullPointerException, "Could not get eglDestroyImageKHR address");
            }
            return JNI_FALSE;
        }
    }
    return destroyImage.load()((EGLDisplay) disp, (EGLImageKHR) img);
}


static std::atomic<PFNGLEGLIMAGETARGETTEXTURE2DOESPROC> eglImageTargetTexture2D{nullptr};

extern "C"
JNIEXPORT void JNICALL
Java_com_termux_gui_views_HardwareBufferSurfaceView_00024EGLImageKHR_00024Companion_nativeEGLImageTargetTexture2DOES(
        JNIEnv *env, jobject thiz, jlong img) {
    if (eglImageTargetTexture2D.load() == nullptr) {
        eglImageTargetTexture2D = (PFNGLEGLIMAGETARGETTEXTURE2DOESPROC) eglGetProcAddress("glEGLImageTargetTexture2DOES");
        if (eglImageTargetTexture2D.load() == nullptr) {
            jclass nullPointerException = env->FindClass("java/lang/NullPointerException");
            if (nullPointerException == nullptr) {
                __android_log_print( ANDROID_LOG_ERROR,"nativeEglDestroyImageKHR", "Could not find class NullPointerException\n");
            } else {
                env->ThrowNew(nullPointerException, "Could not get glEGLImageTargetTexture2DOES address");
            }
            return;
        }
    }
    eglImageTargetTexture2D.load()(GL_TEXTURE_EXTERNAL_OES, (GLeglImageOES) img);
}