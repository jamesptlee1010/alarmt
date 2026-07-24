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
                    onRoutineSaved = { run ->
                        AppRepository.saveRun(run)
                        AppRepository.setTheme(ThemeMode.LIGHT)
                        AppRepository.logReliability(alarm.id, "Routine completed in ${run.durationSeconds} seconds")
                        startService(Intent(this, AlarmService::class.java).apply { action = AlarmService.ACTION_STOP })
                    },
                    onOpenHome = {
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
    onRoutineSaved: (AlarmRun) -> Unit,
    onOpenHome: () -> Unit
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
    var completedRun by remember { mutableStateOf<AlarmRun?>(null) }

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
        val run = AlarmRun(
            alarmId = alarm.id,
            alarmLabel = alarm.label,
            startedAt = startedAt,
            completedAt = System.currentTimeMillis(),
            completed = true,
            penaltyRouteUsed = penaltyMode,
            stepResults = results.toList()
        )
        completedRun = run
        onRoutineSaved(run)
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
        SunriseLandscape(progress = if (completedRun != null) 1f else sunriseProgress, finished = completedRun != null)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(10.dp))
            BrandMark(
                modifier = Modifier.width(220.dp).height(64.dp),
                color = Color.White,
                fullLogo = true
            )
            Spacer(Modifier.height(8.dp))
            if (completedRun != null) {
                AlarmFinishedPane(
                    userName = AppRepository.state.value.userName,
                    onViewSummary = onOpenHome,
                    onBackHome = onOpenHome
                )
                Spacer(Modifier.height(20.dp))
                return@Column
            }
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
private fun SunriseLandscape(progress: Float, finished: Boolean = false) {
    val stars = remember {
        List(42) { Offset(Random.nextFloat(), Random.nextFloat() * .45f) }
    }
    Canvas(Modifier.fillMaxSize()) {
        val eased = if (finished) 1f else FastOutSlowInEasing.transform(progress.coerceIn(0f, 1f))
        val top = lerp(Color(0xFF04123A), Color(0xFFB9E0FF), eased)
        val upperMid = lerp(Color(0xFF1B225C), Color(0xFFF6D3A2), eased)
        val lowerMid = lerp(Color(0xFF35204A), Color(0xFFFFB06F), eased)
        val horizon = lerp(Color(0xFF4B2559), Color(0xFFFFE8A6), eased)
        drawRect(Brush.verticalGradient(listOf(top, upperMid, lowerMid, horizon), endY = size.height * .78f))

        stars.forEachIndexed { index, star ->
            val alpha = ((1f - eased * 1.4f) * (.30f + (index % 5) * .10f)).coerceIn(0f, 1f)
            if (alpha > 0f) drawCircle(Color.White.copy(alpha), radius = if (index % 7 == 0) 2.2f else 1.2f, center = Offset(star.x * size.width, star.y * size.height))
        }

        val sunCenterY = size.height * .78f + (size.height * .38f - size.height * .78f) * eased
        val sunCenter = Offset(size.width * .5f, sunCenterY)
        val sunRadius = size.width * .035f + (size.width * .09f - size.width * .035f) * eased
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF1B8).copy(alpha = .85f), Color(0xFFFFC56B).copy(alpha = .35f), Color.Transparent),
                center = sunCenter,
                radius = sunRadius * 4.5f
            ),
            radius = sunRadius * 4.5f,
            center = sunCenter
        )
        drawCircle(Color(0xFFFFF4D0).copy(alpha = .95f), radius = sunRadius, center = sunCenter)

        // distant mountains
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, size.height * .70f)
                lineTo(size.width * .12f, size.height * .58f)
                lineTo(size.width * .22f, size.height * .64f)
                lineTo(size.width * .34f, size.height * .54f)
                lineTo(size.width * .47f, size.height * .65f)
                lineTo(size.width * .59f, size.height * .56f)
                lineTo(size.width * .74f, size.height * .63f)
                lineTo(size.width * .90f, size.height * .50f)
                lineTo(size.width, size.height * .66f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            },
            color = lerp(Color(0xFF17254F), Color(0xFF8AA1BE), eased)
        )
        // nearer valley walls
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, size.height * .78f)
                lineTo(size.width * .08f, size.height * .73f)
                lineTo(size.width * .17f, size.height * .67f)
                lineTo(size.width * .25f, size.height * .74f)
                lineTo(size.width * .33f, size.height * .80f)
                lineTo(size.width * .40f, size.height)
                close()
            },
            color = lerp(Color(0xFF0D1637), Color(0xFF5C7C4B), eased)
        )
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width, size.height)
                lineTo(size.width, size.height * .77f)
                lineTo(size.width * .92f, size.height * .72f)
                lineTo(size.width * .84f, size.height * .66f)
                lineTo(size.width * .74f, size.height * .73f)
                lineTo(size.width * .66f, size.height * .80f)
                lineTo(size.width * .58f, size.height)
                close()
            },
            color = lerp(Color(0xFF10193B), Color(0xFF668651), eased)
        )

        // lake / water
        val lakeTop = size.height * .72f
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width * .28f, size.height)
                quadraticTo(size.width * .50f, lakeTop, size.width * .72f, size.height)
                close()
            },
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFFB5D7F5).copy(alpha = .65f * eased + .10f),
                    Color(0xFF3E5B86).copy(alpha = .85f)
                ),
                startY = lakeTop,
                endY = size.height
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFFFE7A6).copy(alpha = .75f * eased), Color.Transparent),
                startY = sunCenter.y,
                endY = size.height
            ),
            topLeft = Offset(size.width * .47f, sunCenter.y),
            size = androidx.compose.ui.geometry.Size(size.width * .06f, size.height - sunCenter.y)
        )

        // meadow foreground
        drawRect(
            brush = Brush.verticalGradient(
                listOf(lerp(Color(0xFF2A2B24), Color(0xFF6D8B42), eased), lerp(Color(0xFF15130F), Color(0xFF88A34E), eased)),
                startY = size.height * .82f,
                endY = size.height
            ),
            topLeft = Offset(0f, size.height * .82f),
            size = androidx.compose.ui.geometry.Size(size.width, size.height * .18f)
        )
        // a few meadow flowers / highlights
        repeat(40) { i ->
            val x = (i / 39f) * size.width
            val y = size.height * (.86f + (i % 5) * .025f)
            drawCircle(
                color = listOf(Color(0xFFFFC56B), Color(0xFFE8E3FF), Color(0xFFFF8A7A), Color(0xFFBEE0FF))[i % 4].copy(alpha = .7f),
                radius = 2.2f + (i % 3),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun AlarmFinishedPane(userName: String, onViewSummary: () -> Unit, onBackHome: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .95f)),
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxWidth(.88f)
    ) {
        Column(
            Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(76.dp).background(Color(0xFFEAF8EB), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CheckCircle, null, tint = TazGreen, modifier = Modifier.size(48.dp))
            }
            Text("Alarm Finished!", color = TazNavy, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
            Text("Well done, $userName.", color = Color(0xFF51627A), fontSize = 18.sp)
            Text("You're up and unstoppable.", color = Color(0xFF51627A), fontSize = 18.sp)
            Button(onClick = onViewSummary, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Text("View Summary", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onBackHome, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Back to Home", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun greetingFor(hour: Int, name: String): String = when (hour) {
    in 0..11 -> "Good morning, $name"
    in 12..16 -> "Good afternoon, $name"
    else -> "Good evening, $name"
}

