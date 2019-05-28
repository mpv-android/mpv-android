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

# we need to spoonfeed meson some settings in a file
mkdir -p $build
cat >$build/crossfile.txt <<AAA
[binaries]
c = '$CC'
ar = '$ndk_triple-ar'
strip = '$ndk_triple-strip'
[host_machine]
system = 'linux'
cpu_family = '${ndk_triple%%-*}'
cpu = '${CC%%-*}'
endian = 'little'
[paths]
prefix = '$prefix_dir'
AAA

# meson wants $CC to be the host compiler
unset CC

meson $build \
	--buildtype release --cross-file $build/crossfile.txt \
	--default-library static

ninja -C $build -j$cores
ninja -C $build install
