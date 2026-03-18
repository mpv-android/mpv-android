// only for this file
#define __ANDROID_UNAVAILABLE_SYMBOLS_ARE_WEAK__
#pragma GCC diagnostic error "-Wunguarded-availability"

#include <stdlib.h>
#include <string>

#include <jni.h>
#include <android/font_matcher.h>
#include <ft2build.h>
#include FT_FREETYPE_H

#include "jni_utils.h"
#include "log.h"

extern "C" {
    jni_func(jobject, findFont, jstring family);
};

static std::string get_family_from_font(const char *filename)
{
    FT_Library library;
    FT_Face face;

    if (FT_Init_FreeType(&library)) {
        ALOGE("Failed to init freetype");
        return "";
    }

    if (FT_New_Face(library, filename, 0, &face)) {
        ALOGE("Failed to read font: %s", filename);
        FT_Done_FreeType(library);
        return "";
    }

    std::string ret;
    if (face->family_name)
        ret = face->family_name;

    FT_Done_Face(face);
    FT_Done_FreeType(library);
    return ret;
}

jni_func(jobject, findFont, jstring jfamily) {
    const char *family = env->GetStringUTFChars(jfamily, NULL);
    jobject ret = NULL;

    if (__builtin_available(android 29, *)) {
        auto *matcher = AFontMatcher_create();
        const uint16_t text[] = { 'E' };
        auto *font = AFontMatcher_match(matcher, family, text, 1, NULL);
        const char *path = AFont_getFontFilePath(font);
        ret = env->NewStringUTF(get_family_from_font(path).c_str());
        AFont_close(font);
        AFontMatcher_destroy(matcher);
    }

    env->ReleaseStringUTFChars(jfamily, family);
    return ret;
}
