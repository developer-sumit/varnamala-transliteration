package com.sumit.varnamala.transliteration

/**
 * Script-neutral unit of text. Every reader produces tokens and every writer consumes them,
 * so any supported script can be converted to any other through this one representation.
 *
 * Letters are identified by their offset in the Devanagari block (the ISCII layout shared by
 * all nine Brahmic Unicode blocks): 0x15 is "ka" whichever script it came from.
 */
internal data class Token(
    val kind: Kind,
    /** ISCII offset for letters and vowel signs, 0..9 for digits, 1 or 2 for dandas. */
    val code: Int = 0,
    /** Nukta variant of the letter: क़ q, ज़ z, फ़ f, ड़ ṛ, and Dravidian ṉ, ṟ, ḻ. */
    val nukta: Boolean = false,
    /** Verbatim text for [Kind.OTHER]. */
    val text: String = "",
) {
    enum class Kind {
        CONSONANT, VOWEL, SIGN, VIRAMA, ANUSVARA, CANDRABINDU, VISARGA, AVAGRAHA, DIGIT, DANDA, OTHER
    }

    val isConsonant get() = kind == Kind.CONSONANT
    val isVowelBearing get() = kind == Kind.VOWEL || kind == Kind.SIGN
}

/** Named ISCII offsets, so the tables read as letters rather than numbers. */
internal object Iscii {
    const val CANDRABINDU = 0x01
    const val ANUSVARA = 0x02
    const val VISARGA = 0x03

    const val A = 0x05
    const val AA = 0x06
    const val I = 0x07
    const val II = 0x08
    const val U = 0x09
    const val UU = 0x0A
    const val VOCALIC_R = 0x0B
    const val VOCALIC_L = 0x0C
    const val CANDRA_E = 0x0D
    const val SHORT_E = 0x0E
    const val E = 0x0F
    const val AI = 0x10
    const val CANDRA_O = 0x11
    const val SHORT_O = 0x12
    const val O = 0x13
    const val AU = 0x14
    const val VOCALIC_RR = 0x60
    const val VOCALIC_LL = 0x61

    const val KA = 0x15
    const val KHA = 0x16
    const val GA = 0x17
    const val GHA = 0x18
    const val NGA = 0x19
    const val CA = 0x1A
    const val CHA = 0x1B
    const val JA = 0x1C
    const val JHA = 0x1D
    const val NYA = 0x1E
    const val TTA = 0x1F
    const val TTHA = 0x20
    const val DDA = 0x21
    const val DDHA = 0x22
    const val NNA = 0x23
    const val TA = 0x24
    const val THA = 0x25
    const val DA = 0x26
    const val DHA = 0x27
    const val NA = 0x28
    const val NNNA = 0x29
    const val PA = 0x2A
    const val PHA = 0x2B
    const val BA = 0x2C
    const val BHA = 0x2D
    const val MA = 0x2E
    const val YA = 0x2F
    const val RA = 0x30
    const val RRA = 0x31
    const val LA = 0x32
    const val LLA = 0x33
    const val LLLA = 0x34
    const val VA = 0x35
    const val SHA = 0x36
    const val SSA = 0x37
    const val SA = 0x38
    const val HA = 0x39

    const val NUKTA = 0x3C
    const val AVAGRAHA = 0x3D
    const val VIRAMA = 0x4D
    const val OM = 0x50
    const val DIGIT_ZERO = 0x66

    // Dependent vowel signs.
    const val SIGN_AA = 0x3E
    const val SIGN_I = 0x3F
    const val SIGN_II = 0x40
    const val SIGN_U = 0x41
    const val SIGN_UU = 0x42
    const val SIGN_VOCALIC_R = 0x43
    const val SIGN_VOCALIC_RR = 0x44
    const val SIGN_CANDRA_E = 0x45
    const val SIGN_SHORT_E = 0x46
    const val SIGN_E = 0x47
    const val SIGN_AI = 0x48
    const val SIGN_CANDRA_O = 0x49
    const val SIGN_SHORT_O = 0x4A
    const val SIGN_O = 0x4B
    const val SIGN_AU = 0x4C
    const val SIGN_VOCALIC_L = 0x62
    const val SIGN_VOCALIC_LL = 0x63

    fun isConsonant(offset: Int) = offset in KA..HA
    fun isIndependentVowel(offset: Int) = offset in 0x04..0x14 || offset == VOCALIC_RR || offset == VOCALIC_LL
    fun isVowelSign(offset: Int) =
        offset in SIGN_AA..SIGN_AU || offset == 0x3A || offset == 0x3B || offset == 0x4E || offset == 0x4F ||
            offset in 0x55..0x57 || offset == SIGN_VOCALIC_L || offset == SIGN_VOCALIC_LL

    /** Independent vowel ↔ dependent sign. */
    fun signFor(vowel: Int): Int? = when (vowel) {
        A -> null
        AA -> SIGN_AA
        I -> SIGN_I
        II -> SIGN_II
        U -> SIGN_U
        UU -> SIGN_UU
        VOCALIC_R -> SIGN_VOCALIC_R
        VOCALIC_RR -> SIGN_VOCALIC_RR
        VOCALIC_L -> SIGN_VOCALIC_L
        VOCALIC_LL -> SIGN_VOCALIC_LL
        CANDRA_E -> SIGN_CANDRA_E
        SHORT_E -> SIGN_SHORT_E
        E -> SIGN_E
        AI -> SIGN_AI
        CANDRA_O -> SIGN_CANDRA_O
        SHORT_O -> SIGN_SHORT_O
        O -> SIGN_O
        AU -> SIGN_AU
        else -> null
    }

    fun vowelFor(sign: Int): Int = when (sign) {
        SIGN_AA -> AA
        SIGN_I -> I
        SIGN_II -> II
        SIGN_U -> U
        SIGN_UU -> UU
        SIGN_VOCALIC_R -> VOCALIC_R
        SIGN_VOCALIC_RR -> VOCALIC_RR
        SIGN_VOCALIC_L -> VOCALIC_L
        SIGN_VOCALIC_LL -> VOCALIC_LL
        SIGN_CANDRA_E -> CANDRA_E
        SIGN_SHORT_E -> SHORT_E
        SIGN_E -> E
        SIGN_AI -> AI
        SIGN_CANDRA_O -> CANDRA_O
        SIGN_SHORT_O -> SHORT_O
        SIGN_O -> O
        SIGN_AU -> AU
        else -> E
    }

    /** Places of articulation, used to choose a homorganic nasal for an anusvara. */
    fun nasalFor(nextConsonant: Int?): Int = when (nextConsonant) {
        in KA..NGA -> NGA
        in CA..NYA -> NYA
        in TTA..NNA -> NNA
        in TA..NNNA -> NA
        in PA..MA -> MA
        else -> MA
    }

    fun isStop(offset: Int) = offset in KA..MA && offset != NGA && offset != NYA && offset != NNA &&
        offset != NA && offset != NNNA && offset != MA
}
