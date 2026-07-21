#!/bin/bash -e

. ../../include/path.sh

build=_build$ndk_suffix

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf $build
	exit 0
else
	exit 255
fi

mkdir -p $build
cd $build

../configure \
	--host=$ndk_triple --with-mbedtls="$prefix_dir" --without-libpsl \
	--disable-shared --enable-static --disable-debug \
	--disable-{manual,docs,ares,unix-sockets,tls-srp,doh} \
	--disable-{rtsp,dict,telnet,tftp,pop3,imap,smb,smtp,gopher,mqtt,ntlm}

make -j$cores
make DESTDIR="$prefix_dir" install
