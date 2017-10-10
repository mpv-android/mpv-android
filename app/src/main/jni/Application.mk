APP_ABI := armeabi-v7a
ifneq ($(PREFIX64),)
APP_ABI += arm64-v8a
endif

APP_PLATFORM := android-21
APP_STL := c++_shared
