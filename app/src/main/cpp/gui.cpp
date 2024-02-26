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

static PFNEGLCREATESYNCKHRPROC  eglCreateSyncKHR   = nullptr;
static PFNEGLWAITSYNCKHRPROC    eglWaitSyncKHR     = nullptr;
static PFNEGLDESTROYSYNCKHRPROC eglDestroySyncKHR  = nullptr;

static jfieldID g_field_descriptor       = nullptr;

static jint native_sync(JNIEnv* env, jobject thiz, jobject fence) {
    int          fd = EGL_NO_NATIVE_FENCE_FD_ANDROID;
    EGLDisplay  dpy = EGL_NO_DISPLAY;
    EGLSyncKHR sync = EGL_NO_SYNC_KHR;

    fd = env->GetIntField(fence, g_field_descriptor);
    dpy = eglGetCurrentDisplay();
    if (dpy == EGL_NO_DISPLAY) {
        goto error;
    }
    sync = eglCreateSyncKHR(
            dpy,
            EGL_SYNC_NATIVE_FENCE_ANDROID,
            (EGLint[]){
                EGL_SYNC_NATIVE_FENCE_FD_ANDROID, fd,
                EGL_NONE
            });
    if (sync == EGL_NO_SYNC_KHR) {
        goto error;
    }
    if (eglWaitSyncKHR(dpy, sync, 0) != EGL_TRUE) {
        goto error;
    }
    if (eglDestroySyncKHR(dpy, sync) != EGL_TRUE) {
        goto error;
    }
    return 0;

error:
    if (fd != EGL_NO_NATIVE_FENCE_FD_ANDROID) {
        close(fd);
    }
    return eglGetError();
}

static JNINativeMethod gMethods[] = {
        {"nativeSync", "(Ljava/io/FileDescriptor;)I", (void*) native_sync},
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv* env = nullptr;
    jclass  clz = nullptr;
    jclass  cfd = nullptr;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI_OnLoad", "error get env");
        goto error;
    }

    clz = env->FindClass("com/termux/gui/views/HardwareBufferSurfaceView$EGLSyncKHR$Companion");
    if (clz == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI_OnLoad", "error get clz");
        goto error;
    }

    if (env->RegisterNatives(clz, gMethods, sizeof(gMethods)/sizeof(gMethods[0])) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI_OnLoad", "error register methods");
        goto error;
    }

    eglCreateSyncKHR = (PFNEGLCREATESYNCKHRPROC) eglGetProcAddress("eglCreateSyncKHR");
    if (eglCreateSyncKHR == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI_OnLoad", "error get eglCreateSyncKHR");
        goto error;
    }

    eglWaitSyncKHR = (PFNEGLWAITSYNCKHRPROC) eglGetProcAddress("eglWaitSyncKHR");
    if (eglWaitSyncKHR == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI_OnLoad", "error get eglWaitSyncKHR");
        goto error;
    }

    eglDestroySyncKHR = (PFNEGLDESTROYSYNCKHRPROC) eglGetProcAddress("eglDestroySyncKHR");
    if (eglDestroySyncKHR == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI_OnLoad", "error get eglDestroySyncKHR");
        goto error;
    }

    cfd = env->FindClass("java/io/FileDescriptor");
    if (cfd == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI_OnLoad", "error get FileDescriptor");
        goto error;
    }

    g_field_descriptor = env->GetFieldID(cfd, "descriptor", "I");
    if (g_field_descriptor == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI_OnLoad", "error get field descriptor");
        goto error;
    }

    return JNI_VERSION_1_4;

error:
    __android_log_print(ANDROID_LOG_ERROR, "JNI_OnLoad", "error load class EGLSyncKHR");
    return -1;
}
