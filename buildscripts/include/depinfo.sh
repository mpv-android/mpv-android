#!/bin/bash -e

## Dependency versions
# Make sure to keep v_ndk and v_ndk_n in sync, both are listed on the NDK download page

v_sdk=11076708_latest
v_ndk=r29
v_ndk_n=29.0.14206865
v_sdk_platform=35
v_sdk_build_tools=35.0.0

v_lua=5.2.4
v_unibreak=7.0
v_harfbuzz=14.2.1
v_fribidi=1.0.16
v_freetype=2.14.3
v_mbedtls=3.6.5
v_libxml2=2.15.3
v_fontconfig=2.18.1


## Dependency tree

dep_mbedtls=()
dep_dav1d=()
dep_libxml2=()
dep_ffmpeg=(mbedtls dav1d libxml2)
dep_freetype2=()
dep_fontconfig=(libxml2 freetype2)
dep_fribidi=()
dep_harfbuzz=()
dep_unibreak=()
dep_libass=(freetype2 fontconfig fribidi harfbuzz unibreak)
dep_lua=()
dep_libplacebo=()
dep_mpv=(ffmpeg libass lua libplacebo)
dep_mpv_android=(mpv)


## for CI workflow

# Git inputs used by CI are pinned to the revisions used for the PlayBridge AAR.
# Release archives above are already versioned by their download URLs.
v_ci_dav1d=54706fc6bc0cdecab7e9593974a4039cc038fca7
v_ci_ffmpeg=38b88335f99e76ed89ff3c93f877fdefce736c13
v_ci_freetype=0a0221a1347e2f1e07c395263540026e9a0aa7c7
v_ci_libass=f9fd3d20dff1cd84b7c74c8ae7f79711ad7736fa
v_ci_libplacebo=a7a18af88ff0a17c04840dcb3246047bb6b46df3
v_ci_mpv=94335ab87ab225ca3e36e0faeac831639d3e1d4e
v_ci_gas_preprocessor=ac1836309c2e77023c228b7184485597286289d3

# filename used to uniquely identify a build prefix
ci_tarball="prefix-v2-ndk-${v_ndk}-lua-${v_lua}-unibreak-${v_unibreak}-harfbuzz-${v_harfbuzz}-fribidi-${v_fribidi}-freetype-${v_ci_freetype:0:12}-libxml2-${v_libxml2}-fontconfig-${v_fontconfig}-mbedtls-${v_mbedtls}-dav1d-${v_ci_dav1d:0:12}-ffmpeg-${v_ci_ffmpeg:0:12}-libass-${v_ci_libass:0:12}-libplacebo-${v_ci_libplacebo:0:12}-gas-${v_ci_gas_preprocessor:0:12}.tgz"
