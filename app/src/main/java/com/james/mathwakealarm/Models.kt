package com.james.mathwakealarm

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
enum class StepType { QUESTIONS, BARCODE, PHOTO }

@Serializable
enum class Topic(val displayName: String) {
    MATHS("Maths"),
    WORLD_WAR_II("World War II"),
    CARL_JUNG("Carl Jung"),
    TWENTIETH_CENTURY("20th Century History"),
    GEOGRAPHY("Geography"),
    SCIENCE("Science"),
    SPORT("Sport"),
    GENERAL_KNOWLEDGE("General Knowledge"),
    LOGIC("Logic"),
    DANCE_MOMS("Dance Moms"),
    TEEN_MOM_2("Teen Mom 2"),
    MORMON_WIVES("The Secret Lives of Mormon Wives")
}

@Serializable
data class RoutineStep(
    val id: String = UUID.randomUUID().toString(),
    val type: StepType,
    val title: String,
    val questionsRequired: Int = 0,
    val topics: List<Topic> = listOf(Topic.MATHS),
    val barcodeValue: String = "",
    val barcodeValues: List<String> = emptyList(),
    val referenceUris: List<String> = emptyList(),
    val photoThreshold: Float = 0.72f
)

@Serializable
data class AlarmConfig(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "Weekday Alarm",
    val hour: Int = 6,
    val minute: Int = 30,
    val days: List<Int> = emptyList(),
    val enabled: Boolean = true,
    val skipOccurrenceAt: Long = 0L,
    val sunriseSeconds: Int = 120,
    val vibrate: Boolean = true,
    val routine: List<RoutineStep> = defaultRoutine()
)

@Serializable
data class TopicScore(
    val topic: Topic,
    val correct: Int = 0,
    val attempted: Int = 0
)

@Serializable
data class RunStepResult(
    val stepId: String,
    val type: StepType,
    val title: String,
    val durationSeconds: Int,
    val success: Boolean,
    val attempts: Int = 1,
    val correctAnswers: Int = 0,
    val topicScores: List<TopicScore> = emptyList()
)

@Serializable
data class AlarmRun(
    val id: String = UUID.randomUUID().toString(),
    val alarmId: String,
    val alarmLabel: String,
    val startedAt: Long,
    val completedAt: Long,
    val completed: Boolean,
    val penaltyRouteUsed: Boolean,
    val stepResults: List<RunStepResult>
) {
    val durationSeconds: Int
        get() = ((completedAt - startedAt).coerceAtLeast(0L) / 1000L).toInt()
}

@Serializable
data class ReliabilityEvent(
    val alarmId: String,
    val stage: String,
    val occurredAt: Long = System.currentTimeMillis()
)

@Serializable
data class AppState(
    val userName: String = "",
    val onboardingComplete: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val alarms: List<AlarmConfig> = listOf(defaultAlarm()),
    val runs: List<AlarmRun> = emptyList(),
    val reliabilityEvents: List<ReliabilityEvent> = emptyList(),
    val dismissedRunIds: List<String> = emptyList()
)

fun defaultRoutine(): List<RoutineStep> = emptyList()

fun defaultAlarm(): AlarmConfig = AlarmConfig(routine = defaultRoutine())

fun quickStartRoutine(): List<RoutineStep> = listOf(
    RoutineStep(
        type = StepType.QUESTIONS,
        title = "Answer 2 Questions",
        questionsRequired = 2,
        topics = listOf(Topic.MATHS, Topic.GENERAL_KNOWLEDGE)
    ),
    RoutineStep(type = StepType.BARCODE, title = "Scan Barcode")
)

fun normalWorkdayRoutine(): List<RoutineStep> = listOf(
    RoutineStep(
        type = StepType.QUESTIONS,
        title = "Answer 2 Questions",
        questionsRequired = 2,
        topics = listOf(Topic.MATHS, Topic.GENERAL_KNOWLEDGE)
    ),
    RoutineStep(type = StepType.BARCODE, title = "Scan Barcode"),
    RoutineStep(
        type = StepType.QUESTIONS,
        title = "Answer 3 Questions",
        questionsRequired = 3,
        topics = listOf(Topic.MATHS, Topic.GENERAL_KNOWLEDGE, Topic.LOGIC)
    )
)

fun mustGetUpRoutine(): List<RoutineStep> = listOf(
    RoutineStep(
        type = StepType.QUESTIONS,
        title = "Answer 2 Questions",
        questionsRequired = 2,
        topics = listOf(Topic.MATHS, Topic.GENERAL_KNOWLEDGE)
    ),
    RoutineStep(type = StepType.BARCODE, title = "Scan Barcode"),
    RoutineStep(
        type = StepType.QUESTIONS,
        title = "Answer 3 Questions",
        questionsRequired = 3,
        topics = listOf(Topic.MATHS, Topic.GENERAL_KNOWLEDGE, Topic.LOGIC)
    ),
    RoutineStep(type = StepType.PHOTO, title = "Verify Photo")
)

fun weekendRoutine(): List<RoutineStep> = listOf(
    RoutineStep(
        type = StepType.QUESTIONS,
        title = "Answer 2 Questions",
        questionsRequired = 2,
        topics = listOf(Topic.MATHS, Topic.SPORT, Topic.GENERAL_KNOWLEDGE)
    ),
    RoutineStep(type = StepType.PHOTO, title = "Verify Photo")
)
