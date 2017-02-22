#include <jni.h>
#include <unistd.h>
#include <stdint.h>
#include <mpv/stream_cb.h>

#include "main.h"


static int android_content_open(void *user_data, char *uri, mpv_stream_cb_info *info);
static int64_t callback_read(void *cookie, char *buf, uint64_t nbytes);
static int64_t callback_seek(void *cookie, int64_t offset);
static void callback_close(void *cookie);


static bool methods_initialized = false;
static JavaVM *g_vm;
static jobject appcontext;
static jclass
    android_net_Uri;
static jmethodID
    android_net_Uri_parse, android_content_Context_getContentResolver,
    android_content_ContentResolver_openFileDescriptor,
    android_os_ParcelFileDescriptor_detachFd;

void android_content_init(JNIEnv *env, jobject appctx) {
    if (methods_initialized)
        return;
    env->GetJavaVM(&g_vm);
    appcontext = env->NewGlobalRef(appctx);

    #define FIND_CLASS(name) reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass(name)))
    android_net_Uri = FIND_CLASS("android/net/Uri");
    android_net_Uri_parse = env->GetStaticMethodID(android_net_Uri,
        "parse", "(Ljava/lang/String;)Landroid/net/Uri;");
    jclass android_content_Context = FIND_CLASS("android/content/Context");
    android_content_Context_getContentResolver = env->GetMethodID(android_content_Context,
        "getContentResolver", "()Landroid/content/ContentResolver;");
    env->DeleteGlobalRef(android_content_Context);
    jclass android_content_ContentResolver = FIND_CLASS("android/content/ContentResolver");
    android_content_ContentResolver_openFileDescriptor = env->GetMethodID(android_content_ContentResolver,
        "openFileDescriptor", "(Landroid/net/Uri;Ljava/lang/String;)Landroid/os/ParcelFileDescriptor;");
    env->DeleteGlobalRef(android_content_ContentResolver);
    jclass android_os_ParcelFileDescriptor = FIND_CLASS("android/os/ParcelFileDescriptor");
    android_os_ParcelFileDescriptor_detachFd = env->GetMethodID(android_os_ParcelFileDescriptor,
        "detachFd", "()I");
    env->DeleteGlobalRef(android_os_ParcelFileDescriptor);
    #undef FIND_CLASS

    methods_initialized = true;
}

void android_content_register(mpv_handle *mpv) {
    mpv_stream_cb_add_ro(mpv, "content", NULL, android_content_open);
}

bool acquire_java_stuff(JavaVM *vm, JNIEnv **env)
{
    int ret = vm->GetEnv((void**) env, JNI_VERSION_1_6);
    if (ret == JNI_EDETACHED)
        return vm->AttachCurrentThread(env, NULL) == 0;
    else
        return ret == JNI_OK;
}


static int android_content_open(void *user_data, char *uri, mpv_stream_cb_info *info)
{
    JNIEnv *env;
    if (!acquire_java_stuff(g_vm, &env))
        return MPV_ERROR_LOADING_FAILED;

    jstring url = env->NewStringUTF(uri);
    jstring mode = env->NewStringUTF("r");
    jobject j_uri = NULL;
    jobject content_resolver = NULL;
    jobject parcel_fd = NULL;
    int fd;

    j_uri = env->CallStaticObjectMethod(android_net_Uri, android_net_Uri_parse, url);
    if (env->ExceptionCheck())
        goto error;

    content_resolver = env->CallObjectMethod(appcontext, android_content_Context_getContentResolver);
    if (env->ExceptionCheck())
        goto error;

    parcel_fd = env->CallObjectMethod(content_resolver, android_content_ContentResolver_openFileDescriptor, j_uri, mode);
    if (env->ExceptionCheck())
        goto error;

    fd = env->CallIntMethod(parcel_fd, android_os_ParcelFileDescriptor_detachFd);
    if (env->ExceptionCheck())
        goto error;

    env->DeleteLocalRef(url);
    env->DeleteLocalRef(mode);
    env->DeleteLocalRef(j_uri);
    env->DeleteLocalRef(content_resolver);
    env->DeleteLocalRef(parcel_fd);
    g_vm->DetachCurrentThread();

    info->cookie = (void*)(intptr_t) fd; // would break if sizeof(int) > sizeof(void*)
    info->read_fn = callback_read;
    info->seek_fn = callback_seek;
    info->size_fn = NULL;
    info->close_fn = callback_close;

    return 0;

error:
    env->ExceptionClear();
    env->DeleteLocalRef(url);
    env->DeleteLocalRef(mode);
    if (j_uri)
        env->DeleteLocalRef(j_uri);
    if (content_resolver)
        env->DeleteLocalRef(content_resolver);
    if (parcel_fd)
        env->DeleteLocalRef(parcel_fd);
    g_vm->DetachCurrentThread();

    return MPV_ERROR_LOADING_FAILED;
}

static int64_t callback_read(void *cookie, char *buf, uint64_t nbytes)
{
    int fd = (int)(intptr_t) cookie;
    return read(fd, buf, nbytes);
}

static int64_t callback_seek(void *cookie, int64_t offset)
{
    int fd = (int)(intptr_t) cookie;
    off_t ret = lseek(fd, offset, SEEK_SET);
    return (ret == (off_t)-1) ? MPV_ERROR_GENERIC : ret;
}

static void callback_close(void *cookie)
{
    int fd = (int)(intptr_t) cookie;
    close(fd);
}
