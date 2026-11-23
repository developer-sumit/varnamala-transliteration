package com.sumit.varnamala.utils

import com.sumit.varnamala.transliteration.RomanizationScheme
import com.sumit.varnamala.transliteration.SignboardDictionary
import com.sumit.varnamala.transliteration.TransliterationEngine

/**
 * App-wide access to the shared, platform-independent engine in the `:transliteration` module.
 * [VarnamalaApp][com.sumit.varnamala.VarnamalaApp] installs the signboard dictionary at startup.
 */
object Transliterator {
    @Volatile
    private var engine = TransliterationEngine()

    fun install(dictionary: SignboardDictionary) {
        engine = TransliterationEngine(dictionary)
    }

    /**
     * Converts [text] into the script of [targetLanguage] (a code such as "hi", "ta", or "en").
     * The input script is detected per word, so [sourceLanguage] may be null, "unknown", or a
     * `mixed_*` code from [com.sumit.varnamala.transliteration.LanguageDetector].
     */
    fun transliterate(
        text: String,
        sourceLanguage: String?,
        targetLanguage: String = "en",
        scheme: RomanizationScheme = RomanizationScheme.READABLE,
    ): String = engine.transliterate(text, targetLanguage, sourceLanguage, scheme)

    /**
     * Transliterates [text] using the Aksharamukha engine across Indic scripts and Romanization schemes.
     */
    fun aksharamukha(
        text: String,
        targetScript: String,
        sourceScript: String = "autodetect",
        preOptions: List<String> = emptyList(),
        postOptions: List<String> = emptyList(),
    ): String = engine.aksharamukha(text, targetScript, sourceScript, preOptions, postOptions)
}
