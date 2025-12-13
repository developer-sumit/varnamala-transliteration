package com.sumit.varnamala.data

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagEmoji: String,
    val isSelected: Boolean = false
)

object AppLanguageRepository {
    fun getSupportedLanguages(): List<Language> {
        return listOf(
            Language(
                code = "en",
                name = "English",
                nativeName = "English",
                flagEmoji = "🇺🇸"
            ),
            Language(
                code = "asm",
                name = "Assamese",
                nativeName = "অসমীয়া",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "ben",
                name = "Bengali",
                nativeName = "বাংলা",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "guj",
                name = "Gujarati",
                nativeName = "ગુજરાતી",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "hi",
                name = "Hindi",
                nativeName = "हिन्दी",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "kan",
                name = "Kannada",
                nativeName = "ಕನ್ನಡ",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "mal",
                name = "Malayalam",
                nativeName = "മലയാളം",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "mar",
                name = "Marathi",
                nativeName = "मराठी",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "ori",
                name = "Odia",
                nativeName = "ଓଡ଼ିଆ",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "pan",
                name = "Punjabi",
                nativeName = "ਪੰਜਾਬੀ",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "tam",
                name = "Tamil",
                nativeName = "தமிழ்",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "tel",
                name = "Telugu",
                nativeName = "తెలుగు",
                flagEmoji = "🇮🇳"
            )
        )
    }

    fun getLanguageByCode(code: String): Language? {
        return getSupportedLanguages().find { it.code == code }
    }
}
