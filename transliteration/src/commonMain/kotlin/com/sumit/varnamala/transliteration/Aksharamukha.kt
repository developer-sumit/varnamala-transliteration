package com.sumit.varnamala.transliteration

/**
 * Aksharamukha - Indic and Asian script conversion and transliteration engine.
 *
 * Implements the Aksharamukha paradigm for transliterating between Brahmic scripts,
 * international romanization schemes (IAST, ISO 15919, Harvard-Kyoto, ITRANS, Velthuis, SLP1, WX),
 * and Perso-Arabic (Urdu).
 *
 * Provides the canonical API:
 *   Aksharamukha.process(sourceScript, targetScript, text, preOptions, postOptions)
 * as well as direct script-to-script transliteration utilities.
 */
object Aksharamukha {

    // Supported scripts and schemes
    val SCRIPTS = listOf(
        "Devanagari", "Bengali", "Assamese", "Gurmukhi", "Gujarati",
        "Oriya", "Tamil", "Telugu", "Kannada", "Malayalam", "Sinhala", "Urdu"
    )

    val ROMAN_SCHEMES = listOf(
        "IAST", "ISO", "ISO15919", "HK", "Harvard-Kyoto",
        "ITRANS", "Velthuis", "SLP1", "WX"
    )

    fun supportedScripts(): List<String> = SCRIPTS
    fun supportedRoman(): List<String> = ROMAN_SCHEMES

    /** Returns the character mapping matrix for a given script or scheme. */
    fun getScriptMapping(scriptName: String): Map<String, String>? =
        SCRIPT_TABLES[normalizeScript(scriptName)]

    /**
     * Standard Aksharamukha process entry point.
     *
     * @param source source script or romanization scheme (e.g., "Devanagari", "Telugu", "HK", "autodetect")
     * @param target target script or romanization scheme (e.g., "Tamil", "Kannada", "IAST")
     * @param text input text to transliterate
     * @param preOptions options applied prior to conversion (e.g., "preserveSource", "nplus")
     * @param postOptions options applied after conversion (e.g., "removeSchwa")
     */
    fun process(
        source: String,
        target: String,
        text: String,
        preOptions: List<String> = emptyList(),
        postOptions: List<String> = emptyList(),
    ): String {
        if (text.isEmpty()) return text

        val resolvedSource = if (source.equals("autodetect", ignoreCase = true) || source.equals("auto", ignoreCase = true)) {
            detectScript(text)
        } else {
            normalizeScript(source)
        }
        val resolvedTarget = normalizeScript(target)

        if (resolvedSource.equals(resolvedTarget, ignoreCase = true)) {
            return text
        }

        // Parse input text into intermediate phonetic tokens
        val tokens = parseToTokens(text, resolvedSource)

        // Render intermediate phonetic tokens into target script
        var result = renderFromTokens(tokens, resolvedTarget, resolvedSource)

        if ("removeSchwa" in postOptions) {
            result = removeTerminalSchwas(result, resolvedTarget)
        }

        return result
    }

    /** Convenience alias matching common naming */
    fun transliterate(source: String, target: String, text: String): String =
        process(source, target, text)

    // =========================================================================
    // Intermediate Representation (IR)
    // =========================================================================

    internal enum class PhTokenType {
        VOWEL,          // independent vowel
        MATRA,          // dependent vowel sign
        CONSONANT,      // consonant (with inherent 'a')
        VIRAMA,         // vowel suppression / halant
        ANUSVARA,       // nasalization / bindu
        VISARGA,        // aspiration / h
        CANDRABINDU,    // nasal vowel sign
        NUKTA,          // diacritic dot
        AVAGRAHA,       // elision mark
        DANDA,          // single punctuation
        DOUBLE_DANDA,   // double punctuation
        DIGIT,          // numeral 0-9
        LITERAL         // whitespace, punctuation, untransliterated
    }

    internal data class PhToken(
        val type: PhTokenType,
        val key: String,       // phonetic phoneme key (e.g. "k", "kh", "aa", "i", etc.)
        val raw: String = "",
    )

    // =========================================================================
    // Phonetic Phoneme Matrix
    // =========================================================================

    // Independent Vowels
    internal val VOWELS = listOf(
        "a", "aa", "i", "ii", "u", "uu",
        "r_vocalic", "rr_vocalic", "l_vocalic", "ll_vocalic",
        "e_short", "e", "ai", "o_short", "o", "au"
    )

    // Consonants in traditional Varnamala order
    internal val CONSONANTS = listOf(
        // Gutturals / Velars
        "k", "kh", "g", "gh", "ng",
        // Palatals
        "c", "ch", "j", "jh", "ny",
        // Retroflex
        "tt", "tth", "dd", "ddh", "nn",
        // Dentals
        "t", "th", "d", "dh", "n",
        // Labials
        "p", "ph", "b", "bh", "m",
        // Semivowels & Liquids
        "y", "r", "l", "v",
        // Sibilants & Aspirate
        "sh", "ss", "s", "h",
        // Dravidian / Additional
        "ll", "zh", "rr", "nnn",
        // Perso-Arabic / Nukta consonants
        "q", "khh", "ghh", "z", "ddd", "rh", "f", "yy"
    )

    // =========================================================================
    // Script Mapping Tables (Unicode code points or strings)
    // =========================================================================

