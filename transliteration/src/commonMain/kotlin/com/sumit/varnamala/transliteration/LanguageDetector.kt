package com.sumit.varnamala.transliteration

/**
 * Guesses the language of a text from its scripts. Script identifies the language except where
 * several share one: Devanagari reports Hindi, and Bengali script reports Bengali unless it
 * contains the Assamese letters ৰ or ৱ.
 */
object LanguageDetector {
    const val UNKNOWN = "unknown"

    /**
     * Returns a language code such as "hi", "mixed_hi_en" for two languages, "mixed_multi" for
     * more, or [UNKNOWN] when the text has no letters.
     */
    fun detectLanguage(text: String): String {
        if (text.isBlank()) return UNKNOWN
        val words = text.trim().split(Regex("\\s+"))

        val charCounts = mutableMapOf<String, Int>()
        val wordCounts = mutableMapOf<String, Int>()
        var totalChars = 0
        for (char in text) {
            val language = languageOf(char, text) ?: continue
            charCounts[language] = (charCounts[language] ?: 0) + 1
            totalChars++
        }
        if (totalChars == 0) return UNKNOWN
        for (word in words) {
            val language = word.firstNotNullOfOrNull { languageOf(it, text) } ?: continue
            wordCounts[language] = (wordCounts[language] ?: 0) + 1
        }

        // Characters weigh more than words so one long word is not outvoted by short ones.
        val scores = (charCounts.keys + wordCounts.keys).associateWith { language ->
            0.7f * (charCounts[language] ?: 0) / totalChars + 0.3f * (wordCounts[language] ?: 0) / words.size
        }
        val ranked = scores.entries.sortedByDescending { it.value }.filter { it.value > 0.1f }
        return when (ranked.size) {
            0 -> UNKNOWN
            1 -> ranked[0].key
            2 -> "mixed_${ranked[0].key}_${ranked[1].key}"
            else -> "mixed_multi"
        }
    }

    /** Language for the dominant script of [text], or null when it has no letters. */
    fun detect(text: String): Language? =
        Language.fromCode(detectLanguage(text).removePrefix("mixed_").substringBefore('_'))

    private fun languageOf(char: Char, text: String): String? {
        val script = Script.of(char) ?: return null
        if (script == Script.BENGALI && ('ৰ' in text || 'ৱ' in text)) return Language.ASSAMESE.code
        return Language.defaultFor(script).code
    }
}
