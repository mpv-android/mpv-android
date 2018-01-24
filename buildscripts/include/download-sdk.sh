#!/bin/bash -e

. ./include/depinfo.sh

. ./include/path.sh # load $os var

[ -z "$TRAVIS" ] && TRAVIS=0 # skip steps not required for CI?
[ -z "$WGET" ] && WGET=wget # possibility of calling wget differently 

if [ "$os" == "linux" ]; then
	hash yum &> /dev/null && sudo yum install zlib.i686 ncurses-libs.i686 bzip2-libs.i686 \
		autoconf m4 pkgconfig libtool
	apt-get -v &> /dev/null && [ $TRAVIS -eq 0 ] && \
		sudo apt-get install lib32z1 lib32ncurses5 lib32stdc++6 autoconf m4 pkg-config libtool

	sdk_ext="tgz"
	os_ndk="linux"
elif [ "$os" == "macosx" ]; then
	if ! hash brew 2>/dev/null; then
		echo "Error: brew not found. You need to install Homebrew: https://brew.sh/"
		exit 255
	fi
	brew install \
		automake autoconf libtool pkg-config \
		coreutils gnu-sed wget
	if ! java -version &>/dev/null; then
		echo "Error: missing Java 8 runtime. Manually install it or use:"
		echo "\$ brew tap caskroom/versions"
		echo "\$ brew cask install java8"
		exit 255
	fi

	sdk_ext="zip"
	os_ndk="darwin"
fi

mkdir -p sdk && cd sdk

# android-sdk-linux
if [ $TRAVIS -eq 1 ]; then
	:
elif [ "$sdk_ext" == "tgz" ]; then
	$WGET "http://dl.google.com/android/android-sdk_${v_sdk}-${os}.${sdk_ext}" -O - | \
		tar -xz -f -
elif [ "$sdk_ext" == "zip" ]; then
	$WGET "http://dl.google.com/android/android-sdk_${v_sdk}-${os}.${sdk_ext}"
	unzip -q "android-sdk_${v_sdk}-${os}.${sdk_ext}"
	rm "android-sdk_${v_sdk}-${os}.${sdk_ext}"
fi
[ $TRAVIS -eq 0 ] && \
"./android-sdk-${os}/tools/android" update sdk --no-ui --all --filter \
	build-tools-27.0.3,android-27,extra-android-m2repository,platform-tools

# android-ndk-$v_ndk
$WGET "http://dl.google.com/android/repository/android-ndk-${v_ndk}-${os_ndk}-x86_64.zip"
unzip -q "android-ndk-${v_ndk}-${os_ndk}-x86_64.zip"
rm "android-ndk-${v_ndk}-${os_ndk}-x86_64.zip"

# ndk-toolchain
cd "android-ndk-${v_ndk}"
toolchain_api=21
./build/tools/make_standalone_toolchain.py \
	--arch arm --api $toolchain_api \
	--install-dir `pwd`/../ndk-toolchain
if [ $TRAVIS -eq 0 ]; then
	./build/tools/make_standalone_toolchain.py \
		--arch arm64 --api $toolchain_api \
		--install-dir `pwd`/../ndk-toolchain-arm64
	./build/tools/make_standalone_toolchain.py \
		--arch x86_64 --api $toolchain_api \
		--install-dir `pwd`/../ndk-toolchain-x64
fi
for tc in ndk-toolchain{,-arm64,-x64}; do
	[ ! -d ../$tc ] && continue
	pushd ../$tc

	rm -rf bin/py* lib/{lib,}py* # remove python because it can cause breakage
	# add gas-preprocessor.pl for ffmpeg + clang on ARM
	$WGET "https://git.libav.org/?p=gas-preprocessor.git;a=blob_plain;f=gas-preprocessor.pl;hb=HEAD" \
		-O bin/gas-preprocessor.pl
	chmod +x bin/gas-preprocessor.pl
	# make wrapper to pass api level to gcc (due to Unified Headers)
	exe=`echo bin/*-linux-android*-gcc`
	mv $exe{,.real}
	printf '#!/bin/sh\nexec $0.real -D__ANDROID_API__=%s "$@"\n' $toolchain_api >$exe
	chmod +x $exe

	popd
done
cd ..

cd ..
