#include <jni.h>
#include <stdlib.h>

#include <mpv/client.h>

#include "jni_utils.h"
#include "log.h"
#include "globals.h"
#include "node.h"

extern "C" {
    jni_func(jint, setOptionString, jstring option, jstring value);

    jni_func(jobject, getPropertyInt, jstring property);
    jni_func(void, setPropertyInt, jstring property, jint value);
    jni_func(jobject, getPropertyDouble, jstring property);
    jni_func(void, setPropertyDouble, jstring property, jdouble value);
    jni_func(jobject, getPropertyBoolean, jstring property);
    jni_func(void, setPropertyBoolean, jstring property, jboolean value);
    jni_func(jstring, getPropertyString, jstring jproperty);
    jni_func(void, setPropertyString, jstring jproperty, jstring jvalue);
    jni_func(jobject, getPropertyNode, jstring jproperty);
    jni_func(void, setPropertyNode, jstring jproperty, jobject jnode);

    jni_func(void, observeProperty, jstring property, jint format);
}

jni_func(jint, setOptionString, jstring joption, jstring jvalue) {
    CHECK_MPV_INIT();

    const char *option = env->GetStringUTFChars(joption, NULL);
    const char *value = env->GetStringUTFChars(jvalue, NULL);

    int result = mpv_set_option_string(g_mpv, option, value);

    env->ReleaseStringUTFChars(joption, option);
    env->ReleaseStringUTFChars(jvalue, value);

    return result;
}

static int common_get_property(JNIEnv *env, jstring jproperty, mpv_format format, void *output)
{
    CHECK_MPV_INIT();

    const char *prop = env->GetStringUTFChars(jproperty, NULL);
    int result = mpv_get_property(g_mpv, prop, format, output);
    if (result == MPV_ERROR_PROPERTY_UNAVAILABLE)
        ALOGV("mpv_get_property(%s) format %d was unavailable", prop, format);
    else if (result < 0)
        ALOGE("mpv_get_property(%s) format %d returned error %s", prop, format, mpv_error_string(result));
    env->ReleaseStringUTFChars(jproperty, prop);

    return result;
}

static int common_set_property(JNIEnv *env, jstring jproperty, mpv_format format, void *value)
{
    CHECK_MPV_INIT();

    const char *prop = env->GetStringUTFChars(jproperty, NULL);
    int result = mpv_set_property(g_mpv, prop, format, value);
    if (result < 0)
        ALOGE("mpv_set_property(%s, %p) format %d returned error %s", prop, value, format, mpv_error_string(result));
    env->ReleaseStringUTFChars(jproperty, prop);

    return result;
}

jni_func(jobject, getPropertyInt, jstring jproperty) {
    int64_t value = 0;
    if (common_get_property(env, jproperty, MPV_FORMAT_INT64, &value) < 0)
        return NULL;
    return env->NewObject(java_Integer, java_Integer_init, (jint)value);
}

jni_func(jobject, getPropertyDouble, jstring jproperty) {
    double value = 0;
    if (common_get_property(env, jproperty, MPV_FORMAT_DOUBLE, &value) < 0)
        return NULL;
    return env->NewObject(java_Double, java_Double_init, (jdouble)value);
}

jni_func(jobject, getPropertyBoolean, jstring jproperty) {
    int value = 0;
    if (common_get_property(env, jproperty, MPV_FORMAT_FLAG, &value) < 0)
        return NULL;
    return env->NewObject(java_Boolean, java_Boolean_init, (jboolean)value);
}

jni_func(jstring, getPropertyString, jstring jproperty) {
    char *value;
    if (common_get_property(env, jproperty, MPV_FORMAT_STRING, &value) < 0)
        return NULL;
    jstring jvalue = env->NewStringUTF(value);
    mpv_free(value);
    return jvalue;
}

jni_func(void, setPropertyInt, jstring jproperty, jint jvalue) {
    int64_t value = static_cast<int64_t>(jvalue);
    common_set_property(env, jproperty, MPV_FORMAT_INT64, &value);
}

jni_func(void, setPropertyDouble, jstring jproperty, jdouble jvalue) {
    double value = static_cast<double>(jvalue);
    common_set_property(env, jproperty, MPV_FORMAT_DOUBLE, &value);
}

jni_func(void, setPropertyBoolean, jstring jproperty, jboolean jvalue) {
    int value = jvalue == JNI_TRUE ? 1 : 0;
    common_set_property(env, jproperty, MPV_FORMAT_FLAG, &value);
}

jni_func(void, setPropertyString, jstring jproperty, jstring jvalue) {
    const char *value = env->GetStringUTFChars(jvalue, NULL);
    common_set_property(env, jproperty, MPV_FORMAT_STRING, &value);
    env->ReleaseStringUTFChars(jvalue, value);
}

jni_func(jobject, getPropertyNode, jstring jproperty) {
    CHECK_MPV_INIT();

    const char *property = env->GetStringUTFChars(jproperty, NULL);

    mpv_node result;
    int error = mpv_get_property(g_mpv, property, MPV_FORMAT_NODE, &result);

    env->ReleaseStringUTFChars(jproperty, property);

    if (error < 0) return NULL;

    jobject jresult = mpv_node_to_jobject(env, &result);
    mpv_free_node_contents(&result);

    return jresult;
}

jni_func(void, setPropertyNode, jstring jproperty, jobject jnode) {
    CHECK_MPV_INIT();

    const char *property = env->GetStringUTFChars(jproperty, NULL);

    mpv_node node;
    memset(&node, 0, sizeof(node));
    int parse_error = jobject_to_mpv_node(env, jnode, &node);

    if (parse_error == 0) {
        int result = mpv_set_property(g_mpv, property, MPV_FORMAT_NODE, &node);
        free_mpv_node(&node);

        if (result < 0)
            ALOGE("mpv_set_property(%s) returned error %s", property, mpv_error_string(result));
    }

    env->ReleaseStringUTFChars(jproperty, property);
}

jni_func(void, observeProperty, jstring property, jint format) {
    CHECK_MPV_INIT();
    const char *prop = env->GetStringUTFChars(property, NULL);
    int result = mpv_observe_property(g_mpv, 0, prop, (mpv_format)format);
    if (result < 0)
        ALOGE("mpv_observe_property(%s) format %d returned error %s", prop, format, mpv_error_string(result));
    env->ReleaseStringUTFChars(property, prop);
}