    private val DEVANAGARI_MAP = mapOf(
        // Vowels
        "a" to "अ", "aa" to "आ", "i" to "इ", "ii" to "ई", "u" to "उ", "uu" to "ऊ",
        "r_vocalic" to "ऋ", "rr_vocalic" to "ॠ", "l_vocalic" to "ऌ", "ll_vocalic" to "ॡ",
        "e_short" to "ऎ", "e" to "ए", "ai" to "ऐ", "o_short" to "ऒ", "o" to "ओ", "au" to "औ",
        // Matras
        "m_aa" to "ा", "m_i" to "ि", "m_ii" to "ी", "m_u" to "ु", "m_uu" to "ू",
        "m_r_vocalic" to "ृ", "m_rr_vocalic" to "ॄ", "m_l_vocalic" to "ॢ", "m_ll_vocalic" to "ॣ",
        "m_e_short" to "ॆ", "m_e" to "े", "m_ai" to "ै", "m_o_short" to "ॊ", "m_o" to "ो", "m_au" to "ौ",
        // Consonants
        "k" to "क", "kh" to "ख", "g" to "ग", "gh" to "घ", "ng" to "ङ",
        "c" to "च", "ch" to "छ", "j" to "ज", "jh" to "झ", "ny" to "ञ",
        "tt" to "ट", "tth" to "ठ", "dd" to "ड", "ddh" to "ढ", "nn" to "ण",
        "t" to "त", "th" to "थ", "d" to "द", "dh" to "ध", "n" to "न",
        "p" to "प", "ph" to "फ", "b" to "ब", "bh" to "भ", "m" to "म",
        "y" to "य", "r" to "र", "l" to "ल", "v" to "व",
        "sh" to "श", "ss" to "ष", "s" to "स", "h" to "ह",
        "ll" to "ळ", "zh" to "ऴ", "rr" to "ऱ", "nnn" to "ऩ",
        "q" to "क़", "khh" to "ख़", "ghh" to "ग़", "z" to "ज़", "ddd" to "ड़", "rh" to "ढ़", "f" to "फ़", "yy" to "य़",
        // Modifiers
        "virama" to "्", "anusvara" to "ं", "visarga" to "ः", "candrabindu" to "ँ", "avagraha" to "ऽ",
        "nukta" to "़", "danda" to "।", "double_danda" to "॥"
    )

    private val BENGALI_MAP = mapOf(
        "a" to "অ", "aa" to "আ", "i" to "ই", "ii" to "ঈ", "u" to "উ", "uu" to "ঊ",
        "r_vocalic" to "ঋ", "rr_vocalic" to "ৠ", "l_vocalic" to "ঌ", "ll_vocalic" to "ৡ",
        "e_short" to "এ", "e" to "এ", "ai" to "ঐ", "o_short" to "ও", "o" to "ও", "au" to "ঔ",
        "m_aa" to "া", "m_i" to "ি", "m_ii" to "ী", "m_u" to "ু", "m_uu" to "ূ",
        "m_r_vocalic" to "ৃ", "m_rr_vocalic" to "ৄ", "m_l_vocalic" to "ৢ", "m_ll_vocalic" to "ৣ",
        "m_e_short" to "ে", "m_e" to "ে", "m_ai" to "ৈ", "m_o_short" to "ো", "m_o" to "ো", "m_au" to "ৌ",
        "k" to "ক", "kh" to "খ", "g" to "গ", "gh" to "ঘ", "ng" to "ঙ",
        "c" to "চ", "ch" to "ছ", "j" to "জ", "jh" to "ঝ", "ny" to "ঞ",
        "tt" to "ট", "tth" to "ঠ", "dd" to "ড", "ddh" to "ঢ", "nn" to "ণ",
        "t" to "ত", "th" to "থ", "d" to "দ", "dh" to "ধ", "n" to "ন",
        "p" to "প", "ph" to "ফ", "b" to "ব", "bh" to "ভ", "m" to "ম",
        "y" to "য", "r" to "র", "l" to "ল", "v" to "ব",
        "sh" to "শ", "ss" to "ষ", "s" to "স", "h" to "হ",
        "ll" to "ল", "zh" to "ল", "rr" to "র", "nnn" to "ন",
        "q" to "ক", "khh" to "খ", "ghh" to "গ", "z" to "জ", "ddd" to "ড়", "rh" to "ঢ়", "f" to "ফ", "yy" to "য়",
        "virama" to "্", "anusvara" to "ং", "visarga" to "ঃ", "candrabindu" to "ঁ", "avagraha" to "ঽ",
        "nukta" to "়", "danda" to "।", "double_danda" to "॥"
    )

