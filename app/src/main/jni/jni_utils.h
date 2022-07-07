#pragma once

#include <jni.h>

#define jni_func_name(name) Java_is_xyz_libmpv_MPVLib_##name
#define jni_func(return_type, name, ...) JNIEXPORT return_type JNICALL jni_func_name(name) (JNIEnv *env, jobject obj, ##__VA_ARGS__)

bool acquire_jni_env(JavaVM *vm, JNIEnv **env);
void init_methods_cache(JNIEnv *env);

extern jclass java_Integer, java_Double, java_Boolean;
extern jmethodID java_Integer_init, java_Integer_intValue, java_Double_init, java_Double_doubleValue, java_Boolean_init, java_Boolean_booleanValue;
extern jmethodID java_GLSurfaceView_requestRender;

extern jclass mpv_MPVLib;
extern jmethodID mpv_MPVLib_eventProperty_S, mpv_MPVLib_eventProperty_Sb, mpv_MPVLib_eventProperty_Sl, mpv_MPVLib_eventProperty_Sd, mpv_MPVLib_eventProperty_SS, mpv_MPVLib_event, mpv_MPVLib_eventEndFile, mpv_MPVLib_logMessage_SiS;
