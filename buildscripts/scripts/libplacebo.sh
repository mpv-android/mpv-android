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

# Android provides Vulkan, but no pkgconfig file
mkdir -p "$prefix_dir"/lib/pkgconfig
cat >"$prefix_dir"/lib/pkgconfig/vulkan.pc <<"END"
Name: Vulkan
Description:
Version: 1.1
Libs: -lvulkan
Cflags:
END

ndk_vulkan="$(dirname "$(which ndk-build)")/sources/third_party/vulkan"

patch -Np1 -i ../../scripts/libplacebo.patch || true

echo "ndk"
echo $ndk_vulkan

unset CC CXX
meson $build --cross-file "$prefix_dir"/crossfile.txt \
	-Dvulkan-registry="$ndk_vulkan/src/registry/vk.xml" -Ddemos=false -Dopengl=enabled

ninja -C $build -j$cores
DESTDIR="$prefix_dir" ninja -C $build install
