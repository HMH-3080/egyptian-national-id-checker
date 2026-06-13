package io.github.hmh3080.egyptianid.models

/**
 * Egyptian governorate or region code encoded in digits 8–9 (0-indexed: 7–8)
 * of the National ID.
 */
enum class Governorate(val code: Int, val arabicName: String) {
    CAIRO(1, "القاهرة"),
    ALEXANDRIA(2, "الإسكندرية"),
    PORT_SAID(3, "بورسعيد"),
    SUEZ(4, "السويس"),
    DAMIETTA(5, "دمياط"),
    DAKAHLIA(6, "الدقهلية"),
    SHARKIA(7, "الشرقية"),
    QALYUBIA(8, "القليوبية"),
    KAFR_EL_SHEIKH(9, "كفر الشيخ"),
    GHARBIA(10, "الغربية"),
    MONUFIA(11, "المنوفية"),
    BEHEIRA(12, "البحيرة"),
    ISMAILIA(13, "الإسماعيلية"),
    GIZA(14, "الجيزة"),
    BENI_SUEF(15, "بني سويف"),
    FAYOUM(16, "الفيوم"),
    MINYA(17, "المنيا"),
    ASSIUT(18, "أسيوط"),
    SOHAG(19, "سوهاج"),
    QENA(20, "قنا"),
    LUXOR(21, "الأقصر"),
    ASWAN(22, "أسوان"),
    RED_SEA(23, "البحر الأحمر"),
    NEW_VALLEY(24, "الوادي الجديد"),
    MATROUH(25, "مطروح"),
    NORTH_SINAI(26, "شمال سيناء"),
    SOUTH_SINAI(27, "جنوب سيناء"),
    ABROAD_88(88, "مصريين بالخارج"),
    ABROAD_99(99, "مصريين بالخارج");

    companion object {
        private val map = entries.associateBy { it.code }

        /**
         * Returns the [Governorate] matching the given numeric code.
         *
         * @param code the two-digit governorate code (1–27, 88, 99)
         * @return the matching [Governorate]
         * @throws IllegalArgumentException if the code is unknown
         */
        fun fromCode(code: Int): Governorate =
            map[code] ?: error("Invalid governorate code: $code")

        /**
         * Extracts the governorate code from a 14-digit National ID and returns
         * the matching [Governorate].
         *
         * @param id a 14-digit National ID string (digits only)
         * @return the corresponding [Governorate]
         * @throws IllegalArgumentException if the input is not exactly 14 digits or the code is unknown
         */
        fun fromNationalId(id: String): Governorate {
            require(id.length >= 14 && id.all { it.isDigit() }) {
                "National ID must be exactly 14 digits"
            }
            val govCode = id.substring(7, 9).toIntOrNull()
                ?: error("Invalid governorate code in national ID")
            return fromCode(govCode)
        }
    }
}
