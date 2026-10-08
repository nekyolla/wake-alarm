package com.kindness.wakealarm.util

/**
 * Pure utility for matching keywords against notification text.
 * The alarm triggers when at least `threshold` distinct keywords appear in one message.
 */
object KeywordMatcher {

    data class MatchResult(
        val isTriggered: Boolean,
        val matchedKeywords: List<String>,
        val matchCount: Int
    )

    // Letters, digits and underscore count as "word" characters, in any script.
    private const val WORD_CHAR = """[\p{L}\p{N}_]"""
    private val WHITESPACE = Regex("""\s+""")

    /**
     * Check if the given text contains at least [threshold] distinct keywords.
     *
     * Every keyword (single word, phrase or hyphenated) is matched as a whole: an edge that is a
     * letter or digit must not touch another letter or digit, so "jaga" doesn't match "menjaga" and
     * "co-ass" doesn't match "co-assistant". An edge that is punctuation needs no boundary, so
     * "dr." matches "dr. Rina" and "#igd" matches "cek #igd". Case and runs of whitespace
     * (including newlines) are ignored on both sides.
     *
     * @param text The notification text to check against.
     * @param keywords The list of active keywords.
     * @param threshold Minimum number of distinct keyword matches required (default: 2).
     * @return MatchResult with trigger status and matched keywords (as given in [keywords]).
     */
    fun match(
        text: String,
        keywords: List<String>,
        threshold: Int = 2
    ): MatchResult {
        if (text.isBlank() || keywords.isEmpty()) {
            return MatchResult(isTriggered = false, matchedKeywords = emptyList(), matchCount = 0)
        }

        val normalizedText = normalize(text)
        val matched = keywords
            .filter { it.isNotBlank() }
            .distinctBy { normalize(it) }
            .filter { keyword -> patternFor(normalize(keyword)).containsMatchIn(normalizedText) }

        return MatchResult(
            isTriggered = matched.size >= threshold,
            matchedKeywords = matched,
            matchCount = matched.size
        )
    }

    /** Lowercases, trims and collapses every whitespace run to a single space. */
    internal fun normalize(input: String): String = input.lowercase().replace(WHITESPACE, " ").trim()

    private fun patternFor(keyword: String): Regex {
        val start = if (isWordChar(keyword.first())) "(?<!$WORD_CHAR)" else ""
        val end = if (isWordChar(keyword.last())) "(?!$WORD_CHAR)" else ""
        return Regex(start + Regex.escape(keyword) + end)
    }

    private fun isWordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'
}
