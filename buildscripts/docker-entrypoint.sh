#!/bin/bash
set -e

cd /home/mpvbuilder/mpv-android/buildscripts

case "${1:-}" in
    "shell")
        exec /bin/bash
        ;;
    "download")
        shift
        exec ./download.sh "$@"
        ;;
    "build"|"")
        if [ "${1:-}" = "build" ]; then
            shift
        fi
        exec ./buildall.sh "$@"
        ;;
    *)
        exec ./buildall.sh "$@"
        ;;
esac