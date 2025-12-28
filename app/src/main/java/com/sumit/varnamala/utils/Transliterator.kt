package com.sumit.varnamala.utils

import com.sumit.varnamala.transliteration.IndianSignboardDictionary
import com.sumit.varnamala.transliteration.TransliterationRules

object Transliterator {
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
        if (text.isBlank() || sourceLanguage == null || sourceLanguage == targetLanguage) {
            return text
        }

        // For now, we primarily support Indian script to English
        // and English to Indian scripts
        if (sourceLanguage == "en" || sourceLanguage == "english") {
            return englishToIndianScript(text, targetLanguage)
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
                // Dictionary lookup first (for common signboard words)
                IndianSignboardDictionary.hasWord(token) -> {
                    val translation = IndianSignboardDictionary.getTransliteration(token, normalizedLang)
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

    // Simplified versions for remaining languages (can be expanded similarly)
    private fun transliterateToGujarati(text: String): String = transliterateToDevanagari(text) // Similar to Devanagari
    private fun transliterateToKannada(text: String): String = transliterateToTelugu(text) // Similar structure
    private fun transliterateToMalayalam(text: String): String = transliterateToTelugu(text) // Similar structure
    private fun transliterateToPunjabi(text: String): String = transliterateToDevanagari(text) // Similar to Devanagari
    private fun transliterateToOdia(text: String): String = transliterateToBengali(text) // Similar to Bengali

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

        for (char in text) {
            when {
                char.isLetterOrDigit() -> currentToken.append(char)
                else -> {
                    if (currentToken.isNotEmpty()) {
                        tokens.add(currentToken.toString())
                        currentToken.clear()
                    }
                    tokens.add(char.toString())
                }
            }
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
            "hindi", "hi", "devanagari" -> "hi"
            "bengali", "bn" -> "bn"
            "tamil", "ta" -> "ta"
            "telugu", "te" -> "te"
            "marathi", "mr" -> "mr"
            "gujarati", "gu" -> "gu"
            "kannada", "kn" -> "kn"
            "malayalam", "ml" -> "ml"
            "punjabi", "pa", "gurmukhi" -> "pa"
            "odia", "or", "oriya" -> "or"
            "assamese", "as" -> "as"
            else -> code
        }
    }
}
