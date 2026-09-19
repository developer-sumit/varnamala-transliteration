package com.sumit.varnamala.utils

import android.util.Log
import com.sumit.varnamala.transliteration.IndianSignboardDictionary
import com.sumit.varnamala.transliteration.TransliterationRules

object Transliterator {
    private const val TAG = "Transliterator"

    object Languages {
        const val ENGLISH = "en"
        const val HINDI = "hi"
        const val BENGALI = "bn"
        const val TAMIL = "ta"
        const val TELUGU = "te"
        const val MARATHI = "mr"
        const val GUJARATI = "gu"
        const val KANNADA = "kn"
        const val MALAYALAM = "ml"
        const val PUNJABI = "pa"
        const val ODIA = "or"
        const val ASSAMESE = "as"

        val ALL = listOf(
            ENGLISH, HINDI, BENGALI, TAMIL, TELUGU, MARATHI,
            GUJARATI, KANNADA, MALAYALAM, PUNJABI, ODIA, ASSAMESE
        )
    }

    fun transliterate(
        text: String,
        sourceLanguage: String?,
        targetLanguage: String = Languages.ENGLISH
    ): String {
        if (text.isBlank()) {
            return text
        }

        return transliterateWithIcu(text, sourceLanguage, targetLanguage)
    }

    private fun transliterateWithIcu(
        text: String,
        sourceLanguage: String?,
        targetLanguage: String
    ): String {
        val normalizedSource = sourceLanguage?.let(::normalizeLanguageCode)
        val normalizedTarget = normalizeLanguageCode(targetLanguage)
        val targetScript = scriptForLanguage(normalizedTarget) ?: return text
        val sourceScript = normalizedSource?.let(::scriptForLanguage)

        val transformId = when {
            targetScript == "Latin" -> "Any-Latin; Latin-ASCII"
            sourceScript == null || sourceScript == "Latin" -> "Latin-$targetScript"
            sourceScript == targetScript -> return text
            else -> "$sourceScript-Latin; Latin-$targetScript"
        }

        return runCatching {
            android.icu.text.Transliterator
                .getInstance(transformId)
                .transliterate(text)
        }.getOrElse { error ->
            Log.w(TAG, "ICU transliteration failed for $transformId", error)
            text
        }
    }

    private fun scriptForLanguage(languageCode: String): String? {
        return when (normalizeLanguageCode(languageCode)) {
            "en", "eng", "latin" -> "Latin"
            "hi", "mr", "ne", "sa", "hin", "mar", "nep", "san", "devanagari" -> "Devanagari"
            "bn", "as", "ben", "asm" -> "Bengali"
            "gu", "guj" -> "Gujarati"
            "pa", "pan", "gurmukhi" -> "Gurmukhi"
            "kn", "kan" -> "Kannada"
            "ml", "mal" -> "Malayalam"
            "or", "ori" -> "Oriya"
            "ta", "tam" -> "Tamil"
            "te", "tel" -> "Telugu"
            "ur", "urd", "arabic" -> "Arabic"
            else -> null
        }
    }

    private fun legacyTransliterate(
        text: String,
        sourceLanguage: String?,
        targetLanguage: String = Languages.ENGLISH
    ): String {
        if (text.isBlank() || sourceLanguage == null || sourceLanguage == targetLanguage) {
            return text
        }

        val normalizedTarget = normalizeLanguageCode(targetLanguage)
        Log.d(
            TAG,
            "transliterate: src=$sourceLanguage target=$targetLanguage normalizedTarget=$normalizedTarget"
        )

        // For now, we primarily support Indian script to English
        // and English to Indian scripts
        val source = sourceLanguage.lowercase()
        val isEnglishLike =
            source == "en" || source == "english" || source == "unknown" || source.startsWith("mixed_")
        if (isEnglishLike) {
            return englishToIndianScript(text, normalizedTarget)
        }

        // Indian script to English (reverse transliteration)
        return indianScriptToEnglish(text, sourceLanguage)
    }

    /**
     * Transliterate from English to any Indian script
     * Uses dictionary lookup + rule-based approach
     */
    private fun englishToIndianScript(text: String, targetLang: String): String {
        val normalizedLang = normalizeLanguageCode(targetLang)

        // Split text into words and punctuation
        val tokens = tokenize(text)
        val result = StringBuilder()

        for (token in tokens) {
            when {
                // Preserve spaces and punctuation
                token.isBlank() || isPunctuation(token) -> {
                    result.append(token)
                }
                // If token already contains non-Latin script letters, leave it as-is.
                // This fixes mixed-language input like: "मैं going home".
                token.any { it.code > 127 && it.isLetter() } -> {
                    result.append(token)
                }
                // Dictionary lookup first (for common signboard words)
                IndianSignboardDictionary.hasWord(token) -> {
                    val translation =
                        IndianSignboardDictionary.getTransliteration(token, normalizedLang)
                    result.append(translation ?: applyTransliterationRules(token, normalizedLang))
                }
                // Apply rule-based transliteration
                else -> {
                    result.append(applyTransliterationRules(token, normalizedLang))
                }
            }
        }

        return result.toString()
    }

