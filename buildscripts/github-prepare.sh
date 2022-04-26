#!/bin/bash -e

. ./include/depinfo.sh
export WGET="wget"

echo "==> Fetching mpv"
mkdir -p deps/mpv
$WGET https://github.com/mpv-player/mpv/archive/master.tar.gz -O master.tgz
tar -xzf master.tgz -C deps/mpv --strip-components=1 && rm master.tgz

echo "==> Trying to fetch existing prefix"
mkdir -p build_prefix

echo "==> Fetching deps"
./include/download-deps.sh


if [ "$1" == "arm64" ]; then
./buildall.sh --arch arm64 mpv
elif [ "$1" == "x86_64" ]; then
./buildall.sh --arch x86_64 mpv
fi

# build everything mpv depends on (but not mpv itself)
for x in ${dep_mpv[@]}; do
	echo "==> Building $x"
	./buildall.sh $x
done
	
echo "==> Building mpv"
./buildall.sh -n mpv || {
	# show logfile if configure failed
	[ ! -f deps/mpv/_build/config.h ] && cat deps/mpv/_build/config.log
	exit 1
}
exit 0
