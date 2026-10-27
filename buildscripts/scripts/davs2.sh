#!/bin/bash -e

. ../../include/path.sh

build=_build$ndk_suffix

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf $build
	exit 0
else
	exit 255
fi

# davs2 is the AVS2-P2 (IEEE 1857.4) decoder that FFmpeg-AVSPlus uses for its
# libdavs2 decoder. The saindriches/davs2 fork is pinned (see include/depinfo.sh)
# because stock pkuvcl/davs2 cannot build in 10-bit mode. It uses an
# x264-derived configure that locates its sources at the hardcoded path
# ../../source, so an out-of-tree build directory has to sit exactly two levels
# below the checkout.
mkdir -p $build/linux
cp build/linux/{configure,Makefile,config.guess,config.sub} $build/linux/
cd $build/linux

# The endianness check inspects a test object with ${cross_prefix}strings,
# which does not exist under that name in the NDK. Every Android ABI is little
# endian, so fall back to that instead of aborting. (patches our copy only)
${SED:-sed} -i 's/die "endian test failed"/CPU_ENDIAN="little-endian"/g' configure

args=(
	--host=$ndk_triple
	--prefix=/usr/local
	--disable-cli --disable-shared --enable-pic
	# 10 bit output, otherwise AVS2 Main10 streams (i.e. most Chinese
	# broadcast content) are rejected outright by the decoder
	--bit-depth=10
	# davs2 ships no arm/aarch64 assembly at all -- the Makefile references
	# common/{arm,aarch64}/*.S which simply do not exist -- and its x86 path
	# puts -mavx on *every* translation unit, which is above the Android
	# x86_64 baseline. Plain C it is.
	# Note this is what actually saves us: configure clears AS when asm is
	# off and the whole per-architecture block in the Makefile is wrapped in
	# `ifneq ($(AS),)`, so none of the missing sources are ever referenced.
	--disable-asm
)
# sources are C++ and configure defaults CC to ${cross_prefix}g++.
# STRIP would otherwise default to the host `strip` (--host= does not set
# cross_prefix), which cannot handle cross-compiled objects.
CC="$CXX" STRIP=llvm-strip bash ./configure "${args[@]}"

make -j$cores
make DESTDIR="$prefix_dir" install

# ffmpeg links libavcodec.so with $CC (the C driver), so the C++ runtime
# needed by libdavs2.a has to come from the .pc file. The NDK ships libc++ as
# libc++_static.a + libc++abi.a only -- there is no plain libc++, so "-lc++"
# would fail to resolve. Static is required as well: the APK does not bundle
# libc++_shared.so.
# Also drop -lpthread should configure ever add it: bionic has the pthread API
# in libc and the NDK has no libpthread to link against.
${SED:-sed} -i \
	-e '/^Libs:/ s| -lpthread||' \
	-e '/^Libs:/ s|$| -lc++_static -lc++abi|' \
	"$prefix_dir/lib/pkgconfig/davs2.pc"
