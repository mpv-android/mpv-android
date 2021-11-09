#!/bin/bash -e

. ../../include/path.sh


#setup architecture to ABI mapping
declare -A arch2abi
arch2abi=(['armv7l']='armeabi-v7a' ['arm64']='arm64-v8a' ['x86']='x86' ['x86_64']='x86_64')

function run_cmake () {
    
    if [[ ! -f build/Makefile ]]; then
        
        cmake \
            -DCMAKE_SYSTEM_NAME=Android \
            -DCMAKE_ANDROID_ARCH_ABI="${arch2abi[$arch]}" \
            -DCMAKE_ANDROID_NDK="$ANDROID_NDK" \
            -DCMAKE_INSTALL_PREFIX="$prefix_dir" \
            -DCMAKE_C_FLAGS="-I${prefix_dir}/include" \
            -DCMAKE_SHARED_LINKER_FLAGS="-L${prefix_dir}/lib" \
            -DBUILD_TESTS=OFF -D BUILD_STATIC_LIBS=OFF -B build
    fi
}

function build_libmysofa () {
    run_cmake
    cmake  --build build -v --target all install
}

function clean_libmysofa () {
    if [[ -f build/Makefile ]]; then
        rm -rf build
    fi
   
}


if [ "$1" == "build" ]; then
    build_libmysofa
elif [ "$1" == "clean" ]; then
	clean_libmysofa
	exit 0
else
	exit 255
fi





