package com.james.mathwakealarm

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        AppRepository.initialise(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: AppRepository.activeAlarmId()
        val startedAt = intent.getLongExtra("started_at", AppRepository.activeAlarmStartedAt())
            .takeIf { it > 0L } ?: System.currentTimeMillis()
        val alarm = AppRepository.alarm(alarmId)
        if (alarm == null) {
            finish()
            return
        }
        AppRepository.logReliability(alarm.id, "Challenge started")

        setContent {
            TazAlarmTheme(ThemeMode.DARK) {
                AlarmChallengeScreen(
                    alarm = alarm,
                    startedAt = startedAt,
                    setBrightness = { brightness ->
                        window.attributes = window.attributes.apply { screenBrightness = brightness.coerceIn(.05f, 1f) }
                    },
                    onComplete = { run ->
                        AppRepository.saveRun(run)
                        AppRepository.setTheme(ThemeMode.LIGHT)
                        AppRepository.logReliability(alarm.id, "Routine completed in ${run.durationSeconds} seconds")
                        startService(Intent(this, AlarmService::class.java).apply { action = AlarmService.ACTION_STOP })
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            putExtra(MainActivity.EXTRA_POST_ALARM_FADE, true)
                        })
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun AlarmChallengeScreen(
    alarm: AlarmConfig,
    startedAt: Long,
    setBrightness: (Float) -> Unit,
    onComplete: (AlarmRun) -> Unit
) {
    val context = LocalContext.current
    val sunriseDuration = alarm.sunriseSeconds.coerceAtLeast(120) * 1000L
    var sunriseProgress by remember { mutableFloatStateOf(0f) }
    var stepIndex by remember { mutableIntStateOf(0) }
    var penaltyMode by remember { mutableStateOf(false) }
    var showPenaltyConfirm by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<RunStepResult>() }
    var stepStartedAt by remember { mutableStateOf(System.currentTimeMillis()) }

    var question by remember { mutableStateOf<QuestionEngine.Question?>(null) }
    var answer by remember { mutableStateOf("") }
    var correctCount by remember { mutableIntStateOf(0) }
    var attempts by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    val recentQuestionIds = remember { mutableStateListOf<String>() }
    val topicCorrect = remember { mutableStateMapOf<Topic, Int>() }
    val topicAttempted = remember { mutableStateMapOf<Topic, Int>() }
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    var awaitingInitialSilence by remember { mutableStateOf(true) }

    val currentStep = if (penaltyMode) null else alarm.routine.getOrNull(stepIndex)
    val questionTarget = if (penaltyMode) 50 else currentStep?.questionsRequired ?: 0
    val activeTopics = if (penaltyMode) listOf(Topic.MATHS, Topic.LOGIC, Topic.GENERAL_KNOWLEDGE) else currentStep?.topics.orEmpty()

    fun pauseAlarm(durationMs: Long) {
        context.startService(Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_PAUSE_AUDIO
            putExtra(AlarmService.EXTRA_PAUSE_MS, durationMs)
        })
    }

    fun nextQuestion() {
        question = QuestionEngine.next(activeTopics, recentQuestionIds.takeLast(12))
        question?.id?.let {
            recentQuestionIds.add(it)
            if (recentQuestionIds.size > 30) recentQuestionIds.removeAt(0)
        }
        answer = ""
        pauseAlarm(12_000L)
    }

    fun completeRoutine() {
        onComplete(
            AlarmRun(
                alarmId = alarm.id,
                alarmLabel = alarm.label,
                startedAt = startedAt,
                completedAt = System.currentTimeMillis(),
                completed = true,
                penaltyRouteUsed = penaltyMode,
                stepResults = results.toList()
            )
        )
    }

    fun completeStep(successAttempts: Int = attempts.coerceAtLeast(1), correct: Int = correctCount) {
        val step = currentStep ?: return
        results += RunStepResult(
            stepId = step.id,
            type = step.type,
            title = step.title,
            durationSeconds = ((System.currentTimeMillis() - stepStartedAt) / 1000L).toInt(),
            success = true,
            attempts = successAttempts,
            correctAnswers = correct,
            topicScores = topicAttempted.map { (topic, total) ->
                TopicScore(topic, topicCorrect[topic] ?: 0, total)
            }
        )
        if (stepIndex >= alarm.routine.lastIndex) {
            completeRoutine()
        } else {
            stepIndex += 1
            stepStartedAt = System.currentTimeMillis()
            correctCount = 0
            attempts = 0
            feedback = ""
            topicCorrect.clear()
            topicAttempted.clear()
        }
    }

    fun submitAnswer() {
        val activeQuestion = question ?: return
        if (answer.isBlank()) return
        attempts += 1
        topicAttempted[activeQuestion.topic] = (topicAttempted[activeQuestion.topic] ?: 0) + 1
        if (QuestionEngine.isCorrect(activeQuestion, answer)) {
            topicCorrect[activeQuestion.topic] = (topicCorrect[activeQuestion.topic] ?: 0) + 1
            correctCount += 1
            feedback = "Correct — keep moving"
            if (correctCount >= questionTarget) {
                if (penaltyMode) {
                    results += RunStepResult(
                        stepId = "penalty",
                        type = StepType.QUESTIONS,
                        title = "50-question penalty route",
                        durationSeconds = ((System.currentTimeMillis() - stepStartedAt) / 1000L).toInt(),
                        success = true,
                        attempts = attempts,
                        correctAnswers = correctCount,
                        topicScores = topicAttempted.map { (topic, total) -> TopicScore(topic, topicCorrect[topic] ?: 0, total) }
                    )
                    completeRoutine()
                } else completeStep()
            } else nextQuestion()
        } else {
            feedback = "Not quite. Try again or skip this question."
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val step = currentStep
        if (success && step?.type == StepType.PHOTO) {
            attempts += 1
            val score = captureUri?.let { ImageSimilarity.bestScore(context, it, step.referenceUris) } ?: 0f
            if (score >= step.photoThreshold) {
                feedback = "Photo verified (${(score * 100).roundToInt()}% match)"
                completeStep(successAttempts = attempts, correct = 0)
            } else {
                feedback = "Photo did not match closely enough (${(score * 100).roundToInt()}%). Try a clearer angle."
            }
        } else if (!success) feedback = "Photo cancelled — the alarm is still active."
    }

    LaunchedEffect(startedAt, sunriseDuration) {
        while (sunriseProgress < 1f) {
            sunriseProgress = ((System.currentTimeMillis() - startedAt).toFloat() / sunriseDuration).coerceIn(0f, 1f)
            setBrightness(.08f + .92f * FastOutSlowInEasing.transform(sunriseProgress))
            delay(50L)
        }
    }

    LaunchedEffect(stepIndex, penaltyMode, awaitingInitialSilence) {
        if (!awaitingInitialSilence) {
            when {
                penaltyMode || currentStep?.type == StepType.QUESTIONS -> nextQuestion()
                currentStep?.type == StepType.BARCODE -> pauseAlarm(20_000L)
                currentStep?.type == StepType.PHOTO -> pauseAlarm(30_000L)
            }
        }
        correctCount = 0
        attempts = 0
        answer = ""
        feedback = ""
        stepStartedAt = System.currentTimeMillis()
        topicCorrect.clear()
        topicAttempted.clear()
    }

    if (showPenaltyConfirm) {
        AlertDialog(
            onDismissRequest = { showPenaltyConfirm = false },
            title = { Text("Start the penalty route?") },
            text = { Text("You must answer 50 questions correctly. Once started, you cannot return to the normal routine.") },
            confirmButton = {
                Button(onClick = {
                    penaltyMode = true
                    showPenaltyConfirm = false
                    stepStartedAt = System.currentTimeMillis()
                }) { Text("Start 50 Questions") }
            },
            dismissButton = { TextButton(onClick = { showPenaltyConfirm = false }) { Text("Keep Going") } }
        )
    }

    Box(Modifier.fillMaxSize()) {
        SunriseHorizon(progress = sunriseProgress)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandMark(
                modifier = Modifier.width(220.dp).height(64.dp),
                color = Color.White,
                fullLogo = true
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.VolumeUp, null, tint = TazAmber, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Alarm active • volume rising", color = TazAmber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(greetingFor(alarm.hour, AppRepository.state.value.userName), color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(3.dp))
            Text(
                if (penaltyMode) "Penalty route • $correctCount of 50 correct" else "Step ${stepIndex + 1} of ${alarm.routine.size}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .95f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(.90f)
            ) {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when {
                        awaitingInitialSilence -> {
                            Icon(Icons.Outlined.VolumeUp, null, tint = TazAmber, modifier = Modifier.size(54.dp))
                            Text("Ready to start?", color = TazNavy, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                            Text("Tap SILENCE to stop the alarm briefly and begin your first wake-up step.", color = Color(0xFF60728B))
                            Button(
                                onClick = { awaitingInitialSilence = false },
                                modifier = Modifier.fillMaxWidth().height(58.dp)
                            ) { Text("SILENCE", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
                        }
                        penaltyMode || currentStep?.type == StepType.QUESTIONS -> {
                            val q = question
                            Text(if (penaltyMode) "50-Question Penalty" else currentStep?.title.orEmpty(), color = TazNavy, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
                            Text("$correctCount / $questionTarget correct", color = TazBlue, fontWeight = FontWeight.Bold)
                            if (q != null) {
                                Text(q.prompt, color = TazNavy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text(q.topic.displayName, color = Color(0xFF60728B), fontSize = 13.sp)
                                OutlinedTextField(
                                    value = answer,
                                    onValueChange = { answer = it },
                                    modifier = Modifier.fillMaxWidth().height(62.dp),
                                    label = { Text("Enter answer", color = Color.Black) },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color.Black,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        disabledTextColor = Color.Black,
                                        cursorColor = Color.Black,
                                        focusedBorderColor = Color.Black,
                                        unfocusedBorderColor = Color.Black,
                                        focusedLabelColor = Color.Black,
                                        unfocusedLabelColor = Color.Black,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { submitAnswer() })
                                )
                                Button(onClick = { submitAnswer() }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Submit") }
                                OutlinedButton(onClick = { nextQuestion(); feedback = "Question skipped — you still need $questionTarget correct." }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Skip Question") }
                            }
                        }
                        currentStep?.type == StepType.BARCODE -> {
                            val step = requireNotNull(currentStep)
                            Icon(Icons.Outlined.QrCodeScanner, null, tint = TazBlue, modifier = Modifier.size(46.dp))
                            Text("Scan Barcode", color = TazNavy, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp)
                            val acceptedCodes = (step.barcodeValues + listOf(step.barcodeValue)).filter { it.isNotBlank() }.distinct()
                            Text("Barcode must match one of your saved codes", color = Color(0xFF60728B))
                            Button(
                                onClick = {
                                    attempts += 1
                                    GmsBarcodeScanning.getClient(context).startScan()
                                        .addOnSuccessListener { barcode ->
                                            when {
                                                acceptedCodes.isEmpty() -> {
                                                    feedback = "No barcode is registered for this step. Use the recovery route below, then register one in Routines."
                                                    AppRepository.logReliability(alarm.id, "Barcode step has no registered code")
                                                }
                                                acceptedCodes.any { BarcodeIdentity.matches(it, barcode) } -> {
                                                    feedback = "Barcode accepted"
                                                    completeStep(successAttempts = attempts, correct = 0)
                                                }
                                                else -> {
                                                    feedback = "That scan did not match. Try again, or use ‘Barcode not working?’ below."
                                                    AppRepository.logReliability(alarm.id, "Barcode mismatch attempt $attempts")
                                                }
                                            }
                                        }
                                        .addOnFailureListener { feedback = "Scanner could not start. Try again." }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.QrCodeScanner, null)
                                Text(" Open Scanner")
                            }
                            OutlinedButton(
                                onClick = { showPenaltyConfirm = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Barcode not working? Use recovery route")
                            }
                            Text(
                                "The recovery route requires 50 correct answers; it does not simply stop the alarm.",
                                color = Color(0xFF60728B),
                                fontSize = 12.sp
                            )
                        }
                        currentStep?.type == StepType.PHOTO -> {
                            val step = requireNotNull(currentStep)
                            Icon(Icons.Outlined.CameraAlt, null, tint = TazBlue, modifier = Modifier.size(46.dp))
                            Text(step.title, color = TazNavy, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp)
                            Text("Use the live camera to match one of your saved references.", color = Color(0xFF60728B))
                            Text("${step.referenceUris.size} reference photos available", color = if (step.referenceUris.isNotEmpty()) TazGreen else TazRed)
                            Button(
                                onClick = {
                                    captureUri = PhotoStore.createCaptureUri(context, "alarm_verify")
                                    captureUri?.let { photoLauncher.launch(it) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = step.referenceUris.isNotEmpty()
                            ) {
                                Icon(Icons.Outlined.CameraAlt, null)
                                Text(" Open Live Camera")
                            }
                        }
                    }
                    if (feedback.isNotBlank()) {
                        Text(
                            feedback,
                            color = if (feedback.startsWith("Correct") || feedback.contains("accepted") || feedback.contains("verified")) TazGreen else TazRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            if (!penaltyMode) {
                TextButton(onClick = { showPenaltyConfirm = true }) {
                    Text("Having trouble? Use the 50-question penalty", color = Color.White)
                }
            } else {
                Text("Penalty route locked — complete all 50 correct answers.", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SunriseHorizon(progress: Float) {
    val stars = remember {
        List(42) { Offset(Random.nextFloat(), Random.nextFloat() * .58f) }
    }
    Canvas(Modifier.fillMaxSize()) {
        val eased = FastOutSlowInEasing.transform(progress.coerceIn(0f, 1f))
        val top = lerp(Color(0xFF020817), Color(0xFF86B9DD), eased)
        val midNight = Color(0xFF101B4A)
        val purple = Color(0xFF6D3F88)
        val red = Color(0xFFE25A55)
        val orange = Color(0xFFFFA43B)
        val daylight = Color(0xFFFFE2A8)
        val mid = when {
            eased < .25f -> lerp(midNight, purple, eased / .25f)
            eased < .50f -> lerp(purple, red, (eased - .25f) / .25f)
            eased < .75f -> lerp(red, orange, (eased - .50f) / .25f)
            else -> lerp(orange, daylight, (eased - .75f) / .25f)
        }
        val horizon = lerp(Color(0xFF23142D), Color(0xFFFFD785), eased)
        drawRect(Brush.verticalGradient(listOf(top, mid, horizon), endY = size.height * .82f))
        stars.forEachIndexed { index, star ->
            val alpha = ((1f - eased * 1.5f) * (.35f + (index % 5) * .12f)).coerceIn(0f, 1f)
            if (alpha > 0f) drawCircle(Color.White.copy(alpha), radius = if (index % 7 == 0) 2.2f else 1.2f, center = Offset(star.x * size.width, star.y * size.height))
        }
        val glowAlpha = (.15f + eased * .85f).coerceIn(0f, 1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF3C4).copy(glowAlpha), Color(0xFFFFA12E).copy(glowAlpha * .4f), Color.Transparent),
                center = Offset(size.width * .72f, size.height * .72f),
                radius = size.width * (.15f + eased * .45f)
            ),
            radius = size.width * (.15f + eased * .45f),
            center = Offset(size.width * .72f, size.height * .72f)
        )
        drawCircle(Color(0xFFFFF4D0).copy(alpha = eased), radius = 12f + eased * 20f, center = Offset(size.width * .72f, size.height * .72f))
    }
}

private fun greetingFor(hour: Int, name: String): String = when (hour) {
    in 0..11 -> "Good morning, $name"
    in 12..16 -> "Good afternoon, $name"
    else -> "Good evening, $name"
}

