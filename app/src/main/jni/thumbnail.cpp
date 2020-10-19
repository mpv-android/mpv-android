#include <stdlib.h>
#include <string>

#include <jni.h>
#include <android/bitmap.h>
#include <mpv/client.h>

extern "C" {
    #include <libswscale/swscale.h>
};

#include "jni_utils.h"
#include "globals.h"
#include "log.h"

extern "C" {
    jni_func(jobject, grabThumbnail, jint dimension);
};

jni_func(jobject, grabThumbnail, jint dimension) {
    ALOGV("grabbing thumbnail\n");

    mpv_node result;
    {
        mpv_node c, c_arg0, c_arg1;
        mpv_node c_args[2];
        mpv_node_list c_array;
        c_arg0.format = MPV_FORMAT_STRING;
        c_arg0.u.string = (char*) "screenshot-raw";
        c_args[0] = c_arg0;
        c_arg1.format = MPV_FORMAT_STRING;
        c_arg1.u.string = (char*) "video";
        c_args[1] = c_arg1;
        c_array.num = 2;
        c_array.values = c_args;
        c.format = MPV_FORMAT_NODE_ARRAY;
        c.u.list = &c_array;
        if (mpv_command_node(g_mpv, &c, &result) < 0)
            return NULL;
    }

    // extract relevant property data from the node map mpv returns
    int w, h, stride;
    w = h = stride = 0;
    struct mpv_byte_array *data = NULL;
    {
        if (result.format != MPV_FORMAT_NODE_MAP)
            return NULL;
        for (int i = 0; i < result.u.list->num; i++) {
            std::string key(result.u.list->keys[i]);
            const mpv_node *val = &result.u.list->values[i];
            if (key == "w" || key == "h" || key == "stride") {
                if (val->format != MPV_FORMAT_INT64)
                    return NULL;
                if (key == "w")
                    w = val->u.int64;
                else if (key == "h")
                    h = val->u.int64;
                else
                    stride = val->u.int64;
            } else if (key == "format") {
                if (val->format != MPV_FORMAT_STRING)
                    return NULL;
                // check that format equals BGR0
                if (strcmp(val->u.string, "bgr0") != 0)
                    return NULL;
            } else if (key == "data") {
                if (val->format != MPV_FORMAT_BYTE_ARRAY)
                    return NULL;
                data = val->u.ba;
            }
        }
    }
    if (!w || !h || !stride || !data) {
        mpv_free_node_contents(&result);
        return NULL;
    }
    ALOGV("screenshot w:%d h:%d stride:%d\n", w, h, stride);

    // crop to square
    int crop_left = 0, crop_top = 0;
    int new_w = w, new_h = h;
    if (w > h) {
        crop_left = (w - h) / 2;
        new_w = h;
    } else {
        crop_top = (h - w) / 2;
        new_h = w;
    }
    ALOGV("cropped w:%u h:%u\n", new_w, new_h);

    uint8_t *new_data = (uint8_t*) data->data;
    new_data += crop_left * sizeof(uint32_t); // move begin rightwards
    new_data += stride * crop_top; // move begin downwards

    // convert & scale to appropriate size
    struct SwsContext *ctx = sws_getContext(
        new_w, new_h, AV_PIX_FMT_BGR0,
        dimension, dimension, AV_PIX_FMT_RGB32,
        SWS_BICUBIC, NULL, NULL, NULL);
    if (!ctx) {
        mpv_free_node_contents(&result);
        return NULL;
    }

    jintArray arr = env->NewIntArray(dimension * dimension);
    jint *scaled = env->GetIntArrayElements(arr, NULL);

    uint8_t *src_p[4] = { new_data }, *dst_p[4] = { (uint8_t*) scaled };
    int src_stride[4] = { stride },
        dst_stride[4] = { (int) sizeof(jint) * dimension };
    sws_scale(ctx, src_p, src_stride, 0, new_h, dst_p, dst_stride);
    sws_freeContext(ctx);

    mpv_free_node_contents(&result); // frees data->data

    // create android.graphics.Bitmap
    env->ReleaseIntArrayElements(arr, scaled, 0);

    jobject bitmap_config =
        env->GetStaticObjectField(android_graphics_Bitmap_Config, android_graphics_Bitmap_Config_ARGB_8888);
    jobject bitmap =
        env->CallStaticObjectMethod(android_graphics_Bitmap, android_graphics_Bitmap_createBitmap,
        arr, dimension, dimension, bitmap_config);
    env->DeleteLocalRef(arr);
    env->DeleteLocalRef(bitmap_config);

    return bitmap;
}
