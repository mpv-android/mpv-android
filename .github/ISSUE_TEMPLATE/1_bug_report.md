---
name: 'Bug Report'
about: 'Create a report about a bug or problem'
title: ''
labels: ''
assignees: ''
---

### Important information

Android version:

mpv-android version:
<!-- You can find this under Settings > Advanced > Version information -->
<!-- If you have compiled mpv-android yourself instead of using one of the release builds, MAKE SURE TO MENTION THIS. -->

Which version of mpv-android introduced the problem (if known):

### Description

Describe your issue in detail here.

If the circumstances in which it appears are not obvious, provide step-by-step instructions that can be reliably used to reproduce the issue.

### Log output

The Android SDK includes a tool named [`logcat`](https://developer.android.com/studio/command-line/logcat) that is used to view application and system log messages on device.
You will need a computer to view these logs.
Logs pertaining to mpv can be collected like this:

	adb logcat -s 'mpv:*' '*:F'

You can attach text files on Github directly or use sites like https://0x0.st/ to upload your logs.
Depending on the nature of the bug, a log file might not be required and you can *omit this section*. If in doubt provide one, it will help us find possible issues later.

### Additional information

If your issue is only reproducible with a specific (type of) file, provide a short sample file that can be used to reproduce the problem.
You can again use https://0x0.st/ or other cloud storage to share it.

If your issue relates to a certain set of configuration options, provide a full copy of your `mpv.conf` below.

If no additional information is provided please *delete this section*.
