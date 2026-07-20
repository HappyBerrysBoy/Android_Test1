#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OLD_KEYSTORE="$ROOT_DIR/UnitLoadSystemKey.key.jks"
OUTPUT_DIR="$ROOT_DIR/keystore/play-signing-export"
DEFAULT_PEPK_JAR="$ROOT_DIR/keystore/pepk.jar"
DEFAULT_ENCRYPTION_KEY="$ROOT_DIR/keystore/encryption_public_key.pem"
OUTPUT_FILE="$OUTPUT_DIR/encrypted_private_key.pepk"

fail() {
  printf '\n[error] %s\n' "$1" >&2
  exit 1
}

need_file() {
  [ -f "$1" ] || fail "file not found: $1"
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

print_header() {
  printf '\n== %s ==\n' "$1"
}

need_file "$OLD_KEYSTORE"
mkdir -p "$OUTPUT_DIR"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

print_header "1. Check old keystore password"
printf "Keystore: %s\n" "$OLD_KEYSTORE"
read_secret "Old keystore password: " STOREPASS

if ! keytool -list -keystore "$OLD_KEYSTORE" -storepass "$STOREPASS" >"$TMP_DIR/keytool-list.txt" 2>"$TMP_DIR/keytool-error.txt"; then
  cat "$TMP_DIR/keytool-error.txt" >&2
  fail "keystore password did not work"
fi

printf "Keystore password works.\n"
cat >"$TMP_DIR/ListKeystoreAliases.java" <<'JAVA'
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.MessageDigest;
import java.util.Collections;

public class ListKeystoreAliases {
  private static String hex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
      if (i > 0) result.append(":");
      result.append(String.format("%02X", bytes[i]));
    }
    return result.toString();
  }

  public static void main(String[] args) throws Exception {
    KeyStore store = KeyStore.getInstance(new java.io.File(args[0]), args[1].toCharArray());
    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    for (String alias : Collections.list(store.aliases())) {
      System.out.println("Alias: " + alias);
      Certificate cert = store.getCertificate(alias);
      if (cert != null) {
        byte[] encoded = cert.getEncoded();
        System.out.println("  SHA1: " + hex(sha1.digest(encoded)));
        System.out.println("  SHA256: " + hex(sha256.digest(encoded)));
      }
    }
  }
}
JAVA
javac "$TMP_DIR/ListKeystoreAliases.java"
java -cp "$TMP_DIR" ListKeystoreAliases "$OLD_KEYSTORE" "$STOREPASS" >"$TMP_DIR/aliases.txt"

printf "\nAliases found:\n"
cat "$TMP_DIR/aliases.txt"
ALIAS_COUNT="$(grep -c '^Alias: ' "$TMP_DIR/aliases.txt" || true)"
DEFAULT_ALIAS=""
if [ "$ALIAS_COUNT" = "1" ]; then
  DEFAULT_ALIAS="$(sed -n 's/^Alias: //p' "$TMP_DIR/aliases.txt")"
fi

print_header "2. Check private key password"
printf "Enter the alias shown above.\n"
if [ -n "$DEFAULT_ALIAS" ]; then
  read -r -p "Alias [$DEFAULT_ALIAS]: " ALIAS
  ALIAS="${ALIAS:-$DEFAULT_ALIAS}"
else
  read -r -p "Alias: " ALIAS
fi
[ -n "$ALIAS" ] || fail "alias is required"

printf "If the key password was the same as the keystore password, press Enter here.\n"
read_secret "Key password: " KEYPASS
if [ -z "$KEYPASS" ]; then
  KEYPASS="$STOREPASS"
fi

TEST_P12="$TMP_DIR/key-password-test.p12"
if ! keytool -importkeystore \
  -srckeystore "$OLD_KEYSTORE" \
  -srcstorepass "$STOREPASS" \
  -srcalias "$ALIAS" \
  -srckeypass "$KEYPASS" \
  -destkeystore "$TEST_P12" \
  -deststoretype PKCS12 \
  -deststorepass "temporary-test-password" \
  -destkeypass "temporary-test-password" \
  -noprompt >"$TMP_DIR/import-test.txt" 2>"$TMP_DIR/import-test-error.txt"; then
  cat "$TMP_DIR/import-test-error.txt" >&2
  fail "alias or private key password did not work"
fi

printf "Alias and private key password work.\n"

print_header "3. Optional: create Play App Signing encrypted private key"
printf "To continue, put Play Console downloads here, or enter custom paths:\n"
printf "  PEPK tool default: %s\n" "$DEFAULT_PEPK_JAR"
printf "  Encryption key default: %s\n" "$DEFAULT_ENCRYPTION_KEY"
printf "\n"
read -r -p "Create encrypted_private_key.pepk now? [y/N] " CREATE_EXPORT
case "$CREATE_EXPORT" in
  y|Y|yes|YES)
    read -r -p "PEPK jar path [$DEFAULT_PEPK_JAR]: " PEPK_JAR
    read -r -p "Encryption public key path [$DEFAULT_ENCRYPTION_KEY]: " ENCRYPTION_KEY
    PEPK_JAR="${PEPK_JAR:-$DEFAULT_PEPK_JAR}"
    ENCRYPTION_KEY="${ENCRYPTION_KEY:-$DEFAULT_ENCRYPTION_KEY}"
    need_file "$PEPK_JAR"
    need_file "$ENCRYPTION_KEY"

    java -jar "$PEPK_JAR" \
      --keystore="$OLD_KEYSTORE" \
      --alias="$ALIAS" \
      --output="$OUTPUT_FILE" \
      --rsa-aes-encryption \
      --encryption-key-path="$ENCRYPTION_KEY"

    printf "\nCreated:\n  %s\n" "$OUTPUT_FILE"
    printf "Upload that file to Play Console step 4: 비공개 키 업로드.\n"
    ;;
  *)
    printf "Skipped PEPK export. You can rerun this script after downloading pepk.jar and encryption_public_key.pem.\n"
    ;;
esac

print_header "Done"
printf "No passwords were written to disk by this script.\n"