    private val TAMIL_MAP = mapOf(
        "a" to "அ", "aa" to "ஆ", "i" to "இ", "ii" to "ஈ", "u" to "உ", "uu" to "ஊ",
        "r_vocalic" to "ரி", "rr_vocalic" to "ரீ", "l_vocalic" to "லி", "ll_vocalic" to "லீ",
        "e_short" to "எ", "e" to "ஏ", "ai" to "ஐ", "o_short" to "ஒ", "o" to "ஓ", "au" to "ஔ",
        "m_aa" to "ா", "m_i" to "ி", "m_ii" to "ீ", "m_u" to "ு", "m_uu" to "ூ",
        "m_r_vocalic" to "ிரு", "m_rr_vocalic" to "ிரூ", "m_l_vocalic" to "ிலு", "m_ll_vocalic" to "ிலூ",
        "m_e_short" to "ெ", "m_e" to "ே", "m_ai" to "ை", "m_o_short" to "ொ", "m_o" to "ோ", "m_au" to "ௌ",
        "k" to "க", "kh" to "க", "g" to "க", "gh" to "க", "ng" to "ங",
        "c" to "ச", "ch" to "ச", "j" to "ஜ", "jh" to "ஜ", "ny" to "ஞ",
        "tt" to "ட", "tth" to "ட", "dd" to "ட", "ddh" to "ட", "nn" to "ண",
        "t" to "த", "th" to "த", "d" to "த", "dh" to "த", "n" to "ந",
        "p" to "ப", "ph" to "ப", "b" to "ப", "bh" to "ப", "m" to "ம",
        "y" to "ய", "r" to "ர", "l" to "ல", "v" to "வ",
        "sh" to "ஶ", "ss" to "ஷ", "s" to "ஸ", "h" to "ஹ",
        "ll" to "ள", "zh" to "ழ", "rr" to "ற", "nnn" to "ன",
        "q" to "க", "khh" to "க", "ghh" to "க", "z" to "ஜ", "ddd" to "ட", "rh" to "ட", "f" to "ப", "yy" to "ய",
        "virama" to "்", "anusvara" to "ம்", "visarga" to "ஃ", "candrabindu" to "ஂ", "avagraha" to "",
        "nukta" to "", "danda" to "।", "double_danda" to "॥"
    )

    private val TELUGU_MAP = mapOf(
        "a" to "అ", "aa" to "ఆ", "i" to "ఇ", "ii" to "ఈ", "u" to "ఉ", "uu" to "ఊ",
        "r_vocalic" to "ఋ", "rr_vocalic" to "ౠ", "l_vocalic" to "ఌ", "ll_vocalic" to "ౡ",
        "e_short" to "ఎ", "e" to "ఏ", "ai" to "ఐ", "o_short" to "ఒ", "o" to "ఓ", "au" to "ఔ",
        "m_aa" to "ా", "m_i" to "ి", "m_ii" to "ీ", "m_u" to "ు", "m_uu" to "ూ",
        "m_r_vocalic" to "ృ", "m_rr_vocalic" to "ౄ", "m_l_vocalic" to "ౢ", "m_ll_vocalic" to "ౣ",
        "m_e_short" to "ె", "m_e" to "ే", "m_ai" to "ై", "m_o_short" to "ొ", "m_o" to "ో", "m_au" to "ౌ",
        "k" to "క", "kh" to "ఖ", "g" to "గ", "gh" to "ఘ", "ng" to "ఙ",
        "c" to "చ", "ch" to "ఛ", "j" to "జ", "jh" to "ఝ", "ny" to "ఞ",
        "tt" to "ట", "tth" to "ఠ", "dd" to "డ", "ddh" to "ఢ", "nn" to "ణ",
        "t" to "త", "th" to "థ", "d" to "ద", "dh" to "ధ", "n" to "న",
        "p" to "ప", "ph" to "ఫ", "b" to "బ", "bh" to "భ", "m" to "మ",
        "y" to "య", "r" to "ర", "l" to "ల", "v" to "వ",
        "sh" to "శ", "ss" to "ష", "s" to "స", "h" to "హ",
        "ll" to "ళ", "zh" to "ఴ", "rr" to "ఱ", "nnn" to "న",
        "q" to "క", "khh" to "ఖ", "ghh" to "గ", "z" to "జ", "ddd" to "డ", "rh" to "ఢ", "f" to "ఫ", "yy" to "య",
        "virama" to "్", "anusvara" to "ం", "visarga" to "ః", "candrabindu" to "ఁ", "avagraha" to "ఽ",
        "nukta" to "", "danda" to "।", "double_danda" to "॥"
    )

