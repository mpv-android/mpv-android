#!/bin/bash -e

. ./include/depinfo.sh

[ -z "$TRAVIS" ] && TRAVIS=0
[ -z "$WGET" ] && WGET=wget

mkdir -p deps && cd deps

# elf-cleaner
[ ! -d elf-cleaner ] && git clone https://github.com/termux/termux-elf-cleaner elf-cleaner

# mbedtls
if [ ! -d mbedtls ]; then
	mkdir mbedtls
	$WGET https://github.com/ARMmbed/mbedtls/archive/mbedtls-$v_mbedtls.tar.gz -O - | \
		tar -xz -C mbedtls --strip-components=1
fi

# dav1d
[ ! -d dav1d ] && git clone https://github.com/videolan/dav1d

# ffmpeg
if [ ! -d ffmpeg ]; then
	git clone https://github.com/FFmpeg/FFmpeg ffmpeg
	[ $TRAVIS -eq 1 ] && ( cd ffmpeg; git checkout $v_travis_ffmpeg )
fi

# freetype2
[ ! -d freetype2 ] && git clone --recurse-submodules git://git.sv.nongnu.org/freetype/freetype2.git -b VER-$v_freetype

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

# libass
[ ! -d libass ] && git clone https://github.com/libass/libass

# lua
if [ ! -d lua ]; then
	mkdir lua
	$WGET https://www.lua.org/ftp/lua-$v_lua.tar.gz -O - | \
		tar -xz -C lua --strip-components=1
fi

# mpv
[ ! -d mpv ] && git clone https://github.com/mpv-player/mpv

# openssl
if [ ! -d openssl ]; then
	mkdir openssl
	$WGET https://www.openssl.org/source/openssl-$v_openssl.tar.gz -O - | \
		tar -xz -C openssl --strip-components=1
fi

# python
if [ ! -d python ]; then
	mkdir python
	$WGET https://www.python.org/ftp/python/$v_python/Python-$v_python.tar.xz -O- | \
		tar -xJ -C python --strip-components=1

	cd python
	for name in inplace static_modules; do
		patch -p0 --verbose <../../include/py/$name.patch
	done
	# Enables all modules *except* these
	python3 ../../include/py/uncomment.py Modules/Setup \
		'readline|_test|spwd|grp|_crypt|nis|termios|resource|audio|_md5|_sha[125]|_tkinter|syslog|_curses|_g?dbm|_(multibyte)?codec'
	# SSL path is not used
	sed 's|^SSL=.*|SSL=/var/empty|' -i Modules/Setup
	# hashlib via openssl
	echo '_hashlib _hashopenssl.c -lcrypto' >>Modules/Setup
	cd ..
fi

cd ..

# youtube-dl
$WGET https://kitsunemimi.pw/ytdl/dist.zip
unzip dist.zip -d ../app/src/main/assets/ytdl
rm -f ../app/src/main/assets/ytdl/youtube-dl # don't need it
rm dist.zip
