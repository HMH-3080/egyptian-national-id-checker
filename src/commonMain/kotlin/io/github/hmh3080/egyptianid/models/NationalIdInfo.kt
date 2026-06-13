package io.github.hmh3080.egyptianid.models

import kotlinx.datetime.LocalDate

/**
 * Structured result produced by [io.github.hmh3080.egyptianid.parser.NationalIdParser].
 *
 * @property rawId the original input string (unsanitized)
 * @property dateOfBirth parsed date of birth (falls back to 1970-01-01 on invalid dates)
 * @property century the century base (e.g. 1900, 2000)
 * @property governorate the identified governorate
 * @property gender the identified gender
 * @property serialNumber the 3-digit serial number (digits 10–12)
 * @property checkDigit the 14th digit (checksum)
 * @property isValid `true` when the entire input passed all validation checks
 */
data class NationalIdInfo(
    val rawId: String,
    val dateOfBirth: LocalDate,
    val century: Int,
    val governorate: Governorate,
    val gender: Gender,
    val serialNumber: Int,
    val checkDigit: Int,
    val isValid: Boolean,
) {
    /** The century digit (2 for 1900s, 3 for 2000s). */
    val centuryDigit: Int
        get() = century / 100

    /** The full 4-digit birth year. */
    val birthYear: Int
        get() = dateOfBirth.year
}
