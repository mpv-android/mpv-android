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

# Note: openssl has direct NDK build support, but there's too much magic stuff
# going on for my taste so we'll instead use the closest generic linux targets.
case "$ndk_triple" in
	arm*)
	target=linux-armv4
	;;
	aarch64*)
	target=linux-aarch64
	;;
	i686*)
	target=linux-x86-clang
	;;
	x86_64*)
	target=linux-x86_64-clang
	;;
	*)
	exit 1
	;;
esac

args=(
	$target
	shared no-tests no-docs
	no-legacy no-engine # module stuff
	no-quic # unused, would require extra library (with curl)
	# obscure ciphers not needed for typical TLS/HTTPS usage
	no-{aria,camellia,cast,des,idea,md4,rc2,rc4,rmd160,srp,seed,sm2,sm3,sm4}
	no-autoload-config # there will be no config file
	--libdir=/usr/local/lib # make sure it doesn't pick "lib64"
)

../Configure "${args[@]}"

# we can't package libssl.so.<N> files, so strip out the soname (no better way to do this)
${SED:-sed} -re 's|(-soname=lib[^ ]+\.so)\.[0-9]+|\1|g' -i Makefile

make -j$cores
make DESTDIR="$prefix_dir" install_sw

# because our /usr/local is a symlink to / the paths must not include any ..
if pkg-config --libs openssl | grep -qF '../'; then
	echo >&2 "OpenSSL library path is wrong: $(pkg-config --libs openssl)"
	exit 1
fi

rm -f "$prefix_dir/lib/libcrypto.a" "$prefix_dir/lib/libssl.a"
