package com.sumit.varnamala.utils

import com.sumit.varnamala.data.TransliterationMappingRepository

object Transliterator {
    object Languages {
        const val ENGLISH = "en"
        const val HINDI = "hi"
        const val TELUGU = "te"
        const val PUNJABI = "pa"
        const val MALAYALAM = "ml"

        val ALL = listOf(ENGLISH, HINDI, TELUGU, PUNJABI, MALAYALAM)

        fun getDisplayName(code: String): String = when (code) {
            ENGLISH -> "English"
            HINDI -> "Hindi"
            TELUGU -> "Telugu"
            PUNJABI -> "Punjabi"
            MALAYALAM -> "Malayalam"
            else -> code.uppercase()
        }
    }

    // Cache mappings to avoid re-generating them on every call
    private val allMappings by lazy { TransliterationMappingRepository.getAllMappings() }

    fun transliterate(
        text: String,
        sourceLanguage: String?,
        targetLanguage: String = Languages.ENGLISH
    ): String {
        if (text.isBlank() || sourceLanguage == null || sourceLanguage == targetLanguage) {
            return text
        }

        val latinText = when (sourceLanguage.lowercase()) {
            "hindi", "hi", "devanagari" -> hindiToEnglish(text.lowercase())
            "telugu", "te" -> teluguToEnglish(text.lowercase())
            "punjabi", "pa", "gurmukhi" -> punjabiToEnglish(text.lowercase())
            "malayalam", "ml" -> malayalamToEnglish(text.lowercase())
            "en", "english" -> text
            else -> text.lowercase()
        }

        if (targetLanguage.lowercase() == "en" || targetLanguage.lowercase() == "english") {
            return latinText
        }

        return when (targetLanguage.lowercase()) {
            "hindi", "hi", "devanagari" -> englishToHindi(latinText)
            "telugu", "te" -> englishToTelugu(latinText)
            "punjabi", "pa", "gurmukhi" -> englishToPunjabi(latinText)
            "malayalam", "ml" -> englishToMalayalam(latinText)
            else -> latinText
        }
    }

    private fun hindiToEnglish(text: String): String {
        val hindiToEnglishMap = mutableMapOf<String, String>()

        allMappings.forEach { entry ->
            hindiToEnglishMap[entry.hindiDevanagari] = entry.englishRoman
        }

        hindiToEnglishMap["्"] = "" // Halant (virama) - usually silent in English transliteration

        return transliterateToScript(text, hindiToEnglishMap)
    }

    private fun teluguToEnglish(text: String): String {
        val teluguToEnglishMap = mutableMapOf<String, String>()

        allMappings.forEach { entry ->
            teluguToEnglishMap[entry.teluguScript] = entry.englishRoman
        }

        teluguToEnglishMap["్"] = "" // Halant (virama)

        return transliterateToScript(text, teluguToEnglishMap)
    }

    private fun englishToHindi(text: String): String {
        // 1. Pre-Processing: Case Normalization
        val lowerText = text.lowercase()

        // Build specialized maps for the loop
        val vowels = TransliterationMappingRepository.getVowelMappings()
        val consonants = TransliterationMappingRepository.getConsonantMappings()
        val special = TransliterationMappingRepository.getSpecialSymbols()
        val commonWords = TransliterationMappingRepository.getCommonEnglishWords()

        data class TranslitEntry(
            val hindi: String,
            val matra: String? = null,
            val type: EntryType
        )

        val lookupMap = mutableMapOf<String, TranslitEntry>()

        commonWords.forEach {
            lookupMap[it.englishRoman.lowercase()] =
                TranslitEntry(it.hindiDevanagari, type = EntryType.WORD)
        }

        vowels.forEach {
            lookupMap[it.englishRoman.lowercase()] =
                TranslitEntry(it.hindiDevanagari, it.hindiMatra, EntryType.VOWEL)
        }

        consonants.forEach {
            val baseRoman =
                if (it.englishRoman.endsWith("a")) it.englishRoman.dropLast(1) else it.englishRoman
            lookupMap[baseRoman.lowercase()] =
                TranslitEntry(it.hindiDevanagari, type = EntryType.CONSONANT)
        }

        special.forEach {
            lookupMap[it.englishRoman.lowercase()] =
                TranslitEntry(it.hindiDevanagari, type = EntryType.OTHER)
        }

        val result = StringBuilder()
        var i = 0
        var lastType = EntryType.OTHER

        while (i < lowerText.length) {
            var match: String? = null
            var entry: TranslitEntry? = null

            for (len in 5 downTo 1) {
                if (i + len <= lowerText.length) {
                    val sub = lowerText.substring(i, i + len)
                    if (lookupMap.containsKey(sub)) {
                        match = sub
                        entry = lookupMap[sub]
                        break
                    }
                }
            }

            if (match != null && entry != null) {
                when (entry.type) {
                    EntryType.WORD -> {
                        result.append(entry.hindi)
                        lastType = EntryType.WORD
                    }

                    EntryType.CONSONANT -> {
                        result.append(entry.hindi)

                        var nextIsConsonant = false
                        // Check what the next token would be
                        var nextI = i + match.length
                        if (nextI < lowerText.length) {
                            // Find what the next match would be
                            for (len in 5 downTo 1) {
                                if (nextI + len <= lowerText.length) {
                                    val nextSub = lowerText.substring(nextI, nextI + len)
                                    val nextEntry = lookupMap[nextSub]
                                    if (nextEntry != null) {
                                        if (nextEntry.type == EntryType.CONSONANT) {
                                            nextIsConsonant = true
                                        }
                                        break // Found the next token
                                    }
                                }
                            }
                        }

                        if (nextIsConsonant) {
                            result.append("्") // Halant
                        }

                        lastType = EntryType.CONSONANT
                    }

                    EntryType.VOWEL -> {
                        if (lastType == EntryType.CONSONANT) {
                            // Apply Matra
                            result.append(entry.matra ?: "")
                        } else {
                            // Independent Vowel
                            result.append(entry.hindi)
                        }
                        lastType = EntryType.VOWEL
                    }

                    EntryType.OTHER -> {
                        result.append(entry.hindi)
                        lastType = EntryType.OTHER
                    }
                }
                i += match.length
            } else {
                // No match found, keep original char
                result.append(lowerText[i])
                lastType = EntryType.OTHER
                i++
            }
        }

        return result.toString()
    }

