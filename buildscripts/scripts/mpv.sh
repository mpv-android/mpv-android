#!/bin/bash -e

. ../../include/path.sh

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf _build$ndk_suffix
	exit 0
else
	exit 255
fi

[ -f waf ] || ./bootstrap.py

# ffmpeg-strict-abi: to be removed once https://github.com/mpv-player/mpv/issues/9159 is solved
PKG_CONFIG="pkg-config --static" \
./waf configure \
	--disable-iconv --lua=52 \
	--enable-libmpv-shared --enable-ffmpeg-strict-abi \
	--disable-manpage-build \
	-o "`pwd`/_build$ndk_suffix"

./waf build -j$cores
./waf install --destdir="$prefix_dir"
