# Building

## Download dependencies

`download.sh` will take care of installing the Android SDK/NDK and downloading the sources.

```
./download.sh
```

## Build

```
./buildall.sh
```

Run `buildall.sh` with `--clean` to clean the build directories before building.
Additionally `--clang` is supported to switch to Clang for compilation.

Building for just 32-bit ARM (which is the default) is fine generally.
However if you want to make use of AArch64 or are targeting Intel x86 devices,
these architectures can be optionally be built into the same APK.

To do this run one (or both) of these commands **before** ./buildall.sh:
```
./buildall.sh --arch arm64 mpv
./buildall.sh --arch x86_64 mpv
```

# Developing

## Getting logs

```
adb logcat # get all logs, useful when drivers/vendor libs output to logcat
adb logcat -s "mpv" # get only mpv logs
```

## Rebuilding a single component

If you've made changes to a single component (e.g. ffmpeg or mpv) and want a new build you can of course just run ./buildall.sh but it's also possible to just build a single component like this:

```
./buildall.sh --no-deps ffmpeg
# optional: add --clean to build from a clean state
```

Note that you might need to be rebuild for other architectures (`--arch`) too depending on your device.

Afterwards, build mpv-android and install the apk:

```
./buildall.sh --no-deps mpv-android
adb install -r ../app/build/outputs/apk/debug/app-debug.apk
```

## Using Android Studio

You can use Android Studio to develop the Java part of the codebase. Before using it, make sure to build the project at least once by following the steps in the **Build** section.

You should point Android Studio to existing SDK installation at `mpv-android/buildscripts/sdk/android-sdk-linux`. Then click "Open an existing Android Studio project" and select `mpv-android`.

If Android Studio complains about project sync failing (`Error:Exception thrown while executing model rule: NdkComponentModelPlugin.Rules#createNativeBuildModel`), go to "File -> Project Structure -> SDK Location" and set "Android NDK Location" to `mpv-android/buildscripts/sdk/android-ndk-rVERSION`.

Note that if you build from Android Studio only the Java part will be built. If you make any changes to libraries (ffmpeg, mpv, ...) or mpv-android native code (`app/src/main/jni/*`), first rebuild native code with:

```
./buildall.sh --no-deps mpv-android
```

then build the project from Android Studio.

Also, debugging native code does not work from within the studio at the moment, you will have to use gdb for that.

## Debugging native code with gdb

You first need to rebuild mpv-android with gdbserver support:

```
NDK_DEBUG=1 ./buildall.sh --no-deps mpv-android
adb install -r ../app/build/outputs/apk/debug/app-debug.apk
```

After that, ndk-gdb can be used to debug the app:

```
cd mpv-android/app/src/main/
../../../buildscripts/sdk/android-ndk-r*/ndk-gdb --launch
```

# Credits, notes, etc

Travis will create prebuilt prefixes whenever needed, see `build_prefix()` in `.travis.sh`.
These prefixes contain everything except mpv built for `armv7l` and are uploaded [here](https://github.com/mpv-android/prebuilt-prefixes/releases).

These build scripts were created by @sfan5, thanks!

