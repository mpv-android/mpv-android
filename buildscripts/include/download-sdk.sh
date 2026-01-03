#!/bin/bash -e

. ./include/depinfo.sh

. ./include/path.sh # load $os var

[ -z "$IN_CI" ] && IN_CI=0 # skip steps not required for CI?
[ -z "$WGET" ] && WGET=wget # possibility of calling wget differently

if [ "$os" == "linux" ]; then
	if [ $IN_CI -eq 0 ]; then
		if hash yum &>/dev/null; then
			sudo yum install autoconf pkgconfig libtool ninja-build \
				unzip wget meson python3
		elif apt-get -v &>/dev/null; then
			sudo apt-get install autoconf pkg-config libtool ninja-build \
				unzip wget meson python3
		else
			echo "Note: dependencies were not installed, you have to do that manually."
		fi
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
	if [ $IN_CI -eq 0 ]; then
		if ! hash brew 2>/dev/null; then
			echo "Error: brew not found. You need to install Homebrew: https://brew.sh/"
			exit 255
		fi
		brew install \
			automake autoconf libtool pkg-config \
			coreutils gnu-sed wget meson ninja python
	fi
	if ! javac -version &>/dev/null; then
		echo "Error: missing Java Development Kit. Install it manually."
		exit 255
	fi
fi

mkdir -p sdk && cd sdk

# Android SDK
if [ ! -d "android-sdk-${os}" ]; then
	echo "Android SDK not found. Downloading commandline tools."
	$WGET "https://dl.google.com/android/repository/commandlinetools-${os}-${v_sdk}.zip"
	mkdir "android-sdk-${os}"
	unzip -q -d "android-sdk-${os}" "commandlinetools-${os}-${v_sdk}.zip"
	rm "commandlinetools-${os}-${v_sdk}.zip"
fi
sdkmanager () {
	local exe="./android-sdk-$os/cmdline-tools/latest/bin/sdkmanager"
	[ -x "$exe" ] || exe="./android-sdk-$os/cmdline-tools/bin/sdkmanager"
	"$exe" --sdk_root="${ANDROID_HOME}" "$@"
}
echo y | sdkmanager \
	"platforms;android-${v_sdk_platform}" "build-tools;${v_sdk_build_tools}" \
	"extras;android;m2repository"

# Android NDK (either standalone or installed by SDK)
if [ -d "android-ndk-${v_ndk}" ]; then
	echo "Android NDK directory found."
elif [ -d "android-sdk-$os/ndk/${v_ndk_n}" ]; then
	echo "Creating NDK symlink to SDK."
	ln -s "android-sdk-$os/ndk/${v_ndk_n}" "android-ndk-${v_ndk}"
elif [ -z "${os_ndk}" ]; then
	echo "Downloading NDK with sdkmanager."
	echo y | sdkmanager "ndk;${v_ndk_n}"
	ln -s "android-sdk-$os/ndk/${v_ndk_n}" "android-ndk-${v_ndk}"
else
	echo "Downloading NDK."
	$WGET "http://dl.google.com/android/repository/android-ndk-${v_ndk}-${os_ndk}.zip"
	unzip -q "android-ndk-${v_ndk}-${os_ndk}.zip"
	rm "android-ndk-${v_ndk}-${os_ndk}.zip"
fi
if ! grep -qF "${v_ndk_n}" "android-ndk-${v_ndk}/source.properties"; then
	echo "Error: NDK exists but is not the correct version (expecting ${v_ndk_n})"
	exit 255
fi

# gas-preprocessor
mkdir -p bin
$WGET "https://github.com/FFmpeg/gas-preprocessor/raw/master/gas-preprocessor.pl" \
	-O bin/gas-preprocessor.pl
chmod +x bin/gas-preprocessor.pl

cd ..
