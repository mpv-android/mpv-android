# Building

Compiling the native parts is a process separate from Gradle and the app won't work if you skip this.

This process is supported on Linux and macOS. Windows (or WSL) will **not** work.

## AVS (AVS1/AVS2/AVS3) support

This fork of the build scripts replaces upstream FFmpeg with
[FFmpeg-AVSPlus](https://github.com/xeonliu/FFmpeg-AVSPlus)
(`release/8.0.1-avsplus`), so the resulting `mpv-android` can decode the AVS
family of video standards in addition to the usual formats:

- **AVS1-P2 / AVS1-P16** — built into FFmpeg-AVSPlus as the native `cavs` decoder (no extra library).
- **AVS2-P2 (IEEE 1857.4)** — decoded via the external `libdavs2` library.
- **AVS3-P2 (IEEE 1857.10)** — decoded via the external `libuavs3d` library.

Two new dependencies are pulled in automatically by `download.sh`:

- `uavs3d` — built with the NDK's CMake toolchain (needs `cmake` on the build host).
- `davs2` — built with its own x264-style `configure`. Pinned to the
  [saindriches/davs2](https://github.com/saindriches/davs2) fork (the same
  source [FFmpeg-AVS-Builds](https://github.com/xeonliu/FFmpeg-AVS-Builds)
  uses), because stock pkuvcl/davs2 cannot build in 10-bit mode.

Both derive their version number from `git` at configure time (`davs2` from
`git rev-list origin/master`, `uavs3d` from `git rev-list HEAD`) and FFmpeg's
`configure` checks those versions through pkg-config, so they are cloned in
full instead of shallow.

Only decoding is enabled (AVS encoders are out of scope for `mpv-android`, which
disables encoders anyway). AVS2 is built with `--bit-depth=10`: an 8 bit build
rejects AVS2 Main10 streams outright ("Un-supported bit-depth"), while a 10 bit
build still decodes 8 bit streams and reports the correct pixel format.
`davs2` is built with `--disable-asm` because it has no arm/aarch64 assembly
(the same reason upstream's CI disables it on arm64) — AVS2 decoding is plain C
on ARM. `uavs3d` does ship arm/aarch64 NEON code and uses it.

## ffmpeg patches

`include/download-deps.sh` applies every patch in `patches/` to the FFmpeg
checkout right after it is cloned (idempotently, so re-running is safe):

- `dra-audio-decoder-8.0.1-avsplus.patch` — DRA (GB/T 22726-2008, the DTMB
  digital terrestrial TV audio standard) support. A native `libdra` decoder,
  ported from the upstream `feat/dra-audio-decoder` branch, not merged into
  the FFmpeg-AVSPlus branches yet. Pure C with no external dependencies, so
  no extra build steps or `--enable` flags are needed — `libdra` is
  registered through the usual decoder list and enabled by default.
- `cavsdec-fix-null-deref-invalid-bitreader.patch` — upstream 9c2aabaa3:
  rejects invalid start-code bitreader sizes in the cavs decoder (fixes a
  NULL pointer dereference, e.g. during stream probing of broadcast TS
  captures).
- `cavs-fix-missing-ref-mc.patch` — upstream d85cdd259: checks the reference
  frame before pointer arithmetic in cavs motion compensation (broadcast
  streams opened mid-GOP reference frames that are not in the file).

Bump `v_ffmpeg_patch` in `include/depinfo.sh` when touching any of the
patches to invalidate the CI prefix cache.

## Download dependencies

`download.sh` will take care of installing the Android SDK, NDK and downloading the sources.

If you're running on Debian/Ubuntu or RHEL/Fedora it will also install the necessary dependencies for you.

```sh
./download.sh
```

If you already have the Android SDK installed you can symlink `android-sdk-linux` to your SDK root
before running the script and the necessary SDK packages will still be installed.

A matching NDK version (inside the SDK) will be picked up automatically or downloaded and installed otherwise.

## Build

```sh
./buildall.sh
```

Run `buildall.sh` with `--clean` to clean the build directories before building.
For a guaranteed clean build also run `rm -rf prefix` beforehand.

By default this will build only for 32-bit ARM (`armv7l`).
You probably want to build for AArch64 too, and perhaps Intel x86.

To do this run one (or more) of these commands **before** ./buildall.sh:
```sh
./buildall.sh --arch arm64 mpv
./buildall.sh --arch x86 mpv
./buildall.sh --arch x86_64 mpv
```

# Developing

## Getting logs

```sh
adb logcat # get all logs, useful when drivers/vendor libs output to logcat
adb logcat -s mpv # get only mpv logs
```

## Rebuilding a single component

If you've made changes to a single component (e.g. ffmpeg or mpv) and want a new build you can of course just run ./buildall.sh but it's also possible to just build a single component like this:

```sh
./buildall.sh -n ffmpeg
# optional: add --clean to build from a clean state
```

Note that you might need to rebuild for other architectures (`--arch`) too depending on your device.

Afterwards, build mpv-android and install the apk:

```sh
./buildall.sh -n
adb install -r ../app/build/outputs/apk/default/debug/app-default-universal-debug.apk
```

## Using Android Studio

You can use Android Studio to develop the Java part of the codebase. Before using it, make sure to build the project at least once by following the steps in the **Build** section.

You should point Android Studio to existing SDK installation at `mpv-android/buildscripts/sdk/android-sdk-linux`.
Then click "Open an existing Android Studio project" and select `mpv-android`.

Note that if you build from Android Studio only the Java/Kotlin part will be built.
If you make any changes to libraries (ffmpeg, mpv, ...) or mpv-android native code (`app/src/main/jni/*`), first rebuild native code with:

```sh
./buildall.sh -n
```

then build the project from Android Studio.