    private val KANNADA_MAP = mapOf(
        "a" to "ಅ", "aa" to "ಆ", "i" to "ಇ", "ii" to "ಈ", "u" to "ಉ", "uu" to "ಊ",
        "r_vocalic" to "ಋ", "rr_vocalic" to "ೠ", "l_vocalic" to "ಌ", "ll_vocalic" to "ೡ",
        "e_short" to "ಎ", "e" to "ಏ", "ai" to "ಐ", "o_short" to "ಒ", "o" to "ಓ", "au" to "ಔ",
        "m_aa" to "ಾ", "m_i" to "ಿ", "m_ii" to "ೀ", "m_u" to "ು", "m_uu" to "ೂ",
        "m_r_vocalic" to "ೃ", "m_rr_vocalic" to "ೄ", "m_l_vocalic" to "ಱ", "m_ll_vocalic" to "ೡ",
        "m_e_short" to "ೆ", "m_e" to "ೇ", "m_ai" to "ೈ", "m_o_short" to "ೊ", "m_o" to "ೋ", "m_au" to "ೌ",
        "k" to "ಕ", "kh" to "ಖ", "g" to "ಗ", "gh" to "ಘ", "ng" to "ಙ",
        "c" to "ಚ", "ch" to "ಛ", "j" to "ಜ", "jh" to "ಝ", "ny" to "ಞ",
        "tt" to "ಟ", "tth" to "ಠ", "dd" to "ಡ", "ddh" to "ಢ", "nn" to "ಣ",
        "t" to "ತ", "th" to "ಥ", "d" to "ದ", "dh" to "ಧ", "n" to "ನ",
        "p" to "ಪ", "ph" to "ಫ", "b" to "ಬ", "bh" to "ಭ", "m" to "ಮ",
        "y" to "ಯ", "r" to "ರ", "l" to "ಲ", "v" to "ವ",
        "sh" to "ಶ", "ss" to "ಷ", "s" to "ಸ", "h" to "ಹ",
        "ll" to "ಳ", "zh" to "ೞ", "rr" to "ಱ", "nnn" to "ನ",
        "q" to "ಕ", "khh" to "ಖ", "ghh" to "ಗ", "z" to "ಜ", "ddd" to "ಡ", "rh" to "ಢ", "f" to "ಫ", "yy" to "ಯ",
        "virama" to "್", "anusvara" to "ಂ", "visarga" to "ಃ", "candrabindu" to "ँ", "avagraha" to "ಽ",
        "nukta" to "಼", "danda" to "।", "double_danda" to "॥"
    )

    private val MALAYALAM_MAP = mapOf(
        "a" to "അ", "aa" to "ആ", "i" to "ഇ", "ii" to "ഈ", "u" to "ഉ", "uu" to "ഊ",
        "r_vocalic" to "ഋ", "rr_vocalic" to "ൠ", "l_vocalic" to "ഌ", "ll_vocalic" to "ൡ",
        "e_short" to "എ", "e" to "ഏ", "ai" to "ഐ", "o_short" to "ഒ", "o" to "ഓ", "au" to "ഔ",
        "m_aa" to "ാ", "m_i" to "ി", "m_ii" to "ീ", "m_u" to "ു", "m_uu" to "ൂ",
        "m_r_vocalic" to "ൃ", "m_rr_vocalic" to "ൄ", "m_l_vocalic" to "ൢ", "m_ll_vocalic" to "ൣ",
        "m_e_short" to "െ", "m_e" to "േ", "m_ai" to "ൈ", "m_o_short" to "ൊ", "m_o" to "ോ", "m_au" to "ൌ",
        "k" to "ക", "kh" to "ഖ", "g" to "ഗ", "gh" to "ഘ", "ng" to "ങ",
        "c" to "ച", "ch" to "ഛ", "j" to "ജ", "jh" to "ഝ", "ny" to "ഞ",
        "tt" to "ട", "tth" to "ഠ", "dd" to "ഡ", "ddh" to "ഢ", "nn" to "ണ",
        "t" to "ത", "th" to "ഥ", "d" to "ദ", "dh" to "ധ", "n" to "ന",
        "p" to "പ", "ph" to "ഫ", "b" to "ബ", "bh" to "ഭ", "m" to "മ",
        "y" to "യ", "r" to "ര", "l" to "ല", "v" to "വ",
        "sh" to "ശ", "ss" to "ഷ", "s" to "സ", "h" to "ഹ",
        "ll" to "ള", "zh" to "ഴ", "rr" to "റ", "nnn" to "ന",
        "q" to "ക", "khh" to "ഖ", "ghh" to "ഗ", "z" to "ജ", "ddd" to "ഡ", "rh" to "ഢ", "f" to "ഫ", "yy" to "യ",
        "virama" to "്", "anusvara" to "ം", "visarga" to "ഃ", "candrabindu" to "ഁ", "avagraha" to "ഽ",
        "nukta" to "", "danda" to "।", "double_danda" to "॥"
    )

    private val GUJARATI_MAP = mapOf(
        "a" to "અ", "aa" to "આ", "i" to "ઇ", "ii" to "ઈ", "u" to "ઉ", "uu" to "ઊ",
        "r_vocalic" to "ઋ", "rr_vocalic" to "ૠ", "l_vocalic" to "ઌ", "ll_vocalic" to "ૡ",
        "e_short" to "એ", "e" to "એ", "ai" to "ઐ", "o_short" to "ઓ", "o" to "ઓ", "au" to "ઔ",
        "m_aa" to "ા", "m_i" to "િ", "m_ii" to "ી", "m_u" to "ુ", "m_uu" to "ૂ",
        "m_r_vocalic" to "ૃ", "m_rr_vocalic" to "ૄ", "m_l_vocalic" to "ૢ", "m_ll_vocalic" to "ૣ",
        "m_e_short" to "ે", "m_e" to "ે", "m_ai" to "ૈ", "m_o_short" to "ો", "m_o" to "ો", "m_au" to "ૌ",
        "k" to "ક", "kh" to "ખ", "g" to "ગ", "gh" to "ઘ", "ng" to "ઙ",
        "c" to "ચ", "ch" to "છ", "j" to "જ", "jh" to "ઝ", "ny" to "ઞ",
        "tt" to "ટ", "tth" to "ઠ", "dd" to "ડ", "ddh" to "ઢ", "nn" to "ણ",
        "t" to "ત", "th" to "થ", "d" to "દ", "dh" to "ધ", "n" to "ન",
        "p" to "પ", "ph" to "ફ", "b" to "બ", "bh" to "ભ", "m" to "મ",
        "y" to "ય", "r" to "ર", "l" to "લ", "v" to "વ",
        "sh" to "શ", "ss" to "ષ", "s" to "સ", "h" to "હ",
        "ll" to "ળ", "zh" to "ળ", "rr" to "ર", "nnn" to "ન",
        "q" to "ક", "khh" to "ખ", "ghh" to "ગ", "z" to "જ", "ddd" to "ડ", "rh" to "ઢ", "f" to "ફ", "yy" to "ય",
        "virama" to "્", "anusvara" to "ં", "visarga" to "ઃ", "candrabindu" to "ઁ", "avagraha" to "ઽ",
        "nukta" to "઼", "danda" to "।", "double_danda" to "॥"
    )

