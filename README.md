# TAZLARM v2.2.5

TAZLARM is a personal Android sunrise alarm that continues until its configured wake-up routine is completed.

```text
applicationId: com.james.mathwakealarm
version: 2.2.5 (225)
minimum Android: 8.0 / API 26
```

## v2.2.5 changes

- The animated sunrise now draws edge-to-edge behind the status, cutout and navigation-bar areas.
- The previous solid lower-screen block has been removed so the sunrise continues to the bottom of the display.
- The first-run name field no longer shows explanatory text underneath it.
- Every routine step shown during onboarding has an Edit pencil.
- Question steps can be edited immediately to choose the required count and topics.
- Barcode and photo steps can also be configured from the onboarding pencil editor.
- Routine and Settings cards now use a pale-blue surface.

## Existing behaviour retained

- Two-minute minimum sunrise with no countdown or progress bar.
- TAZLARM header pinned near the top of the live alarm screen.
- Compact question, barcode and photo challenge cards.
- Multiple alarms, one-time or selected-day repeats, custom routines and multiple accepted barcodes.
- Dance Moms and Teen Mom 2 trivia topics.
- Easier maths generation.
- Exact-alarm scheduling, backup delivery, foreground service, wake lock and reboot rescheduling.
- Signed release build through GitHub Actions.

## Build the APK without Android Studio

1. Keep the repository private because the project contains its signing keystore.
2. Extract this ZIP.
3. Replace the contents of the local cloned repository with the extracted files, preserving `.git`.
4. Commit and push through GitHub Desktop.
5. Open **Actions → Build TAZLARM APK**.
6. Run the workflow.
7. Download **TAZLARM-v2.2.5-installable-APK** from the completed run.
8. Extract the downloaded artifact and install `TAZLARM-v2.2.5.apk`.

The supplied validator checks project structure, Android XML, signing material and targeted feature implementation. GitHub Actions performs the authoritative Android unit tests, release compilation, APK signing and signature verification.
