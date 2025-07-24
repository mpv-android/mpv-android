#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

usage() {
    echo "Usage: docker-build.sh [options] [command]"
    echo ""
    echo "Commands:"
    echo "  build       - Download dependencies and build the project"
    echo "  download    - Download dependencies (optionally specify which ones)"
    echo "  clean       - Clean all build directories"
    echo "  build-clean - Clean build directories and then build"
    echo "  shell       - Start an interactive shell"
    echo "  help        - Show this help message"
    echo ""
    echo "Options:"
    echo "  --arch <arch>         Build for specified architecture (armv7l, arm64, x86, x86_64)"
    echo "  --clean               Clean build dirs before compiling"
    echo "  --gcc                 Use gcc compiler (unsupported!)"
    echo "  -n, --no-deps         Do not build dependencies"
    echo "  -i, --interactive     Start shell after command completion"
    echo ""
    echo "Examples:"
    echo "  docker-build.sh build --arch arm64"
    echo "  docker-build.sh download mbedtls ffmpeg"
    echo "  docker-build.sh build-clean --arch armv7l -i"
    exit 0
}

docker_args=()
interactive_mode=false

for arg in "$@"; do
    if [[ "$arg" == "-h" || "$arg" == "--help" ]]; then
        usage
    elif [[ "$arg" == "-i" || "$arg" == "--interactive" ]]; then
        interactive_mode=true
        docker_args+=("$arg")
    else
        docker_args+=("$arg")
    fi
done

echo "Building Docker image..."
docker build -t mpv-android-builder "$SCRIPT_DIR"

echo "Starting Docker container with mpv-android project mounted..."

if [ "$interactive_mode" = true ]; then
    docker_flags="-it"
else
    docker_flags=""
fi

docker run $docker_flags --rm \
    -v "$PROJECT_DIR:/home/mpvbuilder/mpv-android" \
    --env-file <(cat <<EOF
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ANDROID_HOME=/opt/android-sdk
ANDROID_NDK_HOME=/opt/android-sdk/ndk-bundle
EOF
) \
    mpv-android-builder "${docker_args[@]}"