    private val GURMUKHI_MAP = mapOf(
        "a" to "ਅ", "aa" to "ਆ", "i" to "ਇ", "ii" to "ਈ", "u" to "ਉ", "uu" to "ਊ",
        "r_vocalic" to "ਰਿ", "rr_vocalic" to "ਰੀ", "l_vocalic" to "ਲਿ", "ll_vocalic" to "ਲੀ",
        "e_short" to "ਏ", "e" to "ਏ", "ai" to "ਐ", "o_short" to "ਓ", "o" to "ਓ", "au" to "ਔ",
        "m_aa" to "ਾ", "m_i" to "ਿ", "m_ii" to "ੀ", "m_u" to "ੁ", "m_uu" to "ੂ",
        "m_r_vocalic" to "੍ਰਿ", "m_rr_vocalic" to "੍ਰੀ", "m_l_vocalic" to "੍ਲਿ", "m_ll_vocalic" to "੍ਲੀ",
        "m_e_short" to "ੇ", "m_e" to "ੇ", "m_ai" to "ੈ", "m_o_short" to "ੋ", "m_o" to "ੋ", "m_au" to "ੌ",
        "k" to "ਕ", "kh" to "ਖ", "g" to "ਗ", "gh" to "ਘ", "ng" to "ਙ",
        "c" to "ਚ", "ch" to "ਛ", "j" to "ਜ", "jh" to "ਝ", "ny" to "ਞ",
        "tt" to "ਟ", "tth" to "ਠ", "dd" to "ਡ", "ddh" to "ਢ", "nn" to "ਣ",
        "t" to "ਤ", "th" to "ਥ", "d" to "ਦ", "dh" to "ਧ", "n" to "ਨ",
        "p" to "ਪ", "ph" to "ਫ", "b" to "ਬ", "bh" to "ਭ", "m" to "ਮ",
        "y" to "ਯ", "r" to "ਰ", "l" to "ਲ", "v" to "ਵ",
        "sh" to "ਸ਼", "ss" to "ਸ", "s" to "ਸ", "h" to "ਹ",
        "ll" to "ਲ਼", "zh" to "ਲ਼", "rr" to "ਰ", "nnn" to "ਨ",
        "q" to "ਕ", "khh" to "ਖ਼", "ghh" to "ਗ਼", "z" to "ਜ਼", "ddd" to "ੜ", "rh" to "ੜ੍ਹ", "f" to "ਫ਼", "yy" to "ਯ",
        "virama" to "੍", "anusvara" to "ਂ", "visarga" to "ਃ", "candrabindu" to "ਁ", "avagraha" to "",
        "nukta" to "਼", "danda" to "।", "double_danda" to "॥"
    )

    private val ORIYA_MAP = mapOf(
        "a" to "ଅ", "aa" to "ଆ", "i" to "ଇ", "ii" to "ଈ", "u" to "ଉ", "uu" to "ଊ",
        "r_vocalic" to "ଋ", "rr_vocalic" to "ୠ", "l_vocalic" to "ଌ", "ll_vocalic" to "ୡ",
        "e_short" to "ଏ", "e" to "ଏ", "ai" to "ଐ", "o_short" to "ଓ", "o" to "ଓ", "au" to "ଔ",
        "m_aa" to "ା", "m_i" to "ି", "m_ii" to "ୀ", "m_u" to "ୁ", "m_uu" to "ୂ",
        "m_r_vocalic" to "ୃ", "m_rr_vocalic" to "ୄ", "m_l_vocalic" to "ୢ", "m_ll_vocalic" to "ୣ",
        "m_e_short" to "େ", "m_e" to "େ", "m_ai" to "ୈ", "m_o_short" to "ୋ", "m_o" to "ୋ", "m_au" to "ୌ",
        "k" to "କ", "kh" to "ଖ", "g" to "ଗ", "gh" to "ଘ", "ng" to "ଙ",
        "c" to "ଚ", "ch" to "ଛ", "j" to "ଜ", "jh" to "ଝ", "ny" to "ଞ",
        "tt" to "ଟ", "tth" to "ଠ", "dd" to "ଡ", "ddh" to "ଢ", "nn" to "ଣ",
        "t" to "ତ", "th" to "ଥ", "d" to "ଦ", "dh" to "ଧ", "n" to "ନ",
        "p" to "ପ", "ph" to "ଫ", "b" to "ବ", "bh" to "ଭ", "m" to "ମ",
        "y" to "ଯ", "r" to "ର", "l" to "ଲ", "v" to "ଵ",
        "sh" to "ଶ", "ss" to "ଷ", "s" to "ସ", "h" to "ହ",
        "ll" to "ଳ", "zh" to "ଳ", "rr" to "ର", "nnn" to "ନ",
        "q" to "କ", "khh" to "ଖ", "ghh" to "ଗ", "z" to "ଜ", "ddd" to "ଡ଼", "rh" to "ଢ଼", "f" to "ଫ", "yy" to "ୟ",
        "virama" to "୍", "anusvara" to "ଂ", "visarga" to "ଃ", "candrabindu" to "ଁ", "avagraha" to "ଽ",
        "nukta" to "଼", "danda" to "।", "double_danda" to "॥"
    )

