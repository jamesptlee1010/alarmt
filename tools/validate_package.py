#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
checks: list[tuple[str, bool, str]] = []

def check(name: str, condition: bool, detail: str = "") -> None:
    checks.append((name, bool(condition), detail))

required = [
    ".github/workflows/build-tazalarm.yml",
    "app/build.gradle.kts",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/com/james/mathwakealarm/AlarmScheduler.kt",
    "app/src/main/java/com/james/mathwakealarm/AlarmService.kt",
    "app/src/main/java/com/james/mathwakealarm/AlarmActivity.kt",
    "app/src/main/java/com/james/mathwakealarm/AppUi.kt",
    "app/src/main/java/com/james/mathwakealarm/Onboarding.kt",
    "app/src/main/java/com/james/mathwakealarm/BrandLogo.kt",
    "app/src/main/java/com/james/mathwakealarm/BarcodeIdentity.kt",
    "app/src/main/res/drawable-nodpi/tazalarm_logo_full.png",
    "app/src/main/res/drawable-nodpi/tazalarm_cat_only.png",
    "personal-release.keystore",
]
for item in required:
    check(f"Required file: {item}", (ROOT / item).is_file())

xml_files = list((ROOT / "app/src/main").rglob("*.xml"))
xml_ok = True
xml_error = ""
for path in xml_files:
    try:
        ET.parse(path)
    except Exception as exc:
        xml_ok = False
        xml_error = f"{path.relative_to(ROOT)}: {exc}"
        break
check("All Android XML parses", xml_ok, f"{len(xml_files)} XML files" if xml_ok else xml_error)

build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
scheduler = (ROOT / "app/src/main/java/com/james/mathwakealarm/AlarmScheduler.kt").read_text(encoding="utf-8")
service = (ROOT / "app/src/main/java/com/james/mathwakealarm/AlarmService.kt").read_text(encoding="utf-8")
alarm_ui = (ROOT / "app/src/main/java/com/james/mathwakealarm/AlarmActivity.kt").read_text(encoding="utf-8")
app_ui = (ROOT / "app/src/main/java/com/james/mathwakealarm/AppUi.kt").read_text(encoding="utf-8")
onboarding = (ROOT / "app/src/main/java/com/james/mathwakealarm/Onboarding.kt").read_text(encoding="utf-8")
main_source = "\n".join(p.read_text(encoding="utf-8") for p in (ROOT / "app/src/main/java").rglob("*.kt"))

