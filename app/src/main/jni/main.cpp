#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <locale.h>

#include <mpv/client.h>
#include <mpv/opengl_cb.h>

#include <EGL/egl.h>

extern "C" {
    #include <libavcodec/jni.h>
}

#include "main.h"

#define ARRAYLEN(a) (sizeof(a)/sizeof(a[0]))
#define jname(name) Java_is_xyz_mpv_MPVLib_##name
#define jfunc(name, type) JNIEXPORT type JNICALL jname(name)
#define jvoidfunc(name)  jfunc(name, void)

extern "C" {
    jvoidfunc(prepareEnv) (JNIEnv* env, jobject obj);
    jvoidfunc(createLibmpvContext) (JNIEnv* env, jobject obj);
    jvoidfunc(initializeLibmpv) (JNIEnv* env, jobject obj);

    jvoidfunc(initgl) (JNIEnv* env, jobject obj);

    jvoidfunc(setLibmpvOptions) (JNIEnv* env, jobject obj);

    jvoidfunc(destroy) (JNIEnv* env, jobject obj);
    jvoidfunc(destroygl) (JNIEnv* env, jobject obj);

    jvoidfunc(command) (JNIEnv* env, jobject obj, jobjectArray jarray);
    jvoidfunc(resize) (JNIEnv* env, jobject obj, jint width, jint height);
    jvoidfunc(draw) (JNIEnv* env, jobject obj);
    jvoidfunc(step) (JNIEnv* env, jobject obj);
    jvoidfunc(play) (JNIEnv *env, jobject obj);
    jvoidfunc(pause) (JNIEnv *env, jobject obj);
    jvoidfunc(touch_1down) (JNIEnv* env, jobject obj, jint x, jint y);
    jvoidfunc(touch_1move) (JNIEnv* env, jobject obj, jint x, jint y);
    jvoidfunc(touch_1up) (JNIEnv* env, jobject obj, jint x, jint y);
    jvoidfunc(setconfigdir) (JNIEnv* env, jobject obj, jstring path);

    jfunc(getpropertyint, jint) (JNIEnv *env, jobject obj, jstring property);
    jvoidfunc(setpropertyint) (JNIEnv *env, jobject obj, jstring property, jint value);
};

mpv_handle *mpv;
mpv_opengl_cb_context *mpv_gl;

int g_width, g_height;
char g_config_dir[2048];
char **g_command_queue[16] = {NULL};

static void die(const char *msg)
{
    ALOGE("%s", msg);
    exit(1);
}

static void *get_proc_address_mpv(void *fn_ctx, const char *name)
{
    return (void*)eglGetProcAddress(name);
}

jvoidfunc(prepareEnv) (JNIEnv* env, jobject obj) {
    setlocale(LC_NUMERIC, "C");

    JavaVM* vm = NULL;

    if (!env->GetJavaVM(&vm) && vm) {
        av_jni_set_java_vm(vm, NULL);
    }
}

jvoidfunc(createLibmpvContext) (JNIEnv* env, jobject obj) {
    if (mpv)
        die("Called createLibmpvContext when libmpv context already available!");

    mpv = mpv_create();
    if (!mpv)
        die("context init failed");
}

jvoidfunc(initializeLibmpv) (JNIEnv* env, jobject obj) {
    if (!mpv)
        die("Tried to call initializeLibmpv without context!");

    mpv_set_option_string(mpv, "config", "yes");
    mpv_set_option_string(mpv, "config-dir", g_config_dir);

    int osc = 1;
    mpv_set_option(mpv, "osc", MPV_FORMAT_FLAG, &osc);
    mpv_set_option_string(mpv, "script-opts", "osc-scalewindowed=1.5");

    if (mpv_initialize(mpv) < 0)
        die("mpv init failed");
}

jvoidfunc(setLibmpvOptions) (JNIEnv* env, jobject obj) {
    if (!mpv)
        die("setLibmpvOptions: mpv is not initialized");

    mpv_request_log_messages(mpv, "v");

    mpv_set_option_string(mpv, "hwdec", "mediacodec");
    // mpv_set_option_string(mpv, "demuxer", "lavf");

    // if (mpv_set_option_string(mpv, "vo", "opengl-cb:scale=spline36:cscale=spline36:dscale=mitchell:dither-depth=auto:correct-downscaling:sigmoid-upscaling:deband") < 0)
    if (mpv_set_option_string(mpv, "vo", "opengl-cb") < 0)
        die("failed to set VO");
    //if (mpv_set_option_string(mpv, "ao", "openal") < 0)
    if (mpv_set_option_string(mpv, "ao", "opensles") < 0)
        die("failed to set AO");
}

jvoidfunc(initgl) (JNIEnv* env, jobject obj) {
    int ret = -1;
    if (!mpv)
        die("initgl: mpv not initialized");
    if (mpv_gl)
        die("OpenGL ES already initialized!?");

    mpv_gl = (mpv_opengl_cb_context*)mpv_get_sub_api(mpv, MPV_SUB_API_OPENGL_CB);
    if (!mpv_gl)
        die("failed to create mpv GL API handle");

    if ((ret = mpv_opengl_cb_init_gl(mpv_gl, NULL, get_proc_address_mpv, NULL)) < 0) {
        ALOGE("mpv_opengl_cb_init_gl returned error %d", ret);
        die("failed to initialize mpv GL context");
    }
}

