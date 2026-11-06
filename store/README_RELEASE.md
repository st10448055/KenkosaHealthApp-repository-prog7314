# Release readiness kit

## A. Produce the signed production APK (+ AAB)
1. `tools/create_keystore.sh`  (creates kenkosa-upload.jks + keystore.properties - both git-ignored; BACK THEM UP)
2. Add the printed SHA-1 and SHA-256 to Firebase (Project settings > your Android app), re-download google-services.json
   if it changes. Without this, Google sign-in fails in the release build.
3. `tools/build_release.sh` -> store/evidence/ contains the signed .apk, the .aab, and signing-verification.txt
   (apksigner output, package/SDK badging, not-debuggable check, SHA-256).
Needs JDK 17+ and Android SDK (build-tools, platform 36) - i.e. Android Studio's bundled setup.

## B. Screenshots
See store/capture_screenshots.md. Graphics already prepared: store/graphics/app-icon-512.png, feature-graphic-1024x500.png.

## C. Pre-submission checklist
- [ ] New Play apps must be uploaded as an **AAB** (the APK is your verifiable evidence / for sideload testing).
- [ ] Enrol in Play App Signing; the key you made is the *upload* key.
- [ ] Personal developer accounts created after Nov 2023 need a closed test (12+ testers, 14 days) before production.
- [ ] Privacy policy URL (health data + camera + Firebase) - required.
- [ ] Data safety form: email/account info, health & fitness data (food, water, sleep, meds, mood), device ID/FCM token, analytics (Firebase), encrypted in transit (HTTPS).
- [ ] Health apps declaration + disclaimer (not a medical device); content rating questionnaire; target audience 18+.
- [ ] Permission justifications: CAMERA (barcode), ACTIVITY_RECOGNITION (steps), POST_NOTIFICATIONS.
- [ ] Manifest sets camera `required="true"`: consider `false` so tablets/devices without a rear camera aren't excluded.
- [ ] `allowBackup="true"` with health data: review backup_rules.xml / data_extraction_rules.xml.
- [ ] Release build is minify-disabled; consider `isMinifyEnabled = true` (+ `isShrinkResources`) after testing, rules are prepared.
- [ ] versionCode/versionName (currently 1 / "1.0") - bump per upload.
- [ ] Backend on Render free tier may cold-start slowly: confirm the API is reachable for reviewers, supply a test login in App access.
- [ ] `applicationId` is `com.example...` - Play allows it but you cannot change it after publishing; decide now.
