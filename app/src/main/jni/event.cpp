#include <jni.h>

#include <mpv/client.h>

#include "globals.h"
#include "jni_utils.h"
#include "log.h"
#include "node.h"

static void sendPropertyUpdateToJava(JNIEnv *env, mpv_event_property *prop)
{
    jstring jprop = env->NewStringUTF(prop->name);
    jstring jvalue = NULL;
    switch (prop->format) {
    case MPV_FORMAT_NONE:
        env->CallStaticVoidMethod(mpv_MPVLib, mpv_MPVLib_eventProperty_S, jprop);
        break;
    case MPV_FORMAT_FLAG:
        env->CallStaticVoidMethod(mpv_MPVLib, mpv_MPVLib_eventProperty_Sb, jprop,
            (jboolean) (*(int*)prop->data != 0));
        break;
    case MPV_FORMAT_INT64:
        env->CallStaticVoidMethod(mpv_MPVLib, mpv_MPVLib_eventProperty_Sl, jprop,
            (jlong) *(int64_t*)prop->data);
        break;
    case MPV_FORMAT_DOUBLE:
        env->CallStaticVoidMethod(mpv_MPVLib, mpv_MPVLib_eventProperty_Sd, jprop,
            (jdouble) *(double*)prop->data);
        break;
    case MPV_FORMAT_STRING:
        jvalue = env->NewStringUTF(*(const char**)prop->data);
        env->CallStaticVoidMethod(mpv_MPVLib, mpv_MPVLib_eventProperty_SS, jprop, jvalue);
        break;
    case MPV_FORMAT_NODE:
    case MPV_FORMAT_NODE_ARRAY:
    case MPV_FORMAT_NODE_MAP:
        {
            jobject jnode = mpv_node_to_jobject(env, (const mpv_node *) prop->data);
            if (jnode) {
                env->CallStaticVoidMethod(mpv_MPVLib, mpv_MPVLib_eventProperty_SN, jprop, jnode);
                env->DeleteLocalRef(jnode);
            }
        }
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

static void sendEventToJava(JNIEnv *env, int event, mpv_node *event_node)
{
    jobject jnode = mpv_node_to_jobject(env, event_node);
    if (jnode) {
        env->CallStaticVoidMethod(mpv_MPVLib, mpv_MPVLib_event, event, jnode);
        env->DeleteLocalRef(jnode);
    }
}

static void sendLogMessageToJava(JNIEnv *env, mpv_event_log_message *msg)
{
    // filter the most obvious cases of invalid utf-8, since Java would choke on it
    const auto invalid_utf8 = [] (unsigned char c) {
        return c == 0xc0 || c == 0xc1 || c >= 0xf5;
    };
    for (int i = 0; msg->text[i]; i++) {
        if (invalid_utf8(static_cast<unsigned char>(msg->text[i])))
            return;
    }

    jstring jprefix = env->NewStringUTF(msg->prefix);
    jstring jtext = env->NewStringUTF(msg->text);

    env->CallStaticVoidMethod(mpv_MPVLib, mpv_MPVLib_logMessage_SiS,
        jprefix, (jint) msg->log_level, jtext);

    if (jprefix)
        env->DeleteLocalRef(jprefix);
    if (jtext)
        env->DeleteLocalRef(jtext);
}

void *event_thread(void *arg)
{
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
            mpv_node event_node;
            mpv_event_to_node(&event_node, mp_event);
            sendEventToJava(env, mp_event->event_id, &event_node);
            mpv_free_node_contents(&event_node);
            break;
        }
    }

    g_vm->DetachCurrentThread();

    return NULL;
}
