package io.github.hmh3080.egyptianid.validator

import io.github.hmh3080.egyptianid.models.Governorate
import io.github.hmh3080.egyptianid.utils.extractDigits
import kotlinx.datetime.LocalDate

/**
 * Validates Egyptian National ID strings.
 *
 * All public methods accept raw input that may contain non-digit characters
 * (spaces, dashes, Arabic-Indic digits, etc.) — sanitisation is handled internally.
 *
 * The checksum uses the official civil registry weighted mod-11 algorithm with weights
 * `[2, 7, 6, 5, 4, 3, 2, 7, 6, 5, 4, 3, 2]`.
 */
object NationalIdValidator {

    private val ID_LENGTH = 14
    private val WEIGHTS = intArrayOf(2, 7, 6, 5, 4, 3, 2, 7, 6, 5, 4, 3, 2)

    /**
     * Returns `true` if the given string is a valid 14-digit Egyptian National ID.
     *
     * Checks format, date-of-birth validity, governorate code, and checksum.
     */
    fun isValid(id: String): Boolean {
        val digits = id.extractDigits()
        return isValidDigits(digits)
    }

    /**
     * Validates a **pre-sanitized** 14-digit string, avoiding redundant extraction.
     *
     * Use this when the caller has already called [extractDigits] or [sanitize].
     *
     * @param digits a string consisting of exactly 14 digit characters
     */
    fun isValidDigits(digits: String): Boolean {
        if (digits.length != ID_LENGTH) return false
        return try {
            validateDigits(digits)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    /**
     * Checks only the format: exactly 14 digits with a valid century digit (2 or 3).
     */
    fun isValidFormat(id: String): Boolean {
        val digits = id.extractDigits()
        return digits.length == ID_LENGTH && digits.all { it.isDigit() }
    }

    /**
     * Checks only the date-of-birth portion (digits 1–7): valid month, day,
     * and calendar date (including leap years).
     */
    fun isValidDate(id: String): Boolean {
        val digits = id.extractDigits()
        if (digits.length != ID_LENGTH) return false

        return try {
            parseAndValidateDate(digits)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    /**
     * Checks only the governorate code (digits 8–9) against the known list.
     */
    fun isValidGovernorate(id: String): Boolean {
        val digits = id.extractDigits()
        if (digits.length != ID_LENGTH) return false
        return isValidGovernorateDigits(digits)
    }

    /**
     * Checks only the checksum digit (14th digit) using the weighted mod-11 algorithm.
     */
    fun isValidCheckDigit(id: String): Boolean {
        val digits = id.extractDigits()
        if (digits.length != ID_LENGTH) return false
        return isValidCheckDigitDigits(digits)
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Full validation pipeline on pre-sanitized digits.
     * Throws [IllegalArgumentException] on the first failing check.
     */
    private fun validateDigits(digits: String) {
        val centuryDigit = digits[0].digitToInt()
        require(centuryDigit in 2..3) { "Invalid century digit: $centuryDigit" }

        val month = digits.substring(3, 5).toInt()
        val day = digits.substring(5, 7).toInt()
        require(month in 1..12) { "Invalid month: $month" }
        require(day in 1..31) { "Invalid day: $day" }

        parseAndValidateDate(digits)

        val govCode = digits.substring(7, 9).toInt()
        require(Governorate.entries.any { it.code == govCode }) {
            "Invalid governorate code: $govCode"
        }

        if (!isValidCheckDigitDigits(digits)) {
            val checkDigit = digits[13].digitToInt()
            require(false) { "Invalid check digit: $checkDigit" }
        }
    }

    /**
     * Governorate code validation on pre-sanitized digits. Does **not** extract again.
     */
    private fun isValidGovernorateDigits(digits: String): Boolean {
        val code = digits.substring(7, 9).toIntOrNull() ?: return false
        return Governorate.entries.any { it.code == code }
    }

    /**
     * Checksum validation on pre-sanitized digits. Does **not** extract again.
     *
     * Algorithm: sum(d[i] × w[i]) for i in 0..12, mod 11.
     * Expected = 0 if remainder == 0, else 11 − remainder.
     * If expected == 10 the ID is considered invalid.
     */
    private fun isValidCheckDigitDigits(digits: String): Boolean {
        if (digits.any { !it.isDigit() }) return false

        val sum = digits.substring(0, 13)
            .mapIndexed { index, c -> c.digitToInt() * WEIGHTS[index] }
            .sum()

        val remainder = sum % 11
        val expected = if (remainder == 0) 0 else 11 - remainder

        return expected < 10 && expected == digits[13].digitToInt()
    }

    /**
     * Parses and validates the date-of-birth portion by constructing a [LocalDate].
     * Throws [IllegalArgumentException] for invalid calendar dates.
     */
    private fun parseAndValidateDate(digits: String) {
        val centuryDigit = digits[0].digitToInt()
        val year = digits.substring(1, 3).toInt()
        val month = digits.substring(3, 5).toInt()
        val day = digits.substring(5, 7).toInt()

        val centuryBase = 1900 + (centuryDigit - 2) * 100
        val fullYear = centuryBase + year

        LocalDate(fullYear, month, day)
    }
}
