#!/bin/bash -e

. ./include/depinfo.sh

. ./include/path.sh # load $os var

[ -z "$TRAVIS" ] && TRAVIS=0 # skip steps not required for CI?
[ -z "$WGET" ] && WGET=wget # possibility of calling wget differently 

if [ "$os" == "linux" ]; then
	hash yum &> /dev/null && sudo yum install zlib.i686 ncurses-libs.i686 bzip2-libs.i686 \
		autoconf m4 pkgconfig libtool
	apt-get -v &> /dev/null && [ $TRAVIS -eq 0 ] && \
		sudo apt-get install autoconf pkg-config libtool ninja-build python3-pip python3-setuptools && \
		sudo pip3 install meson

	os_ndk="linux"
elif [ "$os" == "mac" ]; then
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

	os_ndk="darwin"
fi

mkdir -p sdk && cd sdk

# android-sdk-$os
$WGET "https://dl.google.com/android/repository/commandlinetools-${os}-${v_sdk}.zip"
mkdir "android-sdk-${os}"
unzip -q -d "android-sdk-${os}" "commandlinetools-${os}-${v_sdk}.zip"
rm "commandlinetools-${os}-${v_sdk}.zip"
echo y | "./android-sdk-${os}/tools/bin/sdkmanager" "--sdk_root=${ANDROID_HOME}" \
	"platforms;android-28" "build-tools;${v_sdk_build_tools}" \
	"extras;android;m2repository" "platform-tools"

# android-ndk-$v_ndk
$WGET "http://dl.google.com/android/repository/android-ndk-${v_ndk}-${os_ndk}-x86_64.zip"
unzip -q "android-ndk-${v_ndk}-${os_ndk}-x86_64.zip"
rm "android-ndk-${v_ndk}-${os_ndk}-x86_64.zip"

# gas-preprocessor
mkdir -p bin
$WGET "https://github.com/FFmpeg/gas-preprocessor/raw/master/gas-preprocessor.pl" \
	-O bin/gas-preprocessor.pl
chmod +x bin/gas-preprocessor.pl

cd ..
