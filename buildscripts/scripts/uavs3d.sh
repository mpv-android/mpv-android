#!/bin/bash -e

. ../../include/path.sh
. ../../include/depinfo.sh # for $v_ndk

build=_build$ndk_suffix

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf $build
	exit 0
else
	exit 255
fi

# uavs3d is the AVS3-P2 (IEEE 1857.10) decoder that FFmpeg-AVSPlus uses for
# its libuavs3d decoder. It's a plain CMake project, so drive it through the
# toolchain file shipped with the NDK instead of the meson cross file.

case "$ndk_triple" in
	arm-*)     abi=armeabi-v7a ;;
	aarch64-*) abi=arm64-v8a ;;
	i686-*)    abi=x86 ;;
	x86_64-*)  abi=x86_64 ;;
	*)
	echo "Unsupported target $ndk_triple" >&2
	exit 1
	;;
esac

ndk_dir="${ANDROID_NDK_HOME:-$DIR/sdk/android-ndk-$v_ndk}"
toolchain="$ndk_dir/build/cmake/android.toolchain.cmake"
if [ ! -f "$toolchain" ]; then
	echo "Can't find the CMake toolchain file inside the NDK ($toolchain)" >&2
	exit 1
fi

# Upstream hardcodes "-mfloat-abi=hard" for armv7 because their CI targets a
# musleabihf toolchain. Android's armeabi-v7a is softfp, mixing the two makes
# the linker reject the objects. This edit is idempotent.
${SED:-sed} -i \
	's/-mcpu=cortex-a9 -mfpu=neon -mfloat-abi=hard/-mcpu=cortex-a8 -mfpu=neon -mfloat-abi=softfp/' \
	source/CMakeLists.txt

unset CC CXX # the toolchain file picks the compilers

# CMAKE_POLICY_VERSION_MINIMUM: uavs3d still asks for cmake_minimum_required(3.1),
# which CMake 4.x refuses outright.
cmake -B $build -G Ninja \
	-DCMAKE_TOOLCHAIN_FILE="$toolchain" \
	-DANDROID_ABI=$abi -DANDROID_PLATFORM=android-23 \
	-DCMAKE_POLICY_VERSION_MINIMUM=3.5 \
	-DCMAKE_BUILD_TYPE=Release \
	-DCMAKE_INSTALL_PREFIX=/usr/local \
	-DBUILD_SHARED_LIBS=NO \
	-DCOMPILE_10BIT=1

# only the library, skip the uavs3dec test binary
cmake --build $build -j$cores --target uavs3d
DESTDIR="$prefix_dir" cmake --install $build

# bionic has the pthread API in libc, there is no libpthread to link against
${SED:-sed} -i 's/ -lpthread//' "$prefix_dir/lib/pkgconfig/uavs3d.pc"
