# GitHub build fix: Compose keyboard imports

The first GitHub build reached Kotlin compilation but failed because `KeyboardOptions` and `KeyboardActions` were imported from `androidx.compose.ui.text.input`.

For Jetpack Compose, these two classes are provided by `androidx.compose.foundation.text`.

Corrected files:

- `app/src/main/java/com/james/mathwakealarm/AlarmActivity.kt`
- `app/src/main/java/com/james/mathwakealarm/AppUi.kt`
- `app/src/main/java/com/james/mathwakealarm/Onboarding.kt`

Correct imports:

```kotlin
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
```

The package validator now explicitly checks this and passes 44/44 checks.
