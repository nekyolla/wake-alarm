package com.kindness.wakealarm.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerPlannerTest {

    @Test
    fun `nothing urgent means nothing to do`() {
        val plan = TriggerPlanner.plan(emptyList<String>(), alarmRunning = false)
        assertNull(plan.ring)
        assertTrue(plan.recordOnly.isEmpty())
    }

    @Test
    fun `newest urgent message rings, the rest are recorded`() {
        val plan = TriggerPlanner.plan(listOf("newest", "older"), alarmRunning = false)
        assertEquals("newest", plan.ring)
        assertEquals(listOf("older"), plan.recordOnly)
    }

    @Test
    fun `while an alarm rings every urgent message is recorded instead of dropped`() {
        val plan = TriggerPlanner.plan(listOf("a", "b"), alarmRunning = true)
        assertNull(plan.ring)
        assertEquals(listOf("a", "b"), plan.recordOnly)
    }
}
