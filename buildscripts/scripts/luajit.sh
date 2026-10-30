#!/bin/bash -e

. ../../include/path.sh

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	make clean
	exit 0
else
	exit 255
fi

# LuaJIT only supports in-tree builds and does not keep
# target-specific artifacts apart, so we always do a clean build.
$0 clean

# LuaJIT's host tools must have the same pointer size as the target.
host_cc=gcc
[[ "$ndk_triple" == "arm"* || "$ndk_triple" == "i686"* ]] && host_cc="gcc -m32"

make -j$cores \
	HOST_CC="$host_cc" \
	CROSS="$ndk_triple-" \
	STATIC_CC="$CC" DYNAMIC_CC="$CC -fPIC" \
	TARGET_LD="$CC" TARGET_AR="$AR rcus" TARGET_STRIP=llvm-strip \
	TARGET_SYS=Linux BUILDMODE=static \
	PREFIX="$prefix_dir"

make PREFIX="$prefix_dir" install
