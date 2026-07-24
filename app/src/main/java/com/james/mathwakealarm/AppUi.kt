package com.james.mathwakealarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import android.widget.NumberPicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    ALARMS("Alarms", Icons.Outlined.Alarm),
    ROUTINES("Routines", Icons.Outlined.List),
    PROGRESS("Progress", Icons.Outlined.BarChart),
    SETTINGS("Settings", Icons.Outlined.Settings)
}

@Composable
fun TazAlarmApp(appState: AppState) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var editingAlarmId by rememberSaveable { mutableStateOf<String?>(null) }

    val editAlarm = editingAlarmId?.let(AppRepository::alarm)
    if (editAlarm != null) {
        AlarmEditorDialog(
            alarm = editAlarm,
            onDismiss = { editingAlarmId = null },
            onSave = {
                AppRepository.upsertAlarm(it)
                if (it.enabled) AlarmScheduler.schedule(context, it) else AlarmScheduler.cancel(context, it.id)
                editingAlarmId = null
            }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, null) },
                        label = { Text(item.label, maxLines = 1) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == AppTab.ALARMS) {
                FloatingActionButton(onClick = {
                    val alarm = AppRepository.addAlarm()
                    editingAlarmId = alarm.id
                }) { Icon(Icons.Outlined.Add, "Add alarm") }
            }
        }
    ) { padding ->
        when (tab) {
            AppTab.HOME -> HomeScreen(
                appState = appState,
                padding = padding,
                onEditAlarm = { editingAlarmId = it },
                onAddAlarm = {
                    val alarm = AppRepository.addAlarm()
                    editingAlarmId = alarm.id
                },
                onEditRoutine = { tab = AppTab.ROUTINES }
            )
            AppTab.ALARMS -> AlarmsScreen(appState, padding, onEdit = { editingAlarmId = it })
            AppTab.ROUTINES -> RoutinesScreen(appState, padding)
            AppTab.PROGRESS -> ProgressScreen(appState, padding)
            AppTab.SETTINGS -> SettingsScreen(appState, padding)
        }
    }
}

@Composable
private fun ScreenContainer(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
private fun HomeScreen(
    appState: AppState,
    padding: PaddingValues,
    onEditAlarm: (String) -> Unit,
    onAddAlarm: () -> Unit,
    onEditRoutine: () -> Unit
) {
    val context = LocalContext.current
    val now = ZonedDateTime.now()
    val next = appState.alarms.filter { it.enabled }
        .minByOrNull { AlarmScheduler.nextOccurrence(it, now).toInstant() }
    val nextAt = next?.let { AlarmScheduler.nextOccurrence(it, now) }
    val lastRun = appState.runs.firstOrNull()
    val greeting = when (now.hour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    ScreenContainer(padding) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            BrandHeader(compact = true)
        }
        Column {
            Text("$greeting, ${appState.userName.ifBlank { "there" }}", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                if (next == null) "Add an alarm to begin." else "Everything looks ready for your next alarm.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (lastRun != null && lastRun.completed && isToday(lastRun.completedAt) && lastRun.id !in appState.dismissedRunIds) {
            CompletionCard(lastRun, onDismiss = { AppRepository.dismissRun(lastRun.id) })
        }

        if (next != null && nextAt != null) {
            NextAlarmCard(
                alarm = next,
                nextAt = nextAt,
                onEdit = { onEditAlarm(next.id) },
                onEditRoutine = onEditRoutine
            )
        } else {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp)) {
                    Text("No active alarms", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Create an alarm and attach a wake-up routine.")
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = onAddAlarm) { Icon(Icons.Outlined.Add, null); Text(" Add Alarm") }
                }
            }
        }

        ReadinessCard()

        if (next != null) {
            SectionTitle("TODAY'S ROUTINE")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .58f)),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (next.routine.isEmpty()) {
                        Text("No routine steps added", Modifier.padding(8.dp))
                    } else {
                        next.routine.forEachIndexed { index, step ->
                            key(step.id) {
                                HomeRoutineStepCard(
                                    number = index + 1,
                                    step = step,
                                    canUp = index > 0,
                                    canDown = index < next.routine.lastIndex,
                                    onOpen = onEditRoutine,
                                    onUp = {
                                        if (index > 0) {
                                            val list = next.routine.toMutableList()
                                            val previous = list[index - 1]
                                            list[index - 1] = list[index]
                                            list[index] = previous
                                            val updated = next.copy(routine = list)
                                            AppRepository.upsertAlarm(updated)
                                            if (updated.enabled) AlarmScheduler.schedule(context, updated)
                                        }
                                    },
                                    onDown = {
                                        if (index < next.routine.lastIndex) {
                                            val list = next.routine.toMutableList()
                                            val following = list[index + 1]
                                            list[index + 1] = list[index]
                                            list[index] = following
                                            val updated = next.copy(routine = list)
                                            AppRepository.upsertAlarm(updated)
                                            if (updated.enabled) AlarmScheduler.schedule(context, updated)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (lastRun != null) {
            SectionTitle("LAST RESULT")
            LastResultCard(lastRun)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CompletionCard(run: AlarmRun, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF8F0)),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("COMPLETED THIS MORNING", color = TazGreen, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.CheckCircle, null, tint = TazGreen)
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Dismiss summary", tint = TazGreen) }
            }
            Text(formatDuration(run.durationSeconds), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = TazNavy)
            val questions = run.stepResults.sumOf { it.correctAnswers }
            CompletionLine("Questions: $questions correct")
            if (run.stepResults.any { it.type == StepType.BARCODE }) CompletionLine("Barcode scanned successfully")
            if (run.stepResults.any { it.type == StepType.PHOTO }) CompletionLine("Photo verified")
            CompletionLine(if (run.penaltyRouteUsed) "Penalty route completed" else "Penalty route not used")
        }
    }
}

@Composable
private fun CompletionLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.CheckCircle, null, tint = TazGreen, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = TazNavy)
    }
}

