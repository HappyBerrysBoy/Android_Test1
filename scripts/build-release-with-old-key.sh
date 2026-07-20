#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OLD_KEYSTORE="$ROOT_DIR/UnitLoadSystemKey.key.jks"
ALIAS="unnitloadsystemkey"
AAB="$ROOT_DIR/app/build/outputs/bundle/release/app-release.aab"

[ -f "$OLD_KEYSTORE" ] || {
  printf "[error] old keystore not found: %s\n" "$OLD_KEYSTORE" >&2
  exit 1
}

read_secret() {
  local prompt="$1"
  local var_name="$2"
  local value
  printf "%s" "$prompt" >&2
  IFS= read -r -s value
  printf "\n" >&2
  printf -v "$var_name" '%s' "$value"
}

read -r -p "Alias [$ALIAS]: " INPUT_ALIAS
ALIAS="${INPUT_ALIAS:-$ALIAS}"

read_secret "Old keystore password: " STOREPASS
printf "If the key password is the same as the keystore password, press Enter.\n"
read_secret "Old key password: " KEYPASS
if [ -z "$KEYPASS" ]; then
  KEYPASS="$STOREPASS"
fi

ANDROID_RELEASE_STORE_FILE="$OLD_KEYSTORE" \
ANDROID_RELEASE_STORE_PASSWORD="$STOREPASS" \
ANDROID_RELEASE_KEY_ALIAS="$ALIAS" \
ANDROID_RELEASE_KEY_PASSWORD="$KEYPASS" \
  "$ROOT_DIR/gradlew" -p "$ROOT_DIR" clean bundleRelease

jarsigner -verify -verbose -certs "$AAB" | tail -45

printf "\nCreated old-key signed AAB:\n  %s\n" "$AAB"
