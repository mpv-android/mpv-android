git clean -fdx -e buildscripts
cd buildscripts
git clean -fdx -e deps -e sdk -e dist.zip

cd deps/dav1d
git clean -fdx
git reset --hard

cd ../elf-cleaner
git clean -fdx
git reset --hard

cd ../ffmpeg
git clean -fdx
git reset --hard

cd ../freetype2
git clean -fdx
git submodule foreach --recursive git clean -xfd
git reset --hard
git submodule foreach --recursive git reset --hard

cd ../libass
git clean -fdx
git reset --hard

cd ../libplacebo
git clean -fdx
git submodule foreach --recursive git clean -xfd
git reset --hard
git submodule foreach --recursive git reset --hard

cd ../mpv
git clean -fdx
git reset --hard

cd ..
git clean -fdx -e dav1d -e elf-cleaner -e ffmpeg -e freetype2 -e libass -e mpv -e "*.tar.gz" -e "*.tar.xz" -e libplacebo
