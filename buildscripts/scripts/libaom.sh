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

mkdir -p _build$ndk_suffix
cd _build$ndk_suffix

cpu=armv7
[[ "$ndk_triple" == "aarch64"* ]] && cpu=arm64

cpuflags=
[[ "$ndk_triple" == "arm"* ]] && cpuflags="$cpuflags -mfloat-abi=softfp -mfpu=neon -mcpu=cortex-a8"
[[ "$ndk_triple" == "aarch64"* ]] && cpuflags="$cpuflags -D__ARM_ARCH=8" # ???

cmake .. \
	-DCMAKE_SYSTEM_NAME=Linux \
	-DCMAKE_C_COMPILER=$CC -DCMAKE_CXX_COMPILER=$ndk_triple-clang++ \
	-DAOM_TARGET_CPU=$cpu -DCMAKE_C_FLAGS="$cpuflags" \
	-DENABLE_{EXAMPLES,DOCS,TESTS}=0

# TODO: report bugs to upstream / move patching elsewhere
sed 's/fseeko\b/fseek/' ../third_party/libwebm/mkvmuxer/mkvwriter.cc -i
sed 's/fseeko\b/fseek/' ../third_party/libwebm/mkvparser/mkvreader.cc -i
sed -r 's/defined\(__ANDROID__\)/0/' ../aom_ports/arm_cpudetect.c -i

make -j6
make DESTDIR="$prefix_dir" install

sed '/^Libs:/ s/ -lpthread//' "$prefix_dir/lib/pkgconfig/aom.pc" -i # ???
