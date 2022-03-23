#!/bin/bash -e

## Dependency versions

v_sdk=6609375_latest
v_ndk=r24
v_sdk_build_tools=30.0.3

v_lua=5.2.4
v_harfbuzz=4.0.1
v_fribidi=1.0.11
v_freetype=2-11-1
v_mbedtls=2.28.0


## Dependency tree
# I would've used a dict but putting arrays in a dict is not a thing

dep_mbedtls=()
dep_dav1d=()
dep_ffmpeg=(mbedtls dav1d)
dep_freetype2=()
dep_fribidi=()
dep_harfbuzz=()
dep_libass=(freetype2 fribidi harfbuzz)
dep_lua=()
dep_mpv=(ffmpeg libass lua)
dep_mpv_android=(mpv)


## Travis-related

# pinned ffmpeg commit used by travis-ci
v_travis_ffmpeg=37480b1b85b0405563962b581dc2899b1b4bec59

# filename used to uniquely identify a build prefix
travis_tarball="prefix-ndk-${v_ndk}-lua-${v_lua}-harfbuzz-${v_harfbuzz}-fribidi-${v_fribidi}-freetype-${v_freetype}-mbedtls-${v_mbedtls}-ffmpeg-${v_travis_ffmpeg}.tgz"
