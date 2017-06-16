#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <locale.h>

#include <mpv/client.h>

#include <pthread.h>

extern "C" {
    #include <libavcodec/jni.h>
}

#include "log.h"
#include "jni_utils.h"

extern void android_content_init(JNIEnv *env, jobject appctx);
extern void android_content_register(mpv_handle *mpv);

#define ARRAYLEN(a) (sizeof(a)/sizeof(a[0]))

extern "C" {
    jni_func(void, create, jobject appctx);
    jni_func(void, init);
    jni_func(void, destroy);

    jni_func(void, step);

    jni_func(void, command, jobjectArray jarray);
};

JavaVM *g_vm;
mpv_handle *g_mpv;

static void prepare_environment(JNIEnv *env, jobject appctx) {
    setlocale(LC_NUMERIC, "C");

    JavaVM* vm = NULL;

    if (!env->GetJavaVM(&vm) && vm) {
        av_jni_set_java_vm(vm, NULL);
    }
    init_methods_cache(env);
    android_content_init(env, appctx);
}

jni_func(void, create, jobject appctx) {
    prepare_environment(env, appctx);

    if (g_mpv)
        die("mpv is already initialized");

    g_mpv = mpv_create();
    if (!g_mpv)
        die("context init failed");

    mpv_request_log_messages(g_mpv, "v");
}

jni_func(void, init) {
    if (!g_mpv)
        die("mpv is not created");

    if (mpv_initialize(g_mpv) < 0)
        die("mpv init failed");

    android_content_register(g_mpv);
#ifdef __aarch64__
    ALOGV("You're using the 64-bit build of mpv!");
#endif
}

jni_func(void, destroy) {
    if (!g_mpv)
        die("mpv destroy called but it's already destroyed");
    mpv_terminate_destroy(g_mpv);
    g_mpv = NULL;
}

jni_func(void, command, jobjectArray jarray) {
    const char *arguments[128] = { 0 };
    int len = env->GetArrayLength(jarray);
    if (!g_mpv)
        die("Cannot run command: mpv is not initialized");
    if (len >= ARRAYLEN(arguments))
        die("Cannot run command: too many arguments");

    for (int i = 0; i < len; ++i)
        arguments[i] = env->GetStringUTFChars((jstring)env->GetObjectArrayElement(jarray, i), NULL);

    mpv_command(g_mpv, arguments);

    for (int i = 0; i < len; ++i)
        env->ReleaseStringUTFChars((jstring)env->GetObjectArrayElement(jarray, i), arguments[i]);
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
    default:
        ALOGV("sendPropertyUpdateToJava: Unknown property update format received in callback: %d!", prop->format);
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
        mpv_event *mp_event = mpv_wait_event(g_mpv, 0);
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
