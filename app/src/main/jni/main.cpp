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
#define jni_func_name(name) Java_is_xyz_mpv_MPVLib_##name
#define jni_func(return_type, name, ...) JNIEXPORT return_type JNICALL jni_func_name(name) (JNIEnv *env, jobject obj, ##__VA_ARGS__)

extern "C" {
    jni_func(void, init, jstring config_path);
    jni_func(void, destroy);

    jni_func(void, initGL);
    jni_func(void, destroyGL);

    jni_func(void, command, jobjectArray jarray);

    jni_func(void, resize, jint width, jint height);
    jni_func(void, draw);
    jni_func(void, step);

    jni_func(jint, getPropertyInt, jstring property);
    jni_func(void, setPropertyInt, jstring property, jint value);
    jni_func(jboolean, getPropertyBoolean, jstring property);
    jni_func(void, setPropertyBoolean, jstring property, jboolean value);

    jni_func(void, observeProperty, jstring property, jint format);
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

static void prepare_environment(JNIEnv *env) {
    setlocale(LC_NUMERIC, "C");

    JavaVM* vm = NULL;

    if (!env->GetJavaVM(&vm) && vm) {
        av_jni_set_java_vm(vm, NULL);
    }
}

static void initialize_libmpv(const char *config_path) {
    if (mpv)
        die("mpv is already initialized");

    mpv = mpv_create();
    if (!mpv)
        die("context init failed");

    mpv_set_option_string(mpv, "config", "yes");
    mpv_set_option_string(mpv, "config-dir", config_path);

    if (mpv_initialize(mpv) < 0)
        die("mpv init failed");
}

jni_func(void, init, jstring config_path) {
    prepare_environment(env);

    const char *path = env->GetStringUTFChars(config_path, NULL);
    initialize_libmpv(path);
    env->ReleaseStringUTFChars(config_path, path);

    mpv_request_log_messages(mpv, "v");

    mpv_set_option_string(mpv, "hwdec", "mediacodec");
    // mpv_set_option_string(mpv, "demuxer", "lavf");

    // if (mpv_set_option_string(mpv, "vo", "opengl-cb:scale=spline36:cscale=spline36:dscale=mitchell:dither-depth=auto:correct-downscaling:sigmoid-upscaling:deband") < 0)
    if (mpv_set_option_string(mpv, "vo", "opengl-cb") < 0)
        die("failed to set VO");

    if (mpv_set_option_string(mpv, "ao", "opensles") < 0)
        die("failed to set AO");
}

jni_func(void, destroy) {
    if (!mpv)
        die("mpv destroy called but it's already destroyed");
    mpv_terminate_destroy(mpv);
    mpv = NULL;
}

jni_func(void, initGL) {
    int ret = -1;
    if (!mpv)
        die("initGL: mpv not initialized");
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

jni_func(void, destroyGL) {
    if (!mpv_gl)
        die("mpv_gl destroy called but it's already destroyed");
    mpv_opengl_cb_uninit_gl(mpv_gl);
    mpv_gl = NULL;
}

jni_func(void, command, jobjectArray jarray) {
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

jni_func(void, resize, jint width, jint height) {
    ALOGV("Resizing! width: %d=>%d, %d=>%d\n", g_width, width, g_height, height);
    g_width = width;
    g_height = height;
}

jni_func(void, draw) {
    mpv_opengl_cb_draw(mpv_gl, 0, g_width, -g_height);
}

void sendPropertyUpdateToJava(JNIEnv *env, mpv_event_property *prop) {
    jmethodID mid;
    jstring jprop = env->NewStringUTF(prop->name);
    jclass clazz = env->FindClass("is/xyz/mpv/MPVLib");
    switch (prop->format) {
    case MPV_FORMAT_NONE:
        mid = env->GetStaticMethodID(clazz, "eventProperty", "(Ljava/lang/String;)V"); // eventProperty(String)
        env->CallStaticVoidMethod(clazz, mid, jprop);
        break;
    case MPV_FORMAT_FLAG:
        mid = env->GetStaticMethodID(clazz, "eventProperty", "(Ljava/lang/String;Z)V"); // eventProperty(String, boolean)
        env->CallStaticVoidMethod(clazz, mid, jprop, *(int*)prop->data);
        break;
    case MPV_FORMAT_INT64:
        mid = env->GetStaticMethodID(clazz, "eventProperty", "(Ljava/lang/String;J)V"); // eventProperty(String, long)
        env->CallStaticVoidMethod(clazz, mid, jprop, *(int64_t*)prop->data);
        break;
    }
}

jni_func(void, step) {
    while (1) {
        mpv_event *mp_event = mpv_wait_event(mpv, 0);
        mpv_event_property *mp_property = NULL;
        mpv_event_log_message *msg = NULL;
        if (mp_event->event_id == MPV_EVENT_NONE)
            break;
        switch (mp_event->event_id) {
        case MPV_EVENT_LOG_MESSAGE:
            msg = (mpv_event_log_message*)mp_event->data;
            ALOGV("[%s:%s] %s", msg->prefix, msg->level, msg->text);
            break;
        case MPV_EVENT_PROPERTY_CHANGE:
            mp_property = (mpv_event_property*)mp_event->data;
            sendPropertyUpdateToJava(env, mp_property);
            break;
        default:
            ALOGV("event: %s\n", mpv_event_name(mp_event->event_id));
            break;
        }
    }
}

static void common_get_property(JNIEnv *env, jstring jproperty, mpv_format format, void *output) {
    if (!mpv)
        die("get_property called but mpv is not initialized");

    const char *prop = env->GetStringUTFChars(jproperty, NULL);
    int result = mpv_get_property(mpv, prop, format, output);
    if (result < 0)
        ALOGE("mpv_get_property(%s) format %d returned error %s", prop, format, mpv_error_string(result));
    env->ReleaseStringUTFChars(jproperty, prop);
}

static void common_set_property(JNIEnv *env, jstring jproperty, mpv_format format, void *value) {
    if (!mpv)
        return;

    const char *prop = env->GetStringUTFChars(jproperty, NULL);
    int result = mpv_set_property(mpv, prop, format, value);
    if (result < 0)
        ALOGE("mpv_set_property(%s, %p) format %d returned error %s", prop, value, format, mpv_error_string(result));
    env->ReleaseStringUTFChars(jproperty, prop);
}

jni_func(jint, getPropertyInt, jstring jproperty) {
    int64_t value = 0;
    common_get_property(env, jproperty, MPV_FORMAT_INT64, &value);
    return value;
}

jni_func(jboolean, getPropertyBoolean, jstring jproperty) {
    int value = 0;
    common_get_property(env, jproperty, MPV_FORMAT_FLAG, &value);
    return value;
}

jni_func(void, setPropertyInt, jstring jproperty, jint jvalue) {
    int64_t value = jvalue;
    common_set_property(env, jproperty, MPV_FORMAT_INT64, &value);
}

jni_func(void, setPropertyBoolean, jstring jproperty, jboolean jvalue) {
    int value = jvalue;
    common_set_property(env, jproperty, MPV_FORMAT_FLAG, &value);
}

jni_func(void, observeProperty, jstring property, jint format) {
    if (!mpv)
        die("mpv is not initialized");
    const char *prop = env->GetStringUTFChars(property, NULL);
    mpv_observe_property(mpv, 0, prop, (mpv_format)format);
    env->ReleaseStringUTFChars(property, prop);
}
