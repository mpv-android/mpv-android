#!/bin/bash -e

prefix_rev=2017-12-22
cd buildscripts

if [ "$1" == "install" ]; then
	TRAVIS=1 ./download.sh
	# link global SDK into our dir structure
	ln -s /usr/local/android-sdk ./sdk/android-sdk-linux

	mkdir -p deps/mpv
	wget https://github.com/mpv-player/mpv/archive/master.tar.gz -O master.tgz
	tar -xzf master.tgz -C deps/mpv --strip-components=1 && rm master.tgz

	mkdir -p prefix
	wget "https://kitsunemimi.pw/tmp/prefix_${prefix_rev}.tgz" -O prefix.tgz
	tar -xzf prefix.tgz -C prefix && rm prefix.tgz
	exit 0
elif [ "$1" == "build" ]; then
	:
else
	exit 1
fi

./buildall.sh --no-deps mpv || {
	# show logfile if configure failed
	[ ! -f deps/mpv/_build/config.h ] && cat deps/mpv/_build/config.log;
	exit 1;
}

./buildall.sh --no-deps

exit 0
