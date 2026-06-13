package io.github.hmh3080.egyptianid.utils

/**
 * Strips all non-digit characters from this string.
 *
 * Handles both ASCII digits (0–9) and Arabic-Indic digits (٠–٩ / ۰–۹)
 * via the Unicode `Nd` category.
 */
fun String.extractDigits(): String = filter { it.isDigit() }

/**
 * Returns `true` if this string contains exactly 14 digits after sanitization.
 */
fun String.isValidNationalIdLength(): Boolean = extractDigits().length == 14

/**
 * Extracts all digit characters and returns the first 14 digits.
 *
 * If fewer than 14 digits are present, returns them as-is.
 */
fun String.sanitize(): String {
    val digits = extractDigits()
    return when {
        digits.length >= 14 -> digits.substring(0, 14)
        else -> digits
    }
}

/**
 * Extracts all digit characters and requires exactly 14.
 *
 * @return the 14-digit string
 * @throws IllegalArgumentException if fewer or more than 14 digits are present
 */
fun String.toNationalIdDigits(): String {
    val digits = extractDigits()
    require(digits.length == 14) {
        "Expected exactly 14 digits but found ${digits.length} after sanitization"
    }
    return digits
}
