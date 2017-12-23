#!/bin/bash -e

. ../../include/path.sh

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf _build$ndk_suffix
	exit 0
else
	exit 255
fi

mkdir -p _build$ndk_suffix
cd _build$ndk_suffix

../configure \
	--host=$ndk_triple \
	--enable-mini-gmp --disable-shared

make -j6
make DESTDIR="$prefix_dir" install
# for ffmpeg:
cat >$prefix_dir/include/gmp.h <<'EOF'
#include <nettle/mini-gmp.h>
#define mpz_div_2exp(q,d,e) mpz_tdiv_q_2exp(q,d,e)
#define mpz_mod_2exp(q,d,e) mpz_tdiv_r_2exp(q,d,e)
EOF
ln -sf libhogweed.a $prefix_dir/lib/libgmp.a
