#!/bin/bash -e

. ../../include/path.sh

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	make clean
	exit 0
else
	exit 255
fi

$0 clean # separate building not supported, always clean

export AR=llvm-ar
# CFLAGS: workaround for https://github.com/ARMmbed/mbedtls/issues/4786
make CFLAGS="-O1" -j$cores no_test
make DESTDIR="$prefix_dir" install