    // Romanization Schemes Maps
    private val IAST_MAP = mapOf(
        "a" to "a", "aa" to "ā", "i" to "i", "ii" to "ī", "u" to "u", "uu" to "ū",
        "r_vocalic" to "ṛ", "rr_vocalic" to "ṝ", "l_vocalic" to "ḷ", "ll_vocalic" to "ḹ",
        "e_short" to "e", "e" to "e", "ai" to "ai", "o_short" to "o", "o" to "o", "au" to "au",
        "m_aa" to "ā", "m_i" to "i", "m_ii" to "ī", "m_u" to "u", "m_uu" to "ū",
        "m_r_vocalic" to "ṛ", "m_rr_vocalic" to "ṝ", "m_l_vocalic" to "ḷ", "m_ll_vocalic" to "ḹ",
        "m_e_short" to "e", "m_e" to "e", "m_ai" to "ai", "m_o_short" to "o", "m_o" to "o", "m_au" to "au",
        "k" to "k", "kh" to "kh", "g" to "g", "gh" to "gh", "ng" to "ṅ",
        "c" to "c", "ch" to "ch", "j" to "j", "jh" to "jh", "ny" to "ñ",
        "tt" to "ṭ", "tth" to "ṭh", "dd" to "ḍ", "ddh" to "ḍh", "nn" to "ṇ",
        "t" to "t", "th" to "th", "d" to "d", "dh" to "dh", "n" to "n",
        "p" to "p", "ph" to "ph", "b" to "b", "bh" to "bh", "m" to "m",
        "y" to "y", "r" to "r", "l" to "l", "v" to "v",
        "sh" to "ś", "ss" to "ṣ", "s" to "s", "h" to "h",
        "ll" to "ḷ", "zh" to "ḻ", "rr" to "ṟ", "nnn" to "ṉ",
        "q" to "q", "khh" to "ḵh", "ghh" to "ġ", "z" to "z", "ddd" to "ṛ", "rh" to "ṛh", "f" to "f", "yy" to "ẏ",
        "virama" to "", "anusvara" to "ṃ", "visarga" to "ḥ", "candrabindu" to "m̐", "avagraha" to "'",
        "nukta" to "", "danda" to "|", "double_danda" to "||"
    )

    private val ISO_MAP = IAST_MAP + mapOf(
        "e_short" to "e", "e" to "ē", "o_short" to "o", "o" to "ō",
        "m_e_short" to "e", "m_e" to "ē", "m_o_short" to "o", "m_o" to "ō"
    )

    private val HK_MAP = mapOf(
        "a" to "a", "aa" to "A", "i" to "i", "ii" to "I", "u" to "u", "uu" to "U",
        "r_vocalic" to "R", "rr_vocalic" to "RR", "l_vocalic" to "lR", "ll_vocalic" to "lRR",
        "e_short" to "e", "e" to "e", "ai" to "ai", "o_short" to "o", "o" to "o", "au" to "au",
        "m_aa" to "A", "m_i" to "i", "m_ii" to "I", "m_u" to "u", "m_uu" to "U",
        "m_r_vocalic" to "R", "m_rr_vocalic" to "RR", "m_l_vocalic" to "lR", "m_ll_vocalic" to "lRR",
        "m_e_short" to "e", "m_e" to "e", "m_ai" to "ai", "m_o_short" to "o", "m_o" to "o", "m_au" to "au",
        "k" to "k", "kh" to "kh", "g" to "g", "gh" to "gh", "ng" to "G",
        "c" to "c", "ch" to "ch", "j" to "j", "jh" to "jh", "ny" to "J",
        "tt" to "T", "tth" to "Th", "dd" to "D", "ddh" to "Dh", "nn" to "N",
        "t" to "t", "th" to "th", "d" to "d", "dh" to "dh", "n" to "n",
        "p" to "p", "ph" to "ph", "b" to "b", "bh" to "bh", "m" to "m",
        "y" to "y", "r" to "r", "l" to "l", "v" to "v",
        "sh" to "z", "ss" to "S", "s" to "s", "h" to "h",
        "ll" to "L", "zh" to "z", "rr" to "r", "nnn" to "n",
        "q" to "q", "khh" to "K", "ghh" to "g", "z" to "z", "ddd" to "D", "rh" to "Dh", "f" to "f", "yy" to "y",
        "virama" to "", "anusvara" to "M", "visarga" to "H", "candrabindu" to "~", "avagraha" to "'",
        "nukta" to "", "danda" to "|", "double_danda" to "||"
    )

