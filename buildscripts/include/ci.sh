#!/bin/bash -e

# go to buildscripts root folder
cd "$( dirname "${BASH_SOURCE[0]}" )/.."

. ./include/depinfo.sh

v_ci_archs="armv7l arm64"

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

	for arch in $v_ci_archs; do
		msg "Compiling deps ($arch)"
		./buildall.sh --only-deps mpv --arch "$arch"
	done

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

	msg "Fetching mpv"
	mkdir -p deps/mpv
	$WGET https://github.com/mpv-player/mpv/archive/master.tar.gz -O master.tgz
	tar -xzf master.tgz -C deps/mpv --strip-components=1
	rm master.tgz

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

for arch in $v_ci_archs; do
	msg "Building mpv ($arch)"
	./buildall.sh -n mpv --arch "$arch" || {
		# show logfile if configure failed
		build_dir="deps/mpv/_build_$arch"
		[ ! -f "$build_dir/config.h" ] && cat "$build_dir/meson-logs/meson-log.txt"
		exit 1
	}
done

msg "Building mpv-android"
./buildall.sh -n

exit 0
