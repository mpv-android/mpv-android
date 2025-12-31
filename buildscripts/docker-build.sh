#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "building Docker image..."
docker build -t mpv-android-builder "$SCRIPT_DIR"

echo "starting the container..."
# forward all arguments
docker run --rm \
    -v "$PROJECT_DIR:/home/mpvbuilder/mpv-android" \
    --env-file <(cat <<EOF
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ANDROID_HOME=/opt/android-sdk
ANDROID_NDK_HOME=/opt/android-sdk/ndk-bundle
EOF
) \
    mpv-android-builder "$@"