Release Process
===============

1. Edit build.gradle, increase versionCode, set versionName to `YYYY-MM-DD-release`, commit
2. Clone this commit to a clean working folder
3. Go through buildscripts setup steps
4. Build libmpv for all architectures
5. Build app via `./buildall.sh --clean -n`
   Make sure to set `ANDROID_SIGNING_KEY=... ANDROID_SIGNING_ALIAS=mpv-android-release` before.
6. Install APK on devices you have on hand, verify that core features work as expected
7. Run `docs/prepare_artifacts.sh`
8. Create Github release with tag name `YYYY-MM-DD`, upload relevant APKs (don't publish yet)
9. Run `docs/print_releasenotes.sh`, copy into release notes
10. Write a changelog (to be put in GH *and* Play Store)
11. Create release in Google Play Console, upload app bundle and symbol ZIP
    If any checks fail for the release they will have to be fixed first, restarting the process.
12. Push commit from step 1
13. Publish Github release & tag
14. Start rollout on Google Play

Should it be necessary to release more than once a day subsequent releases are named e.g. `2021-04-12-2`.
