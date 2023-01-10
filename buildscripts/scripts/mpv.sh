#!/bin/bash -e

. ../../include/path.sh

build=_build$ndk_suffix


# Meson must clean the build directory or it will build a static library instead of a shared one
rm -rf $build

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	exit 0
else
	exit 255
fi

unset CC CXX # meson wants these unset

PKG_CONFIG="pkg-config --static" \
meson $build --cross-file "$prefix_dir"/crossfile.txt \
	--default-library=shared \
	--buildtype=plain \
	-Diconv=disabled \
	-Dlua=enabled \
	-Dlibmpv=true \
	-Dmanpage-build=disabled

ninja -C $build -j$coreshttps://github.com/mpv-android/mpv-android/pull/58
DESTDIR="$prefix_dir" ninja -C $build install
