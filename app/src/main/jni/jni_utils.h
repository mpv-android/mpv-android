#pragma once

#include <jni.h>

#define jni_func_name(name) Java_is_xyz_mpv_MPVLib_##name
#define jni_func(return_type, name, ...) JNIEXPORT return_type JNICALL jni_func_name(name) (JNIEnv *env, jobject obj, ##__VA_ARGS__)

bool acquire_jni_env(JavaVM *vm, JNIEnv **env);
void init_methods_cache(JNIEnv *env);

extern jclass java_Integer, java_Boolean;
extern jmethodID java_Integer_init, java_Integer_intValue, java_Boolean_init, java_Boolean_booleanValue;
extern jmethodID java_GLSurfaceView_requestRender;

extern jclass android_graphics_Bitmap, android_graphics_Bitmap_Config;
extern jmethodID android_graphics_Bitmap_createBitmap;
extern jfieldID android_graphics_Bitmap_Config_ARGB_8888;

extern jclass mpv_MPVLib;
extern jmethodID mpv_MPVLib_eventProperty_S, mpv_MPVLib_eventProperty_Sb, mpv_MPVLib_eventProperty_Sl, mpv_MPVLib_eventProperty_SS, mpv_MPVLib_event;
