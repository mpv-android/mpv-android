#include <jni.h>
#include <stdlib.h>
#include <pthread.h>

#include <mpv/client.h>
#include <mpv/opengl_cb.h>
#include <EGL/egl.h>

#include "jni_utils.h"
#include "globals.h"
#include "log.h"

extern "C" {
    jni_func(void, attachSurface, jobject surface_);
    jni_func(void, detachSurface);
};

static jobject surface;

jni_func(void, attachSurface, jobject surface_) {
    surface = env->NewGlobalRef(surface_);
    int64_t wid = (int64_t)(intptr_t) surface;
    mpv_set_option(g_mpv, "wid", MPV_FORMAT_INT64, (void*) &wid);
}

jni_func(void, detachSurface) {
    int64_t wid = 0;
    mpv_set_option(g_mpv, "wid", MPV_FORMAT_INT64, (void*) &wid);

    env->DeleteGlobalRef(surface);
    surface = NULL;
}
