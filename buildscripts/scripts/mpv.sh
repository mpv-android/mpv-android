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

PKG_CONFIG="pkg-config --static" \
./waf configure \
	--disable-iconv --enable-lua \
	--enable-libmpv-shared \
	--disable-manpage-build \
	-o "`pwd`/_build$ndk_suffix"

./waf build -j$cores
./waf install --destdir="$prefix_dir"
