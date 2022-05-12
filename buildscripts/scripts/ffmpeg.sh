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

cpu=armv7-a
[[ "$ndk_triple" == "aarch64"* ]] && cpu=armv8-a
[[ "$ndk_triple" == "x86_64"* ]] && cpu=generic
[[ "$ndk_triple" == "i686"* ]] && cpu="i686 --disable-asm"

cpuflags=
[[ "$ndk_triple" == "arm"* ]] && cpuflags="$cpuflags -mfpu=neon -mcpu=cortex-a8"

../configure \
	--target-os=android --enable-cross-compile --cross-prefix=$ndk_triple- --cc=$CC \
	--arch=${ndk_triple%%-*} --cpu=$cpu --enable-{jni,mediacodec,mbedtls,libdav1d} \
	--extra-cflags="-I$prefix_dir/include $cpuflags" --extra-ldflags="-L$prefix_dir/lib" \
	--disable-static --enable-shared --enable-{gpl,version3} \
	--pkg-config=pkg-config --disable-{stripping,doc,programs} \
  --disable-programs --enable-small --disable-runtime-cpudetect --disable-swscale-alpha \
  --disable-avdevice --enable-swresample --disable-swscale --disable-postproc --disable-w32threads --disable-os2threads --disable-network --disable-dct --disable-dwt --disable-error-resilience --disable-lsp --disable-mdct --disable-rdft --enable-fft --disable-faan --disable-pixelutils \
  --disable-filters --enable-hwaccels --disable-muxers --disable-parsers --disable-encoders --disable-bsfs --disable-protocols --enable-protocol=file --disable-indevs --disable-outdevs --disable-devices \
  --disable-alsa --disable-appkit --disable-avfoundation --disable-bzlib --disable-coreimage --disable-iconv --disable-lzma --disable-metal --disable-sndio --disable-schannel --disable-sdl2 --disable-securetransport --disable-vulkan --disable-xlib --disable-zlib \
  --disable-audiotoolbox --disable-amf --disable-cuda-llvm --disable-cuvid --disable-d3d11va --disable-dxva2 --disable-ffnvcodec --disable-nvdec --disable-nvenc --disable-vaapi --disable-vdpau --disable-v4l2-m2m --disable-videotoolbox --disable-debug

  # for an av1/opus capabilities only
  # --disable-demuxers --enable-demuxer=ogg,mov --disable-decoders --enable-decoder=mjpegb,movtext,opus,libdav1d \

make -j$cores
make DESTDIR="$prefix_dir" install
