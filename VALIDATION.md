# TAZLARM v2.2.0 validation

The package validator has been updated for the v2.2.0 specification rather than the superseded v2.1.3 expectations.

Validated expectations include:

- versionCode 220 and versionName 2.2.0
- visible application label TAZLARM
- selected sneezing-cat branding with the TAZLARM wordmark
- generic multi-barcode helper wording
- exact alarm and backup-delivery architecture
- one-minute sunrise and brightness ramp
- centred Home branding and no duplicate Home Settings shortcut
- blank first-run name field
- clock-based alarm-time selection
- blank repeat-day selection with one-time scheduling
- empty first-run routine builder
- multiple acceptable barcodes per barcode step
- dismissible completed-alarm summary
- Edit Alarm and Edit Routine actions on the Next Alarm card
- Dance Moms and Teen Mom 2 topic coverage
- easier maths generation
- black alarm answer text
- 50-question recovery route
- compile-safe Compose keyboard imports
- valid release keystore

Result: **55/55 package checks passed locally.**

The GitHub Actions run remains the authoritative Android compilation, unit-test, APK-signing and signature-verification step.
