#include <stdlib.h>
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

    unsigned w, h;
    w = h = 0;
    struct mpv_byte_array *data = NULL;
    {
        if (result.format != MPV_FORMAT_NODE_MAP)
            return NULL;
        for (int i = 0; i < result.u.list->num; i++) {
            const char *key = result.u.list->keys[i];
            const mpv_node *val = &result.u.list->values[i];
            if (!strcmp(key, "w") || !strcmp(key, "h")) {
                if (val->format != MPV_FORMAT_INT64)
                    return NULL;
                if (!strcmp(key, "w"))
                    w = val->u.int64;
                else
                    h = val->u.int64;
            } else if (!strcmp(key, "format")) {
                if (val->format != MPV_FORMAT_STRING)
                    return NULL;
                if (strcmp(val->u.string, "bgr0"))
                    return NULL;
            } else if (!strcmp(key, "data")) {
                if (val->format != MPV_FORMAT_BYTE_ARRAY)
                    return NULL;
                data = val->u.ba;
            }
        }
    }
    if (!w || !h || !data)
        return NULL;
    ALOGV("screenshot w:%u h:%u\n", w, h);

    // crop to square
    unsigned crop_left, crop_right, crop_top, crop_bottom;
    if (w > h) {
        crop_top = crop_bottom = 0;
        int tmp = w - h;
        crop_left = tmp / 2;
        crop_right = tmp / 2 + tmp % 2;
    } else {
        crop_left = crop_right = 0;
        int tmp = h - w;
        crop_top = tmp / 2;
        crop_bottom = tmp / 2 + tmp % 2;
    }
    unsigned new_w, new_h;
    new_w = w - crop_left - crop_right;
    new_h = h - crop_top - crop_bottom;
    ALOGV("cropped w:%u h:%u\n", new_w, new_h);
    uint32_t *new_data = new uint32_t[new_w * new_h];
    for (int y = 0; y < new_h; y++) {
        for (int x = 0; x < new_w; x++) {
            int tx = x + crop_left, ty = y + crop_top;
            new_data[y*new_w + x] = ((uint32_t*) data->data)[ty*w + tx];
        }
    }
    mpv_free_node_contents(&result); // frees data->data

    // convert & scale to appropriate size
    struct SwsContext *ctx = sws_getContext(
        new_w, new_h, AV_PIX_FMT_BGR0,
        dimension, dimension, AV_PIX_FMT_RGB32,
        SWS_BICUBIC, NULL, NULL, NULL);
    if (!ctx)
        return NULL;
    int src_stride = sizeof(uint32_t) * new_w, dst_stride = sizeof(uint32_t) * dimension;
    uint8_t *scaled = new uint8_t[dimension * dst_stride];
    sws_scale(ctx, (uint8_t**) &new_data, &src_stride, 0, new_h, &scaled, &dst_stride);
    delete[] new_data;
    sws_freeContext(ctx);


    // create android.graphics.Bitmap
    jintArray arr = env->NewIntArray(dimension * dimension);
    env->SetIntArrayRegion(arr, 0, dimension * dimension, (jint*) scaled);
    delete[] scaled;

    jobject bitmap_config =
        env->GetStaticObjectField(android_graphics_Bitmap_Config, android_graphics_Bitmap_Config_ARGB_8888);
    jobject bitmap =
        env->CallStaticObjectMethod(android_graphics_Bitmap, android_graphics_Bitmap_createBitmap,
        arr, dimension, dimension, bitmap_config);
    env->DeleteLocalRef(arr);
    env->DeleteLocalRef(bitmap_config);

    return bitmap;
}
