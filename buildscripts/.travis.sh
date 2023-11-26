#!/bin/bash -e

cd "$( dirname "${BASH_SOURCE[0]}" )"

. ./include/depinfo.sh

msg() {
	printf '==> %s\n' "$1"
}

fetch_prefix() {
	if [[ "$CACHE_MODE" == github ]]; then
		$WGET "https://github.com/mpv-android/prebuilt-prefixes/releases/download/prefixes/$travis_tarball" -O prefix.tgz \
		&& tar -xzf prefix.tgz -C prefix && rm prefix.tgz && return 0
	elif [[ "$CACHE_MODE" == folder ]]; then
		local text=
		if [ -f "$CACHE_FOLDER/id.txt" ]; then
			text=$(cat "$CACHE_FOLDER/id.txt")
		else
			echo "Cache seems to be empty"
		fi
		printf 'Expecting "%s",\nfound     "%s".\n' "$travis_tarball" "$text"
		if [[ "$text" == "$travis_tarball" ]]; then
			tar -xzf "$CACHE_FOLDER/data.tgz" -C prefix && return 0
		fi
	fi
	return 1
}

build_prefix() {
	msg "Building the prefix ($travis_tarball)..."

	msg "Fetching deps"
	TRAVIS=1 ./include/download-deps.sh

	# build everything mpv depends on (but not mpv itself)
	for x in ${dep_mpv[@]}; do
		msg "Building $x"
		./buildall.sh $x
	done

	if [[ "$CACHE_MODE" == github && -n "$GITHUB_TOKEN" ]]; then
		msg "Compressing the prefix"
		tar -cvzf $travis_tarball -C prefix .

		msg "Uploading the prefix"
		curl -H "Authorization: token $GITHUB_TOKEN" -H "Content-Type: application/x-gzip" --data-binary @$travis_tarball \
			"https://uploads.github.com/repos/mpv-android/prebuilt-prefixes/releases/9015619/assets?name=$travis_tarball"
	elif [[ "$CACHE_MODE" == folder && -w "$CACHE_FOLDER" ]]; then
		msg "Compressing the prefix"
		tar -cvzf "$CACHE_FOLDER/data.tgz" -C prefix .
		echo "$travis_tarball" >"$CACHE_FOLDER/id.txt"
	fi
}

export WGET="wget --progress=bar:force"

if [ "$1" == "install" ]; then
	if [[ -n "$ANDROID_HOME" && -d "$ANDROID_HOME" ]]; then
		msg "Linking existing SDK"
		mkdir -p sdk
		ln -sv "$ANDROID_HOME" sdk/android-sdk-linux
	fi

	msg "Fetching SDK + NDK"
	TRAVIS=1 ./include/download-sdk.sh

	msg "Fetching mpv"
	mkdir -p deps/mpv
	$WGET https://github.com/mpv-player/mpv/archive/master.tar.gz -O master.tgz
	tar -xzf master.tgz -C deps/mpv --strip-components=1
	rm master.tgz

	msg "Trying to fetch existing prefix"
	mkdir -p prefix
	fetch_prefix || build_prefix
	exit 0
elif [ "$1" == "build" ]; then
	:
else
	exit 1
fi

msg "Building mpv"
./buildall.sh -n mpv || {
	# show logfile if configure failed
	[ ! -f deps/mpv/_build/config.h ] && cat deps/mpv/_build/meson-logs/meson-log.txt
	exit 1
}

msg "Building mpv-android"
./buildall.sh -n

exit 0
