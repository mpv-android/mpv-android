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
# you can double-check the version in vk.xml (ctrl+f VK_API_VERSION)
mkdir -p "$prefix_dir"/lib/pkgconfig
cat >"$prefix_dir"/lib/pkgconfig/vulkan.pc <<"END"
Name: Vulkan
Description:
Version: 1.2.0
Libs: -lvulkan
Cflags:
END

ndk_vulkan="$(dirname "$(which ndk-build)")/sources/third_party/vulkan/src"

unset CC CXX
# use third party over system headers (which are incomplete)
export CFLAGS="-isystem $ndk_vulkan/include"
meson $build --cross-file "$prefix_dir"/crossfile.txt \
	-Dvulkan-registry="$ndk_vulkan/registry/vk.xml" -Ddemos=false

ninja -C $build -j$cores
DESTDIR="$prefix_dir" ninja -C $build install
