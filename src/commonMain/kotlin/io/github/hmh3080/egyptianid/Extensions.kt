package io.github.hmh3080.egyptianid

import io.github.hmh3080.egyptianid.models.NationalIdInfo
import io.github.hmh3080.egyptianid.parser.NationalIdParser
import io.github.hmh3080.egyptianid.utils.extractDigits
import io.github.hmh3080.egyptianid.utils.isValidNationalIdLength
import io.github.hmh3080.egyptianid.utils.sanitize
import io.github.hmh3080.egyptianid.validator.NationalIdValidator

/**
 * Returns `true` if this string is a valid 14-digit Egyptian National ID.
 */
fun String.isValidEgyptianId(): Boolean = NationalIdValidator.isValid(this)

/**
 * Parses this string into a [NationalIdInfo].
 */
fun String.toNationalIdInfo(): NationalIdInfo = NationalIdParser.parse(this)

/**
 * Parses this string into a [NationalIdInfo] or returns `null` if invalid.
 */
fun String.toNationalIdInfoOrNull(): NationalIdInfo? = NationalIdParser.parseOrNull(this)

/**
 * Sanitizes this string by extracting only digit characters and truncating to 14.
 */
fun String.sanitizeEgyptianId(): String = sanitize()

/**
 * Extracts only digit characters from this string.
 */
fun String.extractEgyptianIdDigits(): String = extractDigits()

/**
 * Returns `true` if this string contains exactly 14 digits after extracting all digit characters.
 */
fun String.isValidEgyptianIdLength(): Boolean = isValidNationalIdLength()
