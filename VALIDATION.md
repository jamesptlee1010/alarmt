# TAZALARM v2.1.2 validation

The source package passes **52/52 targeted structural checks** using `tools/validate_package.py`.

Validated areas include:

- Android XML parsing.
- Required Gradle, source, workflow and signing files.
- Application identity `com.james.mathwakealarm`.
- Version 2.1.2 / code 212.
- Exact alarm, foreground-service, backup-trigger, wake-lock and reboot recovery paths.
- One-minute sunrise and brightness ramp.
- Exact supplied logo assets and launcher branding.
- Black alarm answer input colours.
- Robust barcode capture/matching, legacy storage compatibility and recovery route.
- Preservation of barcode/photo registrations when applying presets or adding alarms.
- Photo verification, 50-question penalty, question topics, progress and reliability logs.
- Release keystore readability and alias.

The GitHub Actions run remains the authoritative full Android SDK compilation, unit-test, APK-signing and signature-verification check.
