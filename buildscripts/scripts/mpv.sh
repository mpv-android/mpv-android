#!/bin/bash -e

. ../../path.sh

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf _build$ndk_suffix
	exit 0
else
	exit 255
fi

[ -f waf ] || ./bootstrap.py

extrald=
[[ "$ndk_triple" == "aarch64"* ]] && extrald="-fuse-ld=gold"

LDFLAGS="$extrald" \
./waf configure \
	--disable-iconv --lua=52 \
	--enable-libmpv-shared \
	--disable-manpage-build \
	-o "`pwd`/_build$ndk_suffix"

./waf build -j6
./waf install --destdir="$prefix_dir"
