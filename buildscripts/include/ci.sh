#!/bin/bash -e

# go to buildscripts root folder
cd "$( dirname "${BASH_SOURCE[0]}" )/.."

. ./include/depinfo.sh

msg() {
	printf '==> %s\n' "$1"
}

fetch_prefix() {
	if [[ "$CACHE_MODE" == folder ]]; then
		local text=
		if [ -f "$CACHE_FOLDER/id.txt" ]; then
			text=$(cat "$CACHE_FOLDER/id.txt")
		else
			echo "Cache seems to be empty"
		fi
		printf 'Expecting "%s",\nfound     "%s".\n' "$ci_tarball" "$text"
		if [[ "$text" == "$ci_tarball" ]]; then
			tar -xzf "$CACHE_FOLDER/data.tgz" -C prefix && return 0
		fi
	fi
	return 1
}

build_prefix() {
	msg "Building the prefix ($ci_tarball)..."

	msg "Fetching deps"
	IN_CI=1 ./include/download-deps.sh

	msg "Compiling armv7 dependencies"
	./buildall.sh --arch armv7l --only-deps mpv

	msg "Compiling arm64 dependencies"
	./buildall.sh --arch arm64 --only-deps mpv

	# Preserve the generated FFmpeg feature configuration with the cached
	# prefix so verification works on cache hits without retaining build trees.
	mkdir -p prefix/ci-config
	cp deps/ffmpeg/_build_armv7l/config_components.h prefix/ci-config/ffmpeg-armv7l.h
	cp deps/ffmpeg/_build_arm64/config_components.h prefix/ci-config/ffmpeg-arm64.h

	if [[ "$CACHE_MODE" == folder && -w "$CACHE_FOLDER" ]]; then
		msg "Compressing the prefix"
		tar -cvzf "$CACHE_FOLDER/data.tgz" -C prefix .
		echo "$ci_tarball" >"$CACHE_FOLDER/id.txt"
	fi
}

export WGET="wget --progress=bar:force"

if [ "$1" = "export" ]; then
	# export variable with unique cache identifier
	echo "CACHE_IDENTIFIER=$ci_tarball"
	exit 0
elif [ "$1" = "install" ]; then
	# install deps
	if [[ -n "$ANDROID_HOME" && -d "$ANDROID_HOME" ]]; then
		msg "Linking existing SDK"
		mkdir -p sdk
		ln -sv "$ANDROID_HOME" sdk/android-sdk-linux
	fi

	msg "Fetching SDK + NDK"
	IN_CI=1 ./include/download-sdk.sh

	msg "Fetching pinned mpv"
	mkdir -p deps/mpv
	$WGET "https://github.com/mpv-player/mpv/archive/${v_ci_mpv}.tar.gz" -O mpv.tgz
	tar -xzf mpv.tgz -C deps/mpv --strip-components=1
	rm mpv.tgz

	msg "Trying to fetch existing prefix"
	mkdir -p prefix
	fetch_prefix || build_prefix
	exit 0
elif [ "$1" = "build" ]; then
	# run build
	:
else
	exit 1
fi

msg "Building armv7 mpv"
./buildall.sh --arch armv7l -n mpv || {
	# show logfile if configure failed
	[ ! -f deps/mpv/_build_armv7l/config.h ] && \
		cat deps/mpv/_build_armv7l/meson-logs/meson-log.txt
	exit 1
}

msg "Building arm64 mpv"
./buildall.sh --arch arm64 -n mpv || {
	[ ! -f deps/mpv/_build_arm64/config.h ] && \
		cat deps/mpv/_build_arm64/meson-logs/meson-log.txt
	exit 1
}

msg "Building dual-ABI mpv-android AAR"
./buildall.sh --arch arm64 -n mpv-android

exit 0
