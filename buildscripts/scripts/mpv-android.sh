#!/bin/bash -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BUILD="$DIR/.."
MPV_ANDROID="$DIR/../.."

. $BUILD/include/path.sh
. $BUILD/include/depinfo.sh

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf $MPV_ANDROID/{app,.}/build $MPV_ANDROID/app/src/main/{libs,obj}
	exit 0
else
	exit 255
fi

nativeprefix () {
	if [ -f $BUILD/prefix/$1/lib/libmpv.so ]; then
		echo $BUILD/prefix/$1
	else
		echo >&2 "Warning: libmpv.so not found in native prefix for $1, support will be omitted"
	fi
}

prefix64=$(nativeprefix "arm64")
prefix_x64=$(nativeprefix "x86_64")
prefix_x86=$(nativeprefix "x86")

PREFIX=$BUILD/prefix/armv7l PREFIX64=$prefix64 PREFIX_X64=$prefix_x64 PREFIX_X86=$prefix_x86 \
ndk-build -C app/src/main -j$cores
./gradlew assembleDebug assembleRelease

if [ -n "${ANDROID_SIGNING_KEY:-}" ]; then
	cd "${MPV_ANDROID}/app/build/outputs"
	cp apk/debug/app-debug{,-signed}.apk
	"${ANDROID_HOME}/build-tools/${v_sdk_build_tools}/apksigner" sign --ks "${ANDROID_SIGNING_KEY}" apk/debug/app-debug-signed.apk
	cp apk/release/app-release-{un,}signed.apk
	"${ANDROID_HOME}/build-tools/${v_sdk_build_tools}/apksigner" sign --ks "${ANDROID_SIGNING_KEY}" apk/release/app-release-signed.apk
fi
