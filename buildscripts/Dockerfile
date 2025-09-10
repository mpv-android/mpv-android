FROM ubuntu:24.04

# Set up environment variables
ENV DEBIAN_FRONTEND=noninteractive
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_NDK_HOME=${ANDROID_HOME}/ndk-bundle
ENV PATH=${PATH}:${ANDROID_HOME}/tools:${ANDROID_HOME}/tools/bin:${ANDROID_HOME}/platform-tools

# Install required packages
RUN apt-get update && apt-get install -y \
    build-essential \
    curl \
    git \
    libtool \
    pkg-config \
    python3 \
    python3-pip \
    unzip \
    wget \
    openjdk-17-jdk \
    lld \
    clang \
    cmake \
    ninja-build \
    meson \
    autoconf \
    automake \
    libtool-bin \
    yasm \
    nasm \
    sudo \
    texinfo \
    help2man \
    gperf \
    autopoint \
    bison \
    flex \
    gettext \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

#required by mbedtls
RUN pip3 install --break-system-packages jsonschema jinja2

RUN mkdir -p /home/mpvbuilder/mpv-android
WORKDIR /home/mpvbuilder/mpv-android

COPY docker-entrypoint.sh /home/mpvbuilder/
RUN chmod +x /home/mpvbuilder/docker-entrypoint.sh

ENV HOME=/home/mpvbuilder
ENV PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
ENV PKG_CONFIG_PATH=/home/mpvbuilder/mpv-android/buildscripts/prefix/lib/pkgconfig

ENTRYPOINT ["/home/mpvbuilder/docker-entrypoint.sh"]