@Composable
private fun NextAlarmCard(alarm: AlarmConfig, nextAt: ZonedDateTime, onEdit: () -> Unit, onEditRoutine: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f)),
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NEXT ALARM", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = {
                        AppRepository.toggleAlarm(alarm.id, it)
                        if (it) AlarmScheduler.schedule(context, alarm.copy(enabled = true)) else AlarmScheduler.cancel(context, alarm.id)
                    }
                )
            }
            Text(formatAlarmTime(alarm), fontSize = 46.sp, fontWeight = FontWeight.ExtraBold)
            Text("${relativeDay(nextAt)} • ${alarm.label}", fontWeight = FontWeight.Bold)
            Text("${alarm.routine.size}-step routine • Approximately ${estimateRoutine(alarm.routine)} minutes")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Edit Alarm") }
                Button(onClick = onEditRoutine, modifier = Modifier.weight(1f)) { Text("Edit Routine") }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Skip next occurrence") },
                            leadingIcon = { Icon(Icons.Outlined.SkipNext, null) },
                            onClick = {
                                val occurrence = AlarmScheduler.nextOccurrence(alarm).toInstant().toEpochMilli()
                                AppRepository.setSkipOccurrence(alarm.id, occurrence)
                                AlarmScheduler.schedule(context, alarm.copy(skipOccurrenceAt = occurrence))
                                menu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate alarm") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                            onClick = {
                                val copy = AppRepository.addAlarm(alarm)
                                AlarmScheduler.schedule(context, copy)
                                menu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete alarm", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                AlarmScheduler.cancel(context, alarm.id)
                                AppRepository.deleteAlarm(alarm.id)
                                menu = false
                            }
                        )
                    }
                }
            }
            FilledTonalButton(
                onClick = {
                    AlarmScheduler.scheduleTest(context, alarm.id, 3_000L)
                    Toast.makeText(context, "Test alarm will start in 3 seconds", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.PlayArrow, null)
                Text(" Test Alarm")
            }
        }
    }
}

@Composable
private fun ReadinessCard() {
    val context = LocalContext.current
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    val notificationsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val fullScreenAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        notificationManager.canUseFullScreenIntent()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ready for tomorrow", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReadinessBadge("Exact alarms", exactAllowed, Modifier.weight(1f))
                ReadinessBadge("Notifications", notificationsAllowed, Modifier.weight(1f))
                ReadinessBadge("Lock screen", fullScreenAllowed, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReadinessBadge(label: String, ready: Boolean, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (ready) Color(0xFFEAF8F0) else Color(0xFFFFF4DF)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.NotificationsActive,
                contentDescription = null,
                tint = if (ready) TazGreen else TazAmber,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HomeRoutineStepCard(
    number: Int,
    step: RoutineStep,
    canUp: Boolean,
    canDown: Boolean,
    onOpen: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit
) {
    var dragOffsetY by remember(step.id) { mutableFloatStateOf(0f) }
    var dragging by remember(step.id) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val reorderThresholdPx = with(LocalDensity.current) { 48.dp.toPx() }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (dragging) 10.dp else 1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                translationY = dragOffsetY
                scaleX = if (dragging) 1.015f else 1f
                scaleY = if (dragging) 1.015f else 1f
            }
            .pointerInput(step.id, canUp, canDown) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragging = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragCancel = {
                        dragging = false
                        dragOffsetY = 0f
                    },
                    onDragEnd = {
                        dragging = false
                        dragOffsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y
                        when {
                            dragOffsetY <= -reorderThresholdPx && canUp -> {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onUp()
                                dragOffsetY = 0f
                            }
                            dragOffsetY >= reorderThresholdPx && canDown -> {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDown()
                                dragOffsetY = 0f
                            }
                        }
                    }
                )
            }
            .clickable(onClick = onOpen)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.DragHandle, "Hold and drag to reorder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.size(30.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(number.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            Spacer(Modifier.width(10.dp))
            Icon(stepIcon(step.type), null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (step.type == StepType.BARCODE) "Scan Barcode" else step.title, fontWeight = FontWeight.Bold)
                Text(stepStatus(step), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(enabled = canUp, onClick = onUp) { Icon(Icons.Outlined.ArrowUpward, "Move up") }
            IconButton(enabled = canDown, onClick = onDown) { Icon(Icons.Outlined.ArrowDownward, "Move down") }
        }
    }
}

