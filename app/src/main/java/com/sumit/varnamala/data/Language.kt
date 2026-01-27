package com.sumit.varnamala.data

/**
 * Language data class that handles both OCR language detection and app UI localization.
 *
 * Important: This class uses TWO different language code systems:
 * 1. `code`: 3-letter ISO 639-2 codes (e.g., "ben", "guj", "mar") used by ML Kit for OCR
 * 2. `localeCode`: 2-letter ISO 639-1 codes (e.g., "bn", "gu", "mr") used by Android for resource folders
 *
 * When selecting app UI language, use `localeCode` to match Android resource folders (values-bn, values-gu, etc.)
 * When performing OCR detection, use `code` to match ML Kit language models.
 */
data class Language(
    val code: String, // OCR/ML Kit language code (3-letter ISO 639-2)
    val name: String,
    val nativeName: String,
    val flagEmoji: String,
    val isSelected: Boolean = false,
    val localeCode: String = code // Android locale code (2-letter ISO 639-1)
)

object AppLanguageRepository {
    fun getSupportedLanguages(): List<Language> {
        return listOf(
            Language(
                code = "en",
                name = "English",
                nativeName = "English",
                flagEmoji = "🇺🇸",
                localeCode = "en"
            ),
            Language(
                code = "asm",
                name = "Assamese",
                nativeName = "অসমীয়া",
                flagEmoji = "🇮🇳",
                localeCode = "as"
            ),
            Language(
                code = "ben",
                name = "Bengali",
                nativeName = "বাংলা",
                flagEmoji = "🇮🇳",
                localeCode = "bn"
            ),
            Language(
                code = "guj",
                name = "Gujarati",
                nativeName = "ગુજરાતી",
                flagEmoji = "🇮🇳",
                localeCode = "gu"
            ),
            Language(
                code = "hi",
                name = "Hindi",
                nativeName = "हिन्दी",
                flagEmoji = "🇮🇳",
                localeCode = "hi"
            ),
            Language(
                code = "kan",
                name = "Kannada",
                nativeName = "ಕನ್ನಡ",
                flagEmoji = "🇮🇳",
                localeCode = "kn"
            ),
            Language(
                code = "mal",
                name = "Malayalam",
                nativeName = "മലയാളം",
                flagEmoji = "🇮🇳",
                localeCode = "ml"
            ),
            Language(
                code = "mar",
                name = "Marathi",
                nativeName = "मराठी",
                flagEmoji = "🇮🇳",
                localeCode = "mr"
            ),
            Language(
                code = "ori",
                name = "Odia",
                nativeName = "ଓଡ଼ିଆ",
                flagEmoji = "🇮🇳",
                localeCode = "or"
            ),
            Language(
                code = "pan",
                name = "Punjabi",
                nativeName = "ਪੰਜਾਬੀ",
                flagEmoji = "🇮🇳",
                localeCode = "pa"
            ),
            Language(
                code = "tam",
                name = "Tamil",
                nativeName = "தமிழ்",
                flagEmoji = "🇮🇳",
                localeCode = "ta"
            ),
            Language(
                code = "tel",
                name = "Telugu",
                nativeName = "తెలుగు",
                flagEmoji = "🇮🇳",
                localeCode = "te"
            )
        )
    }

    fun getLanguageByCode(code: String): Language? {
        return getSupportedLanguages().find { it.code == code }
    }
}
