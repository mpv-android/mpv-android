#include <jni.h>
#include <stdlib.h>
#include <pthread.h>

#include <mpv/client.h>
#include <mpv/opengl_cb.h>
#include <EGL/egl.h>

#include "jni_utils.h"
#include "globals.h"
#include "log.h"

extern "C" {
    jni_func(void, initGL, jobject view);
    jni_func(void, destroyGL);

    jni_func(void, resize, jint width, jint height);
    jni_func(void, draw);
};

static mpv_opengl_cb_context *mpv_gl;
static int width, height;
static pthread_mutex_t gl_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_once_t render_key_once = PTHREAD_ONCE_INIT;
static pthread_key_t render_env;
static jobject gl_view;

jni_func(void, resize, jint w, jint h) {
    ALOGV("Resizing! width: %d=>%d, %d=>%d\n", width, w, height, h);
    width = w;
    height = h;
}

jni_func(void, draw) {
    mpv_opengl_cb_draw(mpv_gl, 0, width, -height);
}

static void render_env_destruct(void *arg) {
    g_vm->DetachCurrentThread();
}

static void render_key_create() {
    pthread_key_create(&render_env, render_env_destruct);
}

static void render_cb(void *data) {
    JNIEnv *env;

    pthread_once(&render_key_once, render_key_create);

    env = reinterpret_cast<JNIEnv*>(pthread_getspecific(render_env));
    if (!env) {
        acquire_jni_env(g_vm, &env);
        pthread_setspecific(render_env, env);
    }

    pthread_mutex_lock(&gl_mutex);
    if (gl_view)
        env->CallVoidMethod(gl_view, java_GLSurfaceView_requestRender);
    pthread_mutex_unlock(&gl_mutex);
}

static void *get_proc_address_mpv(void *fn_ctx, const char *name) {
    return (void*)eglGetProcAddress(name);
}

jni_func(void, initGL, jobject view) {
    int ret = -1;
    if (!g_mpv)
        die("initGL: mpv not initialized");
    if (mpv_gl)
        die("OpenGL ES already initialized!?");

    mpv_gl = (mpv_opengl_cb_context*)mpv_get_sub_api(g_mpv, MPV_SUB_API_OPENGL_CB);
    if (!mpv_gl)
        die("failed to create mpv GL API handle");

    if ((ret = mpv_opengl_cb_init_gl(mpv_gl, NULL, get_proc_address_mpv, NULL)) < 0) {
        ALOGE("mpv_opengl_cb_init_gl returned error %d", ret);
        die("failed to initialize mpv GL context");
    }

    gl_view = reinterpret_cast<jobject>(env->NewGlobalRef(view));
    mpv_opengl_cb_set_update_callback(mpv_gl, (mpv_opengl_cb_update_fn)render_cb, NULL);
}

jni_func(void, destroyGL) {
    if (!mpv_gl)
        die("mpv_gl destroy called but it's already destroyed");

    pthread_mutex_lock(&gl_mutex);
    env->DeleteGlobalRef(gl_view);
    gl_view = NULL;
    pthread_mutex_unlock(&gl_mutex);

    mpv_opengl_cb_uninit_gl(mpv_gl);
    mpv_gl = NULL;
}
