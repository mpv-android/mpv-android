#!/bin/bash -e

. ./include/depinfo.sh

[ -z "$IN_CI" ] && IN_CI=0
[ -z "$WGET" ] && WGET=wget

mkdir -p deps && cd deps

clone_pinned () {
	local repository=$1
	local directory=$2
	local revision=$3
	local recursive=${4:-0}

	mkdir "$directory"
	git -C "$directory" init --quiet
	git -C "$directory" remote add origin "$repository"
	git -C "$directory" fetch --depth=1 origin "$revision"
	git -C "$directory" checkout --detach --quiet FETCH_HEAD
	if [ $recursive -eq 1 ]; then
		git -C "$directory" submodule update --init --recursive --depth=1
	fi
}

# mbedtls
if [ ! -d mbedtls ]; then
	mkdir mbedtls
	$WGET https://github.com/Mbed-TLS/mbedtls/releases/download/mbedtls-$v_mbedtls/mbedtls-$v_mbedtls.tar.bz2 -O - | \
		tar -xj -C mbedtls --strip-components=1
fi

# dav1d
if [ ! -d dav1d ]; then
	if [ $IN_CI -eq 1 ]; then
		clone_pinned https://github.com/videolan/dav1d dav1d "$v_ci_dav1d"
	else
		git clone https://github.com/videolan/dav1d
	fi
fi

# ffmpeg
if [ ! -d ffmpeg ]; then
	if [ $IN_CI -eq 1 ]; then
		clone_pinned https://github.com/FFmpeg/FFmpeg ffmpeg "$v_ci_ffmpeg"
	else
		git clone https://github.com/FFmpeg/FFmpeg ffmpeg
	fi
fi

# freetype2
if [ ! -d freetype2 ]; then
	if [ $IN_CI -eq 1 ]; then
		clone_pinned https://gitlab.freedesktop.org/freetype/freetype.git freetype2 "$v_ci_freetype" 1
	else
		git clone --recurse-submodules https://gitlab.freedesktop.org/freetype/freetype.git freetype2 -b VER-${v_freetype//./-}
	fi
fi

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
	$WGET https://github.com/adah1972/libunibreak/releases/download/libunibreak_${v_unibreak//./_}/libunibreak-${v_unibreak}.tar.gz -O - | \
		tar -xz -C unibreak --strip-components=1
fi

# libxml2
if [ ! -d libxml2 ]; then
	mkdir libxml2
	$WGET https://gitlab.gnome.org/GNOME/libxml2/-/archive/v${v_libxml2}/libxml2-v${v_libxml2}.tar.gz -O - | \
		tar -xz -C libxml2 --strip-components=1
fi

# fontconfig
if [ ! -d fontconfig ]; then
	mkdir fontconfig
	$WGET https://gitlab.freedesktop.org/fontconfig/fontconfig/-/archive/${v_fontconfig}/fontconfig-${v_fontconfig}.tar.gz -O - | \
		tar -xz -C fontconfig --strip-components=1
fi

# libass
if [ ! -d libass ]; then
	if [ $IN_CI -eq 1 ]; then
		clone_pinned https://github.com/libass/libass libass "$v_ci_libass"
	else
		git clone https://github.com/libass/libass
	fi
fi

# lua
if [ ! -d lua ]; then
	mkdir lua
	$WGET https://www.lua.org/ftp/lua-$v_lua.tar.gz -O - | \
		tar -xz -C lua --strip-components=1
fi

# libplacebo
if [ ! -d libplacebo ]; then
	if [ $IN_CI -eq 1 ]; then
		clone_pinned https://github.com/haasn/libplacebo libplacebo "$v_ci_libplacebo" 1
	else
		git clone --recursive https://github.com/haasn/libplacebo
	fi
fi

# mpv
if [ ! -d mpv ]; then
	if [ $IN_CI -eq 1 ]; then
		clone_pinned https://github.com/mpv-player/mpv mpv "$v_ci_mpv"
	else
		git clone https://github.com/mpv-player/mpv
	fi
fi

cd ..
