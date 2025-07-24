#!/bin/bash
set -e

echo "== mpv-android Build Environment =="

interactive_requested=false
filtered_args=()

for arg in "$@"; do
    if [[ "$arg" == "-i" || "$arg" == "--interactive" ]]; then
        interactive_requested=true
    else
        filtered_args+=("$arg")
    fi
done

set -- "${filtered_args[@]}"

INTERACTIVE=0
if [ "$interactive_requested" = true ]; then
    INTERACTIVE=1
fi

clean_builds() {
    echo "Cleaning build directories..."
    cd /home/mpvbuilder/mpv-android/buildscripts/deps

    for dir in */; do
        if [ -d "${dir}" ]; then
            echo "Cleaning ${dir}..."
            rm -rf "${dir}/_build" "${dir}/_build-arm64" "${dir}/_build-x64" "${dir}/_build-x86"
        fi
    done

    echo "Build directories cleaned."
}

download_deps() {
    cd /home/mpvbuilder/mpv-android/buildscripts

    if [ $# -eq 0 ]; then
        echo "Downloading all dependencies..."
        ./download.sh
    else
        echo "Downloading specific dependencies: $@"
        ./include/download-sdk.sh

        . ./include/depinfo.sh

        [ -z "$WGET" ] && WGET=wget
        mkdir -p deps && cd deps

        for dep in "$@"; do
            case "$dep" in
                mbedtls)
                    if [ ! -d mbedtls ]; then
                        echo "Downloading mbedtls..."
                        mkdir mbedtls
                        $WGET https://github.com/Mbed-TLS/mbedtls/releases/download/mbedtls-$v_mbedtls/mbedtls-$v_mbedtls.tar.bz2 -O - | \
                            tar -xj -C mbedtls --strip-components=1
                    fi
                    ;;
                dav1d)
                    if [ ! -d dav1d ]; then
                        echo "Downloading dav1d..."
                        git clone https://github.com/videolan/dav1d
                    fi
                    ;;
                ffmpeg)
                    if [ ! -d ffmpeg ]; then
                        echo "Downloading ffmpeg..."
                        git clone https://github.com/FFmpeg/FFmpeg ffmpeg
                        [ ! -z "$IN_CI" ] && [ $IN_CI -eq 1 ] && git -C ffmpeg checkout $v_ci_ffmpeg
                    fi
                    ;;
                freetype2)
                    if [ ! -d freetype2 ]; then
                        echo "Downloading freetype2..."
                        git clone --recurse-submodules https://gitlab.freedesktop.org/freetype/freetype.git freetype2 -b VER-${v_freetype//./-}
                    fi
                    ;;
                fribidi)
                    if [ ! -d fribidi ]; then
                        echo "Downloading fribidi..."
                        mkdir fribidi
                        $WGET https://github.com/fribidi/fribidi/releases/download/v$v_fribidi/fribidi-$v_fribidi.tar.xz -O - | \
                            tar -xJ -C fribidi --strip-components=1
                    fi
                    ;;
                harfbuzz)
                    if [ ! -d harfbuzz ]; then
                        echo "Downloading harfbuzz..."
                        mkdir harfbuzz
                        $WGET https://github.com/harfbuzz/harfbuzz/releases/download/$v_harfbuzz/harfbuzz-$v_harfbuzz.tar.xz -O - | \
                            tar -xJ -C harfbuzz --strip-components=1
                    fi
                    ;;
                unibreak)
                    if [ ! -d unibreak ]; then
                        echo "Downloading unibreak..."
                        mkdir unibreak
                        $WGET https://github.com/adah1972/libunibreak/releases/download/libunibreak_${v_unibreak//./_}/libunibreak-${v_unibreak}.tar.gz -O - | \
                            tar -xz -C unibreak --strip-components=1
                    fi
                    ;;
                libass)
                    if [ ! -d libass ]; then
                        echo "Downloading libass..."
                        git clone https://github.com/libass/libass
                    fi
                    ;;
                lua)
                    if [ ! -d lua ]; then
                        echo "Downloading lua..."
                        mkdir lua
                        $WGET https://www.lua.org/ftp/lua-$v_lua.tar.gz -O - | \
                            tar -xz -C lua --strip-components=1
                    fi
                    ;;
                libplacebo)
                    if [ ! -d libplacebo ]; then
                        echo "Downloading libplacebo..."
                        git clone --recursive https://github.com/haasn/libplacebo
                    fi
                    ;;
                mpv)
                    if [ ! -d mpv ]; then
                        echo "Downloading mpv..."
                        git clone https://github.com/mpv-player/mpv
                    fi
                    ;;
                *)
                    echo "Unknown dependency: $dep"
                    echo "Available dependencies: mbedtls, dav1d, ffmpeg, freetype2, fribidi, harfbuzz, unibreak, libass, lua, libplacebo, mpv"
                    ;;
            esac
        done
        cd ..
    fi
}

build_project() {
    local arch_args=()
    local buildall_args=()
    local target="mpv-android"

    while [ $# -gt 0 ]; do
        case "$1" in
            --arch)
                shift
                arch_args+=(--arch "$1")
                ;;
            --clean)
                buildall_args+=(--clean)
                ;;
            --gcc)
                buildall_args+=(--gcc)
                ;;
            -n|--no-deps)
                buildall_args+=(--no-deps)
                ;;
            armv7l|arm64|x86|x86_64|mpv-android|mbedtls|dav1d|ffmpeg|freetype2|fribidi|harfbuzz|unibreak|libass|lua|libplacebo|mpv)
                target="$1"
                ;;
        esac
        shift
    done

    echo "Downloading dependencies..."
    cd /home/mpvbuilder/mpv-android/buildscripts
    ./download.sh

    echo "Building mpv-android..."

    if [ ${#arch_args[@]} -eq 0 ]; then
        # build all if not specified
        ./buildall.sh "${buildall_args[@]}" --arch armv7l mpv
        ./buildall.sh "${buildall_args[@]}" --arch arm64 mpv
        ./buildall.sh "${buildall_args[@]}" --arch x86 mpv
        ./buildall.sh "${buildall_args[@]}" --arch x86_64 mpv
        ./buildall.sh "${buildall_args[@]}" "$target"
    else
        # build only for what's been specified
        ./buildall.sh "${buildall_args[@]}" "${arch_args[@]}" "$target"
    fi

    echo "Build completed successfully!"
    echo "The build artifacts should be available in the project directory."
}

if [ $# -eq 0 ]; then
    echo "No command specified. Run with 'help' to see available commands."
    exit 1
else
    case "$1" in
        build)
            shift
            build_project "$@"
            ;;
        download)
            shift
            download_deps "$@"
            ;;
        clean)
            clean_builds
            ;;
        build-clean)
            shift
            clean_builds
            build_project "$@"
            ;;
        shell)
            exec /bin/bash
            ;;
        help)
            echo "Available commands: build, download, clean, build-clean, shell, help"
            echo "Run docker-build.sh --help for usage information."
            ;;
        *)
            echo "Unknown command: $1"
            echo "Available commands: build, download, clean, build-clean, shell, help"
            exit 1
            ;;
    esac
fi

# Start a shell if interactive, good for testing after builds
if [ $INTERACTIVE -eq 1 ] && [ "$1" != "shell" ]; then
    echo "Build process completed. Starting a shell for further interaction."
    exec /bin/bash
fi
