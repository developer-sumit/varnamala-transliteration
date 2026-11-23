package com.sumit.varnamala.transliteration

import com.sumit.varnamala.transliteration.Language.ASSAMESE
import com.sumit.varnamala.transliteration.Language.BENGALI
import com.sumit.varnamala.transliteration.Language.ENGLISH
import com.sumit.varnamala.transliteration.Language.GUJARATI
import com.sumit.varnamala.transliteration.Language.HINDI
import com.sumit.varnamala.transliteration.Language.KANNADA
import com.sumit.varnamala.transliteration.Language.MALAYALAM
import com.sumit.varnamala.transliteration.Language.ODIA
import com.sumit.varnamala.transliteration.Language.PUNJABI
import com.sumit.varnamala.transliteration.Language.SANSKRIT
import com.sumit.varnamala.transliteration.Language.TAMIL
import com.sumit.varnamala.transliteration.Language.TELUGU
import com.sumit.varnamala.transliteration.Language.URDU
import org.junit.Test
import org.junit.Assert.assertEquals

class TransliterationEngineTest {
    private val engine = TransliterationEngine()

    private fun check(expected: String, input: String, target: Language, source: Language? = null) =
        assertEquals("$input → ${target.code}", expected, engine.transliterate(input, target, source))

    private fun iso(input: String, source: Language? = null) =
        engine.transliterate(input, ENGLISH, source, RomanizationScheme.ISO_15919)

    // ---- Brahmic → Brahmic ----

    @Test
    fun devanagariToOtherIndicScripts() {
        check("নমস্তে", "नमस्ते", BENGALI)
        check("ਨਮਸਤੇ", "नमस्ते", PUNJABI) // Punjabi drops the virama in ordinary clusters
        check("નમસ્તે", "नमस्ते", GUJARATI)
        check("ନମସ୍ତେ", "नमस्ते", ODIA)
        check("నమస్తే", "नमस्ते", TELUGU)
        check("ನಮಸ್ತೇ", "नमस्ते", KANNADA)
        check("നമസ്തേ", "नमस्ते", MALAYALAM)
        check("நமஸ்தே", "नमस्ते", TAMIL)
    }

    @Test
    fun tamilSubstitutesMissingLetters() {
        check("பாரத்", "भारत", TAMIL) // bh → ப, final schwa becomes a pulli
        check("கம்லா", "कमला", TAMIL) // medial schwa deletion carried over
        check("ஹிந்தீ", "हिंदी", TAMIL) // anusvara → homorganic nasal, ந before த
    }

    @Test
    fun gurmukhiConventions() {
        check("ਪੱਕਾ", "पक्का", PUNJABI) // addak for gemination
        check("ਹਿੰਦੀ", "हिंदी", PUNJABI) // tippi after a short vowel
        check("ਸ਼ਾਮ", "शाम", PUNJABI) // sha written sa + nukta
        check("पक्का", "ਪੱਕਾ", HINDI)
        check("शाम", "ਸ਼ਾਮ", HINDI)
    }

    @Test
    fun bengaliAndAssameseLetters() {
        check("রাম", "राम", BENGALI)
        check("ৰাম", "राम", ASSAMESE)
        check("राम", "ৰাম", HINDI)
        check("জয়", "जय", BENGALI) // য় after a vowel
        check("জগৎ", "जगत्", BENGALI) // khanda ta
    }

    @Test
    fun malayalamChillu() {
        check("പാൽ", "paal", MALAYALAM)
        check("अवन्", "അവൻ", SANSKRIT)
    }

    @Test
    fun dravidianBackToHindiDropsFinalVirama() {
        check("राम", "ராம்", HINDI)
        check("तमिऴ", "தமிழ்", HINDI)
    }

    @Test
    fun nuktaLetters() {
        check("ज़िंदगी", "ज़िंदगी", HINDI)
        check("ਜ਼ਿੰਦਗੀ", "ज़िंदगी", PUNJABI)
        check("জিংদগী", "ज़िंदगी", BENGALI) // Bengali writes no nukta on z
    }

    // ---- Brahmic → Latin ----

    @Test
    fun readableRomanization() {
        check("namaste", "नमस्ते", ENGLISH)
        check("kamla", "कमला", ENGLISH)
        check("bharat", "भारत", ENGLISH)
        check("hindi", "हिंदी", ENGLISH)
        check("samajhna", "समझना", ENGLISH)
        check("mitra", "मित्र", ENGLISH)
        check("vanakkam", "வணக்கம்", ENGLISH)
        check("tamizh", "தமிழ்", ENGLISH)
        check("jadi", "যদি", ENGLISH) // word-initial Bengali য is j
        check("namaskaram", "നമസ്കാരം", ENGLISH)
    }

    @Test
    fun sanskritKeepsInherentVowels() {
        check("ramah", "रामः", ENGLISH, SANSKRIT)
        check("ram", "राम", ENGLISH, HINDI)
    }

