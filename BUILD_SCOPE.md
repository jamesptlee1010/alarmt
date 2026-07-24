# TAZLARM v2.3.2 build scope

- Scenic live-alarm sunrise landscape with night-to-day colour progression.
- Lake, mountain, valley and meadow foreground layers.
- Logo and TAZLARM wordmark moved slightly lower.
- Dedicated Alarm Finished screen immediately after the final routine step.
- View Summary and Back to Home actions.
- Existing SILENCE gate and 12/20/30-second quiet periods retained.
- Wheel time picker, Test Alarm, permission setup and expanded TV trivia retained.
- Version 2.3.2 / versionCode 231.


## v2.3.2 compile correction

- Replaced the two invalid numeric `lerp()` calls in `SunriseLandscape` with explicit float interpolation.
- Preserves the scenic sun movement and size animation.
- Adds a validator regression check so this exact Kotlin compiler error is detected before packaging.
