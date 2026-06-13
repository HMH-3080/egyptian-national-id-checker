package io.github.hmh3080.egyptianid.parser

import io.github.hmh3080.egyptianid.models.Gender
import io.github.hmh3080.egyptianid.models.Governorate
import io.github.hmh3080.egyptianid.models.NationalIdInfo
import io.github.hmh3080.egyptianid.utils.extractDigits
import io.github.hmh3080.egyptianid.utils.toNationalIdDigits
import io.github.hmh3080.egyptianid.validator.NationalIdValidator
import kotlinx.datetime.LocalDate

/**
 * Parses Egyptian National ID strings into structured [NationalIdInfo] objects.
 *
 * Three overloads are provided for different error-handling strategies:
 * - [parse] always succeeds; check [NationalIdInfo.isValid] for validity.
 * - [parseOrNull] returns `null` for invalid IDs.
 * - [parseValid] throws [IllegalArgumentException] for invalid IDs.
 */
object NationalIdParser {

    /**
     * Parses the given string. Always returns a [NationalIdInfo]; inspect
     * [NationalIdInfo.isValid] to determine whether the ID passed all checks.
     */
    fun parse(id: String): NationalIdInfo {
        val digits = id.extractDigits()
        return parseSanitized(id, digits)
    }

    /**
     * Parses the given string and returns `null` if the ID is invalid.
     */
    fun parseOrNull(id: String): NationalIdInfo? {
        val digits = id.extractDigits()
        if (digits.length != 14) return null
        return if (NationalIdValidator.isValid(digits)) {
            parseSanitized(id, digits)
        } else {
            null
        }
    }

    /**
     * Parses the given string and throws [IllegalArgumentException] if the ID is invalid.
     *
     * @throws IllegalArgumentException if the input is not a valid 14-digit National ID
     */
    fun parseValid(id: String): NationalIdInfo {
        val digits = id.toNationalIdDigits()
        require(NationalIdValidator.isValid(digits)) {
            "Invalid Egyptian National ID: $id"
        }
        return parseSanitized(id, digits)
    }

    /**
     * Internal parse implementation that operates on pre-sanitized [digits],
     * avoiding redundant [extractDigits] calls.
     */
    private fun parseSanitized(rawId: String, digits: String): NationalIdInfo {
        if (digits.length != 14) return invalidInfo(rawId)

        val isValid = NationalIdValidator.isValidDigits(digits)

        return try {
            val centuryDigit = digits[0].digitToInt()
            val yearDigits = digits.substring(1, 3).toInt()
            val month = digits.substring(3, 5).toInt()
            val day = digits.substring(5, 7).toInt()
            val govCode = digits.substring(7, 9).toInt()
            val serial = digits.substring(9, 12).toInt()
            val genderDigit = digits[12]
            val checkDigit = digits[13].digitToInt()

            val centuryBase = 1900 + (centuryDigit - 2) * 100
            val fullYear = centuryBase + yearDigits

            val dateOfBirth = try {
                LocalDate(fullYear, month, day)
            } catch (_: IllegalArgumentException) {
                LocalDate(1970, 1, 1)
            }

            val governorate = try {
                Governorate.fromCode(govCode)
            } catch (_: IllegalArgumentException) {
                Governorate.entries.first()
            }

            NationalIdInfo(
                rawId = rawId,
                dateOfBirth = dateOfBirth,
                century = centuryBase,
                governorate = governorate,
                gender = Gender.fromDigit(genderDigit),
                serialNumber = serial,
                checkDigit = checkDigit,
                isValid = isValid,
            )
        } catch (_: IllegalArgumentException) {
            invalidInfo(rawId)
        }
    }

    /**
     * Produces a fallback [NationalIdInfo] with `isValid = false` and
     * placeholder field values.
     */
    private fun invalidInfo(rawId: String) = NationalIdInfo(
        rawId = rawId,
        dateOfBirth = LocalDate(1970, 1, 1),
        century = 0,
        governorate = Governorate.entries.first(),
        gender = Gender.MALE,
        serialNumber = 0,
        checkDigit = 0,
        isValid = false,
    )
}
