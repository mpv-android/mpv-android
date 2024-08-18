#!/bin/bash -e

. ../../include/path.sh

build=_build$ndk_suffix

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf $build
	exit 0
else
	exit 255
fi

cmake -B $build -G "Ninja" \
	-DENABLE_TESTING=OFF -DENABLE_PROGRAMS=OFF

ninja -C $build -j$cores
DESTDIR="$prefix_dir" ninja -C $build install
