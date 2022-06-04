#!/bin/bash -e

. ./include/depinfo.sh

. ./include/path.sh # load $os var

[ -z "$TRAVIS" ] && TRAVIS=0 # skip steps not required for CI?
[ -z "$WGET" ] && WGET=wget # possibility of calling wget differently

if [ "$os" == "linux" ]; then
	if [ $TRAVIS -eq 0 ]; then
		hash yum &>/dev/null && {
			sudo yum install autoconf pkgconfig libtool ninja-build \
			python3-pip python3-setuptools unzip wget;
			sudo pip3 install meson; }
		apt-get -v &>/dev/null && {
			sudo apt-get install autoconf pkg-config libtool ninja-build \
			python3-pip python3-setuptools unzip;
			sudo pip3 install meson; }
	fi

	if ! javac -version &>/dev/null; then
		echo "Error: missing Java Development Kit."
		hash yum &>/dev/null && \
			echo "Install it using e.g. sudo yum install java-latest-openjdk-devel"
		apt-get -v &>/dev/null && \
			echo "Install it using e.g. sudo apt-get install default-jre-headless"
		exit 255
	fi

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

	echo "FIXME: NDK installation. Exiting."; exit 1
fi

mkdir -p sdk && cd sdk

# android-sdk-$os
$WGET "https://dl.google.com/android/repository/commandlinetools-${os}-${v_sdk}.zip"
mkdir "android-sdk-${os}"
unzip -q -d "android-sdk-${os}" "commandlinetools-${os}-${v_sdk}.zip"
rm "commandlinetools-${os}-${v_sdk}.zip"
echo y | "./android-sdk-${os}/cmdline-tools/bin/sdkmanager" "--sdk_root=${ANDROID_HOME}" \
	"platforms;android-30" "build-tools;${v_sdk_build_tools}" \
	"extras;android;m2repository" "platform-tools"

# android-ndk-$v_ndk
$WGET "http://dl.google.com/android/repository/android-ndk-${v_ndk}-${os_ndk}.zip"
unzip -q "android-ndk-${v_ndk}-${os_ndk}.zip"
rm "android-ndk-${v_ndk}-${os_ndk}.zip"

# gas-preprocessor
mkdir -p bin
$WGET "https://github.com/FFmpeg/gas-preprocessor/raw/master/gas-preprocessor.pl" \
	-O bin/gas-preprocessor.pl
chmod +x bin/gas-preprocessor.pl

cd ..