    /**
     * Apply rule-based transliteration with proper matra handling
     */
    private fun applyTransliterationRules(word: String, targetLang: String): String {
        // Only transliterate roman tokens. If the token contains non-ASCII letters,
        // keep it unchanged to support mixed-language text.
        if (word.any { it.code > 127 && it.isLetter() }) return word

        val lowerWord = word.lowercase()

        return when (targetLang) {
            "hi", "mr" -> transliterateToDevanagari(lowerWord) // Hindi/Marathi use same script
            "bn", "as" -> transliterateToBengali(lowerWord) // Bengali/Assamese use same script
            "ta" -> transliterateToTamil(lowerWord)
            "te" -> transliterateToTelugu(lowerWord)
            "gu" -> transliterateToGujarati(lowerWord)
            "kn" -> transliterateToKannada(lowerWord)
            "ml" -> transliterateToMalayalam(lowerWord)
            "pa" -> transliterateToPunjabi(lowerWord)
            "or" -> transliterateToOdia(lowerWord)
            else -> word
        }
    }

    private data class ScriptRules(
        val vowels: Map<String, String>,
        val matras: Map<String, String>,
        val consonants: Map<String, String>,
        val pureConsonants: Map<String, String>,
        val halant: String
    )

    private fun transliterateWithRules(text: String, rules: ScriptRules, maxLen: Int): String {
        val result = StringBuilder()
        var i = 0

        while (i < text.length) {
            var matched = false

            for (len in maxLen downTo 1) {
                if (i + len > text.length) continue
                val substr = text.substring(i, i + len)

                // Prefer explicit consonant clusters first.
                rules.consonants[substr]?.let {
                    result.append(it)
                    i += len
                    matched = true
                }
                if (matched) break

                // Consonant + vowel => consonant + matra
                for (consLen in (len - 1) downTo 1) {
                    val consKey = substr.substring(0, consLen)
                    val vowelKey = substr.substring(consLen)
                    val consonant = rules.pureConsonants[consKey]
                    val matra = rules.matras[vowelKey]
                    if (consonant != null && matra != null) {
                        result.append(consonant).append(matra)
                        i += len
                        matched = true
                        break
                    }
                }
                if (matched) break

                // Pure consonant at end/before non-vowel
                rules.pureConsonants[substr]?.let { consonant ->
                    val nextIndex = i + len
                    val nextChar = if (nextIndex < text.length) text[nextIndex] else null
                    val nextIsVowelStart = when (nextChar) {
                        null -> false
                        'a', 'e', 'i', 'o', 'u' -> true
                        else -> false
                    }
                    if (!nextIsVowelStart) {
                        result.append(consonant).append(rules.halant)
                        i += len
                        matched = true
                    }
                }
                if (matched) break

                // Independent vowel
                rules.vowels[substr]?.let {
                    result.append(it)
                    i += len
                    matched = true
                }
                if (matched) break
            }

            if (!matched) {
                result.append(text[i])
                i++
            }
        }

        return result.toString()
    }

    /**
     * Transliterate to Devanagari (Hindi/Marathi) with proper matra handling
     */
    private fun transliterateToDevanagari(text: String): String {
        val result = StringBuilder()
        var i = 0

        while (i < text.length) {
            // Try to match longest possible sequence
            var matched = false

            // Try consonant + vowel combinations (priority to longer matches)
            // Up to 5 to allow patterns like "ksh" + "aa" in a single token
            for (len in 5 downTo 1) {
                if (i + len <= text.length) {
                    val substr = text.substring(i, i + len)

                    // 1) Check for full consonant clusters like ksha/tra/jnya (longest match)
                    TransliterationRules.Hindi.consonants[substr]?.let {
                        result.append(it)
                        i += len
                        matched = true
                    }

                    if (matched) break

                    // Check for consonant + vowel (e.g., "kaa", "ki")
                    for (consLen in 4 downTo 1) {
                        if (consLen < len) {
                            val consKey = substr.substring(0, consLen)
                            val vowelKey = substr.substring(consLen)

                            val consonant = TransliterationRules.Hindi.pureConsonants[consKey]
                            val matra = TransliterationRules.Hindi.matras[vowelKey]

                            if (consonant != null && matra != null) {
                                result.append(consonant).append(matra)
                                i += len
                                matched = true
                                break
                            }
                        }
                    }

                    if (matched) break

                    // 2) End-of-word / before non-vowel: emit pure consonant + halant.
                    // Example: "sh" at end should become "श्".
                    TransliterationRules.Hindi.pureConsonants[substr]?.let { consonant ->
                        val nextIndex = i + len
                        val nextChar = if (nextIndex < text.length) text[nextIndex] else null
                        val nextIsVowelStart = when (nextChar) {
                            null -> false
                            'a', 'e', 'i', 'o', 'u' -> true
                            else -> false
                        }

                        if (!nextIsVowelStart) {
                            result.append(consonant).append(TransliterationRules.Hindi.halant)
                            i += len
                            matched = true
                        }
                    }

                    if (matched) break

                    // Check for independent vowel
                    TransliterationRules.Hindi.vowels[substr]?.let {
                        result.append(it)
                        i += len
                        matched = true
                    }

                    if (matched) break
                }
            }

            if (!matched) {
                // No match found, copy character as-is
                result.append(text[i])
                i++
            }
        }

        return result.toString()
    }