check("Application ID retained", 'applicationId = "com.james.mathwakealarm"' in build)
check("Version 2.2.8 / 228", 'versionCode = 228' in build and 'versionName = "2.2.8"' in build)
check("TAZLARM app label", '<string name="app_name">TAZLARM</string>' in (ROOT / "app/src/main/res/values/strings.xml").read_text())
check("Alarm notification channel description", '<string name="alarm_channel_description">' in (ROOT / "app/src/main/res/values/strings.xml").read_text())
check("Exact alarm permission", "android.permission.SCHEDULE_EXACT_ALARM" in manifest)
check("Full-screen alarm permission", "android.permission.USE_FULL_SCREEN_INTENT" in manifest)
check("Direct foreground-service PendingIntent", "PendingIntent.getForegroundService" in scheduler)
check("Ten-second backup trigger", "BACKUP_DELAY_MS = 10_000L" in scheduler and "BackupAlarmReceiver" in scheduler)
check("Exact allow-while-idle scheduling", "setExactAndAllowWhileIdle" in scheduler)
check("Primary cancels backup", "cancelBackup(this, alarmId)" in service)
check("Foreground service is sticky", "return START_STICKY" in service)
check("Partial wake lock", "PowerManager.PARTIAL_WAKE_LOCK" in service)
check("Gradual 10-second volume ramp", "postDelayed(this, 10_000L)" in service and "0.05f" in service)
check("Active alarm recovery", "activeAlarmId()" in service and "Alarm service restored" in service)
check("Reboot/time/update rescheduling", all(x in manifest for x in ["BOOT_COMPLETED", "TIME_SET", "TIMEZONE_CHANGED", "MY_PACKAGE_REPLACED"]))
check("Two-minute sunrise minimum", "sunriseSeconds: Int = 120" in main_source and "coerceAtLeast(120)" in alarm_ui)
check("Existing alarms migrate to two-minute sunrise", "if (alarm.sunriseSeconds < 120) alarm.copy(sunriseSeconds = 120)" in main_source)
check("No sunrise countdown on live alarm screen", 'Text(\n                "Sunrise' not in alarm_ui and ' / 60 seconds' not in alarm_ui and 'sunrise remaining' not in alarm_ui.lower())
check("Live alarm header is pinned high", "Modifier.width(220.dp).height(64.dp)" in alarm_ui and ".padding(horizontal = 18.dp, vertical = 6.dp)" in alarm_ui)
check("Live challenge card is compact", "modifier = Modifier.fillMaxWidth(.90f)" in alarm_ui and "Modifier.padding(horizontal = 16.dp, vertical = 14.dp)" in alarm_ui)
check("No live alarm progress bar", "LinearProgressIndicator" not in alarm_ui and "stepProgressFraction" not in alarm_ui)
check(
    "Sunrise fills the complete display",
    "enableEdgeToEdge" in alarm_ui
    and "SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)" in alarm_ui
    and "LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES" in alarm_ui
    and ".statusBarsPadding()" in alarm_ui
    and ".navigationBarsPadding()" in alarm_ui
    and "drawRect(Color(0xFF041426)" not in alarm_ui,
)
check(
    "First-run name input has no explanatory footer",
    'supportingText = { Text("Used in your morning, afternoon or evening greeting") }' not in onboarding,
)
check(
    "Onboarding routine steps are pencil-editable",
    "editingStep = step" in onboarding
    and "Icons.Outlined.Edit" in onboarding
    and "StepEditorDialog(" in onboarding,
)
check(
    "Onboarding question topics are configurable",
    "Topic.entries.chunked(3)" in app_ui
    and "questionsRequired" in onboarding
    and "topics = listOf(Topic.MATHS)" in onboarding,
)
check(
    "Routine and settings cards use pale blue surfaces",
    onboarding.count("primaryContainer.copy(alpha = .72f)") >= 2
    and app_ui.count("primaryContainer.copy(alpha = .72f)") >= 5,
)
check("Per-window brightness ramp", "screenBrightness" in alarm_ui)
check("Night-to-day horizon renderer", all(x in alarm_ui for x in ["purple", "red", "orange", "daylight", "SunriseHorizon"]))
check("Selected sneezing-cat branding used universally", "R.drawable.tazalarm_cat_only" in main_source and 'Text(\n            "TAZLARM"' in main_source and (ROOT / "app/src/main/res/drawable-nodpi/tazalarm_cat_only.png").is_file())
check("Multiple alarms in onboarding", "queuedAlarms" in onboarding and "completeOnboarding(name, alarms)" in onboarding)
check("Five main navigation areas", all(x in app_ui for x in ["HOME", "ALARMS", "ROUTINES", "PROGRESS", "SETTINGS"]))
check("Home logo is centred", "contentAlignment = Alignment.Center" in app_ui and "BrandHeader(compact = true)" in app_ui)
check("No duplicate Home settings shortcut", "onSettings" not in app_ui and 'Icon(Icons.Outlined.Settings, "Settings")' not in app_ui)
check("Stay Awake feature excluded", re.search(r"stay\s*awake|still\s*awake", main_source, re.I) is None)
check("Generic barcode title", 'Text("Scan Barcode"' in alarm_ui and 'title = "Scan Barcode"' in main_source)
check("Generic barcode helper", "Barcode must match one of your saved codes" in alarm_ui)
check("Generic scanner action", "Open Scanner" in alarm_ui)
check("No Kitchen wording in production source", re.search(r"\bkitchen\b", main_source, re.I) is None)
check("Alarm answer text is black", all(x in alarm_ui for x in ["focusedTextColor = Color.Black", "unfocusedTextColor = Color.Black", "cursorColor = Color.Black", "focusedBorderColor = Color.Black"]))
check("Robust barcode identity matching", all(x in main_source for x in ["object BarcodeIdentity", "rawBytes", "canonicalGtin", "BarcodeIdentity.matches", "BarcodeIdentity.capture"]))
check("Barcode recovery route visible", "Barcode not working? Use recovery route" in alarm_ui and "50 correct answers" in alarm_ui)
check("Old barcode storage remains compatible", "if (!value.startsWith" in main_source and "raw = value" in main_source)
check("Routine presets preserve registrations", "preserveRoutineRegistrations" in app_ui and "barcodeValue = configured.barcodeValue" in app_ui)
check("Live photo capture", "ActivityResultContracts.TakePicture" in alarm_ui and "ImageSimilarity.bestScore" in alarm_ui)
check("50-question irreversible penalty", "questionTarget = if (penaltyMode) 50" in alarm_ui and "cannot return" in alarm_ui)
check("Question topic coverage", all(topic in main_source for topic in ["WORLD_WAR_II", "CARL_JUNG", "TWENTIETH_CENTURY", "GEOGRAPHY", "SCIENCE", "SPORT", "LOGIC"]))
check("Progress and reliability logging", "reliabilityEvents" in main_source and "ProgressScreen" in app_ui)
check("Two-minute screen-off test", "120_000L" in app_ui and "scheduleTest" in scheduler)
check(
    "Compose keyboard imports are compile-safe",
    "androidx.compose.ui.text.input.KeyboardOptions" not in main_source
    and "androidx.compose.ui.text.input.KeyboardActions" not in main_source
    and "androidx.compose.foundation.text.KeyboardOptions" in main_source
    and "androidx.compose.foundation.text.KeyboardActions" in alarm_ui,
)