@Composable
private fun LastResultCard(run: AlarmRun) {
    val attempted = run.stepResults.sumOf { result -> result.topicScores.sumOf { it.attempted } }
    val correct = run.stepResults.sumOf { result -> result.topicScores.sumOf { it.correct } }
    val accuracy = if (attempted <= 0) 100 else (correct * 100 / attempted).coerceIn(0, 100)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Completed in ${formatDuration(run.durationSeconds)}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Accuracy $accuracy%", fontWeight = FontWeight.SemiBold)
                Text("${run.stepResults.size} steps", fontWeight = FontWeight.SemiBold)
                Text(if (run.penaltyRouteUsed) "Penalty used" else "No penalty", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(7.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
        }
    }
}

@Composable
private fun AlarmCompactRow(alarm: AlarmConfig, onEdit: (String) -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clickable { onEdit(alarm.id) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(formatAlarmTime(alarm), fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(105.dp))
        Text(daysLabel(alarm.days), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Switch(checked = alarm.enabled, onCheckedChange = {
            AppRepository.toggleAlarm(alarm.id, it)
            if (it) AlarmScheduler.schedule(context, alarm.copy(enabled = true)) else AlarmScheduler.cancel(context, alarm.id)
        })
    }
}

@Composable
private fun AlarmsScreen(appState: AppState, padding: PaddingValues, onEdit: (String) -> Unit) {
    val context = LocalContext.current
    ScreenContainer(padding) {
        PageHeader("Alarms", "Create independent alarms with their own days and wake-up routines.")
        appState.alarms.forEach { alarm ->
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(formatAlarmTime(alarm), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                            Text(alarm.label, fontWeight = FontWeight.Bold)
                            Text(daysLabel(alarm.days), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = alarm.enabled, onCheckedChange = {
                            AppRepository.toggleAlarm(alarm.id, it)
                            if (it) AlarmScheduler.schedule(context, alarm.copy(enabled = true)) else AlarmScheduler.cancel(context, alarm.id)
                        })
                    }
                    Text("${alarm.routine.size} steps • ${alarm.sunriseSeconds}-second sunrise")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { onEdit(alarm.id) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Edit, null); Text(" Edit")
                        }
                        FilledTonalButton(onClick = {
                            AlarmScheduler.scheduleTest(context, alarm.id, 5_000L)
                            Toast.makeText(context, "Test alarm scheduled", Toast.LENGTH_SHORT).show()
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.PlayArrow, null); Text(" Test")
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(76.dp))
    }
}

@Composable
private fun AlarmEditorDialog(alarm: AlarmConfig, onDismiss: () -> Unit, onSave: (AlarmConfig) -> Unit) {
    var hour24 by remember(alarm.id) { mutableIntStateOf(alarm.hour) }
    var minute by remember(alarm.id) { mutableIntStateOf(alarm.minute) }
    var days by remember(alarm.id) { mutableStateOf(alarm.days) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Edit Alarm", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TazNavy)
                        Text(
                            "Alarm in ${timeUntilAlarmText(hour24, minute, days)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, null)
                        Text(" Close")
                    }
                }

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = TazBlueLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Pick a time", fontWeight = FontWeight.Bold, color = TazNavy)
                        WheelTimePicker(
                            hour24 = hour24,
                            minute = minute,
                            onTimeChange = { h, m ->
                                hour24 = h
                                minute = m
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = TazBlueLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Repeat", fontWeight = FontWeight.Bold, color = TazNavy)
                        RepeatPresetRow(
                            selectedDays = days,
                            onSelectOnce = { days = emptyList() },
                            onSelectWeekdays = { days = listOf(1, 2, 3, 4, 5) },
                            onSelectCustom = { if (days.isEmpty() || days == listOf(1, 2, 3, 4, 5)) days = listOf(1) }
                        )
                        if (days.isNotEmpty() && days != listOf(1, 2, 3, 4, 5)) {
                            AlarmDaySelector(days) { day ->
                                days = if (day in days) (days - day).sorted() else (days + day).sorted()
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSave(
                                alarm.copy(
                                    hour = hour24,
                                    minute = minute,
                                    days = days,
                                    enabled = if (!alarm.enabled && alarm.label == "New Alarm") true else alarm.enabled
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Alarm")
                    }
                }
            }
        }
    }
}

@Composable
fun WheelTimePicker(
    hour24: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayHour = ((hour24 + 11) % 12) + 1
    val amPmIndex = if (hour24 < 12) 0 else 1
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberPickerColumn(value = displayHour, range = 1..12, onValueChange = { newHour12 ->
                val newHour24 = when {
                    amPmIndex == 0 && newHour12 == 12 -> 0
                    amPmIndex == 0 -> newHour12
                    amPmIndex == 1 && newHour12 == 12 -> 12
                    else -> newHour12 + 12
                }
                onTimeChange(newHour24, minute)
            })
            Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            NumberPickerColumn(value = minute, range = 0..59, format = { it.toString().padStart(2, '0') }, onValueChange = { onTimeChange(hour24, it) })
            NumberPickerLabels(value = amPmIndex, values = listOf("AM", "PM"), onValueChange = { newIndex ->
                val currentHour12 = displayHour
                val newHour24 = when {
                    newIndex == 0 && currentHour12 == 12 -> 0
                    newIndex == 0 -> currentHour12
                    newIndex == 1 && currentHour12 == 12 -> 12
                    else -> currentHour12 + 12
                }
                onTimeChange(newHour24, minute)
            })
        }
    }
}

