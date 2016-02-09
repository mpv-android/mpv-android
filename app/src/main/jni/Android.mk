LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libmpv
LOCAL_SRC_FILES := $(PREFIX)/lib/libmpv.so
LOCAL_EXPORT_C_INCLUDES := $(PREFIX)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := libplayer
LOCAL_CFLAGS    := -Werror
LOCAL_SRC_FILES := main.cpp
LOCAL_LDLIBS    := -llog -lGLESv3 -lEGL
LOCAL_SHARED_LIBRARIES := mpv

include $(BUILD_SHARED_LIBRARY)