    /**
     * Transliterate to Bengali/Assamese with proper matra handling
     */
    private fun transliterateToBengali(text: String): String {
        val result = StringBuilder()
        var i = 0

        while (i < text.length) {
            var matched = false

            for (len in 4 downTo 1) {
                if (i + len <= text.length) {
                    val substr = text.substring(i, i + len)

                    // Try consonant + vowel combination
                    for (consLen in 3 downTo 1) {
                        if (consLen < len) {
                            val consKey = substr.substring(0, consLen)
                            val vowelKey = substr.substring(consLen)

                            val consonant = TransliterationRules.Bengali.pureConsonants[consKey]
                            val matra = TransliterationRules.Bengali.matras[vowelKey]

                            if (consonant != null && matra != null) {
                                result.append(consonant).append(matra)
                                i += len
                                matched = true
                                break
                            }
                        }
                    }

                    if (matched) break

                    TransliterationRules.Bengali.consonants[substr]?.let {
                        result.append(it)
                        i += len
                        matched = true
                    }

                    if (matched) break

                    TransliterationRules.Bengali.vowels[substr]?.let {
                        result.append(it)
                        i += len
                        matched = true
                    }

                    if (matched) break
                }
            }

            if (!matched) {
                result.append(text[i])
                i++
            }
        }

        return result.toString()
    }

    /**
     * Transliterate to Tamil with proper matra handling
     */
    private fun transliterateToTamil(text: String): String {
        val result = StringBuilder()
        var i = 0

        while (i < text.length) {
            var matched = false

            for (len in 4 downTo 1) {
                if (i + len <= text.length) {
                    val substr = text.substring(i, i + len)

                    // Try consonant + vowel combination
                    for (consLen in 3 downTo 1) {
                        if (consLen < len) {
                            val consKey = substr.substring(0, consLen)
                            val vowelKey = substr.substring(consLen)

                            val consonant = TransliterationRules.Tamil.pureConsonants[consKey]
                            val matra = TransliterationRules.Tamil.matras[vowelKey]

                            if (consonant != null && matra != null) {
                                result.append(consonant).append(matra)
                                i += len
                                matched = true
                                break
                            }
                        }
                    }

                    if (matched) break

                    TransliterationRules.Tamil.consonants[substr]?.let {
                        result.append(it)
                        i += len
                        matched = true
                    }

                    if (matched) break

                    TransliterationRules.Tamil.vowels[substr]?.let {
                        result.append(it)
                        i += len
                        matched = true
                    }

                    if (matched) break
                }
            }

            if (!matched) {
                result.append(text[i])
                i++
            }
        }

        return result.toString()
    }

    /**
     * Transliterate to Telugu with proper matra handling
     */
    private fun transliterateToTelugu(text: String): String {
        val result = StringBuilder()
        var i = 0

        while (i < text.length) {
            var matched = false

            for (len in 4 downTo 1) {
                if (i + len <= text.length) {
                    val substr = text.substring(i, i + len)

                    // Try consonant + vowel combination
                    for (consLen in 3 downTo 1) {
                        if (consLen < len) {
                            val consKey = substr.substring(0, consLen)
                            val vowelKey = substr.substring(consLen)

                            val consonant = TransliterationRules.Telugu.pureConsonants[consKey]
                            val matra = TransliterationRules.Telugu.matras[vowelKey]

                            if (consonant != null && matra != null) {
                                result.append(consonant).append(matra)
                                i += len
                                matched = true
                                break
                            }
                        }
                    }

                    if (matched) break

                    TransliterationRules.Telugu.consonants[substr]?.let {
                        result.append(it)
                        i += len
                        matched = true
                    }

                    if (matched) break

                    TransliterationRules.Telugu.vowels[substr]?.let {
                        result.append(it)
                        i += len
                        matched = true
                    }

                    if (matched) break
                }
            }

            if (!matched) {
                result.append(text[i])
                i++
            }
        }

        return result.toString()
    }

