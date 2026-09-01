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

[ -f configure ] || ./autogen.sh

asm_args=--enable-asm
[[ "$ndk_triple" == "arm"* ]] && asm_args=

mkdir -p _build$ndk_suffix
cd _build$ndk_suffix

../configure \
	--host=$ndk_triple --with-pic $asm_args \
	--enable-static --disable-shared \
	--enable-libunibreak --enable-fontconfig

make -j$cores
make DESTDIR="$prefix_dir" install
