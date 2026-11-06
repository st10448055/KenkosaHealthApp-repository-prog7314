#!/usr/bin/env bash
# Creates the upload keystore + keystore.properties. Run ONCE, then back up the .jks file and passwords.
# Losing this key means you cannot update the app (unless you enrolled in Play App Signing, which you should).
set -euo pipefail
cd "$(dirname "$0")/.."
KS=kenkosa-upload.jks
ALIAS=kenkosa-upload
[ -f "$KS" ] && { echo "$KS already exists - refusing to overwrite."; exit 1; }
read -rsp "Choose a keystore/key password (min 6 chars): " PW; echo
read -rp  "Your name or org (CN) [Kenkosa Health]: " CN; CN=${CN:-Kenkosa Health}
keytool -genkeypair -v -keystore "$KS" -alias "$ALIAS" -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$PW" -keypass "$PW" -dname "CN=$CN, OU=Mobile, O=Kenkosa, C=ZA"
cat > keystore.properties <<EOP
storeFile=$KS
storePassword=$PW
keyAlias=$ALIAS
keyPassword=$PW
EOP
echo; echo "SHA-1 / SHA-256 (add BOTH to Firebase > Project settings > your Android app, or Google sign-in will fail in release):"
keytool -list -v -keystore "$KS" -alias "$ALIAS" -storepass "$PW" | grep -E "SHA1|SHA256"
