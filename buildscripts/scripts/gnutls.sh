#!/bin/bash -e

. ../../path.sh

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf _build$dir_suffix
	exit 0
else
	exit 255
fi

mkdir -p _build$dir_suffix
cd _build$dir_suffix

extra=
[[ "$ndk_triple" == "aarch64"* && "$CC" == *"clang" ]] \
	&& extra="--disable-hardware-acceleration"

../configure \
	--host=$ndk_triple $extra \
	--enable-static --disable-shared \
	--with-nettle-mini --with-included-{libtasn1,unistring} \
	--disable-{doc,tools,cxx,tests} --without-p11-kit

make -j6
make DESTDIR="`pwd`/../../../prefix$dir_suffix" install
# fix linking (pkg-config seems to ignore Requires.private)
${SED:-sed} '/^Libs:/ s|$| -lnettle -lhogweed|' -i \
	../../../prefix$dir_suffix/lib/pkgconfig/gnutls.pc
