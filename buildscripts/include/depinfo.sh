#!/bin/bash -e

## Dependency versions
# Make sure to keep v_ndk and v_ndk_n in sync, both are listed on the NDK download page

v_sdk=11076708_latest
v_ndk=r30
v_ndk_n=30.0.16248370
v_sdk_platform=36
v_sdk_build_tools=36.0.0

v_lua=5.2.4
v_unibreak=8.0
v_harfbuzz=14.6.0
v_fribidi=1.0.17
v_freetype=2.15.0
v_mbedtls=3.6.7
v_libxml2=2.15.4
v_fontconfig=2.18.3
v_curl=8.23.0

## FFmpeg source
# mpv-android builds FFmpeg-AVSPlus instead of upstream FFmpeg. It is a fork
# that carries full AVS support: AVS1-P2/P16 (native, "cavs"), AVS2-P2 via
# libdavs2 and AVS3-P2 via libuavs3d.
# Set these back to https://github.com/FFmpeg/FFmpeg + a release tag (and drop
# uavs3d/davs2 from dep_ffmpeg below) to return to stock FFmpeg.
v_ffmpeg_repo=https://github.com/xeonliu/FFmpeg-AVSPlus
v_ffmpeg_branch=release/8.0.1-avsplus

## AVS decoder libraries required by the fork
# revisions are the ones FFmpeg-AVS-Builds pins, see
# https://github.com/xeonliu/FFmpeg-AVS-Builds/tree/master/scripts.d
v_uavs3d=0e20d2c291853f196c68922a264bcd8471d75b68
# saindriches/davs2 carries the xatabhk/davs2-10bit work: stock pkuvcl/davs2
# cannot build in 10-bit mode, which AVS2 Main10 streams require
v_davs2_repo=https://github.com/saindriches/davs2
v_davs2=f50435051b72c168c2b566c544e27fcff71ba61a

## DRA audio decoder patch (see buildscripts/patches/)
# ported from FFmpeg-AVSPlus feat/dra-audio-decoder; applied to the ffmpeg
# checkout in include/download-deps.sh. Bump to invalidate the CI cache.
v_dra_patch=1


## Dependency tree

dep_mbedtls=()
dep_dav1d=()
dep_libxml2=()
dep_uavs3d=()
dep_davs2=()
dep_ffmpeg=(mbedtls dav1d libxml2 uavs3d davs2)
dep_freetype2=()
dep_fontconfig=(libxml2 freetype2)
dep_fribidi=()
dep_harfbuzz=()
dep_unibreak=()
dep_libass=(freetype2 fontconfig fribidi harfbuzz unibreak)
dep_lua=()
dep_libplacebo=()
dep_curl=(mbedtls)
dep_mpv=(ffmpeg libass lua libplacebo curl)
dep_mpv_android=(mpv)


## for CI workflow

# pinned ffmpeg revision (branch of $v_ffmpeg_repo)
v_ci_ffmpeg=$v_ffmpeg_branch

# filename used to uniquely identify a build prefix
ci_tarball="prefix-n${v_ndk}-l${v_lua}-u${v_unibreak}-h${v_harfbuzz}-fr${v_fribidi}-ft${v_freetype}-x${v_libxml2}-fo${v_fontconfig}-m${v_mbedtls}-c${v_curl}-ff${v_ci_ffmpeg//\//_}-u3${v_uavs3d:0:8}-d2${v_davs2:0:8}-dra${v_dra_patch}.tgz"
