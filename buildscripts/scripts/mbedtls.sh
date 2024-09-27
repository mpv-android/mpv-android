#!/bin/bash -e

. ../../include/path.sh

build=_build$ndk_suffix

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf $build
	make clean
	exit 0
else
	exit 255
fi

$0 clean # separate building not supported, always clean
if [[ "$ndk_triple" == "i686"* ]]; then
	./scripts/config.py unset MBEDTLS_AESNI_C
else
	./scripts/config.py set MBEDTLS_AESNI_C
fi

mkdir -p $build
cd $build

cmake .. \
	-DENABLE_TESTING=OFF \
	-DUSE_SHARED_MBEDTLS_LIBRARY=ON \
	-DCMAKE_PREFIX_PATH="$prefix_dir" \
	-DCMAKE_PLATFORM_NO_VERSIONED_SONAME=ON 

make -j$cores
make DESTDIR="$prefix_dir" install