jvoidfunc(destroygl) (JNIEnv* env, jobject obj) {
    if (!mpv_gl)
        die("mpv_gl destroy called but it's already destroyed");
    mpv_opengl_cb_uninit_gl(mpv_gl);
    mpv_gl = NULL;
}

jvoidfunc(destroy) (JNIEnv* env, jobject obj) {
    if (!mpv)
        die("mpv destroy called but it's already destroyed");
    mpv_terminate_destroy(mpv);
    mpv = NULL;
}

#define CHKVALID() if (!mpv) return;

jvoidfunc(command) (JNIEnv* env, jobject obj, jobjectArray jarray) {
    const char *arguments[128] = { 0 };
    int len = env->GetArrayLength(jarray);
    if (!mpv)
        die("Cannot run command: mpv is not initialized");
    if (len >= ARRAYLEN(arguments))
        die("Cannot run command: too many arguments");

    for (int i = 0; i < len; ++i)
        arguments[i] = env->GetStringUTFChars((jstring)env->GetObjectArrayElement(jarray, i), NULL);

    mpv_command(mpv, arguments);

    for (int i = 0; i < len; ++i)
        env->ReleaseStringUTFChars((jstring)env->GetObjectArrayElement(jarray, i), arguments[i]);
}

static void mouse_pos(int x, int y) {
    char sx[5], sy[5];
    const char *cmd[] = {"mouse", sx, sy, NULL};
    snprintf(sx, sizeof(sx), "%d", x);
    snprintf(sy, sizeof(sy), "%d", y);
    mpv_command(mpv, cmd);
}

static void mouse_trigger(int down, int btn) {
    // "mouse" doesn't actually send keydown events so we need to do it manually
    char k[16];
    const char *cmd[] = {down?"keydown":"keyup", k, NULL};
    snprintf(k, sizeof(k), "MOUSE_BTN%d", btn);
    mpv_command(mpv, cmd);
}

jvoidfunc(touch_1down) (JNIEnv* env, jobject obj, jint x, jint y) {
    CHKVALID();
    mouse_pos(x, y);
    mouse_trigger(1, 0);
}

jvoidfunc(touch_1move) (JNIEnv* env, jobject obj, jint x, jint y) {
    CHKVALID();
    mouse_pos(x, y);
}

jvoidfunc(touch_1up) (JNIEnv* env, jobject obj, jint x, jint y) {
    CHKVALID();
    mouse_trigger(0, 0);
    // move the cursor to the top left corner where it doesn't trigger the OSC
    // FIXME: this causes the OSC to receive a mouse_btn0 up event with x and y == 0
    //        but sometimes it gets the correct coords (threading/async?)
    //mouse_pos(0, 0);
}

jvoidfunc(resize) (JNIEnv* env, jobject obj, jint width, jint height) {
    ALOGV("Resizing! width: %d=>%d, %d=>%d\n", g_width, width, g_height, height);
    g_width = width;
    g_height = height;
}

jvoidfunc(draw) (JNIEnv* env, jobject obj) {
    mpv_opengl_cb_draw(mpv_gl, 0, g_width, -g_height);
}

jvoidfunc(step) (JNIEnv* env, jobject obj) {
    while (1) {
        mpv_event *mp_event = mpv_wait_event(mpv, 0);
        mpv_event_log_message *msg;
        if (mp_event->event_id == MPV_EVENT_NONE)
            break;
        switch (mp_event->event_id) {
        case MPV_EVENT_LOG_MESSAGE:
            msg = (mpv_event_log_message*)mp_event->data;
            ALOGV("[%s:%s] %s", msg->prefix, msg->level, msg->text);
            break;
        default:
            ALOGV("event: %s\n", mpv_event_name(mp_event->event_id));
            break;
        }
    }
}

jvoidfunc(play) (JNIEnv* env, jobject obj) {
    CHKVALID();
    int paused = 0;
    mpv_set_property(mpv, "pause", MPV_FORMAT_FLAG, &paused);
}

jvoidfunc(pause) (JNIEnv* env, jobject obj) {
    CHKVALID();
    int paused = 1;
    mpv_set_property(mpv, "pause", MPV_FORMAT_FLAG, &paused);
}

jvoidfunc(setconfigdir) (JNIEnv* env, jobject obj, jstring jpath) {
    const char *path = env->GetStringUTFChars(jpath, NULL);
    strncpy(g_config_dir, path, sizeof(g_config_dir) - 1);
    env->ReleaseStringUTFChars(jpath, path);
}

jfunc(getpropertyint, jint) (JNIEnv *env, jobject obj, jstring jproperty) {
    if (!mpv)
        return 0;

    const char *prop = env->GetStringUTFChars(jproperty, NULL);
    int64_t value = 0;
    int result = mpv_get_property(mpv, prop, MPV_FORMAT_INT64, &value);
    if (result < 0)
        ALOGE("mpv_get_property(%s) returned error %s", prop, mpv_error_string(result));
    env->ReleaseStringUTFChars(jproperty, prop);
    return value;
}

jvoidfunc(setpropertyint) (JNIEnv *env, jobject obj, jstring jproperty, jint value) {
    if (!mpv)
        return;

    const char *prop = env->GetStringUTFChars(jproperty, NULL);
    int64_t value64 = value;
    int result = mpv_set_property(mpv, prop, MPV_FORMAT_INT64, &value64);
    if (result < 0)
        ALOGE("mpv_set_property(%s, %d) returned error %s", prop, value, mpv_error_string(result));
    env->ReleaseStringUTFChars(jproperty, prop);
}
