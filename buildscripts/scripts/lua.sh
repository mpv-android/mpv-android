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

# Building seperately from source tree is not supported, this means we are forced to always clean
$0 clean

# LUA_T= and LUAC_T= to disable building lua & luac
# -Dgetlocaledecpoint()=('.') fixes bionic missing decimal_point in localeconv
#
make HOST_CC="clang -m$bits" HOST_CFLAGS="-I/usr/include" CROSS="$ndk_triple"- \
	STATIC_CC="$CC -Dgetlocaledecpoint\(\)=\(\'.\'\)" DYNAMIC_CC="$CC -fPIC -Dgetlocaledecpoint\(\)=\(\'.\'\)" \
	TARGET_LD=$CC TARGET_AR="llvm-ar rcus"\
	TARGET_STRIP="llvm-strip" -j$cores

# TO_BIN=/dev/null disables installing lua & luac
make PREFIX=$prefix_dir install

# Force mpv to link statically with luajit
rm -rf $prefix_dir/lib/libluajit-*.so*