#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION=9.3.1

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/neo-wrapper/$GRADLE_VERSION"
DIST_DIR="$CACHE_DIR/gradle-$GRADLE_VERSION"
ZIP="$CACHE_DIR/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$DIST_DIR/bin/gradle" ]; then
  mkdir -p "$CACHE_DIR"
  if [ ! -f "$ZIP" ]; then
    echo "Downloading Gradle $GRADLE_VERSION..."
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL --retry 3 -o "$ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    elif command -v wget >/dev/null 2>&1; then
      wget -q -O "$ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    else
      echo "curl or wget is required to bootstrap Gradle." >&2
      exit 1
    fi
  fi
  rm -rf "$DIST_DIR"
  unzip -q "$ZIP" -d "$CACHE_DIR"
fi

exec "$DIST_DIR/bin/gradle" "$@"
