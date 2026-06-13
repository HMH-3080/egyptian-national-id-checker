package io.github.hmh3080.egyptianid.models

/**
 * Gender extracted from the 13th digit (0-indexed: 12) of an Egyptian National ID.
 *
 * Odd digit → [MALE], even digit → [FEMALE].
 */
enum class Gender(val arabicName: String) {
    MALE("ذكر"),
    FEMALE("أنثى");

    companion object {
        private const val GENDER_DIGIT_INDEX = 12

        /**
         * Determines the gender from a single digit character.
         *
         * @param digit the 13th digit of the National ID ('0'–'9')
         * @return [MALE] if the digit is odd, [FEMALE] if even
         */
        fun fromDigit(digit: Char): Gender =
            if (digit.digitToInt() % 2 == 1) MALE else FEMALE

        /**
         * Extracts and determines the gender directly from a 14-digit Egyptian National ID.
         *
         * @param id a 14-digit National ID string (digits only)
         * @return the corresponding [Gender]
         * @throws IllegalArgumentException if the input is not exactly 14 digits
         */
        fun fromNationalId(id: String): Gender {
            require(id.length == 14 && id.all { it.isDigit() }) {
                "National ID must be exactly 14 digits"
            }
            return fromDigit(id[GENDER_DIGIT_INDEX])
        }
    }
}
