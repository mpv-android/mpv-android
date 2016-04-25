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
    jni_func(void, create);
    jni_func(void, init);
    jni_func(void, destroy);

    jni_func(void, initGL);
    jni_func(void, destroyGL);

    jni_func(void, command, jobjectArray jarray);

    jni_func(void, resize, jint width, jint height);
    jni_func(void, draw);
    jni_func(void, step);

    jni_func(jint, setOptionString, jstring option, jstring value);

    jni_func(jobject, getPropertyInt, jstring property);
    jni_func(void, setPropertyInt, jstring property, jobject value);
    jni_func(jobject, getPropertyBoolean, jstring property);
    jni_func(void, setPropertyBoolean, jstring property, jobject value);
    jni_func(jstring, getPropertyString, jstring jproperty);
    jni_func(void, setPropertyString, jstring jproperty, jstring jvalue);

    jni_func(void, observeProperty, jstring property, jint format);
};

mpv_handle *mpv;
mpv_opengl_cb_context *mpv_gl;
int g_width, g_height;

static void die(const char *msg)
{
    ALOGE("%s", msg);
    exit(1);
}

static void *get_proc_address_mpv(void *fn_ctx, const char *name)
{
    return (void*)eglGetProcAddress(name);
}

// Apparently it's considered slow to FindClass and GetMethodID every time we need them,
// so let's have a nice cache here
bool methods_initialized;
jclass java_Integer, java_Boolean;
jmethodID java_Integer_init, java_Integer_intValue, java_Boolean_init, java_Boolean_booleanValue;

static void init_methods_cache(JNIEnv *env) {
    if (methods_initialized)
        return;
    #define FIND_CLASS(name) reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass(name)))
    java_Integer = FIND_CLASS("java/lang/Integer");
    java_Integer_init = env->GetMethodID(java_Integer, "<init>", "(I)V");
    java_Integer_intValue = env->GetMethodID(java_Integer, "intValue", "()I");
    java_Boolean = FIND_CLASS("java/lang/Boolean");
    java_Boolean_init = env->GetMethodID(java_Boolean, "<init>", "(Z)V");
    java_Boolean_booleanValue = env->GetMethodID(java_Boolean, "booleanValue", "()Z");
    #undef FIND_CLASS
    methods_initialized = true;
}

static void prepare_environment(JNIEnv *env) {
    setlocale(LC_NUMERIC, "C");

    JavaVM* vm = NULL;

    if (!env->GetJavaVM(&vm) && vm) {
        av_jni_set_java_vm(vm, NULL);
    }
    init_methods_cache(env);
}

jni_func(void, create) {
    prepare_environment(env);

    if (mpv)
        die("mpv is already initialized");

    mpv = mpv_create();
    if (!mpv)
        die("context init failed");
}

jni_func(void, init) {
    if (!mpv)
        die("mpv is not created");

    if (mpv_initialize(mpv) < 0)
        die("mpv init failed");

    mpv_request_log_messages(mpv, "v");
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
    jstring jvalue = NULL;
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
    case MPV_FORMAT_STRING:
        mid = env->GetStaticMethodID(clazz, "eventProperty", "(Ljava/lang/String;Ljava/lang/String;)V"); // eventProperty(String, String)
        jvalue = env->NewStringUTF(*(const char**)prop->data);
        env->CallStaticVoidMethod(clazz, mid, jprop, jvalue);
        break;
    }
}

static void sendEventToJava(JNIEnv *env, int event) {
    jclass clazz = env->FindClass("is/xyz/mpv/MPVLib");
    jmethodID mid = env->GetStaticMethodID(clazz, "event", "(I)V"); // event(int)
    env->CallStaticVoidMethod(clazz, mid, event);
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
            sendEventToJava(env, mp_event->event_id);
            break;
        }
    }
}

jni_func(jint, setOptionString, jstring joption, jstring jvalue) {
    if (!mpv)
        die("mpv is not initialized");

    const char *option = env->GetStringUTFChars(joption, NULL);
    const char *value = env->GetStringUTFChars(jvalue, NULL);

    int result = mpv_set_option_string(mpv, option, value);

    env->ReleaseStringUTFChars(joption, option);
    env->ReleaseStringUTFChars(jvalue, value);

    return result;
}

static int common_get_property(JNIEnv *env, jstring jproperty, mpv_format format, void *output) {
    if (!mpv)
        die("get_property called but mpv is not initialized");

    const char *prop = env->GetStringUTFChars(jproperty, NULL);
    int result = mpv_get_property(mpv, prop, format, output);
    if (result < 0)
        ALOGE("mpv_get_property(%s) format %d returned error %s", prop, format, mpv_error_string(result));
    env->ReleaseStringUTFChars(jproperty, prop);

    return result;
}

static int common_set_property(JNIEnv *env, jstring jproperty, mpv_format format, void *value) {
    if (!mpv)
        die("set_property called but mpv is not initialized");

    const char *prop = env->GetStringUTFChars(jproperty, NULL);
    int result = mpv_set_property(mpv, prop, format, value);
    if (result < 0)
        ALOGE("mpv_set_property(%s, %p) format %d returned error %s", prop, value, format, mpv_error_string(result));
    env->ReleaseStringUTFChars(jproperty, prop);

    return result;
}

jni_func(jobject, getPropertyInt, jstring jproperty) {
    int64_t value = 0;
    if (common_get_property(env, jproperty, MPV_FORMAT_INT64, &value) < 0)
        return NULL;
    return env->NewObject(java_Integer, java_Integer_init, value);
}

jni_func(jobject, getPropertyBoolean, jstring jproperty) {
    int value = 0;
    if (common_get_property(env, jproperty, MPV_FORMAT_FLAG, &value) < 0)
        return NULL;
    return env->NewObject(java_Boolean, java_Boolean_init, value);
}

jni_func(jstring, getPropertyString, jstring jproperty) {
    char *value;
    if (common_get_property(env, jproperty, MPV_FORMAT_STRING, &value) < 0)
        return NULL;
    jstring jvalue = env->NewStringUTF(value);
    mpv_free(value);
    return jvalue;
}

jni_func(void, setPropertyInt, jstring jproperty, jobject jvalue) {
    int64_t value = env->CallIntMethod(jvalue, java_Integer_intValue);
    common_set_property(env, jproperty, MPV_FORMAT_INT64, &value);
}

jni_func(void, setPropertyBoolean, jstring jproperty, jobject jvalue) {
    int value = env->CallBooleanMethod(jvalue, java_Boolean_booleanValue);
    common_set_property(env, jproperty, MPV_FORMAT_FLAG, &value);
}

jni_func(void, setPropertyString, jstring jproperty, jstring jvalue) {
    const char *value = env->GetStringUTFChars(jvalue, NULL);
    common_set_property(env, jproperty, MPV_FORMAT_STRING, &value);
    env->ReleaseStringUTFChars(jvalue, value);
}

jni_func(void, observeProperty, jstring property, jint format) {
    if (!mpv)
        die("mpv is not initialized");
    const char *prop = env->GetStringUTFChars(property, NULL);
    mpv_observe_property(mpv, 0, prop, (mpv_format)format);
    env->ReleaseStringUTFChars(property, prop);
}