    private fun transliterateToGujarati(text: String): String = transliterateWithRules(
        text,
        ScriptRules(
            vowels = TransliterationRules.Gujarati.vowels,
            matras = TransliterationRules.Gujarati.matras,
            consonants = TransliterationRules.Gujarati.consonants,
            pureConsonants = TransliterationRules.Gujarati.pureConsonants,
            halant = TransliterationRules.Gujarati.halant
        ),
        maxLen = 5
    )

    private fun transliterateToKannada(text: String): String = transliterateWithRules(
        text,
        ScriptRules(
            vowels = TransliterationRules.Kannada.vowels,
            matras = TransliterationRules.Kannada.matras,
            consonants = TransliterationRules.Kannada.consonants,
            pureConsonants = TransliterationRules.Kannada.pureConsonants,
            halant = TransliterationRules.Kannada.halant
        ),
        maxLen = 4
    )

    private fun transliterateToMalayalam(text: String): String = transliterateWithRules(
        text,
        ScriptRules(
            vowels = TransliterationRules.Malayalam.vowels,
            matras = TransliterationRules.Malayalam.matras,
            consonants = TransliterationRules.Malayalam.consonants,
            pureConsonants = TransliterationRules.Malayalam.pureConsonants,
            halant = TransliterationRules.Malayalam.halant
        ),
        maxLen = 4
    )

    private fun transliterateToPunjabi(text: String): String = transliterateWithRules(
        text,
        ScriptRules(
            vowels = TransliterationRules.Punjabi.vowels,
            matras = TransliterationRules.Punjabi.matras,
            consonants = TransliterationRules.Punjabi.consonants,
            pureConsonants = TransliterationRules.Punjabi.pureConsonants,
            halant = TransliterationRules.Punjabi.halant
        ),
        maxLen = 4
    )

    private fun transliterateToOdia(text: String): String = transliterateWithRules(
        text,
        ScriptRules(
            vowels = TransliterationRules.Odia.vowels,
            matras = TransliterationRules.Odia.matras,
            consonants = TransliterationRules.Odia.consonants,
            pureConsonants = TransliterationRules.Odia.pureConsonants,
            halant = TransliterationRules.Odia.halant
        ),
        maxLen = 4
    )

    /**
     * Indian script to English (reverse transliteration)
     * Basic implementation - can be enhanced
     */
    private fun indianScriptToEnglish(text: String, sourceLang: String): String {
        // Simplified reverse transliteration
        // This would need character-to-roman mapping tables
        // For now, return as-is (can be implemented based on requirement)
        return text
    }

    /**
     * Tokenize text into words and punctuation
     */
    private fun tokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val currentToken = StringBuilder()
        val MODE_LETTERS = 1
        val MODE_DIGITS = 2
        val MODE_OTHER = 3
        var mode: Int? = null

        for (char in text) {
            val nextMode = when {
                char.isLetter() -> MODE_LETTERS
                char.isDigit() -> MODE_DIGITS
                else -> MODE_OTHER
            }

            if (nextMode == MODE_OTHER) {
                if (currentToken.isNotEmpty()) {
                    tokens.add(currentToken.toString())
                    currentToken.clear()
                    mode = null
                }
                tokens.add(char.toString())
                continue
            }

            if (mode != null && mode != nextMode) {
                if (currentToken.isNotEmpty()) {
                    tokens.add(currentToken.toString())
                    currentToken.clear()
                }
            }

            mode = nextMode
            currentToken.append(char)
        }

        if (currentToken.isNotEmpty()) {
            tokens.add(currentToken.toString())
        }

        return tokens
    }

    private fun isPunctuation(token: String): Boolean {
        return token.length == 1 && !token[0].isLetterOrDigit()
    }

    private fun normalizeLanguageCode(code: String): String {
        return when (code.lowercase()) {
            // Devanagari
            "english", "en", "eng", "latin" -> "en"
            "hindi", "hi", "hin", "devanagari" -> "hi"
            "marathi", "mr", "mar" -> "mr"
            "nepali", "ne", "nep" -> "ne"
            "sanskrit", "sa", "san" -> "sa"

            // Bengali-Assamese
            "bengali", "bn", "ben" -> "bn"
            "assamese", "as", "asm" -> "as"

            // Dravidian
            "tamil", "ta", "tam" -> "ta"
            "telugu", "te", "tel" -> "te"
            "kannada", "kn", "kan" -> "kn"
            "malayalam", "ml", "mal" -> "ml"

            // Others
            "gujarati", "gu", "guj" -> "gu"
            "punjabi", "pa", "pan", "gurmukhi" -> "pa"
            "odia", "or", "ori", "oriya" -> "or"
            "urdu", "ur", "urd" -> "ur"

            else -> code.lowercase()
        }
    }
}
