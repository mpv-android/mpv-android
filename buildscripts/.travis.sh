#!/bin/bash -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR

. ./version.sh

build_prefix() {
	echo "==> Building the prefix ($travis_tarball)..."

	echo "==> Fetching deps"
	TRAVIS=1 ./download-deps.sh

	rm -rf prefix && mkdir prefix
	for x in ${dep_mpv[@]}; do
		echo "==> Building $x"
		./buildall.sh $x
	done

	if [ ! -z "$GITHUB_TOKEN" ]; then
		echo "==> Compressing the prefix"
		tar -cvzf $travis_tarball -C prefix .

		echo "==> Uploading the prefix"
		curl -H "Authorization: token $GITHUB_TOKEN" -H "Content-Type: application/x-gzip" --data-binary @$travis_tarball \
			"https://uploads.github.com/repos/mpv-android/prebuilt-prefixes/releases/9015619/assets?name=$travis_tarball"
	fi
}

if [ "$1" == "install" ]; then
	TRAVIS=1 ./download-sdk.sh
	# link global SDK into our dir structure
	ln -s /usr/local/android-sdk ./sdk/android-sdk-linux

	mkdir -p deps/mpv
	$WGET https://github.com/mpv-player/mpv/archive/master.tar.gz -O master.tgz
	tar -xzf master.tgz -C deps/mpv --strip-components=1 && rm master.tgz

	mkdir -p prefix
	(
		$WGET "https://github.com/mpv-android/prebuilt-prefixes/releases/download/prefixes/$travis_tarball" -O prefix.tgz \
		&& tar -xzf prefix.tgz -C prefix \
		&& rm prefix.tgz
	) || build_prefix
	exit 0
elif [ "$1" == "build" ]; then
	:
else
	exit 1
fi

./buildall.sh --no-deps mpv || {
	# show logfile if configure failed
	[ ! -f deps/mpv/_build/config.h ] && cat deps/mpv/_build/config.log
	exit 1
}

./buildall.sh --no-deps

exit 0
