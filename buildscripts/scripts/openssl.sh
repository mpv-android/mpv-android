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

export CFLAGS="-Os"

mkdir -p _build$ndk_suffix
cd _build$ndk_suffix

case "$ndk_triple" in
	arm*)
	target=linux-armv4
	;;
	aarch64*)
	target=linux-aarch64
	;;
	i686*)
	target=linux-x86-clang
	;;
	x86_64*)
	target=linux-x86_64-clang
	;;
	*)
	exit 1
	;;
esac

../Configure $target no-shared
make -j$cores
make DESTDIR="$prefix_dir" install_sw
