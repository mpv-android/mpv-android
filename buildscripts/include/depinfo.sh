#!/bin/bash -e

## Dependency versions

v_sdk=6200805_latest
v_ndk=r21b
v_sdk_build_tools=28.0.3

v_lua=5.2.4
v_fribidi=1.0.9
v_freetype=2-10-2
v_mbedtls=2.16.6


## Dependency tree
# I would've used a dict but putting arrays in a dict is not a thing

dep_mbedtls=()
dep_ffmpeg=(mbedtls)
dep_freetype2=()
dep_fribidi=()
dep_libass=(freetype2 fribidi)
dep_lua=()
dep_mpv=(ffmpeg libass lua)
dep_mpv_android=(mpv)


## Travis-related

# pinned ffmpeg commit used by travis-ci
v_travis_ffmpeg=c7c8f141ebd95b73baebf4b5013d3c6389cbe2c6

# filename used to uniquely identify a build prefix
travis_tarball="prefix-ndk-${v_ndk}-lua-${v_lua}-fribidi-${v_fribidi}-freetype-${v_freetype}-mbedtls-${v_mbedtls}-ffmpeg-${v_travis_ffmpeg}.tgz"
