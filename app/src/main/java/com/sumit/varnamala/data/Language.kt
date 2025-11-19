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
                code = "hi",
                name = "Hindi",
                nativeName = "हिन्दी",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "pa",
                name = "Punjabi",
                nativeName = "ਪੰਜਾਬੀ",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "bn",
                name = "Bengali",
                nativeName = "বাংলা",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "te",
                name = "Telugu",
                nativeName = "తెలుగు",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "ta",
                name = "Tamil",
                nativeName = "தமிழ்",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "mr",
                name = "Marathi",
                nativeName = "मराठी",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "gu",
                name = "Gujarati",
                nativeName = "ગુજરાતી",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "kn",
                name = "Kannada",
                nativeName = "ಕನ್ನಡ",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "ml",
                name = "Malayalam",
                nativeName = "മലയാളം",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "or",
                name = "Odia",
                nativeName = "ଓଡ଼ିଆ",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "as",
                name = "Assamese",
                nativeName = "অসমীয়া",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "ur",
                name = "Urdu",
                nativeName = "اردو",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "sa",
                name = "Sanskrit",
                nativeName = "संस्कृतम्",
                flagEmoji = "🇮🇳"
            ),
            Language(
                code = "ne",
                name = "Nepali",
                nativeName = "नेपाली",
                flagEmoji = "🇳🇵"
            )
        )
    }

    fun getLanguageByCode(code: String): Language? {
        return getSupportedLanguages().find { it.code == code }
    }
}