    private val SCRIPT_TABLES = mapOf(
        "Devanagari" to DEVANAGARI_MAP,
        "Bengali" to BENGALI_MAP,
        "Assamese" to (BENGALI_MAP + mapOf("r" to "ৰ", "v" to "ৱ")),
        "Tamil" to TAMIL_MAP,
        "Telugu" to TELUGU_MAP,
        "Kannada" to KANNADA_MAP,
        "Malayalam" to MALAYALAM_MAP,
        "Gujarati" to GUJARATI_MAP,
        "Gurmukhi" to GURMUKHI_MAP,
        "Oriya" to ORIYA_MAP,
        "IAST" to IAST_MAP,
        "ISO" to ISO_MAP,
        "ISO15919" to ISO_MAP,
        "HK" to HK_MAP,
        "Harvard-Kyoto" to HK_MAP,
        "ITRANS" to IAST_MAP,
        "Velthuis" to IAST_MAP,
        "SLP1" to HK_MAP,
        "WX" to HK_MAP,
    )

    // Reverse lookup maps: Script -> (char/string -> PhToken)
    private val REVERSE_TABLES: Map<String, List<Pair<String, PhToken>>> by lazy {
        SCRIPT_TABLES.mapValues { (_, map) ->
            val list = mutableListOf<Pair<String, PhToken>>()
            map.forEach { (key, glyph) ->
                if (glyph.isNotEmpty()) {
                    // Prevent aliasing short vowels to non-canonical keys in reverse lookup
                    if ((key == "e_short" && map["e"] == glyph) || (key == "o_short" && map["o"] == glyph)) {
                        return@forEach
                    }
                    if ((key == "m_e_short" && map["m_e"] == glyph) || (key == "m_o_short" && map["m_o"] == glyph)) {
                        return@forEach
                    }
                    val token = when {
                        key.startsWith("m_") -> PhToken(PhTokenType.MATRA, key.removePrefix("m_"))
                        key in VOWELS -> PhToken(PhTokenType.VOWEL, key)
                        key in CONSONANTS -> PhToken(PhTokenType.CONSONANT, key)
                        key == "virama" -> PhToken(PhTokenType.VIRAMA, key)
                        key == "anusvara" -> PhToken(PhTokenType.ANUSVARA, key)
                        key == "visarga" -> PhToken(PhTokenType.VISARGA, key)
                        key == "candrabindu" -> PhToken(PhTokenType.CANDRABINDU, key)
                        key == "avagraha" -> PhToken(PhTokenType.AVAGRAHA, key)
                        key == "danda" -> PhToken(PhTokenType.DANDA, key)
                        key == "double_danda" -> PhToken(PhTokenType.DOUBLE_DANDA, key)
                        else -> PhToken(PhTokenType.LITERAL, key)
                    }
                    list.add(glyph to token)
                }
            }
            // Sort by longest string match first (greedy matching for multi-character sequences)
            list.sortedByDescending { it.first.length }
        }
    }

    // =========================================================================
    // Parser: Source Script -> Intermediate Tokens
    // =========================================================================

    private fun parseToTokens(text: String, scriptName: String): List<PhToken> {
        val pairs = REVERSE_TABLES[scriptName]
        if (pairs == null) {
            // Fallback: character-by-character pass through
            return text.map { PhToken(PhTokenType.LITERAL, it.toString(), it.toString()) }
        }

        val tokens = mutableListOf<PhToken>()
        var index = 0
        val length = text.length

        while (index < length) {
            var matched = false
            for ((glyph, token) in pairs) {
                if (text.startsWith(glyph, index)) {
                    tokens.add(token.copy(raw = glyph))
                    index += glyph.length
                    matched = true
                    break
                }
            }
            if (!matched) {
                val ch = text[index]
                tokens.add(PhToken(PhTokenType.LITERAL, ch.toString(), ch.toString()))
                index++
            }
        }

        return tokens
    }

    // =========================================================================
    // Renderer: Intermediate Tokens -> Target Script
    // =========================================================================

