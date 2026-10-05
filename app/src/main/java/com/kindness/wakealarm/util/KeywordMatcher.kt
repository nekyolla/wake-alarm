package com.kindness.wakealarm.util

/**
 * Pure utility for matching keywords against notification text.
 * Returns true if >= 2 distinct keywords are found as substrings (case-insensitive).
 */
object KeywordMatcher {

    data class MatchResult(
        val isTriggered: Boolean,
        val matchedKeywords: List<String>,
        val matchCount: Int
    )

    /**
     * Check if the given text contains at least [threshold] distinct keywords.
     * Uses regex word boundaries for single words, or exact phrase containment for multi-word keywords.
     *
     * @param text The notification text to check against.
     * @param keywords The list of active keywords.
     * @param threshold Minimum number of distinct keyword matches required (default: 2).
     * @return MatchResult with trigger status and matched keywords.
     */
    fun match(
        text: String,
        keywords: List<String>,
        threshold: Int = 2
    ): MatchResult {
        if (text.isBlank() || keywords.isEmpty()) {
            return MatchResult(isTriggered = false, matchedKeywords = emptyList(), matchCount = 0)
        }

        val lowerText = text.lowercase()
        val matched = keywords
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .filter { keyword ->
                val trimmed = keyword.trim().lowercase()
                if (trimmed.isEmpty()) return@filter false

                // If the keyword contains multiple words or punctuation (e.g. "code blue", "co-ass"),
                // check with standard substring or boundary
                if (trimmed.contains(" ") || trimmed.contains("-")) {
                    lowerText.contains(trimmed)
                } else {
                    // Single word: use word boundary regex to avoid partial substring false positives
                    // e.g., keyword "anak" will not match "anaknya", "kanak", "beranak" unless specified,
                    // and "ada" won't match inside "padahal" or "sedang".
                    val regex = Regex("""(?i)\b${Regex.escape(trimmed)}\b""")
                    regex.containsMatchIn(lowerText)
                }
            }

        return MatchResult(
            isTriggered = matched.size >= threshold,
            matchedKeywords = matched,
            matchCount = matched.size
        )
    }
}
