package io.github.hmh3080.egyptianid

import io.github.hmh3080.egyptianid.models.Gender
import io.github.hmh3080.egyptianid.models.Governorate
import io.github.hmh3080.egyptianid.models.NationalIdInfo
import io.github.hmh3080.egyptianid.parser.NationalIdParser
import io.github.hmh3080.egyptianid.utils.extractDigits
import io.github.hmh3080.egyptianid.utils.isValidNationalIdLength
import io.github.hmh3080.egyptianid.utils.sanitize
import io.github.hmh3080.egyptianid.utils.toNationalIdDigits
import io.github.hmh3080.egyptianid.validator.NationalIdValidator
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for the Egyptian National ID Checker library.
 *
 * All valid test IDs were constructed by manually computing the weighted mod-11
 * checksum and verified against the library's own algorithm.
 */
class NationalIdTest {

    // ─────────────────────────────────────────────────────────────────────
    // 1.  Helper — compute the mod-11 check digit for test construction
    // ─────────────────────────────────────────────────────────────────────

    companion object {
        private val WEIGHTS = intArrayOf(2, 7, 6, 5, 4, 3, 2, 7, 6, 5, 4, 3, 2)

        /** Builds a full 14-digit ID from the first 13 digits + computed checksum. */
        private fun buildId(first13: String): String {
            val sum = first13.mapIndexed { i, c -> c.digitToInt() * WEIGHTS[i] }.sum()
            val remainder = sum % 11
            val checkDigit = if (remainder == 0) 0 else 11 - remainder
            return first13 + checkDigit
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2.  Known valid IDs (pre-computed for deterministic tests)
    //     Format: C YY MM DD GG SSS G CHK
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Male, born 1990-01-01, Cairo (01), serial 000, check digit 7.
     * Digits: 2 90 01 01 01 000 1 7
     */
    private val VALID_ID_1990_MALE_CAIRO = "29001010100017"

    /**
     * Female, born 2005-12-25, Alexandria (02), serial 123, check digit 7.
     * Digits: 3 05 12 25 02 123 2 7
     */
    private val VALID_ID_2005_FEMALE_ALEX = "30512250212327"

    /**
     * Male, born 2000-02-29 (leap year), Giza (14), serial 555, check digit 8.
     * Digits: 3 00 02 29 14 555 3 8
     */
    private val VALID_ID_2000_MALE_GIZA = "30002291455538"

    /**
     * Female, born 1995-06-15, Dakahlia (06), serial 999, check digit 4.
     * Digits: 2 95 06 15 06 999 2 4
     */
    private val VALID_ID_1995_FEMALE_DAK = "29506150699924"

    /** Collection of all valid IDs for parameterised-style testing. */
    private val validIds = listOf(
        VALID_ID_1990_MALE_CAIRO,
        VALID_ID_2005_FEMALE_ALEX,
        VALID_ID_2000_MALE_GIZA,
        VALID_ID_1995_FEMALE_DAK,
    )

    // ─────────────────────────────────────────────────────────────────────
    // 3.  Validation — NationalIdValidator.isValid
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun `isValid returns true for well-formed IDs`() {
        for (id in validIds) {
            assertTrue(NationalIdValidator.isValid(id), "Expected valid: $id")
        }
    }

    @Test
    fun `isValid returns false when ID contains non-digit characters`() {
        assertFalse(NationalIdValidator.isValid("abc"))
        assertFalse(NationalIdValidator.isValid("29001010A00017"))
        assertFalse(NationalIdValidator.isValid(""))
    }

    @Test
    fun `isValid returns false for wrong length 13 or 15 digits`() {
        val first13 = VALID_ID_1990_MALE_CAIRO.substring(0, 13)
        val with15 = VALID_ID_1990_MALE_CAIRO + "0"
        assertFalse(NationalIdValidator.isValid(first13))
        assertFalse(NationalIdValidator.isValid(with15))
    }

    @Test
    fun `isValid returns false for invalid century digit`() {
        // Century 4 is not assigned (only 2 and 3 are valid)
        assertFalse(NationalIdValidator.isValid("49001010100013"))
    }

    @Test
    fun `isValid returns false for month 13 or month 0`() {
        assertFalse(NationalIdValidator.isValid("29001301010017"))
        assertFalse(NationalIdValidator.isValid("29000001010017"))
    }

    @Test
    fun `isValid returns false for day 32 or day 0`() {
        assertFalse(NationalIdValidator.isValid("29001032010017"))
        assertFalse(NationalIdValidator.isValid("29001000010017"))
    }

    @Test
    fun `isValid returns false for impossible calendar date Feb 30`() {
        // February 30 does not exist in any year
        assertFalse(NationalIdValidator.isValid("29002300100017"))
    }

    @Test
    fun `isValid returns false for Feb 29 on a non-leap year`() {
        // 2005 is not a leap year → Feb 29 is invalid
        assertFalse(NationalIdValidator.isValid("30502290100011"))
    }

    @Test
    fun `isValid returns true for Feb 29 on a leap year`() {
        // 2000 is a leap year → Feb 29 is valid
        assertTrue(NationalIdValidator.isValid(VALID_ID_2000_MALE_GIZA))
    }

    @Test
    fun `isValid returns false for unknown governorate code`() {
        // Governorate code 30 is not in the enum (max is 27, 88, 99)
        assertFalse(NationalIdValidator.isValid("29001013000013"))
    }

    @Test
    fun `isValid returns false when checksum digit is wrong`() {
        // Take a valid ID and flip the last digit
        val invalid = VALID_ID_1990_MALE_CAIRO.substring(0, 13) + "0"
        assertFalse(NationalIdValidator.isValid(invalid))
    }

    // ─────────────────────────────────────────────────────────────────────
    // 4.  Validation — individual predicate methods
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun `isValidFormat accepts 14-digit strings and rejects others`() {
        assertTrue(NationalIdValidator.isValidFormat(VALID_ID_1990_MALE_CAIRO))
        assertFalse(NationalIdValidator.isValidFormat("2900101010001"))           // 13 digits
        assertFalse(NationalIdValidator.isValidFormat("290010101000177"))         // 15 digits
        assertFalse(NationalIdValidator.isValidFormat("id2900101010001x"))        // letters + 13 digits → 13
        assertFalse(NationalIdValidator.isValidFormat(""))                        // empty
    }

    @Test
    fun `isValidDate passes for real dates and fails for impossible ones`() {
        assertTrue(NationalIdValidator.isValidDate(VALID_ID_1990_MALE_CAIRO))  // 1990-01-01
        assertTrue(NationalIdValidator.isValidDate(VALID_ID_2000_MALE_GIZA))   // 2000-02-29
        assertFalse(NationalIdValidator.isValidDate("29002300100017"))         // Feb 30
        assertFalse(NationalIdValidator.isValidDate("30502290100011"))         // Feb 29 non-leap
    }

    @Test
    fun `isValidGovernorate matches known codes`() {
        assertTrue(NationalIdValidator.isValidGovernorate(VALID_ID_1990_MALE_CAIRO))   // 01
        assertTrue(NationalIdValidator.isValidGovernorate(VALID_ID_2000_MALE_GIZA))    // 14
        assertFalse(NationalIdValidator.isValidGovernorate("29001013000013"))          // 30
    }

    @Test
    fun `isValidCheckDigit detects checksum tampering`() {
        val good = VALID_ID_1990_MALE_CAIRO
        assertTrue(NationalIdValidator.isValidCheckDigit(good))
        // Construct an ID with a deliberately wrong checksum
        val tampered = good.substring(0, 13) + ((good.last().digitToInt() + 1) % 10)
        assertFalse(NationalIdValidator.isValidCheckDigit(tampered))
    }

    @Test
    fun `isValidDigits avoids redundant extraction and matches isValid`() {
        val digits = VALID_ID_1990_MALE_CAIRO.extractDigits()
        assertEquals(
            NationalIdValidator.isValid(VALID_ID_1990_MALE_CAIRO),
            NationalIdValidator.isValidDigits(digits),
        )
        // Sanitized input should also work
        assertTrue(NationalIdValidator.isValidDigits(digits))
        assertFalse(NationalIdValidator.isValidDigits("123"))
        assertFalse(NationalIdValidator.isValidDigits(""))
    }

    // ─────────────────────────────────────────────────────────────────────
    // 5.  Model — Gender
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun `Gender fromDigit maps odd to Male and even to Female`() {
        assertEquals(Gender.MALE, Gender.fromDigit('1'))
        assertEquals(Gender.MALE, Gender.fromDigit('3'))
        assertEquals(Gender.MALE, Gender.fromDigit('9'))
        assertEquals(Gender.FEMALE, Gender.fromDigit('0'))
        assertEquals(Gender.FEMALE, Gender.fromDigit('2'))
        assertEquals(Gender.FEMALE, Gender.fromDigit('8'))
    }

    @Test
    fun `Gender fromNationalId extracts digit 12 correctly`() {
        assertEquals(Gender.MALE, Gender.fromNationalId(VALID_ID_1990_MALE_CAIRO))    // digit=1
        assertEquals(Gender.FEMALE, Gender.fromNationalId(VALID_ID_2005_FEMALE_ALEX)) // digit=2
        assertEquals(Gender.MALE, Gender.fromNationalId(VALID_ID_2000_MALE_GIZA))     // digit=3
        assertEquals(Gender.FEMALE, Gender.fromNationalId(VALID_ID_1995_FEMALE_DAK))  // digit=2
    }

    @Test
    fun `Gender fromNationalId throws for non-digit input`() {
        assertFailsWith<IllegalArgumentException> {
            Gender.fromNationalId("abc")
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 6.  Model — Governorate
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun `Governorate fromCode returns correct entry`() {
        assertEquals(Governorate.CAIRO, Governorate.fromCode(1))
        assertEquals(Governorate.ALEXANDRIA, Governorate.fromCode(2))
        assertEquals(Governorate.GIZA, Governorate.fromCode(14))
        assertEquals(Governorate.SOUTH_SINAI, Governorate.fromCode(27))
        assertEquals(Governorate.ABROAD_88, Governorate.fromCode(88))
        assertEquals(Governorate.ABROAD_99, Governorate.fromCode(99))
    }

    @Test
    fun `Governorate fromCode throws for unknown code`() {
        assertFailsWith<IllegalStateException> {
            Governorate.fromCode(0)
        }
        assertFailsWith<IllegalStateException> {
            Governorate.fromCode(30)
        }
        assertFailsWith<IllegalStateException> {
            Governorate.fromCode(-1)
        }
    }

    @Test
    fun `Governorate fromNationalId extracts digits 7-8 correctly`() {
        assertEquals(Governorate.CAIRO, Governorate.fromNationalId(VALID_ID_1990_MALE_CAIRO))
        assertEquals(Governorate.ALEXANDRIA, Governorate.fromNationalId(VALID_ID_2005_FEMALE_ALEX))
        assertEquals(Governorate.GIZA, Governorate.fromNationalId(VALID_ID_2000_MALE_GIZA))
        assertEquals(Governorate.DAKAHLIA, Governorate.fromNationalId(VALID_ID_1995_FEMALE_DAK))
    }

    // ─────────────────────────────────────────────────────────────────────
    // 7.  Parser — NationalIdParser
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun `parse extracts correct date of birth`() {
        val info = NationalIdParser.parse(VALID_ID_1990_MALE_CAIRO)
        assertEquals(LocalDate(1990, 1, 1), info.dateOfBirth)
    }

    @Test
    fun `parse extracts correct governorate`() {
        val info = NationalIdParser.parse(VALID_ID_1990_MALE_CAIRO)
        assertEquals(Governorate.CAIRO, info.governorate)
    }

    @Test
    fun `parse extracts correct gender`() {
        assertEquals(Gender.MALE, NationalIdParser.parse(VALID_ID_1990_MALE_CAIRO).gender)
        assertEquals(Gender.FEMALE, NationalIdParser.parse(VALID_ID_2005_FEMALE_ALEX).gender)
    }

    @Test
    fun `parse sets century correctly for 1900s and 2000s`() {
        assertEquals(1900, NationalIdParser.parse(VALID_ID_1990_MALE_CAIRO).century)   // digit 2
        assertEquals(2000, NationalIdParser.parse(VALID_ID_2005_FEMALE_ALEX).century)  // digit 3
    }

    @Test
    fun `parse extracts serial number and check digit`() {
        val info = NationalIdParser.parse(VALID_ID_1990_MALE_CAIRO)
        assertEquals(0, info.serialNumber)   // 000
        assertEquals(7, info.checkDigit)
    }

    @Test
    fun `parse preserves rawId and sets isValid to true for valid input`() {
        val info = NationalIdParser.parse(VALID_ID_1990_MALE_CAIRO)
        assertEquals(VALID_ID_1990_MALE_CAIRO, info.rawId)
        assertTrue(info.isValid)
    }

    @Test
    fun `parse returns isValid=false for invalid input without throwing`() {
        val info = NationalIdParser.parse("12345")
        assertFalse(info.isValid)
    }

    @Test
    fun `parse covers the leap year birthday correctly`() {
        val info = NationalIdParser.parse(VALID_ID_2000_MALE_GIZA)
        assertEquals(LocalDate(2000, 2, 29), info.dateOfBirth)
        assertTrue(info.isValid)
    }

    @Test
    fun `parseOrNull returns NationalIdInfo for valid IDs`() {
        assertNotNull(NationalIdParser.parseOrNull(VALID_ID_1990_MALE_CAIRO))
    }

    @Test
    fun `parseOrNull returns null for invalid IDs`() {
        assertNull(NationalIdParser.parseOrNull(""))
        assertNull(NationalIdParser.parseOrNull("29001010100010"))  // wrong checksum
        assertNull(NationalIdParser.parseOrNull("12345"))
    }

    @Test
    fun `parseValid returns info for valid IDs`() {
        val info = NationalIdParser.parseValid(VALID_ID_1990_MALE_CAIRO)
        assertTrue(info.isValid)
    }

    @Test
    fun `parseValid throws for invalid IDs`() {
        assertFailsWith<IllegalArgumentException> {
            NationalIdParser.parseValid("invalid")
        }
    }

    @Test
    fun `NationalIdInfo computed properties work correctly`() {
        val info = NationalIdParser.parse(VALID_ID_1990_MALE_CAIRO)
        // centuryDigit = century / 100 = 1900 / 100 = 19
        assertEquals(19, info.centuryDigit)
        assertEquals(1990, info.birthYear)
    }

    // ─────────────────────────────────────────────────────────────────────
    // 8.  Sanitizer — utils
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun `extractDigits removes non-digit characters`() {
        assertEquals("29001010100017", "290-0101-0100017".extractDigits())
        assertEquals("29001010100017", "290 01 01 0100017".extractDigits())
        assertEquals("29001010100017", "abc29001010100017xyz".extractDigits())
    }

    @Test
    fun `extractDigits preserves Arabic-Indic digits`() {
        // Arabic-Indic digits (U+0660..U+0669) are in the Unicode Nd category,
        // so they pass isDigit() and are preserved.
        val arabicIndic = "٢٩٠٠١٠١٠١٠٠٠١٧"
        val extracted = arabicIndic.extractDigits()
        assertEquals(14, extracted.length)
        assertTrue(extracted.all { it.isDigit() })
        // digitToInt() on Arabic-Indic digits returns the correct numeric value
        assertEquals(2, extracted[0].digitToInt())
        assertEquals(9, extracted[1].digitToInt())
    }

    @Test
    fun `sanitize truncates to 14 digits when input is longer`() {
        val result = "2900101010001777".sanitize()
        assertEquals("29001010100017", result)
        assertEquals(14, result.length)
    }

    @Test
    fun `sanitize returns fewer than 14 digits when input is shorter`() {
        assertEquals("123", "123".sanitize())
    }

    @Test
    fun `isValidNationalIdLength returns true only for exactly 14 digits`() {
        assertTrue("29001010100017".isValidNationalIdLength())
        assertFalse("123".isValidNationalIdLength())
        assertFalse("290010101000177".isValidNationalIdLength())
    }

    @Test
    fun `toNationalIdDigits returns 14-digit string or throws`() {
        assertEquals("29001010100017", "29001010100017".toNationalIdDigits())
        assertFailsWith<IllegalArgumentException> {
            "123".toNationalIdDigits()
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 9.  Sanitisation tolerance — handles messy real-world input
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun `validator handles IDs with spaces and hyphens`() {
        // Same valid ID with typical real-world formatting
        val withSpaces = "305 12 25 02 123 2 7"
        val withHyphens = "305-12-25-02123-27"
        assertTrue(NationalIdValidator.isValid(withSpaces))
        assertTrue(NationalIdValidator.isValid(withHyphens))
    }

    @Test
    fun `parser handles IDs with spaces and hyphens`() {
        val withSpaces = "305 12 25 02 123 2 7"
        val info = NationalIdParser.parse(withSpaces)
        assertTrue(info.isValid)
        assertEquals(LocalDate(2005, 12, 25), info.dateOfBirth)
        assertEquals(Governorate.ALEXANDRIA, info.governorate)
        assertEquals(Gender.FEMALE, info.gender)
    }

    // ─────────────────────────────────────────────────────────────────────
    // 10. Extension functions — Extensions.kt
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun `isValidEgyptianId extension matches validator`() {
        assertTrue(VALID_ID_1990_MALE_CAIRO.isValidEgyptianId())
        assertFalse("12345".isValidEgyptianId())
    }

    @Test
    fun `toNationalIdInfo extension returns valid info`() {
        val info = VALID_ID_1990_MALE_CAIRO.toNationalIdInfo()
        assertTrue(info.isValid)
        assertEquals(LocalDate(1990, 1, 1), info.dateOfBirth)
    }

    @Test
    fun `toNationalIdInfoOrNull extension returns null for invalid`() {
        assertNotNull(VALID_ID_1990_MALE_CAIRO.toNationalIdInfoOrNull())
        assertNull("bad".toNationalIdInfoOrNull())
    }

    @Test
    fun `sanitizeEgyptianId extension cleans input`() {
        assertEquals("29001010100017", "290-0101-0100017".sanitizeEgyptianId())
    }

    @Test
    fun `extractEgyptianIdDigits extension strips non-digits`() {
        assertEquals("29001010100017", "abc290-0101-0100017".extractEgyptianIdDigits())
    }

    @Test
    fun `isValidEgyptianIdLength extension returns correct result`() {
        assertTrue(VALID_ID_1990_MALE_CAIRO.isValidEgyptianIdLength())
        assertFalse("123".isValidEgyptianIdLength())
    }

    // ─────────────────────────────────────────────────────────────────────
    // 11. Edge cases
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun `empty string is never valid`() {
        assertFalse(NationalIdValidator.isValid(""))
        assertNull(NationalIdParser.parseOrNull(""))
        assertFalse("".isValidEgyptianId())
    }

    @Test
    fun `null-safe string equality on NationalIdInfo`() {
        val info = NationalIdParser.parse(VALID_ID_1990_MALE_CAIRO)
        // Verify that data class equality works as expected
        assertEquals(VALID_ID_1990_MALE_CAIRO, info.rawId)
    }

    @Test
    fun `parse out-of-range year falls back gracefully`() {
        // The library does not restrict years, but kotlinx.datetime may.
        // This test ensures no crash for reasonable extreme values.
        val extreme = buildId("3999999999999")  // 2099-99-99, improbable but should not crash
        val info = NationalIdParser.parse(extreme)
        assertFalse(info.isValid) // invalid date, but parse should not throw
    }
}
