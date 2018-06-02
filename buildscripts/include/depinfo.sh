#!/bin/bash -e

## Dependency versions

v_sdk=3859397
v_ndk=r17

# shaderc is provided by the NDK
v_lua=5.2.4
v_libass=0.14.0
v_fribidi=1.0.3
v_freetype=2-9-1
v_gnutls=3.6.2
v_nettle=3.4


## Dependency tree
# I would've used a dict but putting arrays in a dict is not a thing

dep_nettle=()
dep_gnutls=(nettle)
dep_ffmpeg=(gnutls)
dep_freetype2=()
dep_fribidi=()
dep_libass=(freetype2 fribidi)
dep_lua=()
dep_shaderc=()
dep_mpv=(ffmpeg libass lua shaderc)
dep_mpv_android=(mpv)


## Travis-related

# pinned ffmpeg commit used by travis-ci
v_travis_ffmpeg=53688b62ca96ad9a3b0e7d201caca61c79a68648

# filename used to uniquely identify a build prefix
travis_tarball="prefix-ndk-${v_ndk}-lua-${v_lua}-libass-${v_libass}-fribidi-${v_fribidi}-freetype-${v_freetype}-gnutls-${v_gnutls}-nettle-${v_nettle}-ffmpeg-${v_travis_ffmpeg}.tgz"
