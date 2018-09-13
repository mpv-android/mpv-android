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

mkdir -p _build$ndk_suffix
cd _build$ndk_suffix

PKG_CONFIG=/bin/false \
../configure \
	--host=$ndk_triple \
	--enable-static --disable-shared \
	--disable-docs

make -j$cores
make DESTDIR="$prefix_dir" install
