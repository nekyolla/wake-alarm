package com.kindness.wakealarm.util

import com.kindness.wakealarm.util.KeywordValidator.Result
import org.junit.Assert.assertEquals
import org.junit.Test

class KeywordValidatorTest {

    @Test
    fun `normalizes case and whitespace`() {
        assertEquals(Result.Valid("code blue"), KeywordValidator.validate("  Code   BLUE ", emptyList()))
    }

    @Test
    fun `rejects blank input`() {
        assertEquals(Result.Empty, KeywordValidator.validate("   ", emptyList()))
    }

    @Test
    fun `rejects too short and too long`() {
        assertEquals(Result.TooShort, KeywordValidator.validate("a", emptyList()))
        assertEquals(Result.TooLong, KeywordValidator.validate("x".repeat(KeywordValidator.MAX_LENGTH + 1), emptyList()))
    }

    @Test
    fun `accepts boundary lengths`() {
        assertEquals(Result.Valid("ab"), KeywordValidator.validate("ab", emptyList()))
        val max = "x".repeat(KeywordValidator.MAX_LENGTH)
        assertEquals(Result.Valid(max), KeywordValidator.validate(max, emptyList()))
    }

    @Test
    fun `rejects duplicates regardless of case or spacing`() {
        assertEquals(Result.Duplicate, KeywordValidator.validate("IGD", listOf("igd")))
        assertEquals(Result.Duplicate, KeywordValidator.validate("code  blue", listOf("Code Blue")))
    }
}
