#!/bin/bash -e

cd "$( dirname "${BASH_SOURCE[0]}" )/../.."

aar=${1:-libmpv-android/build/outputs/aar/libmpv-android-release.aar}
[ -s "$aar" ] || { echo "Missing AAR: $aar" >&2; exit 1; }

entries=$(unzip -Z1 "$aar")
actual_abis=$(printf '%s\n' "$entries" | awk -F/ '/^jni\/[^\/]+\/.*\.so$/ {print $2}' | sort -u)
expected_abis=$(printf '%s\n' arm64-v8a armeabi-v7a)
[ "$actual_abis" = "$expected_abis" ] || {
	echo "Unexpected AAR ABI set" >&2
	printf 'Expected:\n%s\nActual:\n%s\n' "$expected_abis" "$actual_abis" >&2
	exit 1
}

for abi in arm64-v8a armeabi-v7a; do
	for library in \
		libavcodec.so libavdevice.so libavfilter.so libavformat.so libavutil.so \
		libc++_shared.so libmpv.so libplayer.so libswresample.so libswscale.so; do
		printf '%s\n' "$entries" | grep -qx "jni/$abi/$library" || {
			echo "Missing jni/$abi/$library" >&2
			exit 1
		}
	done
done

for arch in armv7l arm64; do
	config="buildscripts/prefix/ci-config/ffmpeg-${arch}.h"
	[ -f "$config" ] || { echo "Missing FFmpeg config: $config" >&2; exit 1; }
	grep -qx '#define CONFIG_IMAGE_PNG_PIPE_DEMUXER 0' "$config" || {
		echo "image_png_pipe is not disabled for $arch" >&2
		exit 1
	}
	grep -qx '#define CONFIG_PNG_DECODER 1' "$config" || {
		echo "PNG decoding is not enabled for $arch" >&2
		exit 1
	}
done

sha256_file="${aar}.sha256"
if command -v sha256sum >/dev/null 2>&1; then
	sha256sum "$aar" > "$sha256_file"
else
	shasum -a 256 "$aar" > "$sha256_file"
fi

echo "Verified dual-ABI AAR"
echo "- ABIs: armeabi-v7a, arm64-v8a"
echo "- image_png_pipe demuxer: disabled"
echo "- PNG decoder: enabled"
cat "$sha256_file"