    @Test
    fun isoRomanizationIsLossless() {
        assertEquals("namastē", iso("नमस्ते"))
        assertEquals("rāṣṭra", iso("राष्ट्र"))
        assertEquals("kr̥ṣṇa", iso("कृष्ण"))
        assertEquals("vaṇakkam", iso("வணக்கம்"))
        for (word in listOf("नमस्ते", "राष्ट्र", "कृष्ण", "ज़िंदगी", "संस्कृत", "दुःख")) {
            assertEquals("round trip $word", word, engine.transliterate(iso(word), SANSKRIT))
        }
    }

    // ---- Latin → native ----

    @Test
    fun casualRomanizedInput() {
        check("नमस्ते", "namaste", HINDI)
        check("कमला", "kamla", HINDI) // final a is long; silent medial vowel restored
        check("करना", "karna", HINDI)
        check("धर्म", "dharm", HINDI) // other clusters stay conjuncts
        check("राजा", "raaja", HINDI)
        check("पानी", "paani", HINDI) // final i is long
        check("हिंदी", "hindi", HINDI) // n before a stop is an anusvara
        check("अम्मा", "amma", HINDI) // doubled nasal stays a conjunct
        check("क्षमा", "kshama", HINDI)
        check("ज़िंदगी", "zindagi", HINDI)
        check("फ़ोन", "fon", HINDI)
        check("Namaste", "Namaste", ENGLISH)
    }

    @Test
    fun casualInputForDravidianTargets() {
        check("மரம்", "maram", TAMIL) // final virama
        check("அம்மா", "amma", TAMIL) // Tamil final a is long
        check("நான்", "naan", TAMIL) // ந word-initially, ன elsewhere
        check("తెలుగు", "telugu", TELUGU)
        check("ಬಸ್", "bas", KANNADA)
        check("മരം", "maram", MALAYALAM)
    }

    @Test
    fun englishSpellingPatterns() {
        check("स्तेशन", "steshan", HINDI) // casual t is dental; English loanwords need the dictionary
        check("नेशनल", "neshanal", HINDI)
        check("सिती", "city", HINDI) // soft c, final y
        check("क्वीन", "queen", HINDI)
    }

    // ---- Urdu ----

    @Test
    fun urduToAndFromDevanagari() {
        check("دوست", "दोस्त", URDU)
        check("کتاب", "किताब", URDU)
        check("ہندی", "हिंदी", URDU)
        check("दोस्त", "دوست", HINDI)
        check("यह", "یہ", HINDI)
        check("नहीं", "نہیں", HINDI)
        check("भी", "بھی", HINDI)
    }

    // ---- Mixed text and passthrough ----

    @Test
    fun mixedScriptsConvertIndependently() {
        check("मैं घर जा रहा", "मैं ghar ja raha", HINDI)
        check("main going home", "मैं going home", ENGLISH)
        check("नमस्ते, 2024!", "नमस्ते, 2024!", HINDI)
        check("namaste. ok", "नमस्ते। ok", ENGLISH)
        check("১২৩", "१२३", BENGALI)
    }

    @Test
    fun codeOverloadResolvesAliases() {
        assertEquals("নমস্তে", engine.transliterate("नमस्ते", "ben"))
        assertEquals("नमस्ते", engine.transliterate("नमस्ते", "xx"))
        assertEquals("namaste", engine.transliterate("नमस्ते", "en", "mixed_hi_en"))
    }

    @Test
    fun dictionaryWinsForLatinWords() {
        val dictionary = SignboardDictionary.fromJson(
            """{"station": {"hi": "स्टेशन", "ta": "ஸ்டேஷன்", "ml": "bad\u001b"}}"""
        )
        val withDictionary = TransliterationEngine(dictionary)
        assertEquals("स्टेशन", withDictionary.transliterate("Station", HINDI))
        assertEquals("स्टेशन", withDictionary.transliterate("station", Language.MARATHI)) // same script
        assertEquals("سٹیشن", withDictionary.transliterate("station", URDU)) // via the Hindi entry
        assertEquals(null, dictionary.lookup("station", MALAYALAM)) // corrupted entry skipped
    }

    // ---- Detection ----

    @Test
    fun detectsLanguagesByScript() {
        assertEquals("hi", LanguageDetector.detectLanguage("नमस्ते दुनिया"))
        assertEquals("kn", LanguageDetector.detectLanguage("ನಮಸ್ಕಾರ"))
        assertEquals("ml", LanguageDetector.detectLanguage("നമസ്കാരം"))
        assertEquals("or", LanguageDetector.detectLanguage("ନମସ୍କାର"))
        assertEquals("ur", LanguageDetector.detectLanguage("السلام علیکم"))
        assertEquals("as", LanguageDetector.detectLanguage("অসমীয়া ৰাজ্য"))
        assertEquals("mixed_hi_en", LanguageDetector.detectLanguage("नमस्ते दुनिया hello"))
        assertEquals("unknown", LanguageDetector.detectLanguage("123 !!"))
    }
}
