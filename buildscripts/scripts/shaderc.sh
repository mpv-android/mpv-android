#!/bin/bash -e

. ../../include/path.sh

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf local include libs
	exit 0
else
	exit 255
fi

builddir=$PWD
application_mk=$PWD/../../../app/src/main/jni/Application.mk # APP_{PLATFORM,STL} are imported from here

abi=armeabi-v7a
[[ "$ndk_triple" == "aarch64"* ]] && abi=arm64-v8a
[[ "$ndk_triple" == "x86_64"* ]] && abi=x86_64
[[ "$ndk_triple" == "i686"* ]] && abi=x86

# build using the NDK's scripts, but keep object files in our build dir
cd "$(dirname "$(which ndk-build)")/sources/third_party/shaderc"
ndk-build -j$cores \
	NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=Android.mk \
	NDK_APPLICATION_MK="$application_mk" APP_ABI=$abi \
	NDK_APP_OUT="$builddir" NDK_APP_LIBS_OUT="$builddir/libs" \
	libshaderc_combined

cd "$builddir"
cp -r include/* "$prefix_dir/include"
cp libs/*/$abi/libshaderc.a "$prefix_dir/lib/libshaderc_combined.a"

# create a pkgconfig file
# 'libc++' instead of 'libstdc++': workaround for meson linking bug
mkdir -p "$prefix_dir"/lib/pkgconfig
cat >"$prefix_dir"/lib/pkgconfig/shaderc_combined.pc <<"END"
prefix=/usr/local
includedir=${prefix}/include
libdir=${prefix}/lib

Name: shaderc_combined
Description:
Version: 2021.0-unknown
Libs: -L${libdir} -lshaderc_combined -llibc++
Cflags: -I${includedir}
END
