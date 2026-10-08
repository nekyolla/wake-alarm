package com.kindness.wakealarm.util

/**
 * Validates user-entered custom keywords before they are stored.
 */
object KeywordValidator {

    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 40

    sealed interface Result {
        data class Valid(val normalized: String) : Result
        data object Empty : Result
        data object TooShort : Result
        data object TooLong : Result
        /** Only punctuation/symbols, e.g. "--": would match almost any message or none at all. */
        data object NoLetterOrDigit : Result
        data object Duplicate : Result
    }

    /** Lowercases, trims and collapses inner whitespace so "Code   Blue" == "code blue". */
    fun normalize(input: String): String =
        input.trim().lowercase().replace(Regex("\\s+"), " ")

    fun validate(input: String, existing: Collection<String>): Result {
        val normalized = normalize(input)
        return when {
            normalized.isEmpty() -> Result.Empty
            normalized.length < MIN_LENGTH -> Result.TooShort
            normalized.length > MAX_LENGTH -> Result.TooLong
            normalized.none { it.isLetterOrDigit() } -> Result.NoLetterOrDigit
            existing.any { normalize(it) == normalized } -> Result.Duplicate
            else -> Result.Valid(normalized)
        }
    }
}
