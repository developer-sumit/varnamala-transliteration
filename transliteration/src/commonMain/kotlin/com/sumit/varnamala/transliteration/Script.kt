package com.sumit.varnamala.transliteration

/** Writing systems the engine reads and writes. */
enum class Script(internal val blockStart: Int) {
    DEVANAGARI(0x0900),
    BENGALI(0x0980),
    GURMUKHI(0x0A00),
    GUJARATI(0x0A80),
    ORIYA(0x0B00),
    TAMIL(0x0B80),
    TELUGU(0x0C00),
    KANNADA(0x0C80),
    MALAYALAM(0x0D00),
    LATIN(-1),

    /** Perso-Arabic script as written for Urdu. */
    URDU(-1);

    internal val isBrahmic: Boolean get() = blockStart >= 0

    /** Scripts of the ISCII family that have no short e/o distinction. */
    internal val isIndoAryan: Boolean
        get() = this == DEVANAGARI || this == BENGALI || this == GURMUKHI || this == GUJARATI || this == ORIYA

    companion object {
        private val brahmic = entries.filter { it.isBrahmic }

        /** Script a single character belongs to, or null for spaces, digits, punctuation, etc. */
        fun of(char: Char): Script? {
            val code = char.code
            brahmic.firstOrNull { code in it.blockStart until it.blockStart + 0x80 }?.let {
                // Danda and double danda live in the Devanagari block but are shared punctuation.
                return if (code == DANDA || code == DOUBLE_DANDA) null else it
            }
            return when {
                isUrdu(code) -> URDU
                isLatinLetter(char) -> LATIN
                else -> null
            }
        }

        internal const val DANDA = 0x0964
        internal const val DOUBLE_DANDA = 0x0965

        internal fun isLatinLetter(char: Char): Boolean {
            val code = char.code
            return (char in 'a'..'z') || (char in 'A'..'Z') ||
                (code in 0x00C0..0x024F && code != 0x00D7 && code != 0x00F7) ||
                code in 0x1E00..0x1EFF
        }

        internal fun isLatinCombiningMark(char: Char) = char.code in 0x0300..0x036F

        private fun isUrdu(code: Int): Boolean =
            (code in 0x0600..0x06FF && code !in 0x0660..0x0669 && code !in 0x06F0..0x06F9 &&
                code != 0x060C && code != 0x061B && code != 0x061F && code != 0x06D4) ||
                code in 0x0750..0x077F || code in 0xFB50..0xFDFF || code in 0xFE70..0xFEFF
    }
}
