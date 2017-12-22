#!/bin/bash -e

cleanbuild=0
nodeps=0
clang=0
target=mpv-android
arch=armv7l

# i would've used a dict but putting arrays in a dict is not a thing

dep_nettle=()
dep_gnutls=(nettle)
dep_ffmpeg=(gnutls)
dep_freetype2=()
dep_fribidi=()
dep_libass=(freetype2 fribidi)
dep_lua=()
dep_mpv=(ffmpeg libass lua)
dep_mpv_android=(mpv)

getdeps () {
	varname="dep_${1//-/_}[*]"
	echo ${!varname}
}

loadarch () {
	if [ "$1" == "armv7l" ]; then
		export ndk_suffix=
		export ndk_triple=arm-linux-androideabi
		export dir_suffix=
	elif [ "$1" == "arm64" ]; then
		export ndk_suffix=-arm64
		export ndk_triple=aarch64-linux-android
		export dir_suffix=64
	elif [ "$1" == "x86_64" ]; then
		export ndk_suffix=-x64
		export ndk_triple=x86_64-linux-android
		export dir_suffix=-x64
	else
		echo "Invalid architecture"
		exit 1
	fi
	if [ $clang -eq 1 ]; then
		export CC=$ndk_triple-clang
	else
		export CC=$ndk_triple-gcc
	fi

	if [ ! -d "./prefix$dir_suffix" ]; then
		mkdir "prefix$dir_suffix"
		# enforce flat structure (/usr/local -> /)
		ln -s . "./prefix$dir_suffix/usr"
		ln -s . "./prefix$dir_suffix/local"
	fi
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
	echo "--clang        Use clang compiler"
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
		--clang)
		clang=1
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
build $target

[ "$target" == "mpv-android" ] && \
	ls -lh ../app/build/outputs/apk/debug/app-debug.apk

exit 0
