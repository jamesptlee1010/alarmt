package com.james.mathwakealarm

import android.app.TimePickerDialog
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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.QrCodeScanner
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

    val timePicker = remember(selectedHour, selectedMinute) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
            },
            selectedHour,
            selectedMinute,
            false
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
                    progress = { (page + 1) / 4f },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(28.dp))

                when (page) {
                    0 -> {
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
                            supportingText = { Text("Used in your morning, afternoon or evening greeting") },
                            singleLine = true
                        )
                    }
                    1 -> {
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
                        OutlinedButton(onClick = { timePicker.updateTime(selectedHour, selectedMinute); timePicker.show() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Alarm, null)
                            Text(" Pick time: ${formatPickedTime(selectedHour, selectedMinute)}")
                        }
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
                    2 -> {
                        Text("Prepare your routine", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Start with a blank routine. Add the steps you want to complete before the alarm stops.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(20.dp))
                        if (routineSteps.isEmpty()) {
                            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Outlined.QuestionMark, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(10.dp))
                                    Text("Your routine has no steps yet.", fontWeight = FontWeight.Bold)
                                    Text("Tap Add Step to choose Questions, Scan Barcode, Verify Photo and more.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            routineSteps.forEachIndexed { index, step ->
                                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
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
                                        IconButton(onClick = { routineSteps = routineSteps.filterNot { it.id == step.id } }) {
                                            Icon(Icons.Outlined.Delete, null)
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
                                        routineSteps = routineSteps + RoutineStep(
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
                                        routineSteps = routineSteps + RoutineStep(type = StepType.BARCODE, title = "Scan Barcode")
                                        addStepMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Verify Photo") },
                                    leadingIcon = { Icon(Icons.Outlined.CameraAlt, null) },
                                    onClick = {
                                        routineSteps = routineSteps + RoutineStep(type = StepType.PHOTO, title = "Verify Photo")
                                        addStepMenu = false
                                    }
                                )
                            }
                        }
                    }
                    else -> {
                        Icon(Icons.Outlined.CheckCircle, null, tint = TazGreen)
                        Spacer(Modifier.height(14.dp))
                        Text("Everything is ready", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "TAZLARM will use a one-minute sunrise, rising alarm volume and your configured routine.",
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
                            if (page < 3) {
                                page++
                            } else {
                                val alarms = (queuedAlarms + draftAlarm())
                                AppRepository.completeOnboarding(name, alarms)
                                AlarmScheduler.scheduleAll(context)
                            }
                        },
                        enabled = when (page) {
                            0 -> name.isNotBlank()
                            2 -> routineSteps.isNotEmpty()
                            else -> true
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (page == 3) "Finish Setup" else "Continue") }
                }
                Spacer(Modifier.height(26.dp))
            }
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
