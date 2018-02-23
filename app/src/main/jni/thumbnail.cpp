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
    jni_func(jobject, grabThumbnail);
};

// jobject STATUS_ONE = env->GetStaticObjectField(clSTATUS, fidONE);

jni_func(jobject, grabThumbnail) {
    ALOGV("Grabbing thumbnail...\n");
    
    mpv_node result;
    {
        mpv_node c, c_arg0, c_arg1;
        mpv_node c_args[2];
        mpv_node_list c_array;
        c_arg0.format = MPV_FORMAT_STRING;
        c_arg0.u.string = (char*)"screenshot-raw";
        c_args[0] = c_arg0;
        c_arg1.format = MPV_FORMAT_STRING;
        c_arg1.u.string = (char*)"video";
        c_args[1] = c_arg1;
        c_array.num = 2;
        c_array.values = c_args;
        c.format = MPV_FORMAT_NODE_ARRAY;
        c.u.list = &c_array;
        if (mpv_command_node(g_mpv, &c, &result) < 0)
            return NULL;
    }

    unsigned w, h, stride;
    w = h = stride = 0;
    struct mpv_byte_array *data = NULL;
    {
        if (result.format != MPV_FORMAT_NODE_MAP)
            return NULL;
        for (int i = 0; i < result.u.list->num; i++) {
            const char *key = result.u.list->keys[i];
            const mpv_node *val = &result.u.list->values[i];
            if (!strcmp(key, "w") || !strcmp(key, "h") || !strcmp(key, "stride")) {
                if (val->format != MPV_FORMAT_INT64)
                    return NULL;
                if (!strcmp(key, "w"))
                    w = val->u.int64;
                else if (!strcmp(key, "h"))
                    h = val->u.int64;
                else
                    stride = val->u.int64;
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

    if (!w || !h || !stride || !data){ALOGE("miss\n");
        return NULL;}
    ALOGV("w:%u h:%u stride:%u\n", w, h, stride);

    // determine cropping parameters (square)
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
    ALOGV("new_w:%u new_h:%u\n", new_w, new_h);
    
    // copy to new buffer while applying crop
    uint32_t *new_data = new uint32_t[new_w * new_h];
    for (int y = 0; y < new_h; y++)
        for (int x = 0; x < new_w; x++) {
            int tx = x + crop_left, ty = y + crop_top;
            new_data[y*new_w + x] = ((uint32_t*) data->data)[ty*w + tx];
        }
    mpv_free_node_contents(&result);

    // convert & scale to appropriate size
    const int dimension = 128;
    struct SwsContext *ctx = sws_getContext(
        new_w, new_h, AV_PIX_FMT_BGR0,
        dimension, dimension, AV_PIX_FMT_ARGB,
        SWS_BICUBIC, NULL, NULL, NULL);
    if (!ctx)
        return NULL;
    uint8_t *srcSlice[] = { (uint8_t*) new_data };
    int stride_[] = { sizeof(uint32_t) };
    uint8_t *dstSlice[1];
    dstSlice[0] = new uint8_t[new_w * new_h * stride_[0]];
    if (sws_scale(ctx, srcSlice, stride_, 0, new_h, dstSlice, stride_) <= 0) {ALOGE("swscale err\n");
        return NULL;}
    delete srcSlice[0];
    sws_freeContext(ctx);

    // create android.graphics.Bitmap
    jintArray arr = env->NewIntArray(new_w * new_h);
    int assumption[sizeof(jint) == sizeof(uint32_t) ? 0 : -1];
    env->SetIntArrayRegion(arr, 0, new_w * new_h, (jint*) dstSlice[0]);
    delete dstSlice[0];
    jobject bitmap_config = env->GetStaticObjectField(android_graphics_Bitmap_Config, android_graphics_Bitmap_Config_ARGB_8888);
    jobject bitmap = env->CallStaticObjectMethod(android_graphics_Bitmap, android_graphics_Bitmap_createBitmap, arr, new_w, new_h, bitmap_config);
    env->DeleteLocalRef(arr);
    env->DeleteLocalRef(bitmap_config);

    return bitmap;
}
