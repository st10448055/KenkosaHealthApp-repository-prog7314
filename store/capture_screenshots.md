# Capturing real screenshots (Play requires real app screens)
1. Android Studio > Device Manager > Pixel 8 (API 34+/36) emulator. Install the RELEASE build:
   `adb install -r store/evidence/kenkosa-1.0-release.apk`
2. Sign in with a test account and add sample data (meals, water, a fast, sleep, a medication, a mood).
3. For each screen: `adb exec-out screencap -p > store/screenshots/raw/NN-name.png`
   Names that get nice captions: 01-login 02-home 03-diary 04-barcode 05-water 06-fasting 07-sleep 08-medication 09-wellness 10-goals 11-settings
4. `python3 tools/frame_screenshots.py` -> store/screenshots/framed/ (1080x1920, within Play's 320-3840px / 9:16 rules). Need 2-8 phone screenshots.
