#!/bin/bash -e

. ../../include/path.sh

build=_build$ndk_suffix
#
if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf $build
	exit 0
else
	exit 255
fi


case "$ndk_triple" in
	arm*)
	target=arm-linux
	;;
	aarch64*)
	target=aarch64-linux
	;;
	i686*)
	target=linux-x86-clang
	;;
	x86_64*)
	target=linux-x86_64-clang
	;;
esac

mkdir -p libSBRdec/include/log/
echo "void android_errorWriteLog(int i, const char *string){}" > libSBRdec/include/log/log.h

./autogen.sh

./configure --host=$target \
--disable-shared \
--disable-frontend \
--enable-static

make clean
make -j$cores 
make DESTDIR="$prefix_dir" install 
