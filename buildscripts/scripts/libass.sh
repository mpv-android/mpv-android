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

asm_args=
if [ "$ndk_triple" != "arm-linux-androideabi" ]; then
	asm_args=--enable-asm
fi

mkdir -p _build$ndk_suffix
cd _build$ndk_suffix

../configure \
	--host=$ndk_triple --with-pic \
	--enable-static --disable-shared \
	--enable-libunibreak --enable-fontconfig \
	$asm_args

make -j$cores
make DESTDIR="$prefix_dir" install
