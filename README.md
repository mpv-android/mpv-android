# mpv for Android

[![Build Status](https://github.com/mpv-android/mpv-android/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/mpv-android/mpv-android/actions/workflows/build.yml)

mpv-android is a video player for Android based on [libmpv](https://github.com/mpv-player/mpv).

## Features

* Hardware and software video decoding
* Gesture-based seeking, volume/brightness control and more
* libass support for styled subtitles
* Secondary (or dual) subtitle support
* Advanced video settings (interpolation, debanding, scalers, ...)
* Play network streams with the "Open URL" function
* Background playback, Picture-in-Picture, keyboard input supported

Note that mpv-android is *not* a library you can embed into your app, but you can look here for inspiration.
The important parts are [`MPVLib`](app/src/main/java/is/xyz/mpv/MPVLib.java), [`MPVView`](app/src/main/java/is/xyz/mpv/MPVView.kt) and the [native code](app/src/main/jni/).
libmpv/ffmpeg is built by [these scripts](buildscripts/).

## Downloads

You can download mpv-android from the [Releases section](https://github.com/mpv-android/mpv-android/releases) or

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="80">](https://play.google.com/store/apps/details?id=is.xyz.mpv)

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/is.xyz.mpv)

## Building from source

Take a look at [README.md](buildscripts/README.md) inside the `buildscripts` directory.
