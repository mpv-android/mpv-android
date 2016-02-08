#include <jni.h>
#include <stdlib.h>
#include <time.h>
#include <locale.h>

#include <mpv/client.h>
#include <mpv/opengl_cb.h>

#include <EGL/egl.h>

#include "main.h"

extern "C" {
    JNIEXPORT void JNICALL Java_is_xyz_mpv_MPVLib_init(JNIEnv* env, jobject obj);
    JNIEXPORT void JNICALL Java_is_xyz_mpv_MPVLib_resize(JNIEnv* env, jobject obj, jint width, jint height);
    JNIEXPORT void JNICALL Java_is_xyz_mpv_MPVLib_step(JNIEnv* env, jobject obj);
};


static void die(const char *msg)
{
    ALOGE("%s", msg);
    exit(1);
}

static void *get_proc_address_mpv(void *fn_ctx, const char *name)
{
    return (void*)eglGetProcAddress(name);
}

mpv_handle *mpv;
mpv_opengl_cb_context *mpv_gl;

JNIEXPORT void JNICALL
Java_is_xyz_mpv_MPVLib_init(JNIEnv* env, jobject obj) {
    ALOGE("init");

    setlocale(LC_NUMERIC, "C");

    mpv = mpv_create();
    if (!mpv)
        die("context init failed");

    int res;
    int terminal = 1;
    res = mpv_set_option(mpv, "terminal", MPV_FORMAT_FLAG, &terminal);
    ALOGE("mpv_set_option terminal ret %d", res);
    res = mpv_set_option_string(mpv, "msg-level", "all=v");
    ALOGE("mpv_set_option_string msg-level ret %d", res);

    if (mpv_initialize(mpv) < 0)
        die("mpv init failed");
    mpv_gl = (mpv_opengl_cb_context*)mpv_get_sub_api(mpv, MPV_SUB_API_OPENGL_CB);
    if (!mpv_gl)
        die("failed to create mpv GL API handle");

    if (mpv_opengl_cb_init_gl(mpv_gl, NULL, get_proc_address_mpv, NULL) < 0)
        die("failed to initialize mpv GL context");

    if (mpv_set_option_string(mpv, "vo", "opengl-cb") < 0)
        die("failed to set VO");
    if (mpv_set_option_string(mpv, "ao", "openal") < 0)
        die("failed to set AO");

    const char *cmd[] = {"loadfile", "/sdcard1/1.mp4", NULL};
    res = mpv_command(mpv, cmd);
    ALOGE("mpv_command returns %d", res);
}

JNIEXPORT void JNICALL
Java_is_xyz_mpv_MPVLib_resize(JNIEnv* env, jobject obj, jint width, jint height) {
}

JNIEXPORT void JNICALL
Java_is_xyz_mpv_MPVLib_step(JNIEnv* env, jobject obj) {
    mpv_opengl_cb_draw(mpv_gl, 0, 1000, -1000);
}
