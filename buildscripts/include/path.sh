#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

os=linux
[[ "$OSTYPE" == "darwin"* ]] && os=macosx
export os

if [ "$os" == "macosx" ]; then
	[ -z "$cores" ] && cores=$(sysctl -n hw.ncpu)
	# various things rely on GNU behaviour
	export INSTALL=`which ginstall`
	export SED=gsed
else
	[ -z "$cores" ] && cores=$(grep -c ^processor /proc/cpuinfo)
fi
cores=${cores:-4}

# configure pkg-config paths if inside buildscripts
if [ -n "$ndk_triple" ]; then
	export PKG_CONFIG_SYSROOT_DIR="$prefix_dir"
	export PKG_CONFIG_LIBDIR="$PKG_CONFIG_SYSROOT_DIR/lib/pkgconfig"
fi

export PATH="$DIR/sdk/ndk-toolchain$ndk_suffix/bin:$DIR/sdk/android-ndk-r19:$DIR/sdk/android-sdk-$os/tools:$PATH"
export ANDROID_HOME="$DIR/sdk/android-sdk-$os"
