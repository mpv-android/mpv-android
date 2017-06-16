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
// so let's have a nice cache here
bool methods_initialized;
jclass java_Integer, java_Boolean;
jmethodID java_Integer_init, java_Integer_intValue, java_Boolean_init, java_Boolean_booleanValue;
jmethodID java_GLSurfaceView_requestRender;

void init_methods_cache(JNIEnv *env) {
    static bool methods_initialized = false;
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
    jclass java_GLSurfaceView = env->FindClass("android/opengl/GLSurfaceView");
    java_GLSurfaceView_requestRender = env->GetMethodID(java_GLSurfaceView, "requestRender", "()V");

    methods_initialized = true;
}
