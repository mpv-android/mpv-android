#include <jni.h>

#include <mpv/client.h>

#include "globals.h"
#include "jni_utils.h"
#include "log.h"

static void sendPropertyUpdateToJava(JNIEnv *env, mpv_event_property *prop) {
    jstring jprop = env->NewStringUTF(prop->name);
    jstring jvalue = NULL;
    switch (prop->format) {
    case MPV_FORMAT_NONE:
        env->CallStaticVoidMethod(mpv_MPVLib, mpv_MPVLib_eventProperty_S, jprop);
        break;
    case MPV_FORMAT_FLAG:
        env->CallStaticVoidMethod(mpv_MPVLib, mpv_MPVLib_eventProperty_Sb, jprop, *(int*)prop->data);
        break;
    case MPV_FORMAT_INT64:
        env->CallStaticVoidMethod(mpv_MPVLib, mpv_MPVLib_eventProperty_Sl, jprop, *(int64_t*)prop->data);
        break;
    case MPV_FORMAT_STRING:
        jvalue = env->NewStringUTF(*(const char**)prop->data);
        env->CallStaticVoidMethod(mpv_MPVLib, mpv_MPVLib_eventProperty_SS, jprop, jvalue);
        break;
    default:
        ALOGV("sendPropertyUpdateToJava: Unknown property update format received in callback: %d!", prop->format);
        break;
    }
    if (jprop)
        env->DeleteLocalRef(jprop);
    if (jvalue)
        env->DeleteLocalRef(jvalue);
}

static void sendEventToJava(JNIEnv *env, int event) {
    env->CallStaticVoidMethod(mpv_MPVLib, mpv_MPVLib_event, event);
}

static void sendLogMessageToJava(JNIEnv *env, mpv_event_log_message *msg) {
    jstring jprefix = env->NewStringUTF(msg->prefix);
    jstring jtext = env->NewStringUTF(msg->text);

    env->CallStaticVoidMethod(mpv_MPVLib, mpv_MPVLib_logMessage_SiS,
        jprefix, (jint) msg->log_level, jtext);

    if (jprefix)
        env->DeleteLocalRef(jprefix);
    if (jtext)
        env->DeleteLocalRef(jtext);
}

void *event_thread(void *arg) {
    JNIEnv *env = NULL;
    acquire_jni_env(g_vm, &env);
    if (!env)
        die("failed to acquire java env");

    while (1) {
        mpv_event *mp_event;
        mpv_event_property *mp_property = NULL;
        mpv_event_log_message *msg = NULL;

        mp_event = mpv_wait_event(g_mpv, -1.0);

        if (g_event_thread_request_exit)
            break;

        if (mp_event->event_id == MPV_EVENT_NONE)
            continue;

        switch (mp_event->event_id) {
        case MPV_EVENT_LOG_MESSAGE:
            msg = (mpv_event_log_message*)mp_event->data;
            ALOGV("[%s:%s] %s", msg->prefix, msg->level, msg->text);
            sendLogMessageToJava(env, msg);
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

    g_vm->DetachCurrentThread();

    return NULL;
}
