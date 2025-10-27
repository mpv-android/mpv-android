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

unset CC CXX
meson setup $build --cross-file "$prefix_dir"/crossfile.txt \
	-Dvk-proc-addr=enabled -Ddemos=false

ninja -C $build -j$cores
DESTDIR="$prefix_dir" ninja -C $build install

# add missing library for static linking
# this isn't "-lstdc++" due to a meson bug: https://github.com/mesonbuild/meson/issues/11300
${SED:-sed} '/^Libs:/ s|$| -lc++|' "$prefix_dir/lib/pkgconfig/libplacebo.pc" -i
