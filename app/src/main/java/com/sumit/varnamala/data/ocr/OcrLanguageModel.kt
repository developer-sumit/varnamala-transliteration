package com.sumit.varnamala.data.ocr

private const val TESSDATA_BEST_BASE_URL =
    "https://raw.githubusercontent.com/tesseract-ocr/tessdata_best/main/"

enum class OcrModelCategory {
    LANGUAGE,
    SPECIAL
}

data class OcrLanguageModel(
    val code: String,
    val displayName: String,
    val nativeName: String = displayName,
    val category: OcrModelCategory = OcrModelCategory.LANGUAGE,
    val trainedDataPath: String = "$code.traineddata",
    val downloadUrl: String = TESSDATA_BEST_BASE_URL + trainedDataPath,
) {
    val fileName: String
        get() = trainedDataPath.substringAfterLast('/')

    val isSelectableSource: Boolean
        get() = category == OcrModelCategory.LANGUAGE
}

object OcrLanguageCatalog {
    const val DEFAULT_SOURCE_CODE = "eng"

    private val displayNames = mapOf(
        "afr" to "Afrikaans",
        "amh" to "Amharic",
        "ara" to "Arabic",
        "asm" to "Assamese",
        "aze" to "Azerbaijani",
        "aze_cyrl" to "Azerbaijani Cyrillic",
        "bel" to "Belarusian",
        "ben" to "Bengali",
        "bod" to "Tibetan",
        "bos" to "Bosnian",
        "bre" to "Breton",
        "bul" to "Bulgarian",
        "cat" to "Catalan",
        "ceb" to "Cebuano",
        "ces" to "Czech",
        "chi_sim" to "Chinese Simplified",
        "chi_sim_vert" to "Chinese Simplified Vertical",
        "chi_tra" to "Chinese Traditional",
        "chi_tra_vert" to "Chinese Traditional Vertical",
        "chr" to "Cherokee",
        "cos" to "Corsican",
        "cym" to "Welsh",
        "dan" to "Danish",
        "deu" to "German",
        "deu_latf" to "German Fraktur",
        "div" to "Dhivehi",
        "dzo" to "Dzongkha",
        "ell" to "Greek",
        "eng" to "English",
        "enm" to "Middle English",
        "epo" to "Esperanto",
        "est" to "Estonian",
        "eus" to "Basque",
        "fao" to "Faroese",
        "fas" to "Persian",
        "fil" to "Filipino",
        "fin" to "Finnish",
        "fra" to "French",
        "frm" to "Middle French",
        "fry" to "Western Frisian",
        "gla" to "Scottish Gaelic",
        "gle" to "Irish",
        "glg" to "Galician",
        "grc" to "Ancient Greek",
        "guj" to "Gujarati",
        "hat" to "Haitian Creole",
        "heb" to "Hebrew",
        "hin" to "Hindi",
        "hrv" to "Croatian",
        "hun" to "Hungarian",
        "hye" to "Armenian",
        "iku" to "Inuktitut",
        "ind" to "Indonesian",
        "isl" to "Icelandic",
        "ita" to "Italian",
        "ita_old" to "Old Italian",
        "jav" to "Javanese",
        "jpn" to "Japanese",
        "jpn_vert" to "Japanese Vertical",
        "kan" to "Kannada",
        "kat" to "Georgian",
        "kat_old" to "Old Georgian",
        "kaz" to "Kazakh",
        "khm" to "Khmer",
        "kir" to "Kyrgyz",
        "kmr" to "Kurmanji Kurdish",
        "kor" to "Korean",
        "kor_vert" to "Korean Vertical",
        "lao" to "Lao",
        "lat" to "Latin",
        "lav" to "Latvian",
        "lit" to "Lithuanian",
        "ltz" to "Luxembourgish",
        "mal" to "Malayalam",
        "mar" to "Marathi",
        "mkd" to "Macedonian",
        "mlt" to "Maltese",
        "mon" to "Mongolian",
        "mri" to "Maori",
        "msa" to "Malay",
        "mya" to "Myanmar",
        "nep" to "Nepali",
        "nld" to "Dutch",
        "nor" to "Norwegian",
        "oci" to "Occitan",
        "ori" to "Odia",
        "osd" to "Orientation and Script Detection",
        "pan" to "Punjabi",
        "pol" to "Polish",
        "por" to "Portuguese",
        "pus" to "Pashto",
        "que" to "Quechua",
        "ron" to "Romanian",
        "rus" to "Russian",
        "san" to "Sanskrit",
        "sin" to "Sinhala",
        "slk" to "Slovak",
        "slv" to "Slovenian",
        "snd" to "Sindhi",
        "spa" to "Spanish",
        "spa_old" to "Old Spanish",
        "sqi" to "Albanian",
        "srp" to "Serbian",
        "srp_latn" to "Serbian Latin",
        "sun" to "Sundanese",
        "swa" to "Swahili",
        "swe" to "Swedish",
        "syr" to "Syriac",
        "tam" to "Tamil",
        "tat" to "Tatar",
        "tel" to "Telugu",
        "tgk" to "Tajik",
        "tha" to "Thai",
        "tir" to "Tigrinya",
        "ton" to "Tongan",
        "tur" to "Turkish",
        "uig" to "Uyghur",
        "ukr" to "Ukrainian",
        "urd" to "Urdu",
        "uzb" to "Uzbek",
        "uzb_cyrl" to "Uzbek Cyrillic",
        "vie" to "Vietnamese",
        "yid" to "Yiddish",
        "yor" to "Yoruba",
        "equ" to "Math and Equation"
    )