@Composable
private fun NumberPickerColumn(
    value: Int,
    range: IntRange,
    format: (Int) -> String = { it.toString() },
    onValueChange: (Int) -> Unit
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentFormat by rememberUpdatedState(format)
    AndroidView(
        factory = { context ->
            NumberPicker(context).apply {
                minValue = range.first
                maxValue = range.last
                wrapSelectorWheel = true
                descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                setFormatter { currentFormat(it) }
                setOnValueChangedListener { _, _, newVal -> currentOnValueChange(newVal) }
            }
        },
        update = {
            it.minValue = range.first
            it.maxValue = range.last
            it.setFormatter { v -> currentFormat(v) }
            it.setOnValueChangedListener { _, _, newVal -> currentOnValueChange(newVal) }
            if (it.value != value) it.value = value
        },
        modifier = Modifier.width(84.dp).height(170.dp)
    )
}

@Composable
private fun NumberPickerLabels(value: Int, values: List<String>, onValueChange: (Int) -> Unit) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    AndroidView(
        factory = { context ->
            NumberPicker(context).apply {
                minValue = 0
                maxValue = values.lastIndex
                wrapSelectorWheel = false
                descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                displayedValues = values.toTypedArray()
                setOnValueChangedListener { _, _, newVal -> currentOnValueChange(newVal) }
            }
        },
        update = {
            it.displayedValues = null
            it.minValue = 0
            it.maxValue = values.lastIndex
            it.displayedValues = values.toTypedArray()
            it.setOnValueChangedListener { _, _, newVal -> currentOnValueChange(newVal) }
            if (it.value != value) it.value = value
        },
        modifier = Modifier.width(92.dp).height(170.dp)
    )
}

@Composable
private fun RepeatPresetRow(
    selectedDays: List<Int>,
    onSelectOnce: () -> Unit,
    onSelectWeekdays: () -> Unit,
    onSelectCustom: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selectedDays.isEmpty(),
            onClick = onSelectOnce,
            label = { Text("ONCE") },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selectedDays == listOf(1, 2, 3, 4, 5),
            onClick = onSelectWeekdays,
            label = { Text("WEEKDAYS") },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selectedDays.isNotEmpty() && selectedDays != listOf(1, 2, 3, 4, 5),
            onClick = onSelectCustom,
            label = { Text("CUSTOM") },
            modifier = Modifier.weight(1f)
        )
    }
}

private fun timeUntilAlarmText(hour: Int, minute: Int, days: List<Int>): String {
    val now = java.time.ZonedDateTime.now()
    var candidate = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
    if (days.isEmpty()) {
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
    } else {
        val targetDays = days.map { if (it == 7) java.time.DayOfWeek.SUNDAY.value else it }.toSet()
        repeat(8) {
            if (candidate.isAfter(now) && candidate.dayOfWeek.value in targetDays) return@repeat
            candidate = candidate.plusDays(1).withHour(hour).withMinute(minute)
        }
        while (!(candidate.isAfter(now) && candidate.dayOfWeek.value in targetDays)) {
            candidate = candidate.plusDays(1).withHour(hour).withMinute(minute)
        }
    }
    val duration = java.time.Duration.between(now, candidate)
    val totalMinutes = duration.toMinutes().coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours hours and $minutes minutes"
        hours > 0 -> "$hours hours"
        else -> "$minutes minutes"
    }
}

