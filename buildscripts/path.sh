#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

os=linux
[[ "$OSTYPE" == "darwin"* ]] && os=macosx
export os

if [ "$os" == "macosx" ]; then
	# various things rely on GNU behaviour
	export INSTALL=`which ginstall`
	export SED=gsed
fi

export PATH="$DIR/sdk/ndk-toolchain$ndk_suffix/bin:$DIR/sdk/android-ndk-r16b:$DIR/sdk/android-sdk-$os/tools:$PATH"
export ANDROID_HOME="$DIR/sdk/android-sdk-$os"
