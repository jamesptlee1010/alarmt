package com.james.mathwakealarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultRoutineTest {
    @Test
    fun defaultRoutineStartsBlankForUserConfiguration() {
        assertTrue(defaultRoutine().isEmpty())
        assertTrue(defaultAlarm().routine.isEmpty())
    }

    @Test
    fun mustGetUpPresetStillProvidesTheFourStageExample() {
        val routine = mustGetUpRoutine()
        assertEquals(4, routine.size)
        assertEquals(
            listOf(StepType.QUESTIONS, StepType.BARCODE, StepType.QUESTIONS, StepType.PHOTO),
            routine.map { it.type }
        )
        assertEquals("Scan Barcode", routine[1].title)
        assertTrue(routine.none { it.title.contains("Kitchen", ignoreCase = true) })
    }
}
