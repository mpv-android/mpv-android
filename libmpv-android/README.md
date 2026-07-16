# libmpv Android library

This module packages mpv-android's reusable Kotlin API, JNI bridge, assets,
and native runtime libraries as an Android AAR. It is intended for embedders
such as PlayBridge; the existing `app` module remains the upstream integration
harness.

The library preserves the upstream `MPVLib` JNI entry points and adds the
small source-compatibility surface currently required by PlayBridge:

- a Kotlin vararg `MPVLib.command` adapter;
- `MPVNode.None`; and
- node-valued property and event callback overloads.

The native build disables FFmpeg's `image_png_pipe` demuxer so a complete PNG
prefix cannot hide a following MPEG-TS payload. PNG decoding and encoding
remain enabled.

## Build

Prepare the pinned SDK, NDK, and native sources using the repository's normal
buildscript workflow. Then build each required ABI before packaging the AAR:

```shell
cd buildscripts
cores=4 ./buildall.sh --arch armv7l
cores=4 ./buildall.sh --arch arm64
```

Each native invocation also runs the Android build. Once all desired native
prefixes exist, the release AAR is written to:

```text
libmpv-android/build/outputs/aar/libmpv-android-release.aar
```

The AAR only contains ABIs whose `libmpv.so` exists under
`buildscripts/prefix/<abi>/lib`. Missing ABIs are deliberately omitted rather
than downloaded or replaced with prebuilt binaries.

CI pins moving Git dependencies, builds `armeabi-v7a` and `arm64-v8a`, checks
the generated FFmpeg feature configuration, and uploads the release AAR with
its SHA-256 file. Run the same artifact checks locally with:

```shell
buildscripts/include/verify-aar.sh
```
