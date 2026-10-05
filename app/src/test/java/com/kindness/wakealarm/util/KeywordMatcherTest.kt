package com.kindness.wakealarm.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordMatcherTest {

    @Test
    fun `empty text returns no match`() {
        val result = KeywordMatcher.match("", listOf("jaga", "urgent"))
        assertFalse(result.isTriggered)
        assertEquals(0, result.matchCount)
    }

    @Test
    fun `empty keywords returns no match`() {
        val result = KeywordMatcher.match("ada jaga urgent di sini", emptyList())
        assertFalse(result.isTriggered)
        assertEquals(0, result.matchCount)
    }

    @Test
    fun `single keyword match does not trigger`() {
        val result = KeywordMatcher.match("ada jaga di sini", listOf("jaga", "urgent", "segera"))
        assertFalse(result.isTriggered)
        assertEquals(1, result.matchCount)
        assertEquals(listOf("jaga"), result.matchedKeywords)
    }

    @Test
    fun `two keyword matches triggers alarm`() {
        val result = KeywordMatcher.match(
            "jaga malam urgent pasien datang",
            listOf("jaga", "urgent", "segera")
        )
        assertTrue(result.isTriggered)
        assertEquals(2, result.matchCount)
        assertTrue(result.matchedKeywords.containsAll(listOf("jaga", "urgent")))
    }

    @Test
    fun `three keyword matches triggers alarm`() {
        val result = KeywordMatcher.match(
            "jaga malam urgent segera ke igd",
            listOf("jaga", "urgent", "segera", "dokter")
        )
        assertTrue(result.isTriggered)
        assertEquals(3, result.matchCount)
    }

    @Test
    fun `case insensitive matching`() {
        val result = KeywordMatcher.match(
            "JAGA malam URGENT datang",
            listOf("jaga", "urgent")
        )
        assertTrue(result.isTriggered)
        assertEquals(2, result.matchCount)
    }

    @Test
    fun `whole word and phrase matching works`() {
        val result = KeywordMatcher.match(
            "jadwal jaga dan pasien baru",
            listOf("jaga", "pasien")
        )
        assertTrue(result.isTriggered)
        assertEquals(2, result.matchCount)
    }

    @Test
    fun `partial word substring does not falsely trigger`() {
        // e.g. "padahal" contains "ada", "menjagakan" vs "jaga" with threshold 2
        val result = KeywordMatcher.match(
            "padahal hari ini tenang",
            listOf("ada", "jaga"),
            threshold = 2
        )
        assertFalse(result.isTriggered)
        assertEquals(0, result.matchCount)
    }

    @Test
    fun `duplicate keywords in list counted only once`() {
        val result = KeywordMatcher.match(
            "ada jaga di sini",
            listOf("jaga", "JAGA", "Jaga")
        )
        assertFalse(result.isTriggered)
        assertEquals(1, result.matchCount)
    }

    @Test
    fun `blank keywords are filtered out`() {
        val result = KeywordMatcher.match(
            "ada jaga urgent",
            listOf("jaga", "", "  ", "urgent")
        )
        assertTrue(result.isTriggered)
        assertEquals(2, result.matchCount)
    }

    @Test
    fun `no keyword match in text`() {
        val result = KeywordMatcher.match(
            "hari ini cuaca cerah",
            listOf("jaga", "urgent", "segera")
        )
        assertFalse(result.isTriggered)
        assertEquals(0, result.matchCount)
    }

    @Test
    fun `whatsapp group notification format`() {
        val result = KeywordMatcher.match(
            "Co-Ass Anak: Jaga malam urgent, segera ke IGD lantai 3",
            listOf("jaga", "urgent", "segera", "igd", "co-ass")
        )
        assertTrue(result.isTriggered)
        assertTrue(result.matchCount >= 2)
    }

    @Test
    fun `custom threshold of 1`() {
        val result = KeywordMatcher.match(
            "ada jaga di sini",
            listOf("jaga", "urgent"),
            threshold = 1
        )
        assertTrue(result.isTriggered)
        assertEquals(1, result.matchCount)
    }

    @Test
    fun `custom threshold of 3`() {
        val result = KeywordMatcher.match(
            "jaga urgent di sini",
            listOf("jaga", "urgent", "segera"),
            threshold = 3
        )
        assertFalse(result.isTriggered)
        assertEquals(2, result.matchCount)
    }
}
