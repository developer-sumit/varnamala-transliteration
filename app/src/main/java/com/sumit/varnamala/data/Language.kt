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
                code = "te",
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
