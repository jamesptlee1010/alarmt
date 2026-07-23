package com.james.mathwakealarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_POST_ALARM_FADE = "post_alarm_fade"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppRepository.initialise(this)
        enableEdgeToEdge()
        val fadeFromBlack = intent.getBooleanExtra(EXTRA_POST_ALARM_FADE, false)
        setContent {
            val appState by AppRepository.state
            TazAlarmTheme(appState.themeMode) {
                MainContent(appState = appState, fadeFromBlack = fadeFromBlack)
            }
        }
    }
}

@Composable
private fun MainContent(appState: AppState, fadeFromBlack: Boolean) {
    var blackOverlayVisible by remember(fadeFromBlack) { mutableStateOf(fadeFromBlack) }
    val blackAlpha by animateFloatAsState(
        targetValue = if (blackOverlayVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 2_000),
        label = "postAlarmBlackFade"
    )

    LaunchedEffect(fadeFromBlack) {
        if (fadeFromBlack) {
            delay(180)
            blackOverlayVisible = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (appState.onboardingComplete) {
            TazAlarmApp(appState)
        } else {
            OnboardingScreen(appState)
        }
        if (blackAlpha > 0.001f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(blackAlpha)
                    .background(Color.Black)
            )
        }
    }
}