    private enum class EntryType {
        CONSONANT, VOWEL, WORD, OTHER
    }

    private fun englishToTelugu(text: String): String {
        val englishToTeluguMap = mutableMapOf<String, String>()

        allMappings.forEach { entry ->
            englishToTeluguMap[entry.englishRoman] = entry.teluguScript
        }

        // Convert text to lowercase for case-insensitive matching
        return transliterateToScript(text.lowercase(), englishToTeluguMap)
    }

    private fun transliterateToScript(text: String, mappingTable: Map<String, String>): String {
        var result = text

        // Sort mappings by length (longest first) to ensure greedy matching
        // e.g. match "sha" before "sa", "tha" before "ta"
        val sortedMappings = mappingTable.entries.sortedByDescending { it.key.length }

        for ((source, target) in sortedMappings) {
            // Use literal replacement (not regex) to avoid escaping issues
            // This replaces all occurrences in the text
            result = result.replace(source, target)
        }

        if (mappingTable.containsValue("क")) { // Check if target is Devanagari (heuristic)
            result = result.replace("् ", " ")
                .replace("्$", "")
                .replace("्\n", "\n")
                .replace("्\r", "\r")
                .replace("्\t", "\t")
                .replace("्.", ".")
                .replace("्,", ",")
                .replace("्!", "!")
                .replace("्?", "?")
        }

        return result
    }

    private fun punjabiToEnglish(text: String): String {
        val punjabiToEnglishMap = mutableMapOf<String, String>()

        allMappings.forEach { entry ->
            entry.punjabiScript?.let { punjabiToEnglishMap[it] = entry.englishRoman }
        }

        punjabiToEnglishMap["੍"] = "" // Halant (virama)

        return transliterateToScript(text, punjabiToEnglishMap)
    }

    private fun malayalamToEnglish(text: String): String {
        val malayalamToEnglishMap = mutableMapOf<String, String>()

        allMappings.forEach { entry ->
            entry.malayalamScript?.let { malayalamToEnglishMap[it] = entry.englishRoman }
        }

        malayalamToEnglishMap["്"] = "" // Halant (virama)

        return transliterateToScript(text, malayalamToEnglishMap)
    }

    private fun englishToPunjabi(text: String): String {
        val lowerText = text.lowercase()
        val englishToPunjabiMap = mutableMapOf<String, String>()

        allMappings.forEach { entry ->
            entry.punjabiScript?.let { englishToPunjabiMap[entry.englishRoman.lowercase()] = it }
        }

        return transliterateToScript(lowerText, englishToPunjabiMap)
    }

    private fun englishToMalayalam(text: String): String {
        val lowerText = text.lowercase()
        val englishToMalayalamMap = mutableMapOf<String, String>()

        allMappings.forEach { entry ->
            entry.malayalamScript?.let {
                englishToMalayalamMap[entry.englishRoman.lowercase()] = it
            }
        }

        return transliterateToScript(lowerText, englishToMalayalamMap)
    }

    fun getSupportedTargetLanguages(sourceLanguage: String?): List<String> {
        if (sourceLanguage == null) return emptyList()

        // We support conversion from any language to English, Hindi, Telugu, Punjabi, and Malayalam
        return listOf(
            Languages.ENGLISH,
            Languages.HINDI,
            Languages.TELUGU,
            Languages.PUNJABI,
            Languages.MALAYALAM
        )
    }
}
