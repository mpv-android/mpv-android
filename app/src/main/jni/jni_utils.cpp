#define UTIL_EXTERN
#include "jni_utils.h"

#include <jni.h>
#include <stdlib.h>

bool acquire_jni_env(JavaVM *vm, JNIEnv **env)
{
    int ret = vm->GetEnv((void**) env, JNI_VERSION_1_6);
    if (ret == JNI_EDETACHED)
        return vm->AttachCurrentThread(env, NULL) == 0;
    else
        return ret == JNI_OK;
}

// Apparently it's considered slow to FindClass and GetMethodID every time we need them,
// so let's have a nice cache here.

void init_methods_cache(JNIEnv *env)
{
    static bool methods_initialized = false;
    if (methods_initialized)
        return;

    #define FIND_CLASS(name) reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass(name)))
    java_Integer = FIND_CLASS("java/lang/Integer");
    java_Integer_init = env->GetMethodID(java_Integer, "<init>", "(I)V");
    java_Double = FIND_CLASS("java/lang/Double");
    java_Double_init = env->GetMethodID(java_Double, "<init>", "(D)V");
    java_Boolean = FIND_CLASS("java/lang/Boolean");
    java_Boolean_init = env->GetMethodID(java_Boolean, "<init>", "(Z)V");

    android_graphics_Bitmap = FIND_CLASS("android/graphics/Bitmap");
    // createBitmap(int[], int, int, android.graphics.Bitmap$Config)
    android_graphics_Bitmap_createBitmap = env->GetStaticMethodID(android_graphics_Bitmap, "createBitmap", "([IIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    android_graphics_Bitmap_Config = FIND_CLASS("android/graphics/Bitmap$Config");
    // static final android.graphics.Bitmap$Config ARGB_8888
    android_graphics_Bitmap_Config_ARGB_8888 = env->GetStaticFieldID(android_graphics_Bitmap_Config, "ARGB_8888", "Landroid/graphics/Bitmap$Config;");

    mpv_MPVLib = FIND_CLASS("is/xyz/mpv/MPVLib");
    mpv_MPVLib_eventProperty_S  = env->GetStaticMethodID(mpv_MPVLib, "eventProperty", "(Ljava/lang/String;)V"); // eventProperty(String)
    mpv_MPVLib_eventProperty_Sb = env->GetStaticMethodID(mpv_MPVLib, "eventProperty", "(Ljava/lang/String;Z)V"); // eventProperty(String, boolean)
    mpv_MPVLib_eventProperty_Sl = env->GetStaticMethodID(mpv_MPVLib, "eventProperty", "(Ljava/lang/String;J)V"); // eventProperty(String, long)
    mpv_MPVLib_eventProperty_Sd = env->GetStaticMethodID(mpv_MPVLib, "eventProperty", "(Ljava/lang/String;D)V"); // eventProperty(String, double)
    mpv_MPVLib_eventProperty_SS = env->GetStaticMethodID(mpv_MPVLib, "eventProperty", "(Ljava/lang/String;Ljava/lang/String;)V"); // eventProperty(String, String)
    mpv_MPVLib_eventProperty_SN = env->GetStaticMethodID(mpv_MPVLib, "eventProperty", "(Ljava/lang/String;Lis/xyz/mpv/MPVNode;)V"); // eventProperty(String, MPVNode)
    mpv_MPVLib_event = env->GetStaticMethodID(mpv_MPVLib, "event", "(ILis/xyz/mpv/MPVNode;)V"); // event(int, MPVNode)
    mpv_MPVLib_logMessage_SiS = env->GetStaticMethodID(mpv_MPVLib, "logMessage", "(Ljava/lang/String;ILjava/lang/String;)V"); // logMessage(String, int, String)

    // for array node creation, tbh, it might be better to use "List" instead but i wanted consitent naming
    mpv_MPVNode = FIND_CLASS("is/xyz/mpv/MPVNode");

    mpv_MPVNode_None = FIND_CLASS("is/xyz/mpv/MPVNode$None");
    mpv_MPVNode_None_INSTANCE = env->GetStaticFieldID(mpv_MPVNode_None, "INSTANCE", "Lis/xyz/mpv/MPVNode$None;");

    mpv_MPVNode_StringNode = FIND_CLASS("is/xyz/mpv/MPVNode$StringNode");
    mpv_MPVNode_StringNode_init = env->GetMethodID(mpv_MPVNode_StringNode, "<init>", "(Ljava/lang/String;)V");

    mpv_MPVNode_BooleanNode = FIND_CLASS("is/xyz/mpv/MPVNode$BooleanNode");
    mpv_MPVNode_BooleanNode_init = env->GetMethodID(mpv_MPVNode_BooleanNode, "<init>", "(Z)V");

    mpv_MPVNode_IntNode = FIND_CLASS("is/xyz/mpv/MPVNode$IntNode");
    mpv_MPVNode_IntNode_init = env->GetMethodID(mpv_MPVNode_IntNode, "<init>", "(J)V");

    mpv_MPVNode_DoubleNode = FIND_CLASS("is/xyz/mpv/MPVNode$DoubleNode");
    mpv_MPVNode_DoubleNode_init = env->GetMethodID(mpv_MPVNode_DoubleNode, "<init>", "(D)V");

    mpv_MPVNode_ArrayNode = FIND_CLASS("is/xyz/mpv/MPVNode$ArrayNode");
    mpv_MPVNode_ArrayNode_init = env->GetMethodID(mpv_MPVNode_ArrayNode, "<init>", "([Lis/xyz/mpv/MPVNode;)V");

    mpv_MPVNode_MapNode = FIND_CLASS("is/xyz/mpv/MPVNode$MapNode");
    mpv_MPVNode_MapNode_init = env->GetMethodID(mpv_MPVNode_MapNode, "<init>", "(Ljava/util/Map;)V");

    java_util_ArrayList = FIND_CLASS("java/util/ArrayList");
    java_util_ArrayList_init = env->GetMethodID(java_util_ArrayList, "<init>", "()V");
    java_util_ArrayList_add = env->GetMethodID(java_util_ArrayList, "add", "(Ljava/lang/Object;)Z");

    java_util_HashMap = FIND_CLASS("java/util/HashMap");
    java_util_HashMap_init = env->GetMethodID(java_util_HashMap, "<init>", "()V");
    java_util_HashMap_put = env->GetMethodID(java_util_HashMap, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

    #undef FIND_CLASS

    methods_initialized = true;
}
