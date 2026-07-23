# TAZLARM v2.2.8 validation

The package validator passes 73/73 structural and targeted source checks.

Validated changes include:
- versionCode 228 / versionName 2.2.8
- two-minute sunrise minimum and migration of older alarm settings
- initial SILENCE gate before the first task appears
- 12-second quiet window for every newly displayed question
- 20-second barcode and 30-second photo quiet windows
- wheel-scroller time selection in onboarding and Edit Alarm
- simplified Edit Alarm time/repeat flow
- lock-screen full-screen alarm presentation and permission guidance
- release signing keystore readability

The included GitHub Actions workflow remains the authoritative full Android compile, unit-test, signing and APK verification step.