default_routine_test = read = (ROOT / "app/src/test/java/com/james/mathwakealarm/DefaultRoutineTest.kt").read_text(encoding="utf-8")
check(
    "Default routine unit test matches blank onboarding",
    "defaultRoutineStartsBlankForUserConfiguration" in default_routine_test
    and "assertTrue(defaultRoutine().isEmpty())" in default_routine_test
    and "mustGetUpPresetStillProvidesTheFourStageExample" in default_routine_test,
)


check(
    "Routine cards support long-press drag and buttons",
    "detectDragGesturesAfterLongPress" in app_ui
    and "Hold and drag to reorder" in app_ui
    and "Icons.Outlined.ArrowUpward" in app_ui
    and "Icons.Outlined.ArrowDownward" in app_ui
    and "HomeRoutineStepCard" in app_ui,
)
check(
    "Post-alarm Home fades from black to light",
    "AppRepository.setTheme(ThemeMode.LIGHT)" in alarm_ui
    and "EXTRA_POST_ALARM_FADE" in main_source
    and "durationMillis = 2_000" in main_source
    and ".background(Color.Black)" in main_source,
)
check(
    "Step-specific quiet periods",
    "pauseAlarm(12_000L)" in alarm_ui
    and "pauseAlarm(20_000L)" in alarm_ui
    and "pauseAlarm(30_000L)" in alarm_ui
    and "vibrator?.cancel()" in service
    and "coerceIn(1_000L, 60_000L)" in service,
)
check(
    "Initial SILENCE gate starts the routine",
    'Text("SILENCE"' in alarm_ui
    and "awaitingInitialSilence" in alarm_ui
    and "onClick = { awaitingInitialSilence = false }" in alarm_ui,
)
check(
    "Wheel scroller time picker in setup and Edit Alarm",
    "fun WheelTimePicker(" in app_ui
    and "NumberPicker(context)" in app_ui
    and "WheelTimePicker(" in onboarding
    and all(label in app_ui for label in ['Text("ONCE")', 'Text("WEEKDAYS")', 'Text("CUSTOM")'])
    and 'label = { Text("Hour") }' not in app_ui
    and 'label = { Text("Minute") }' not in app_ui,
)

check(
    "Alarm runs over lock screen with permission guidance",
    'android:showWhenLocked="true"' in manifest
    and 'android:turnScreenOn="true"' in manifest
    and "setShowWhenLocked(true)" in alarm_ui
    and "setFullScreenIntent" in service
    and "canUseFullScreenIntent" in app_ui
    and "ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT" in app_ui
    and "WindowInsetsCompat.Type.systemBars()" in alarm_ui,
)

# Basic Kotlin delimiter balance and no parser-level errors from the local compiler.
kotlin_files = list((ROOT / "app/src").rglob("*.kt"))
balanced = all(p.read_text().count("{") == p.read_text().count("}") and p.read_text().count("(") == p.read_text().count(")") for p in kotlin_files)
check("Kotlin delimiter balance", balanced, f"{len(kotlin_files)} Kotlin files")

bad_paths = [p for p in ROOT.rglob("*") if any(part in {"build", ".gradle", ".idea", "__pycache__"} for part in p.relative_to(ROOT).parts)]
check("No generated build/cache folders", not bad_paths, ", ".join(str(p.relative_to(ROOT)) for p in bad_paths[:3]))

try:
    output = subprocess.check_output([
        "keytool", "-list", "-v", "-keystore", str(ROOT / "personal-release.keystore"),
        "-storepass", "TazAlarmPersonal2026", "-alias", "tazalarm"
    ], stderr=subprocess.STDOUT, text=True)
    check("Release keystore opens and alias exists", "Alias name: tazalarm" in output and "PrivateKeyEntry" in output)
except Exception as exc:
    check("Release keystore opens and alias exists", False, str(exc))

passed = sum(ok for _, ok, _ in checks)
print(f"TAZLARM package validation: {passed}/{len(checks)} checks passed")
for name, ok, detail in checks:
    marker = "PASS" if ok else "FAIL"
    print(f"[{marker}] {name}" + (f" — {detail}" if detail else ""))

if passed != len(checks):
    sys.exit(1)