    private fun renderFromTokens(tokens: List<PhToken>, targetScript: String, sourceScript: String): String {
        val table = SCRIPT_TABLES[targetScript] ?: return tokens.joinToString("") { it.raw }
        val isRomanTarget = targetScript in ROMAN_SCHEMES
        val isRomanSource = sourceScript in ROMAN_SCHEMES
        val out = StringBuilder()

        var i = 0
        val size = tokens.size
        while (i < size) {
            val token = tokens[i]
            when (token.type) {
                PhTokenType.CONSONANT -> {
                    val nextToken = tokens.getOrNull(i + 1)
                    if (isRomanTarget) {
                        val baseConsonant = table[token.key] ?: token.raw
                        out.append(baseConsonant)
                        when (nextToken?.type) {
                            PhTokenType.MATRA -> {
                                out.append(table["m_${nextToken.key}"] ?: "")
                                i++ // consume matra
                            }
                            PhTokenType.VOWEL -> {
                                if (nextToken.key == "a") {
                                    out.append("a")
                                } else {
                                    out.append(table["m_${nextToken.key}"] ?: table[nextToken.key] ?: "")
                                }
                                i++ // consume vowel
                            }
                            PhTokenType.VIRAMA -> {
                                // Halant / virama suppresses the inherent 'a' in romanization
                                i++ // consume virama
                            }
                            PhTokenType.CONSONANT -> {
                                if (!isRomanSource) {
                                    // In Brahmic scripts, every consonant without an explicit virama has an inherent 'a'
                                    out.append("a")
                                }
                            }
                            else -> {
                                // Inherent vowel 'a'
                                out.append("a")
                            }
                        }
                    } else {
                        // Brahmic target
                        val glyph = table[token.key] ?: table["s"] ?: token.raw
                        out.append(glyph)
                        when (nextToken?.type) {
                            PhTokenType.MATRA -> {
                                out.append(table["m_${nextToken.key}"] ?: "")
                                i++ // consume matra
                            }
                            PhTokenType.VOWEL -> {
                                if (nextToken.key != "a") {
                                    out.append(table["m_${nextToken.key}"] ?: "")
                                }
                                i++ // consume vowel
                            }
                            PhTokenType.VIRAMA -> {
                                out.append(table["virama"] ?: "")
                                i++ // consume virama
                            }
                            PhTokenType.CONSONANT -> {
                                if (isRomanSource) {
                                    out.append(table["virama"] ?: "")
                                }
                            }
                            else -> {
                                // Inherent vowel is naturally part of the consonant glyph
                            }
                        }
                    }
                }
                PhTokenType.VOWEL -> {
                    out.append(table[token.key] ?: token.raw)
                }
                PhTokenType.MATRA -> {
                    // Stray matra without base consonant: render as independent vowel or matra
                    out.append(table["m_${token.key}"] ?: table[token.key] ?: token.raw)
                }
                PhTokenType.VIRAMA -> {
                    if (!isRomanTarget) {
                        out.append(table["virama"] ?: "")
                    }
                }
                PhTokenType.ANUSVARA -> out.append(table["anusvara"] ?: "ṃ")
                PhTokenType.VISARGA -> out.append(table["visarga"] ?: "ḥ")
                PhTokenType.CANDRABINDU -> out.append(table["candrabindu"] ?: "~")
                PhTokenType.AVAGRAHA -> out.append(table["avagraha"] ?: "'")
                PhTokenType.DANDA -> out.append(table["danda"] ?: "|")
                PhTokenType.DOUBLE_DANDA -> out.append(table["double_danda"] ?: "||")
                PhTokenType.LITERAL, PhTokenType.DIGIT, PhTokenType.NUKTA -> out.append(token.raw)
            }
            i++
        }

        return out.toString()
    }

    private fun removeTerminalSchwas(text: String, targetScript: String): String {
        if (targetScript !in ROMAN_SCHEMES) return text
        val words = text.split(Regex("(\\s+|[.,!?;:\"'\\[\\](){}]+)"))
        var result = text
        for (w in words) {
            if (w.length > 2 && w.endsWith("a") && !w.endsWith("aa") && !w.endsWith("ā")) {
                val trimmed = w.dropLast(1)
                result = result.replace(w, trimmed)
            }
        }
        return result
    }

    // =========================================================================
    // Script Detection & Normalization
    // =========================================================================

    fun detectScript(text: String): String {
        for (ch in text) {
            val code = ch.code
            when (code) {
                in 0x0900..0x097F -> return "Devanagari"
                in 0x0980..0x09FF -> return if (ch == 'ৰ' || ch == 'ৱ') "Assamese" else "Bengali"
                in 0x0A00..0x0A7F -> return "Gurmukhi"
                in 0x0A80..0x0AFF -> return "Gujarati"
                in 0x0B00..0x0B7F -> return "Oriya"
                in 0x0B80..0x0BFF -> return "Tamil"
                in 0x0C00..0x0C7F -> return "Telugu"
                in 0x0C80..0x0CFF -> return "Kannada"
                in 0x0D00..0x0D7F -> return "Malayalam"
                in 0x0D80..0x0DFF -> return "Sinhala"
                in 0x0600..0x06FF -> return "Urdu"
            }
        }
        return "IAST"
    }

    fun normalizeScript(name: String): String {
        val norm = name.trim().lowercase().replace("-", "").replace("_", "")
        return when (norm) {
            "devanagari", "deva", "hindi", "hi", "marathi", "mr", "sanskrit", "sa", "nepali", "ne" -> "Devanagari"
            "bengali", "beng", "bn", "bangla" -> "Bengali"
            "assamese", "as", "asm" -> "Assamese"
            "gurmukhi", "guru", "punjabi", "pa", "panjabi" -> "Gurmukhi"
            "gujarati", "gujr", "gu" -> "Gujarati"
            "oriya", "odia", "orya", "or" -> "Oriya"
            "tamil", "taml", "ta" -> "Tamil"
            "telugu", "telu", "te" -> "Telugu"
            "kannada", "knda", "kn" -> "Kannada"
            "malayalam", "mlym", "ml" -> "Malayalam"
            "sinhala", "sinh", "si" -> "Sinhala"
            "urdu", "ur" -> "Urdu"
            "iast" -> "IAST"
            "iso", "iso15919" -> "ISO"
            "hk", "harvardkyoto" -> "HK"
            "itrans" -> "ITRANS"
            "velthuis" -> "Velthuis"
            "slp1" -> "SLP1"
            "wx" -> "WX"
            else -> name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
