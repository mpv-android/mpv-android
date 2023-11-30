#!/bin/bash -e
top=$PWD

. "$top"/buildscripts/include/depinfo.sh

commit_hash () {
	git -C "$1" rev-parse --verify HEAD
}

pushd "$top"/buildscripts/deps
lines=(
	"[...]"
	"## Full set of build dependencies"
	"* Android NDK $v_ndk"
	"* mbedtls $v_mbedtls"
	"* dav1d videolan/dav1d@$(commit_hash dav1d)"
	"* ffmpeg ffmpeg/ffmpeg@$(commit_hash ffmpeg)"
	"* freetype ${v_freetype//-/.}"
	"* fribidi $v_fribidi"
	"* harfbuzz $v_harfbuzz"
	"* libunibreak $v_unibreak"
	"* libass libass/libass@$(commit_hash libass)"
	"* lua $v_lua"
	"* libplacebo haasn/libplacebo@$(commit_hash libplacebo)"
	"* libmpv mpv-player/mpv@$(commit_hash mpv)"
	"* mpv-android $(commit_hash ../..)"
	""
	"[...]"
)
popd

printf '%s\n' "${lines[@]}"
exit 0
