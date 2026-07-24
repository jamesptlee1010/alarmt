package com.james.mathwakealarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
fun OnboardingScreen(appState: AppState) {
    val context = LocalContext.current
    var page by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(appState.userName) }
    var alarmLabel by remember { mutableStateOf("Weekday Alarm") }
    var selectedHour by remember { mutableIntStateOf(6) }
    var selectedMinute by remember { mutableIntStateOf(30) }
    var days by remember { mutableStateOf(emptyList<Int>()) }
    var queuedAlarms by remember { mutableStateOf(emptyList<AlarmConfig>()) }
    var routineSteps by remember { mutableStateOf(emptyList<RoutineStep>()) }
    var addStepMenu by remember { mutableStateOf(false) }
    var editingStep by remember { mutableStateOf<RoutineStep?>(null) }
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    var permissionRefresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionRefresh += 1
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionRefresh += 1
    }

    val permissionRefreshSnapshot = permissionRefresh
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    val powerManager = context.getSystemService(PowerManager::class.java)
    val notificationsAllowed = permissionRefreshSnapshot.let {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val exactAllowed = permissionRefreshSnapshot.let {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }
    val fullScreenAllowed = permissionRefreshSnapshot.let {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || notificationManager.canUseFullScreenIntent()
    }
    val batteryUnrestricted = permissionRefreshSnapshot.let {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            captureUri?.toString()?.let { uri ->
                editingStep = editingStep?.copy(
                    referenceUris = (editingStep?.referenceUris.orEmpty() + uri).distinct().take(5)
                )
            }
        }
    }

    fun formatPickedTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "am" else "pm"
        val displayHour = when (val h = hour % 12) { 0 -> 12; else -> h }
        return "$displayHour:${minute.toString().padStart(2, '0')} $amPm"
    }

    fun draftAlarm(): AlarmConfig = AlarmConfig(
        label = alarmLabel.ifBlank { "Alarm" },
        hour = selectedHour,
        minute = selectedMinute,
        days = days,
        routine = routineSteps
    )

    editingStep?.let { step ->
        StepEditorDialog(
            step = step,
            onDismiss = { editingStep = null },
            onScan = {
                GmsBarcodeScanning.getClient(context).startScan()
                    .addOnSuccessListener { barcode ->
                        val captured = BarcodeIdentity.capture(barcode)
                        editingStep = editingStep?.let { current ->
                            val existing = (current.barcodeValues + current.barcodeValue)
                                .filter { it.isNotBlank() }
                            current.copy(
                                barcodeValue = captured,
                                barcodeValues = (existing + captured).distinct(),
                                title = "Scan Barcode"
                            )
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Scanner could not start", Toast.LENGTH_SHORT).show()
                    }
            },
            onCapture = {
                captureUri = PhotoStore.createCaptureUri(context, "onboarding_routine")
                captureUri?.let { photoLauncher.launch(it) }
            },
            onSave = { saved ->
                val exists = routineSteps.any { it.id == saved.id }
                routineSteps = if (exists) {
                    routineSteps.map { if (it.id == saved.id) saved else it }
                } else {
                    routineSteps + saved
                }
                editingStep = null
            }
        )
    }

    Scaffold { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primaryContainer.copy(alpha = .35f))
                    )
                )
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BrandHeader()
                Spacer(Modifier.height(24.dp))
                LinearProgressIndicator(
                    progress = { (page + 1) / 5f },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(28.dp))

                when (page) {
                    0 -> {
                        Icon(Icons.Outlined.Settings, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Prepare TAZLARM", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Allow the essential alarm permissions now so TAZLARM can ring reliably and open over the lock screen.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(22.dp))
                        OnboardingPermissionCard(
                            icon = Icons.Outlined.NotificationsActive,
                            title = "Alarm notifications",
                            ready = notificationsAllowed,
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        OnboardingPermissionCard(
                            icon = Icons.Outlined.Alarm,
                            title = "Exact alarm access",
                            ready = exactAllowed,
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                                }
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        OnboardingPermissionCard(
                            icon = Icons.Outlined.NotificationsActive,
                            title = "Full-screen lock-screen alarm",
                            ready = fullScreenAllowed,
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")))
                                }
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        OnboardingPermissionCard(
                            icon = Icons.Outlined.BatteryChargingFull,
                            title = "Battery optimisation",
                            ready = batteryUnrestricted,
                            statusText = if (batteryUnrestricted) "Unrestricted" else "Open settings and choose Unrestricted / Don't optimise",
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")))
                                }.onFailure {
                                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                }
                            }
                        )
                        Text(
                            "You can continue and return to these checks later in Settings.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    1 -> {
                        Icon(Icons.Outlined.Alarm, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Welcome to TAZLARM", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "A sunrise alarm that makes sure you are genuinely awake.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(28.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Your name") },
                            placeholder = { Text("Enter your name") },
                            singleLine = true
                        )
                    }
                    2 -> {
                        Text("Create your first alarm", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Choose a time, then optionally select repeat days. If no days are selected, the alarm will run once.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        if (queuedAlarms.isNotEmpty()) {
                            Text("${queuedAlarms.size} alarm${if (queuedAlarms.size == 1) "" else "s"} already added", color = TazGreen, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                        }
                        OutlinedTextField(
                            value = alarmLabel,
                            onValueChange = { alarmLabel = it },
                            label = { Text("Alarm name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Pick time", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        WheelTimePicker(
                            hour24 = selectedHour,
                            minute = selectedMinute,
                            onTimeChange = { hour, minute ->
                                selectedHour = hour
                                selectedMinute = minute
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            formatPickedTime(selectedHour, selectedMinute),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(Modifier.height(20.dp))
                        Text("Repeat on", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold)
                        DayChipRows(days) { day ->
                            days = if (day in days) days - day else (days + day).sorted()
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (days.isEmpty()) "No repeat days selected — alarm will run once."
                            else "Repeats on ${daysLabel(days)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                queuedAlarms = queuedAlarms + draftAlarm()
                                alarmLabel = "Alarm ${queuedAlarms.size + 2}"
                                selectedHour = 7
                                selectedMinute = 0
                                days = emptyList()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Alarm, null)
                            Text(" Add This Alarm and Create Another")
                        }
                    }
                    3 -> {
                        Text("Prepare your routine", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Start with a blank routine. Add the steps you want to complete before the alarm stops.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(20.dp))
                        if (routineSteps.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Outlined.QuestionMark, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(10.dp))
                                    Text("Your routine has no steps yet.", fontWeight = FontWeight.Bold)
                                    Text("Tap Add Step to choose Questions, Scan Barcode, Verify Photo and more.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            routineSteps.forEachIndexed { index, step ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f)),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            when (step.type) {
                                                StepType.QUESTIONS -> Icons.Outlined.QuestionMark
                                                StepType.BARCODE -> Icons.Outlined.QrCodeScanner
                                                StepType.PHOTO -> Icons.Outlined.CameraAlt
                                            },
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text("${index + 1}. ${step.title}", fontWeight = FontWeight.Bold)
                                            Text(
                                                when (step.type) {
                                                    StepType.QUESTIONS -> "${step.questionsRequired} correct • ${step.topics.joinToString { it.displayName }}"
                                                    StepType.BARCODE -> "Configure one or more accepted barcodes in Routines"
                                                    StepType.PHOTO -> "Configure reference photos in Routines"
                                                },
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 13.sp
                                            )
                                        }
                                        IconButton(onClick = { editingStep = step }) {
                                            Icon(Icons.Outlined.Edit, contentDescription = "Edit step")
                                        }
                                        IconButton(onClick = { routineSteps = routineSteps.filterNot { it.id == step.id } }) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "Delete step")
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                        Box(Modifier.fillMaxWidth()) {
                            Button(onClick = { addStepMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Outlined.Add, null)
                                Text(" Add Step")
                            }
                            DropdownMenu(expanded = addStepMenu, onDismissRequest = { addStepMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Questions") },
                                    leadingIcon = { Icon(Icons.Outlined.QuestionMark, null) },
                                    onClick = {
                                        editingStep = RoutineStep(
                                            type = StepType.QUESTIONS,
                                            title = "Answer Questions",
                                            questionsRequired = 2,
                                            topics = listOf(Topic.MATHS)
                                        )
                                        addStepMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Scan Barcode") },
                                    leadingIcon = { Icon(Icons.Outlined.QrCodeScanner, null) },
                                    onClick = {
                                        editingStep = RoutineStep(type = StepType.BARCODE, title = "Scan Barcode")
                                        addStepMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Verify Photo") },
                                    leadingIcon = { Icon(Icons.Outlined.CameraAlt, null) },
                                    onClick = {
                                        editingStep = RoutineStep(type = StepType.PHOTO, title = "Verify Photo")
                                        addStepMenu = false
                                    }
                                )
                            }
                        }
                    }
                    4 -> {
                        Icon(Icons.Outlined.CheckCircle, null, tint = TazGreen)
                        Spacer(Modifier.height(14.dp))
                        Text("Everything is ready", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "TAZLARM will use a two-minute sunrise, rising alarm volume and your configured routine.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(22.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                val alarms = queuedAlarms + draftAlarm()
                                Text("${alarms.size} alarm${if (alarms.size == 1) "" else "s"}", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                                alarms.forEach { alarm ->
                                    Text("${alarm.label}: ${formatPickedTime(alarm.hour, alarm.minute)} • ${daysLabel(alarm.days)}", fontWeight = FontWeight.SemiBold)
                                }
                                Text(if (routineSteps.isEmpty()) "No routine steps added" else "${routineSteps.size} routine step${if (routineSteps.size == 1) "" else "s"} added")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(34.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (page > 0) {
                        OutlinedButton(onClick = { page-- }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.ArrowBack, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Back")
                        }
                    }
                    Button(
                        onClick = {
                            if (page < 4) {
                                page++
                            } else {
                                val alarms = (queuedAlarms + draftAlarm())
                                AppRepository.completeOnboarding(name, alarms)
                                AlarmScheduler.scheduleAll(context)
                            }
                        },
                        enabled = when (page) {
                            1 -> name.isNotBlank()
                            3 -> routineSteps.isNotEmpty()
                            else -> true
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (page == 4) "Finish Setup" else "Continue") }
                }
                Spacer(Modifier.height(26.dp))
            }
        }
    }
}

@Composable
private fun OnboardingPermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    ready: Boolean,
    statusText: String = if (ready) "Allowed" else "Tap to allow",
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(statusText, color = if (ready) TazGreen else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            if (ready) Icon(Icons.Outlined.CheckCircle, null, tint = TazGreen)
        }
    }
}

@Composable
private fun DayChipRows(selected: List<Int>, onToggle: (Int) -> Unit) {
    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.take(4).forEachIndexed { index, label ->
                FilterChip(
                    selected = index + 1 in selected,
                    onClick = { onToggle(index + 1) },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.drop(4).forEachIndexed { index, label ->
                val day = index + 5
                FilterChip(
                    selected = day in selected,
                    onClick = { onToggle(day) },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

fun daysLabel(days: List<Int>): String {
    val sorted = days.distinct().sorted()
    return when {
        sorted.isEmpty() -> "Runs once"
        sorted == listOf(1, 2, 3, 4, 5) -> "Mon–Fri"
        sorted == listOf(6, 7) -> "Weekend"
        sorted == listOf(1, 2, 3, 4, 5, 6, 7) -> "Every day"
        else -> sorted.joinToString(" · ") { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[it - 1] }
    }
}
