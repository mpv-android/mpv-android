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

make HAVE_READLINE=no -j$cores
make HAVE_READLINE=no DESTDIR="$prefix_dir" install