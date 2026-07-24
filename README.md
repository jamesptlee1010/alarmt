# TAZLARM v2.2.9

Android sunrise alarm application.

- Application ID: `com.james.mathwakealarm`
- Version: 2.2.9 (229)
- Minimum Android version: API 26
- Target/compile SDK: 36

## v2.2.9 highlights

- Light-mode Home dashboard selected from the approved mockup direction.
- Routine cards reorder by long-press dragging or Up/Down buttons.
- Haptic feedback and a lifted-card effect while dragging.
- Two-second fade from black into the light Home screen when the alarm is completed.
- Quiet grace periods of 7 seconds for questions, 20 seconds for barcode and 30 seconds for photo verification.
- Sound and vibration both pause during grace periods.
- Immersive alarm screen over the lock screen, with Android 14+ full-screen alarm permission guidance.

Run `python tools/validate_package.py` before uploading. The included GitHub Actions workflow performs the authoritative Android unit-test and release build.


## v2.2.9 highlights

- Initial full-screen **SILENCE** button before the first wake-up task begins.
- Question stages now give a 12-second quiet period after each new question appears before sound/vibration resume.
- Barcode and photo stages retain 20-second and 30-second quiet windows respectively.
- Embedded wheel-style time picker added to onboarding alarm creation and Edit Alarm.
- Edit Alarm simplified to a clean scroller-first time editing flow with repeat controls.


## v2.2.9 additions

- Stable wheel-scroller state across hour, minute and AM/PM changes.
- Three-second Test Alarm on the Home and Alarms screens.
- First-launch permission setup for reliable alarm delivery.
- 50 questions each for Dance Moms, Teen Mom 2 and The Secret Lives of Mormon Wives.