@Composable
private fun AlarmDaySelector(selected: List<Int>, onToggle: (Int) -> Unit) {
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEachIndexed { index, label ->
            FilterChip(
                selected = index + 1 in selected,
                onClick = { onToggle(index + 1) },
                label = { Text(label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RoutinesScreen(appState: AppState, padding: PaddingValues) {
    val context = LocalContext.current
    var selectedId by rememberSaveable { mutableStateOf(appState.alarms.firstOrNull()?.id) }
    val selected = appState.alarms.firstOrNull { it.id == selectedId } ?: appState.alarms.firstOrNull()
    var editingStep by remember { mutableStateOf<RoutineStep?>(null) }
    var captureUri by remember { mutableStateOf<Uri?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val uri = captureUri?.toString()
            if (uri != null) editingStep = editingStep?.copy(referenceUris = (editingStep!!.referenceUris + uri).distinct().take(5))
        }
    }

    fun persistRoutine(routine: List<RoutineStep>) {
        selected?.let {
            val updated = it.copy(routine = routine)
            AppRepository.upsertAlarm(updated)
            if (updated.enabled) AlarmScheduler.schedule(context, updated)
        }
    }

    if (editingStep != null && selected != null) {
        StepEditorDialog(
            step = editingStep!!,
            onDismiss = { editingStep = null },
            onScan = {
                GmsBarcodeScanning.getClient(context).startScan()
                    .addOnSuccessListener { barcode ->
                        val captured = BarcodeIdentity.capture(barcode)
                        editingStep = editingStep?.let { current ->
                            current.copy(
                                barcodeValue = captured,
                                barcodeValues = (current.savedBarcodeValues() + captured).distinct(),
                                title = "Scan Barcode"
                            )
                        }
                    }
                    .addOnFailureListener { Toast.makeText(context, "Scanner could not start", Toast.LENGTH_SHORT).show() }
            },
            onCapture = {
                captureUri = PhotoStore.createCaptureUri(context, "routine")
                captureUri?.let { photoLauncher.launch(it) }
            },
            onSave = { saved ->
                val exists = selected.routine.any { it.id == saved.id }
                persistRoutine(if (exists) selected.routine.map { if (it.id == saved.id) saved else it } else selected.routine + saved)
                editingStep = null
            }
        )
    }

    ScreenContainer(padding) {
        PageHeader("Wake-Up Routines", "Build the exact sequence required to stop each alarm.")
        if (appState.alarms.isEmpty()) {
            Text("Create an alarm first.")
            return@ScreenContainer
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            appState.alarms.forEach { alarm ->
                FilterChip(
                    selected = selected?.id == alarm.id,
                    onClick = { selectedId = alarm.id },
                    label = { Text(alarm.label) }
                )
            }
        }
        selected?.let { alarm ->
            SectionTitle("${alarm.label.uppercase()} • ${alarm.routine.size} STEPS")
            alarm.routine.forEachIndexed { index, step ->
                key(step.id) {
                    RoutineStepCard(
                        number = index + 1,
                        step = step,
                        canUp = index > 0,
                        canDown = index < alarm.routine.lastIndex,
                        onEdit = { editingStep = step },
                        onDuplicate = {
                            val list = alarm.routine.toMutableList()
                            list.add(index + 1, step.copy(id = UUID.randomUUID().toString()))
                            persistRoutine(list)
                        },
                        onDelete = { persistRoutine(alarm.routine.filterNot { it.id == step.id }) },
                        onUp = {
                            if (index > 0) {
                                val list = alarm.routine.toMutableList()
                                val previous = list[index - 1]
                                list[index - 1] = list[index]
                                list[index] = previous
                                persistRoutine(list)
                            }
                        },
                        onDown = {
                            if (index < alarm.routine.lastIndex) {
                                val list = alarm.routine.toMutableList()
                                val following = list[index + 1]
                                list[index + 1] = list[index]
                                list[index] = following
                                persistRoutine(list)
                            }
                        }
                    )
                }
            }
            OutlinedButton(
                onClick = { editingStep = RoutineStep(type = StepType.QUESTIONS, title = "Answer Questions", questionsRequired = 2) },
                modifier = Modifier.fillMaxWidth()
            ) { Icon(Icons.Outlined.Add, null); Text(" Add Step") }

            SectionTitle("ROUTINE PRESETS")
            PresetCard("Quick Start", "2 questions → barcode", "2–3 min") { persistRoutine(preserveRoutineRegistrations(alarm.routine, quickStartRoutine())) }
            PresetCard("Normal Workday", "2 questions → barcode → 3 questions", "4–6 min") { persistRoutine(preserveRoutineRegistrations(alarm.routine, normalWorkdayRoutine())) }
            PresetCard("Must Get Up", "questions → barcode → questions → photo", "6–9 min") { persistRoutine(preserveRoutineRegistrations(alarm.routine, mustGetUpRoutine())) }
            PresetCard("Weekend", "lighter questions → photo", "2–4 min") { persistRoutine(preserveRoutineRegistrations(alarm.routine, weekendRoutine())) }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun RoutineStepCard(
    number: Int,
    step: RoutineStep,
    canUp: Boolean,
    canDown: Boolean,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit
) {
    var dragOffsetY by remember(step.id) { mutableFloatStateOf(0f) }
    var dragging by remember(step.id) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val reorderThresholdPx = with(LocalDensity.current) { 54.dp.toPx() }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (dragging) 12.dp else 2.dp),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                translationY = dragOffsetY
                scaleX = if (dragging) 1.02f else 1f
                scaleY = if (dragging) 1.02f else 1f
            }
            .pointerInput(step.id, canUp, canDown) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragging = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragCancel = {
                        dragging = false
                        dragOffsetY = 0f
                    },
                    onDragEnd = {
                        dragging = false
                        dragOffsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y
                        when {
                            dragOffsetY <= -reorderThresholdPx && canUp -> {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onUp()
                                dragOffsetY = 0f
                            }
                            dragOffsetY >= reorderThresholdPx && canDown -> {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDown()
                                dragOffsetY = 0f
                            }
                        }
                    }
                )
            }
            .clickable(onClick = onEdit)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.DragHandle,
                    contentDescription = "Hold and drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(38.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(number.toString(), color = Color.White, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(12.dp))
                Icon(stepIcon(step.type), null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (step.type == StepType.BARCODE) "Scan Barcode" else step.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(stepStatus(step), color = if (stepReady(step)) TazGreen else TazAmber, fontSize = 13.sp)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Edit") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(enabled = canUp, onClick = onUp) { Icon(Icons.Outlined.ArrowUpward, "Move up") }
                IconButton(enabled = canDown, onClick = onDown) { Icon(Icons.Outlined.ArrowDownward, "Move down") }
                IconButton(onClick = onDuplicate) { Icon(Icons.Outlined.ContentCopy, "Duplicate") }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun StepEditorDialog(
    step: RoutineStep,
    onDismiss: () -> Unit,
    onScan: () -> Unit,
    onCapture: () -> Unit,
    onSave: (RoutineStep) -> Unit
) {
    var type by remember(step.id) { mutableStateOf(step.type) }
    var title by remember(step.id, step.title) { mutableStateOf(step.title) }
    var count by remember(step.id) { mutableIntStateOf(step.questionsRequired.coerceAtLeast(2).coerceAtMost(10)) }
    var topics by remember(step.id) { mutableStateOf(step.topics.ifEmpty { listOf(Topic.MATHS) }) }
    var topicMessage by remember(step.id) { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(TazBlueLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when (type) {
                                StepType.QUESTIONS -> Icons.Outlined.QuestionMark
                                StepType.BARCODE -> Icons.Outlined.QrCodeScanner
                                StepType.PHOTO -> Icons.Outlined.CameraAlt
                            },
                            null,
                            tint = TazBlue
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Configure Step", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TazNavy)
                        Text("Set up exactly what this wake-up step should do.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Close") }
                }

                Text("Step type", fontWeight = FontWeight.Bold, color = TazNavy)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StepType.entries.forEach { option ->
                        val selected = type == option
                        OutlinedCard(
                            modifier = Modifier.weight(1f).clickable { type = option },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (selected) TazBlueLight else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    when (option) {
                                        StepType.QUESTIONS -> Icons.Outlined.QuestionMark
                                        StepType.BARCODE -> Icons.Outlined.QrCodeScanner
                                        StepType.PHOTO -> Icons.Outlined.CameraAlt
                                    },
                                    null,
                                    tint = if (selected) TazBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    when (option) {
                                        StepType.QUESTIONS -> "Questions"
                                        StepType.BARCODE -> "Barcode"
                                        StepType.PHOTO -> "Photo"
                                    },
                                    color = if (selected) TazBlue else TazNavy,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (type != StepType.BARCODE) {
                    Text("Step name", fontWeight = FontWeight.Bold, color = TazNavy)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(if (type == StepType.QUESTIONS) "Answer Questions" else "Verify Photo") }
                    )
                }

                when (type) {
                    StepType.QUESTIONS -> {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = TazBlueLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Correct answers required", fontWeight = FontWeight.Bold, color = TazNavy)
                                Text(count.toString(), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = TazBlue)
                                Slider(
                                    value = count.toFloat(),
                                    onValueChange = { count = it.toInt().coerceIn(1, 10) },
                                    valueRange = 1f..10f,
                                    steps = 8
                                )
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    (1..10).forEach { value ->
                                        Text(
                                            value.toString(),
                                            fontSize = 12.sp,
                                            color = if (value == count) TazBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Text("Question topics", fontWeight = FontWeight.Bold, color = TazNavy)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Topic.entries.chunked(3).forEach { rowTopics ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowTopics.forEach { topic ->
                                        val selected = topic in topics
                                        FilterChip(
                                            selected = selected,
                                            onClick = {
                                                topicMessage = false
                                                topics = when {
                                                    selected && topics.size == 1 -> {
                                                        topicMessage = true
                                                        topics
                                                    }
                                                    selected -> topics - topic
                                                    else -> topics + topic
                                                }
                                            },
                                            label = { Text(topic.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(3 - rowTopics.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                        if (topicMessage) {
                            Text("Choose at least one topic.", color = TazBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    StepType.BARCODE -> {
                        val savedBarcodes = step.savedBarcodeValues()
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = TazBlueLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Scan Barcode", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TazNavy)
                                Text(
                                    if (savedBarcodes.isEmpty()) "No barcodes registered yet" else "${savedBarcodes.size} accepted barcode${if (savedBarcodes.size == 1) "" else "s"}",
                                    color = if (savedBarcodes.isEmpty()) TazAmber else TazGreen
                                )
                                savedBarcodes.forEachIndexed { index, _ ->
                                    Text("Barcode ${index + 1} saved", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                OutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Outlined.QrCodeScanner, null)
                                    Text(if (savedBarcodes.isEmpty()) " Add Barcode" else " Add Another Barcode")
                                }
                                Text("Any one saved barcode can complete this step when the alarm goes off.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    StepType.PHOTO -> {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = TazBlueLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Verify Photo", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TazNavy)
                                Text("${step.referenceUris.size} reference photos", color = if (step.referenceUris.size >= 3) TazGreen else TazAmber)
                                OutlinedButton(onClick = onCapture, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Outlined.CameraAlt, null)
                                    Text(" Add Live Reference Photo")
                                }
                                Text("Add 3–5 photos from slightly different angles and lighting.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (type == StepType.QUESTIONS && topics.isEmpty()) {
                                topicMessage = true
                            } else {
                                onSave(
                                    step.copy(
                                        type = type,
                                        title = when (type) {
                                            StepType.BARCODE -> "Scan Barcode"
                                            StepType.QUESTIONS -> title.ifBlank { "Answer Questions" }
                                            StepType.PHOTO -> title.ifBlank { "Verify Photo" }
                                        },
                                        questionsRequired = if (type == StepType.QUESTIONS) count.coerceIn(1, 10) else 0,
                                        topics = if (type == StepType.QUESTIONS) topics.ifEmpty { listOf(Topic.MATHS) } else emptyList()
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Step")
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetCard(title: String, description: String, duration: String, onClick: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.RestartAlt, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            Text(duration, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProgressScreen(appState: AppState, padding: PaddingValues) {
    val completed = appState.runs.filter { it.completed }
    val average = completed.map { it.durationSeconds }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
    val penaltyCount = completed.count { it.penaltyRouteUsed }
    val topicTotals = mutableMapOf<Topic, Pair<Int, Int>>()
    completed.flatMap { it.stepResults }.flatMap { it.topicScores }.forEach { score ->
        val existing = topicTotals[score.topic] ?: (0 to 0)
        topicTotals[score.topic] = (existing.first + score.correct) to (existing.second + score.attempted)
    }

    ScreenContainer(padding) {
        PageHeader("Progress", "See which parts of your routine are actually getting you awake.")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("Completed", completed.size.toString(), Modifier.weight(1f))
            MetricCard("Avg time", formatDuration(average), Modifier.weight(1f))
            MetricCard("Penalties", penaltyCount.toString(), Modifier.weight(1f))
        }
        SectionTitle("RECENT MORNINGS")
        if (completed.isEmpty()) {
            Card(Modifier.fillMaxWidth()) { Text("Your completed alarm history will appear here.", Modifier.padding(18.dp)) }
        } else {
            completed.take(10).forEach { run ->
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = TazGreen)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(run.alarmLabel, fontWeight = FontWeight.Bold)
                            Text(formatDateTime(run.completedAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatDuration(run.durationSeconds), fontWeight = FontWeight.ExtraBold)
                            Text("${run.stepResults.size} steps", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        SectionTitle("TOPIC ACCURACY")
        Topic.entries.forEach { topic ->
            val total = topicTotals[topic]
            val percentage = if (total == null || total.second == 0) 0 else total.first * 100 / total.second
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(topic.displayName, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(if (total == null) "No data" else "$percentage%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        SectionTitle("ALARM RELIABILITY")
        appState.reliabilityEvents.take(12).forEach { event ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Check, null, tint = TazGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(event.stage, fontWeight = FontWeight.SemiBold)
                    Text(formatDateTime(event.occurredAt), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsScreen(appState: AppState, padding: PaddingValues) {
    val context = LocalContext.current
    var name by remember(appState.userName) { mutableStateOf(appState.userName) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    val notificationAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    val fullScreenAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        notificationManager.canUseFullScreenIntent()

    ScreenContainer(padding) {
        PageHeader("Settings", "Personalisation, appearance and Android reliability checks.")
        SectionTitle("PROFILE")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Button(onClick = { AppRepository.setName(name) }, modifier = Modifier.fillMaxWidth()) { Text("Save Name") }
            }
        }
        SectionTitle("APPEARANCE")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                ThemeChoice(ThemeMode.SYSTEM, "Follow phone", Icons.Outlined.Tune, appState.themeMode)
                ThemeChoice(ThemeMode.LIGHT, "Light mode", Icons.Outlined.LightMode, appState.themeMode)
                ThemeChoice(ThemeMode.DARK, "Dark mode", Icons.Outlined.DarkMode, appState.themeMode)
            }
        }
        SectionTitle("ALARM RELIABILITY")
        PermissionCard(
            Icons.Outlined.NotificationsActive,
            "Alarm notifications",
            if (notificationAllowed) "Allowed" else "Permission required",
            notificationAllowed
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        PermissionCard(
            Icons.Outlined.NotificationsActive,
            "Full-screen lock-screen alarms",
            if (fullScreenAllowed) "Allowed — alarm can open over the lock screen" else "Permission required",
            fullScreenAllowed
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }.onFailure {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                    )
                }
            }
        }
        PermissionCard(
            Icons.Outlined.Schedule,
            "Exact alarm timing",
            if (exactAllowed) "Allowed" else "Open Android setting",
            exactAllowed
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
            }
        }
        PermissionCard(
            Icons.Outlined.BatteryChargingFull,
            "Battery optimisation",
            "Request unrestricted background operation",
            false
        ) {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")))
            }.onFailure {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WbSunny, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Two-minute screen-off test", fontWeight = FontWeight.Bold)
                        Text("Lock the phone after pressing test. The alarm should still open and sound.", fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val alarm = appState.alarms.firstOrNull()
                        if (alarm != null) {
                            AlarmScheduler.scheduleTest(context, alarm.id, 120_000L)
                            Toast.makeText(context, "Test scheduled in 2 minutes — lock the phone", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = appState.alarms.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Schedule Screen-Off Test") }
            }
        }
        OutlinedButton(onClick = { AlarmScheduler.scheduleAll(context) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.RestartAlt, null); Text(" Reschedule All Alarms")
        }
        Text("TAZLARM v2.2.9", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ThemeChoice(mode: ThemeMode, label: String, icon: ImageVector, current: ThemeMode) {
    Row(Modifier.fillMaxWidth().clickable { AppRepository.setTheme(mode) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null)
        Spacer(Modifier.width(10.dp))
        Text(label, modifier = Modifier.weight(1f))
        RadioButton(selected = current == mode, onClick = { AppRepository.setTheme(mode) })
    }
}

@Composable
private fun PermissionCard(icon: ImageVector, title: String, subtitle: String, ready: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.Edit, null, tint = if (ready) TazGreen else TazAmber)
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String) {
    Column {
        BrandHeader(compact = true)
        Spacer(Modifier.height(18.dp))
        Text(title, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun stepIcon(type: StepType): ImageVector = when (type) {
    StepType.QUESTIONS -> Icons.Outlined.QuestionMark
    StepType.BARCODE -> Icons.Outlined.QrCodeScanner
    StepType.PHOTO -> Icons.Outlined.CameraAlt
}


private fun RoutineStep.savedBarcodeValues(): List<String> = (barcodeValues + listOf(barcodeValue)).filter { it.isNotBlank() }.distinct()

private fun preserveRoutineRegistrations(
    existing: List<RoutineStep>,
    preset: List<RoutineStep>
): List<RoutineStep> {
    val barcodes = existing.filter { it.type == StepType.BARCODE && it.savedBarcodeValues().isNotEmpty() }
    val photos = existing.filter { it.type == StepType.PHOTO && it.referenceUris.isNotEmpty() }
    var barcodeIndex = 0
    var photoIndex = 0

    return preset.map { step ->
        when (step.type) {
            StepType.BARCODE -> {
                val configured = barcodes.getOrNull(barcodeIndex++) ?: barcodes.firstOrNull()
                if (configured == null) step else step.copy(barcodeValue = configured.barcodeValue, barcodeValues = configured.savedBarcodeValues())
            }
            StepType.PHOTO -> {
                val configured = photos.getOrNull(photoIndex++) ?: photos.firstOrNull()
                if (configured == null) step else step.copy(
                    referenceUris = configured.referenceUris,
                    photoThreshold = configured.photoThreshold
                )
            }
            StepType.QUESTIONS -> step
        }
    }
}

private fun stepReady(step: RoutineStep): Boolean = when (step.type) {
    StepType.QUESTIONS -> step.questionsRequired > 0 && step.topics.isNotEmpty()
    StepType.BARCODE -> step.savedBarcodeValues().isNotEmpty()
    StepType.PHOTO -> step.referenceUris.size >= 3
}

private fun stepStatus(step: RoutineStep): String = when (step.type) {
    StepType.QUESTIONS -> "${step.questionsRequired} correct • ${step.topics.joinToString { it.displayName }}"
    StepType.BARCODE -> if (step.savedBarcodeValues().isEmpty()) "No barcodes registered" else "${step.savedBarcodeValues().size} accepted barcode${if (step.savedBarcodeValues().size == 1) "" else "s"} • Ready"
    StepType.PHOTO -> "${step.referenceUris.size} reference photos${if (step.referenceUris.size >= 3) " • Ready" else ""}"
}

private fun formatAlarmTime(alarm: AlarmConfig): String {
    val date = ZonedDateTime.now().withHour(alarm.hour).withMinute(alarm.minute)
    return date.format(DateTimeFormatter.ofPattern("h:mm a"))
}

private fun relativeDay(value: ZonedDateTime): String {
    val today = ZonedDateTime.now().toLocalDate()
    return when (value.toLocalDate()) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> value.format(DateTimeFormatter.ofPattern("EEEE"))
    }
}

private fun estimateRoutine(routine: List<RoutineStep>): String {
    val min = routine.sumOf {
        when (it.type) {
            StepType.QUESTIONS -> it.questionsRequired * 10
            StepType.BARCODE -> 35
            StepType.PHOTO -> 40
        }
    } / 60
    val low = min.coerceAtLeast(2)
    return "$low–${low + 3}"
}

private fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val minutes = safe / 60
    val remainder = safe % 60
    return if (minutes > 0) "${minutes}m ${remainder}s" else "${remainder}s"
}

private fun formatDateTime(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("EEE d MMM, h:mm a"))

private fun isToday(epochMillis: Long): Boolean = Instant.ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault()).toLocalDate() == ZonedDateTime.now().toLocalDate()
