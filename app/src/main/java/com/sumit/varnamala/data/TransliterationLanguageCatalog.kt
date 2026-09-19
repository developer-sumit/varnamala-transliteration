package com.sumit.varnamala.data

object TransliterationLanguageCatalog {
    val supportedLanguages: List<Language> = listOf(
        Language("eng", "English", "English", "", localeCode = "en"),
        Language("asm", "Assamese", "অসমীয়া", "", localeCode = "as"),
        Language("ben", "Bengali", "বাংলা", "", localeCode = "bn"),
        Language("guj", "Gujarati", "ગુજરાતી", "", localeCode = "gu"),
        Language("hi", "Hindi", "हिन्दी", "", localeCode = "hi"),
        Language("kan", "Kannada", "ಕನ್ನಡ", "", localeCode = "kn"),
        Language("mal", "Malayalam", "മലയാളം", "", localeCode = "ml"),
        Language("mar", "Marathi", "मराठी", "", localeCode = "mr"),
        Language("nep", "Nepali", "नेपाली", "", localeCode = "ne"),
        Language("ori", "Odia", "ଓଡ଼ିଆ", "", localeCode = "or"),
        Language("pan", "Punjabi", "ਪੰਜਾਬੀ", "", localeCode = "pa"),
        Language("san", "Sanskrit", "संस्कृतम्", "", localeCode = "sa"),
        Language("tam", "Tamil", "தமிழ்", "", localeCode = "ta"),
        Language("tel", "Telugu", "తెలుగు", "", localeCode = "te"),
        Language("urd", "Urdu", "اردو", "", localeCode = "ur")
    )

    fun find(code: String?): Language? {
        if (code.isNullOrBlank()) return null
        return supportedLanguages.firstOrNull {
            it.code == code || it.localeCode == code
        }
    }

    fun defaultTarget(): Language {
        return find("hi") ?: supportedLanguages.first()
    }
}
