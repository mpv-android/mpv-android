#!/bin/bash -e

. ./include/depinfo.sh

[ -z "$TRAVIS" ] && TRAVIS=0
[ -z "$WGET" ] && WGET=wget

mkdir -p deps && cd deps

# mbedtls
mkdir mbedtls
cd mbedtls
$WGET https://tls.mbed.org/download/mbedtls-$v_mbedtls-apache.tgz -O - | \
	tar -xz -f - --strip-components=1
cd ..

# ffmpeg
git clone https://github.com/FFmpeg/FFmpeg ffmpeg
[ $TRAVIS -eq 1 ] && ( cd ffmpeg; git checkout $v_travis_ffmpeg )

# freetype2
git clone git://git.sv.nongnu.org/freetype/freetype2.git -b VER-$v_freetype

# fribidi
mkdir fribidi
cd fribidi
$WGET https://github.com/fribidi/fribidi/releases/download/v$v_fribidi/fribidi-$v_fribidi.tar.bz2 -O - | \
	tar -xj -f - --strip-components=1
cd ..

# libass
git clone https://github.com/libass/libass

# lua
mkdir lua
cd lua
$WGET http://www.lua.org/ftp/lua-$v_lua.tar.gz -O - | \
	tar -xz -f - --strip-components=1
cd ..

# mpv (travis downloads a tar.gz snapshot instead)
[ $TRAVIS -eq 0 ] && \
	git clone https://github.com/mpv-player/mpv

cd ..
