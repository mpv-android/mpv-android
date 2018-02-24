#include <stdlib.h>
#include <vector>
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
    unsigned w, h;
    w = h = 0;
    struct mpv_byte_array *data = NULL;
    {
        if (result.format != MPV_FORMAT_NODE_MAP)
            return NULL;
        for (int i = 0; i < result.u.list->num; i++) {
            std::string key(result.u.list->keys[i]);
            const mpv_node *val = &result.u.list->values[i];
            if (key == "w" || key == "h") {
                if (val->format != MPV_FORMAT_INT64)
                    return NULL;
                if (key == "w")
                    w = val->u.int64;
                else
                    h = val->u.int64;
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
    if (!w || !h || !data)
        return NULL;
    ALOGV("screenshot w:%u h:%u\n", w, h);

    // crop to square
    unsigned crop_left = 0, crop_top = 0;
    unsigned new_w = w, new_h = h;
    if (w > h) {
        crop_left = (w - h) / 2;
        new_w = h;
    } else {
        crop_top = (h - w) / 2;
        new_h = w;
    }
    ALOGV("cropped w:%u h:%u\n", new_w, new_h);

    std::vector<uint32_t> new_data(new_w * new_h);
    for (int y = 0; y < new_h; y++) {
        int ty = y + crop_top;
        memcpy(&new_data[y*new_w],
            &((uint32_t*) data->data)[ty*w + crop_left],
            new_w * sizeof(uint32_t));
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
    std::vector<uint8_t> scaled(dimension * dst_stride);
    uint8_t *src_p[4] = { (uint8_t*) new_data.data() }, *dst_p[4] = { scaled.data() };
    sws_scale(ctx, src_p, &src_stride, 0, new_h, dst_p, &dst_stride);
    sws_freeContext(ctx);


    // create android.graphics.Bitmap
    jintArray arr = env->NewIntArray(dimension * dimension);
    env->SetIntArrayRegion(arr, 0, dimension * dimension, (jint*) scaled.data());

    jobject bitmap_config =
        env->GetStaticObjectField(android_graphics_Bitmap_Config, android_graphics_Bitmap_Config_ARGB_8888);
    jobject bitmap =
        env->CallStaticObjectMethod(android_graphics_Bitmap, android_graphics_Bitmap_createBitmap,
        arr, dimension, dimension, bitmap_config);
    env->DeleteLocalRef(arr);
    env->DeleteLocalRef(bitmap_config);

    return bitmap;
}