    private val nativeNames = mapOf(
        "asm" to "অসমীয়া",
        "ben" to "বাংলা",
        "eng" to "English",
        "guj" to "ગુજરાતી",
        "hin" to "हिन्दी",
        "kan" to "ಕನ್ನಡ",
        "mal" to "മലയാളം",
        "mar" to "मराठी",
        "nep" to "नेपाली",
        "ori" to "ଓଡ଼ିଆ",
        "pan" to "ਪੰਜਾਬੀ",
        "san" to "संस्कृतम्",
        "tam" to "தமிழ்",
        "tel" to "తెలుగు",
        "urd" to "اردو"
    )

    private val rootCodes = listOf(
        "afr", "amh", "ara", "asm", "aze", "aze_cyrl", "bel", "ben", "bod", "bos",
        "bre", "bul", "cat", "ceb", "ces", "chi_sim", "chi_sim_vert", "chi_tra",
        "chi_tra_vert", "chr", "cos", "cym", "dan", "deu", "deu_latf", "div",
        "dzo", "ell", "eng", "enm", "epo", "equ", "est", "eus", "fao", "fas",
        "fil", "fin", "fra", "frm", "fry", "gla", "gle", "glg", "grc", "guj",
        "hat", "heb", "hin", "hrv", "hun", "hye", "iku", "ind", "isl", "ita",
        "ita_old", "jav", "jpn", "jpn_vert", "kan", "kat", "kat_old", "kaz",
        "khm", "kir", "kmr", "kor", "kor_vert", "lao", "lat", "lav", "lit",
        "ltz", "mal", "mar", "mkd", "mlt", "mon", "mri", "msa", "mya", "nep",
        "nld", "nor", "oci", "ori", "osd", "pan", "pol", "por", "pus", "que",
        "ron", "rus", "san", "sin", "slk", "slv", "snd", "spa", "spa_old",
        "sqi", "srp", "srp_latn", "sun", "swa", "swe", "syr", "tam", "tat",
        "tel", "tgk", "tha", "tir", "ton", "tur", "uig", "ukr", "urd", "uzb",
        "uzb_cyrl", "vie", "yid", "yor"
    )

    val all: List<OcrLanguageModel> = rootCodes.map { code ->
        OcrLanguageModel(
            code = code,
            displayName = displayNames[code] ?: code.uppercase(),
            nativeName = nativeNames[code] ?: displayNames[code] ?: code.uppercase(),
            category = if (code == "osd" || code == "equ") {
                OcrModelCategory.SPECIAL
            } else {
                OcrModelCategory.LANGUAGE
            }
        )
    }

    val selectableSources: List<OcrLanguageModel> = all.filter { it.isSelectableSource }

    fun find(code: String?): OcrLanguageModel? {
        val normalized = normalizeCode(code ?: return null)
        return all.firstOrNull { it.code == normalized }
    }

    fun sourceOrDefault(code: String?): OcrLanguageModel {
        return find(code)?.takeIf { it.isSelectableSource }
            ?: find(DEFAULT_SOURCE_CODE)
            ?: selectableSources.first()
    }

    fun normalizeCode(code: String): String {
        return when (code.lowercase()) {
            "en", "english" -> "eng"
            "hi", "hindi" -> "hin"
            "bn", "bengali" -> "ben"
            "as", "assamese" -> "asm"
            "gu", "gujarati" -> "guj"
            "kn", "kannada" -> "kan"
            "ml", "malayalam" -> "mal"
            "mr", "marathi" -> "mar"
            "or", "odia", "oriya" -> "ori"
            "pa", "punjabi", "gurmukhi" -> "pan"
            "sa", "sanskrit" -> "san"
            "ta", "tamil" -> "tam"
            "te", "telugu" -> "tel"
            "ur", "urdu" -> "urd"
            else -> code.lowercase()
        }
    }
}
