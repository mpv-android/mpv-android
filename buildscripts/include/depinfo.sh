#!/bin/bash -e

## Dependency versions

v_sdk=3859397
v_ndk=r21
v_sdk_build_tools=28.0.3

v_lua=5.2.4
v_fribidi=1.0.5
v_freetype=2-10-1
v_mbedtls=2.16.2


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
v_travis_ffmpeg=e0fab59624c6fa556f820b147906460fac7fb5fc

# filename used to uniquely identify a build prefix
travis_tarball="prefix-ndk-${v_ndk}-lua-${v_lua}-fribidi-${v_fribidi}-freetype-${v_freetype}-mbedtls-${v_mbedtls}-ffmpeg-${v_travis_ffmpeg}.tgz"
