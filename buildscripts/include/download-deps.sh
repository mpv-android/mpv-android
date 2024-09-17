#!/bin/bash -e

. ./include/depinfo.sh

[ -z "$TRAVIS" ] && TRAVIS=0
[ -z "$WGET" ] && WGET=wget

mkdir -p deps && cd deps

# mbedtls
if [ ! -d mbedtls ]; then
	mkdir mbedtls
	$WGET https://github.com/Mbed-TLS/mbedtls/releases/download/mbedtls-$v_mbedtls/mbedtls-$v_mbedtls.tar.bz2 -O - | \
		tar -xj -C mbedtls --strip-components=1
fi

# dav1d
[ ! -d dav1d ] && git clone --filter=blob:none https://github.com/videolan/dav1d

# ffmpeg
if [ ! -d ffmpeg ]; then
	git clone --filter=blob:none https://github.com/FFmpeg/FFmpeg ffmpeg
	[ $TRAVIS -eq 1 ] && ( cd ffmpeg; git checkout $v_travis_ffmpeg )
fi

# freetype2
[ ! -d freetype2 ] && git clone --filter=blob:none --recurse-submodules git://git.sv.nongnu.org/freetype/freetype2.git -b VER-$v_freetype

# fribidi
if [ ! -d fribidi ]; then
	mkdir fribidi
	$WGET https://github.com/fribidi/fribidi/releases/download/v$v_fribidi/fribidi-$v_fribidi.tar.xz -O - | \
		tar -xJ -C fribidi --strip-components=1
fi

# harfbuzz
if [ ! -d harfbuzz ]; then
	mkdir harfbuzz
	$WGET https://github.com/harfbuzz/harfbuzz/releases/download/$v_harfbuzz/harfbuzz-$v_harfbuzz.tar.xz -O - | \
		tar -xJ -C harfbuzz --strip-components=1
fi

# unibreak
if [ ! -d unibreak ]; then
	mkdir unibreak
	$WGET https://github.com/adah1972/libunibreak/releases/download/libunibreak_${v_unibreak/./_}/libunibreak-${v_unibreak}.tar.gz -O - | \
		tar -xz -C unibreak --strip-components=1
fi

# libass
[ ! -d libass ] && git clone --filter=blob:none https://github.com/libass/libass

# lua
if [ ! -d lua ]; then
	mkdir lua
	$WGET https://www.lua.org/ftp/lua-$v_lua.tar.gz -O - | \
		tar -xz -C lua --strip-components=1
fi

# libplacebo
[ ! -d libplacebo ] && git clone --filter=blob:none --recursive https://github.com/haasn/libplacebo

# mpv
[ ! -d mpv ] && git clone --filter=blob:none https://github.com/mpv-player/mpv

cd ..
