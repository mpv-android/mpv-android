#!/bin/bash -e

cd "$( dirname "${BASH_SOURCE[0]}" )"
. ./include/depinfo.sh

cleanbuild=0
nodeps=0
clang=1
target=mpv-android
arch=armv7l

getdeps () {
	varname="dep_${1//-/_}[*]"
	echo ${!varname}
}

loadarch () {
	unset CC CXX CPATH LIBRARY_PATH C_INCLUDE_PATH CPLUS_INCLUDE_PATH

	apilvl=21
	# ndk_triple: what the toolchain actually is
	# cc_triple: what Google pretends the toolchain is
	if [ "$1" == "armv7l" ]; then
		export ndk_suffix=
		export ndk_triple=arm-linux-androideabi
		cc_triple=armv7a-linux-androideabi$apilvl
		prefix_name=armv7l
	elif [ "$1" == "arm64" ]; then
		export ndk_suffix=-arm64
		export ndk_triple=aarch64-linux-android
		cc_triple=$ndk_triple$apilvl
		prefix_name=arm64
	elif [ "$1" == "x86" ]; then
		export ndk_suffix=-x86
		export ndk_triple=i686-linux-android
		cc_triple=$ndk_triple$apilvl
		prefix_name=x86
	elif [ "$1" == "x86_64" ]; then
		export ndk_suffix=-x64
		export ndk_triple=x86_64-linux-android
		cc_triple=$ndk_triple$apilvl
		prefix_name=x86_64
	else
		echo "Invalid architecture"
		exit 1
	fi
	export prefix_dir="$PWD/prefix/$prefix_name"
	if [ $clang -eq 1 ]; then
		export CC=$cc_triple-clang
		export CXX=$cc_triple-clang++
	else
		export CC=$cc_triple-gcc
		export CXX=$cc_triple-g++
	fi
}

setup_prefix () {
	[ -d "$prefix_dir" ] && return 0
	mkdir -p "$prefix_dir"
	# enforce flat structure (/usr/local -> /)
	ln -s . "$prefix_dir/usr"
	ln -s . "$prefix_dir/local"

	# meson wants to be spoonfed this file, so create it ahead of time
	cat >"$prefix_dir/crossfile.txt" <<CROSSFILE
[binaries]
c = '$CC'
cpp = '$CXX'
ar = '$ndk_triple-ar'
strip = '$ndk_triple-strip'
pkgconfig = 'pkg-config'
[host_machine]
system = 'linux'
cpu_family = '${ndk_triple%%-*}'
cpu = '${CC%%-*}'
endian = 'little'
CROSSFILE
}

build () {
	if [ $1 != "mpv-android" ] && [ ! -d deps/$1 ]; then
		echo >&2 -e "\033[1;31mTarget $1 not found\033[m"
		return 1
	fi
	echo >&2 -e "\033[1;34mBuilding $1...\033[m"
	if [ $nodeps -eq 0 ]; then
		deps=$(getdeps $1)
		echo >&2 "Dependencies: $deps"
		for dep in $deps; do
			build $dep
		done
	fi
	if [ "$1" == "mpv-android" ]; then
		pushd ..
		BUILDSCRIPT=buildscripts/scripts/$1.sh
	else
		pushd deps/$1
		BUILDSCRIPT=../../scripts/$1.sh
	fi
	[ $cleanbuild -eq 1 ] && $BUILDSCRIPT clean
	$BUILDSCRIPT build
	popd
}

usage () {
	echo "Usage: buildall.sh [options] [target]"
	echo "Builds the specified target (default: $target)"
	echo "--clean        Clean build dirs before compiling"
	echo "--no-deps      Do not build dependencies"
	echo "--gcc          Use gcc compiler (not officially supported!)"
	echo "--arch <arch>  Build for specified architecture (default: $arch; supported: armv7l, arm64, x86_64)"
	exit 0
}

while [ $# -gt 0 ]; do
	case "$1" in
		--clean)
		cleanbuild=1
		;;
		--no-deps)
		nodeps=1
		;;
		--gcc)
		clang=0
		;;
		--arch)
		shift
		arch=$1
		;;
		-h|--help)
		usage
		;;
		*)
		target=$1
		;;
	esac
	shift
done

loadarch $arch
setup_prefix
build $target

[ "$target" == "mpv-android" ] && \
	ls -lh ../app/build/outputs/apk/{debug,release}/*.apk

exit 0
