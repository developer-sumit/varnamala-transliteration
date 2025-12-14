package com.sumit.varnamala.utils

object LanguageDetector {

    private fun getScriptForChar(c: Char): String? {
        return when (Character.UnicodeBlock.of(c)) {
            Character.UnicodeBlock.DEVANAGARI -> "hi"
            Character.UnicodeBlock.GURMUKHI -> "pa"
            Character.UnicodeBlock.GUJARATI -> "gu"
            Character.UnicodeBlock.BENGALI -> "bn"
            Character.UnicodeBlock.TAMIL -> "ta"
            Character.UnicodeBlock.TELUGU -> "te"
            Character.UnicodeBlock.BASIC_LATIN,
            Character.UnicodeBlock.LATIN_1_SUPPLEMENT,
            Character.UnicodeBlock.LATIN_EXTENDED_A,
            Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL -> "en"

            else -> null
        }
    }

    private fun getScriptForWord(word: String): String? {
        val clean = word.replace(Regex("[^\\p{L}]"), "")
        if (clean.isEmpty()) return null
        return getScriptForChar(clean.first())
    }

    fun detectLanguage(text: String): String {
        if (text.isBlank()) return "unknown"

        val normalizedText = text.trim()
        val words = normalizedText.split("\\s+".toRegex())

        val charCounts = mutableMapOf<String, Int>()
        val wordCounts = mutableMapOf<String, Int>()
        var totalChars = 0

        for (char in normalizedText) {
            val script = getScriptForChar(char)
            if (script != null) {
                charCounts[script] = charCounts.getOrDefault(script, 0) + 1
                totalChars++
            }
        }

        for (word in words) {
            val script = getScriptForWord(word)
            if (script != null) {
                wordCounts[script] = wordCounts.getOrDefault(script, 0) + 1
            }
        }

        if (totalChars == 0) return "unknown"

        val scriptPercentages = charCounts.mapValues { it.value.toFloat() / totalChars }
        val wordPercentages = wordCounts.mapValues { it.value.toFloat() / words.size }

        val combinedScores = mutableMapOf<String, Float>()
        val languages = (charCounts.keys + wordCounts.keys).toSet()

        for (lang in languages) {
            val scriptScore = scriptPercentages.getOrDefault(lang, 0f)
            val wordScore = wordPercentages.getOrDefault(lang, 0f)
            combinedScores[lang] = (0.7f * scriptScore) + (0.3f * wordScore)
        }

        val sortedLangs = combinedScores.entries
            .sortedByDescending { it.value }
            .filter { it.value > 0.1f }

        return when {
            sortedLangs.isEmpty() -> "unknown"
            sortedLangs.size == 1 -> sortedLangs[0].key
            sortedLangs.size == 2 -> "mixed_${sortedLangs[0].key}_${sortedLangs[1].key}"
            else -> "mixed_multi"
        }
    }
}
