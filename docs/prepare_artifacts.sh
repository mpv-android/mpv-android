#!/bin/bash -e
top=$PWD
if ! stat -t "$top"/app/build/outputs/apk/default/release/*-signed.apk &>/dev/null; then
	echo "Do a release build first" >&2
	exit 1
fi
output=$PWD/output
mkdir -p "$output/googleplay" "$output/github"
copy_apk () {
	[[ "$1" == *"-signed.apk" ]] || return 0
	local name=$(basename "$1")
	cp -vp "$1" "$output/$2/${name/-signed/}"
}
pushd "$top"/app/build/outputs/apk
for apk in api29/*/*-universal*.apk; do
	copy_apk "$apk" github
done
for apk in default/{release/*.apk,debug/*-universal*.apk}; do
	copy_apk "$apk" github
done
popd
pushd "$top"/app/build/outputs/bundle
cp -vp */*-signed.aab "$output/googleplay"
popd
pushd "$top"/app/src/main/obj/local
zip -9r "$output/github/debug-objs.zip" *
zip -9r "$output/googleplay/sym.zip" */*.so
popd
echo "All done, find the files in $output."
exit 0
