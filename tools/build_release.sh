#!/usr/bin/env bash
# Builds + verifies the signed release APK and the Play Store AAB, and writes evidence into store/evidence/.
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f keystore.properties ] || { echo "Run tools/create_keystore.sh first."; exit 1; }
./gradlew clean :app:assembleRelease :app:bundleRelease
OUT=store/evidence; mkdir -p "$OUT"
APK=app/build/outputs/apk/release/app-release.apk
AAB=app/build/outputs/bundle/release/app-release.aab
cp "$APK" "$OUT/kenkosa-1.0-release.apk"; cp "$AAB" "$OUT/kenkosa-1.0-release.aab"
BT=$(ls -d "${ANDROID_HOME:-$HOME/Android/Sdk}"/build-tools/* | sort -V | tail -1)
{
  echo "== apksigner verify =="; "$BT/apksigner" verify --verbose --print-certs "$OUT/kenkosa-1.0-release.apk"
  echo; echo "== badging =="; "$BT/aapt2" dump badging "$OUT/kenkosa-1.0-release.apk" | grep -E "^package|sdkVersion|targetSdk|application-label:"
  echo; echo "== debuggable check (must print nothing) =="; "$BT/aapt2" dump xmltree --file AndroidManifest.xml "$OUT/kenkosa-1.0-release.apk" | grep -i debuggable || true
  echo; echo "== sha256 =="; sha256sum "$OUT"/kenkosa-1.0-release.*
} | tee "$OUT/signing-verification.txt"
echo "Evidence saved in $OUT"
