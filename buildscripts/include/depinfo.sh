#!/bin/bash -e

## Dependency versions

v_sdk=6609375_latest
v_ndk=r23
v_sdk_build_tools=30.0.2

v_lua=5.2.4
v_harfbuzz=3.0.0
v_fribidi=1.0.11
v_freetype=2-11-0
v_mbedtls=2.27.0
v_libmysofa=1.2.1

## Dependency tree
# I would've used a dict but putting arrays in a dict is not a thing

dep_mbedtls=()
dep_dav1d=()
dep_libmysofa=()
dep_ffmpeg=(mbedtls dav1d libmysofa)
dep_freetype2=()
dep_fribidi=()
dep_harfbuzz=()
dep_libass=(freetype2 fribidi harfbuzz)
dep_lua=()
dep_mpv=(ffmpeg libass lua)
dep_mpv_android=(mpv)


## Travis-related

# pinned ffmpeg commit used by travis-ci
v_travis_ffmpeg=75001ae8440d819d23443709091fca4c39e395a1

# filename used to uniquely identify a build prefix
travis_tarball="prefix-ndk-${v_ndk}-lua-${v_lua}-harfbuzz-${v_harfbuzz}-fribidi-${v_fribidi}-freetype-${v_freetype}-mbedtls-${v_mbedtls}-libmysofa-${v_libmysofa}-ffmpeg-${v_travis_ffmpeg}.tgz"
