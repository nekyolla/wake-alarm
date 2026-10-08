package com.kindness.wakealarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmHistoryCodecTest {

    private val events = listOf(
        AlarmEvent(1_700_000_000_000L, "Dr. Rina · Koas Anak", "Jaga malam \"urgent\", segera ke IGD", listOf("jaga", "urgent", "igd")),
        AlarmEvent(1_699_000_000_000L, "", "code blue lantai 3\nsegera", listOf("code blue", "segera"), whileRinging = true)
    )

    @Test
    fun `round trip preserves every field including quotes and newlines`() {
        assertEquals(events, AlarmHistoryCodec.decode(AlarmHistoryCodec.encode(events)))
    }

    @Test
    fun `empty or missing data decodes to empty list`() {
        assertTrue(AlarmHistoryCodec.decode(null).isEmpty())
        assertTrue(AlarmHistoryCodec.decode("").isEmpty())
        assertTrue(AlarmHistoryCodec.decode(AlarmHistoryCodec.encode(emptyList())).isEmpty())
    }

    @Test
    fun `corrupt data decodes to empty list instead of crashing`() {
        assertTrue(AlarmHistoryCodec.decode("{not json").isEmpty())
    }

    @Test
    fun `entries written before whileRinging existed still decode`() {
        val legacy = """[{"t":1700000000000,"s":"Rina","m":"jaga urgent","k":["jaga","urgent"]}]"""
        val decoded = AlarmHistoryCodec.decode(legacy)
        assertEquals(1, decoded.size)
        assertFalse(decoded.single().whileRinging)
        assertEquals(listOf("jaga", "urgent"), decoded.single().keywords)
    }
}
