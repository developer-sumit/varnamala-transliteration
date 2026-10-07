package com.sumit.varnamala.transliteration

/**
 * A language as the engine sees it: the script it is written in and the spelling conventions
 * that decide how romanized input is read and how native text is romanized.
 */
enum class Language(
    val code: String,
    val script: Script,
    /** Word-final and medial inherent vowels go silent when read aloud (Hindi "kamal", not "kamala"). */
    internal val deletesSchwa: Boolean,
    /** Romanized word-final "a" after a consonant means the long vowel (raja → राजा). */
    internal val finalALong: Boolean,
    /** Romanized word-final "i"/"u" after a consonant mean long vowels (pani → पानी). */
    internal val finalIULong: Boolean,
    /** A word ending in a bare consonant is written with a visible virama (bus → బస్). */
    internal val writesFinalVirama: Boolean,
    /** Romanized "e"/"o" are short vowels (Dravidian); "ae"/"oa" give the long ones. */
    internal val shortEO: Boolean,
) {
    ENGLISH("en", Script.LATIN, false, false, false, false, false),
    HINDI("hi", Script.DEVANAGARI, true, true, true, false, false),
    MARATHI("mr", Script.DEVANAGARI, true, true, true, false, false),
    NEPALI("ne", Script.DEVANAGARI, true, true, true, false, false),
    SANSKRIT("sa", Script.DEVANAGARI, false, false, false, true, false),
    BENGALI("bn", Script.BENGALI, true, true, false, false, false),
    ASSAMESE("as", Script.BENGALI, true, true, false, false, false),
    PUNJABI("pa", Script.GURMUKHI, true, true, true, false, false),
    GUJARATI("gu", Script.GUJARATI, true, true, true, false, false),
    ODIA("or", Script.ORIYA, false, true, false, false, false),
    TAMIL("ta", Script.TAMIL, false, true, false, true, true), // amma, appa: final a is long
    TELUGU("te", Script.TELUGU, false, false, false, true, true),
    KANNADA("kn", Script.KANNADA, false, false, false, true, true),
    MALAYALAM("ml", Script.MALAYALAM, false, false, false, true, true),
    URDU("ur", Script.URDU, true, true, true, false, false);

    companion object {
        private val aliases: Map<String, Language> = buildMap {
            fun alias(language: Language, vararg names: String) = names.forEach { put(it, language) }
            alias(ENGLISH, "en", "eng", "english", "latin", "unknown")
            alias(HINDI, "hi", "hin", "hindi", "devanagari")
            alias(MARATHI, "mr", "mar", "marathi")
            alias(NEPALI, "ne", "nep", "nepali")
            alias(SANSKRIT, "sa", "san", "sanskrit")
            alias(BENGALI, "bn", "ben", "bengali", "bangla")
            alias(ASSAMESE, "as", "asm", "assamese")
            alias(PUNJABI, "pa", "pan", "punjabi", "gurmukhi")
            alias(GUJARATI, "gu", "guj", "gujarati")
            alias(ODIA, "or", "ori", "odia", "oriya")
            alias(TAMIL, "ta", "tam", "tamil")
            alias(TELUGU, "te", "tel", "telugu")
            alias(KANNADA, "kn", "kan", "kannada")
            alias(MALAYALAM, "ml", "mal", "malayalam")
            alias(URDU, "ur", "urd", "urdu", "arabic")
        }

        /**
         * Resolves ISO 639-1/639-2 codes, English names, and script names. Locale tags such as
         * "hi-IN" resolve by their language part. Returns null for anything unsupported.
         */
        fun fromCode(code: String?): Language? {
            if (code.isNullOrBlank()) return null
            val normalized = code.trim().lowercase().replace('_', '-')
            return aliases[normalized] ?: aliases[normalized.substringBefore('-')]
        }

        /** Default language for text found in a script, used when no better hint exists. */
        fun defaultFor(script: Script): Language = when (script) {
            Script.DEVANAGARI -> HINDI
            Script.BENGALI -> BENGALI
            Script.GURMUKHI -> PUNJABI
            Script.GUJARATI -> GUJARATI
            Script.ORIYA -> ODIA
            Script.TAMIL -> TAMIL
            Script.TELUGU -> TELUGU
            Script.KANNADA -> KANNADA
            Script.MALAYALAM -> MALAYALAM
            Script.LATIN -> ENGLISH
            Script.URDU -> URDU
        }
    }
}
