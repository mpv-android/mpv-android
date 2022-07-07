#!/bin/bash -e

## Dependency versions
# Make sure to keep v_ndk and v_ndk_n in sync, the numeric version can be found in source.properties
# also remember to update path.sh

v_sdk=8512546_latest
v_ndk=r24
v_ndk_n=24.0.8215888
v_sdk_build_tools=30.0.3

v_lua=5.2.4
v_harfbuzz=4.3.0
v_fribidi=1.0.12
v_freetype=2-12-1
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

# Build libmpv/ffmpeg from master branch?
b_master=1

# If not, use the pinned commit below instead.
v_libmpv=
v_ffmpeg=

v_git_args='--depth=1'
