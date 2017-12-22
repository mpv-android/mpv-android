#!/bin/bash -e

. ../../path.sh

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf _build$dir_suffix
	exit 0
else
	exit 255
fi

# You'll need to use the non-git version of fribidi as the git version is missing some files

mkdir -p _build$dir_suffix
cd _build$dir_suffix

PKG_CONFIG=/bin/false \
../configure \
	--host=$ndk_triple \
	--enable-static --disable-shared

make -j6
make DESTDIR="`pwd`/../../../prefix$dir_suffix" install
