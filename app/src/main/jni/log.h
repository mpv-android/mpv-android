#pragma once

#include <android/log.h>

#define DEBUG 1

#define LOG_TAG "mpv"
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#if DEBUG
#define ALOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#else
#define ALOGV(...) (void)0
#endif

__attribute__((noreturn)) void die(const char *msg);

#define CHECK_MPV_INIT() do { \
	if (__builtin_expect(!g_mpv, 0)) \
        die("libmpv is not initialized"); \
	} while (0)
