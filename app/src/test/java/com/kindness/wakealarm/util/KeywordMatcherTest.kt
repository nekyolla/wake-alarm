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

    @Test
    fun `keyword ending in punctuation matches`() {
        val result = KeywordMatcher.match("dr. Rina ke IGD sekarang", listOf("dr.", "igd"), threshold = 2)
        assertTrue(result.isTriggered)
        assertEquals(listOf("dr.", "igd"), result.matchedKeywords)
    }

    @Test
    fun `keyword ending in punctuation matches when glued to the next word`() {
        assertEquals(1, KeywordMatcher.match("dr.Rina", listOf("dr."), threshold = 1).matchCount)
    }

    @Test
    fun `keyword starting with punctuation matches`() {
        assertEquals(1, KeywordMatcher.match("cek #igd sekarang", listOf("#igd"), threshold = 1).matchCount)
    }

    @Test
    fun `hyphenated keyword is matched as a whole word`() {
        assertEquals(0, KeywordMatcher.match("co-assistant baru", listOf("co-ass"), threshold = 1).matchCount)
        assertEquals(1, KeywordMatcher.match("panggil co-ass, segera", listOf("co-ass"), threshold = 1).matchCount)
    }

    @Test
    fun `phrase is matched as a whole phrase`() {
        assertEquals(0, KeywordMatcher.match("code blues band", listOf("code blue"), threshold = 1).matchCount)
        assertEquals(1, KeywordMatcher.match("CODE BLUE lantai 3", listOf("code blue"), threshold = 1).matchCount)
    }

    @Test
    fun `phrase matches across newlines and repeated spaces`() {
        assertEquals(1, KeywordMatcher.match("code\nblue lantai 3", listOf("code blue"), threshold = 1).matchCount)
        assertEquals(1, KeywordMatcher.match("code    blue", listOf("code  blue"), threshold = 1).matchCount)
    }

    @Test
    fun `punctuation around a keyword does not block the match`() {
        val result = KeywordMatcher.match("(urgent) segera ke IGD!", listOf("urgent", "igd"))
        assertTrue(result.isTriggered)
        assertEquals(2, result.matchCount)
    }

    @Test
    fun `digits count as part of a word`() {
        assertEquals(0, KeywordMatcher.match("pasien2 sudah pulang", listOf("pasien"), threshold = 1).matchCount)
    }

    @Test
    fun `non ascii letters are treated as word characters`() {
        assertEquals(0, KeywordMatcher.match("caféteria", listOf("café"), threshold = 1).matchCount)
        assertEquals(1, KeywordMatcher.match("ke café sekarang", listOf("café"), threshold = 1).matchCount)
    }

    @Test
    fun `matched keywords keep their original spelling`() {
        val result = KeywordMatcher.match("igd segera", listOf("IGD", "Segera"))
        assertEquals(listOf("IGD", "Segera"), result.matchedKeywords)
    }
}
