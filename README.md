# TAZLARM v2.2.6

Android sunrise alarm application.

- Application ID: `com.james.mathwakealarm`
- Version: 2.2.6 (226)
- Minimum Android version: API 26
- Target/compile SDK: 36

## v2.2.6 highlights

- Light-mode Home dashboard selected from the approved mockup direction.
- Routine cards reorder by long-press dragging or Up/Down buttons.
- Haptic feedback and a lifted-card effect while dragging.
- Two-second fade from black into the light Home screen when the alarm is completed.
- Quiet grace periods of 7 seconds for questions, 20 seconds for barcode and 30 seconds for photo verification.
- Sound and vibration both pause during grace periods.
- Immersive alarm screen over the lock screen, with Android 14+ full-screen alarm permission guidance.

Run `python tools/validate_package.py` before uploading. The included GitHub Actions workflow performs the authoritative Android unit-test and release build.
