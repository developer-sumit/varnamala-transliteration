package com.sumit.varnamala.data

data class MappingEntry(
    val englishRoman: String,          // Roman/English representation (e.g., "ka")
    val hindiDevanagari: String,       // Hindi in Devanagari script (e.g., "क")
    val teluguScript: String,          // Telugu script (e.g., "క")
    val punjabiScript: String? = null, // Punjabi/Gurmukhi script (e.g., "ਕ")
    val malayalamScript: String? = null, // Malayalam script (e.g., "ക")
    val tamilScript: String? = null,   // Tamil script (e.g., "க")
    val hindiMatra: String? = null,    // Hindi matra/vowel modifier if applicable
    val teluguMatra: String? = null,   // Telugu matra/vowel modifier if applicable
    val punjabiMatra: String? = null,  // Punjabi matra/vowel modifier if applicable
    val malayalamMatra: String? = null, // Malayalam matra/vowel modifier if applicable
    val tamilMatra: String? = null,    // Tamil matra/vowel modifier if applicable
    val description: String? = null    // Optional description/notes
)

object TransliterationMappingRepository {

    /**
     * Returns all basic vowel (Swara) mappings
     */
    fun getVowelMappings(): List<MappingEntry> {
        return listOf(
            // Independent Vowels (Swaras)
            MappingEntry(
                englishRoman = "a",
                hindiDevanagari = "अ",
                teluguScript = "అ",
                punjabiScript = "ਅ",
                malayalamScript = "അ",
                tamilScript = "அ",
                description = "Inherent 'a' sound"
            ),
            MappingEntry(
                englishRoman = "aa",
                hindiDevanagari = "आ",
                teluguScript = "ఆ",
                punjabiScript = "ਆ",
                malayalamScript = "ആ",
                tamilScript = "ஆ",
                hindiMatra = "ा",
                teluguMatra = "ా",
                punjabiMatra = "ਾ",
                malayalamMatra = "ാ",
                tamilMatra = "ா",
                description = "Long 'a' sound"
            ),
            MappingEntry(
                englishRoman = "i",
                hindiDevanagari = "इ",
                teluguScript = "ఇ",
                punjabiScript = "ਇ",
                malayalamScript = "ഇ",
                hindiMatra = "ि",
                teluguMatra = "ి",
                punjabiMatra = "ਿ",
                malayalamMatra = "ി",
                description = "Short 'i' sound"
            ),
            MappingEntry(
                englishRoman = "ii",
                hindiDevanagari = "ई",
                teluguScript = "ఈ",
                punjabiScript = "ਈ",
                malayalamScript = "ഈ",
                hindiMatra = "ी",
                teluguMatra = "ీ",
                punjabiMatra = "ੀ",
                malayalamMatra = "ീ",
                description = "Long 'i' sound"
            ),
            MappingEntry(
                englishRoman = "u",
                hindiDevanagari = "उ",
                teluguScript = "ఉ",
                punjabiScript = "ਉ",
                malayalamScript = "ഉ",
                hindiMatra = "ु",
                teluguMatra = "ు",
                punjabiMatra = "ੁ",
                malayalamMatra = "ു",
                description = "Short 'u' sound"
            ),
            MappingEntry(
                englishRoman = "uu",
                hindiDevanagari = "ऊ",
                teluguScript = "ఊ",
                punjabiScript = "ਊ",
                malayalamScript = "ഊ",
                hindiMatra = "ू",
                teluguMatra = "ూ",
                punjabiMatra = "ੂ",
                malayalamMatra = "ൂ",
                description = "Long 'u' sound"
            ),
            MappingEntry(
                englishRoman = "ri",
                hindiDevanagari = "ऋ",
                teluguScript = "ఋ",
                punjabiScript = "ਰਿ",
                malayalamScript = "ഋ",
                hindiMatra = "ृ",
                teluguMatra = "ృ",
                malayalamMatra = "ൃ",
                description = "Vocalic 'ri' sound"
            ),
            MappingEntry(
                englishRoman = "e",
                hindiDevanagari = "ए",
                teluguScript = "ఎ",
                punjabiScript = "ਏ",
                malayalamScript = "എ",
                hindiMatra = "े",
                teluguMatra = "ె",
                punjabiMatra = "ੇ",
                malayalamMatra = "െ",
                description = "Short 'e' sound"
            ),
            MappingEntry(
                englishRoman = "ee",
                hindiDevanagari = "ई",
                teluguScript = "ఏ",
                punjabiScript = "ਈ",
                malayalamScript = "ഏ",
                teluguMatra = "ే",
                malayalamMatra = "േ",
                description = "Long 'e' sound"
            ),
            MappingEntry(
                englishRoman = "ai",
                hindiDevanagari = "ऐ",
                teluguScript = "ఐ",
                punjabiScript = "ਐ",
                malayalamScript = "ഐ",
                hindiMatra = "ै",
                teluguMatra = "ై",
                punjabiMatra = "ੈ",
                malayalamMatra = "ൈ",
                description = "'ai' sound"
            ),
            MappingEntry(
                englishRoman = "o",
                hindiDevanagari = "ओ",
                teluguScript = "ఒ",
                punjabiScript = "ਓ",
                malayalamScript = "ഒ",
                hindiMatra = "ो",
                teluguMatra = "ొ",
                punjabiMatra = "ੋ",
                malayalamMatra = "ൊ",
                description = "Short 'o' sound"
            ),
            MappingEntry(
                englishRoman = "oo",
                hindiDevanagari = "ऊ",
                teluguScript = "ఓ",
                punjabiScript = "ਊ",
                malayalamScript = "ഓ",
                teluguMatra = "ో",
                malayalamMatra = "ോ",
                description = "Long 'o' sound"
            ),
            MappingEntry(
                englishRoman = "au",
                hindiDevanagari = "औ",
                teluguScript = "ఔ",
                punjabiScript = "ਔ",
                malayalamScript = "ഔ",
                hindiMatra = "ौ",
                teluguMatra = "ౌ",
                punjabiMatra = "ੌ",
                malayalamMatra = "ൌ",
                description = "'au' or 'ow' sound"
            )
        )
    }

    /**
     * Returns all consonant (Vyanjana) mappings
     */
    fun getConsonantMappings(): List<MappingEntry> {
        return listOf(
            // Velar consonants (Kavya Varga)
            MappingEntry(
                englishRoman = "ka",
                hindiDevanagari = "क",
                teluguScript = "క",
                punjabiScript = "ਕ",
                malayalamScript = "ക",
                tamilScript = "க",
                description = "Unvoiced velar stop"
            ),
            MappingEntry(
                englishRoman = "kha",
                hindiDevanagari = "ख",
                teluguScript = "ఖ",
                punjabiScript = "ਖ",
                malayalamScript = "ഖ",
                tamilScript = "க",
                description = "Aspirated unvoiced velar stop"
            ),
            MappingEntry(
                englishRoman = "ga",
                hindiDevanagari = "ग",
                teluguScript = "గ",
                punjabiScript = "ਗ",
                malayalamScript = "ഗ",
                tamilScript = "க",
                description = "Voiced velar stop"
            ),
            MappingEntry(
                englishRoman = "gha",
                hindiDevanagari = "घ",
                teluguScript = "ఘ",
                punjabiScript = "ਘ",
                malayalamScript = "ഘ",
                tamilScript = "க",
                description = "Aspirated voiced velar stop"
            ),
            MappingEntry(
                englishRoman = "nga",
                hindiDevanagari = "ङ",
                teluguScript = "ఙ",
                punjabiScript = "ਙ",
                malayalamScript = "ങ",
                tamilScript = "ங",
                description = "Nasal velar stop"
            ),

            // Palatal consonants (Chavya Varga)
            MappingEntry(
                englishRoman = "cha",
                hindiDevanagari = "च",
                teluguScript = "చ",
                punjabiScript = "ਚ",
                malayalamScript = "ച",
                tamilScript = "ச",
                description = "Unvoiced palatal stop"
            ),
            MappingEntry(
                englishRoman = "chha",
                hindiDevanagari = "छ",
                teluguScript = "ఛ",
                punjabiScript = "ਛ",
                malayalamScript = "ഛ",
                tamilScript = "ச",
                description = "Aspirated unvoiced palatal stop"
            ),
            MappingEntry(
                englishRoman = "ja",
                hindiDevanagari = "ज",
                teluguScript = "జ",
                punjabiScript = "ਜ",
                malayalamScript = "ജ",
                tamilScript = "ஜ",
                description = "Voiced palatal stop"
            ),
            MappingEntry(
                englishRoman = "jha",
                hindiDevanagari = "झ",
                teluguScript = "ఝ",
                punjabiScript = "ਝ",
                malayalamScript = "ഝ",
                description = "Aspirated voiced palatal stop"
            ),
            MappingEntry(
                englishRoman = "nya",
                hindiDevanagari = "ञ",
                teluguScript = "ఞ",
                punjabiScript = "ਞ",
                malayalamScript = "ഞ",
                description = "Nasal palatal stop"
            ),

            // Retroflex consonants (Moordha Varga)
            MappingEntry(
                englishRoman = "Ta",
                hindiDevanagari = "ट",
                teluguScript = "ట",
                punjabiScript = "ਟ",
                malayalamScript = "ട",
                description = "Unvoiced retroflex stop"
            ),
            MappingEntry(
                englishRoman = "Tha",
                hindiDevanagari = "ठ",
                teluguScript = "ఠ",
                description = "Aspirated unvoiced retroflex stop"
            ),
            MappingEntry(
                englishRoman = "Da",
                hindiDevanagari = "ड",
                teluguScript = "డ",
                description = "Voiced retroflex stop"
            ),
            MappingEntry(
                englishRoman = "Dha",
                hindiDevanagari = "ढ",
                teluguScript = "ఢ",
                description = "Aspirated voiced retroflex stop"
            ),
            MappingEntry(
                englishRoman = "Tha",
                hindiDevanagari = "ठ",
                teluguScript = "ఠ",
                punjabiScript = "ਠ",
                malayalamScript = "ഠ",
                description = "Aspirated unvoiced retroflex stop"
            ),
            MappingEntry(
                englishRoman = "Da",
                hindiDevanagari = "ड",
                teluguScript = "డ",
                punjabiScript = "ਡ",
                malayalamScript = "ഡ",
                description = "Voiced retroflex stop"
            ),
            MappingEntry(
                englishRoman = "Dha",
                hindiDevanagari = "ढ",
                teluguScript = "ఢ",
                punjabiScript = "ਢ",
                malayalamScript = "ഢ",
                description = "Aspirated voiced retroflex stop"
            ),
            MappingEntry(
                englishRoman = "Na",
                hindiDevanagari = "ण",
                teluguScript = "ణ",
                punjabiScript = "ਣ",
                malayalamScript = "ണ",
                description = "Nasal retroflex stop"
            ),

            // Dental consonants (Dantya Varga)
            MappingEntry(
                englishRoman = "ta",
                hindiDevanagari = "त",
                teluguScript = "త",
                punjabiScript = "ਤ",
                malayalamScript = "ത",
                description = "Unvoiced dental stop"
            ),
            MappingEntry(
                englishRoman = "tha",
                hindiDevanagari = "थ",
                teluguScript = "థ",
                punjabiScript = "ਥ",
                malayalamScript = "ഥ",
                description = "Aspirated unvoiced dental stop"
            ),
            MappingEntry(
                englishRoman = "da",
                hindiDevanagari = "द",
                teluguScript = "ద",
                punjabiScript = "ਦ",
                malayalamScript = "ദ",
                description = "Voiced dental stop"
            ),
            MappingEntry(
                englishRoman = "dha",
                hindiDevanagari = "ध",
                teluguScript = "ధ",
                punjabiScript = "ਧ",
                malayalamScript = "ധ",
                description = "Aspirated voiced dental stop"
            ),
            MappingEntry(
                englishRoman = "na",
                hindiDevanagari = "न",
                teluguScript = "న",
                punjabiScript = "ਨ",
                malayalamScript = "ന",
                description = "Nasal dental stop"
            ),

            // Labial consonants (Oshya Varga)
            MappingEntry(
                englishRoman = "pa",
                hindiDevanagari = "प",
                teluguScript = "ప",
                punjabiScript = "ਪ",
                malayalamScript = "പ",
                description = "Unvoiced labial stop"
            ),
            MappingEntry(
                englishRoman = "pha",
                hindiDevanagari = "फ",
                teluguScript = "ఫ",
                punjabiScript = "ਫ",
                malayalamScript = "ഫ",
                description = "Aspirated unvoiced labial stop"
            ),
            MappingEntry(
                englishRoman = "ba",
                hindiDevanagari = "ब",
                teluguScript = "బ",
                punjabiScript = "ਬ",
                malayalamScript = "ബ",
                description = "Voiced labial stop"
            ),
            MappingEntry(
                englishRoman = "bha",
                hindiDevanagari = "भ",
                teluguScript = "భ",
                punjabiScript = "ਭ",
                malayalamScript = "ഭ",
                description = "Aspirated voiced labial stop"
            ),
            MappingEntry(
                englishRoman = "ma",
                hindiDevanagari = "म",
                teluguScript = "మ",
                punjabiScript = "ਮ",
                malayalamScript = "മ",
                description = "Nasal labial stop"
            ),

            // Semivowels and approximants
            MappingEntry(
                englishRoman = "ya",
                hindiDevanagari = "य",
                teluguScript = "య",
                punjabiScript = "ਯ",
                malayalamScript = "യ",
                description = "Palatal approximant"
            ),
            MappingEntry(
                englishRoman = "ra",
                hindiDevanagari = "र",
                teluguScript = "ర",
                punjabiScript = "ਰ",
                malayalamScript = "ര",
                description = "Retroflex approximant"
            ),
            MappingEntry(
                englishRoman = "la",
                hindiDevanagari = "ल",
                teluguScript = "ల",
                punjabiScript = "ਲ",
                malayalamScript = "ല",
                description = "Alveolar lateral approximant"
            ),
            MappingEntry(
                englishRoman = "va",
                hindiDevanagari = "व",
                teluguScript = "వ",
                punjabiScript = "ਵ",
                malayalamScript = "വ",
                description = "Labio-dental approximant"
            ),
            MappingEntry(
                englishRoman = "wa",
                hindiDevanagari = "व",
                teluguScript = "వ",
                punjabiScript = "ਵ",
                malayalamScript = "വ",
                description = "Labio-dental approximant (variant)"
            ),

            // Sibilants
            MappingEntry(
                englishRoman = "sha",
                hindiDevanagari = "श",
                teluguScript = "శ",
                punjabiScript = "ਸ਼",
                malayalamScript = "ശ",
                description = "Palatal sibilant"
            ),
            MappingEntry(
                englishRoman = "Sha",
                hindiDevanagari = "ष",
                teluguScript = "ష",
                punjabiScript = "ਸ਼",
                malayalamScript = "ഷ",
                description = "Retroflex sibilant"
            ),
            MappingEntry(
                englishRoman = "sa",
                hindiDevanagari = "स",
                teluguScript = "స",
                punjabiScript = "ਸ",
                malayalamScript = "സ",
                description = "Dental sibilant"
            ),

            // Aspirate
            MappingEntry(
                englishRoman = "ha",
                hindiDevanagari = "ह",
                teluguScript = "హ",
                punjabiScript = "ਹ",
                malayalamScript = "ഹ",
                description = "Velar fricative/aspirate"
            ),
            
            // Additional characters
            MappingEntry(
                englishRoman = "fa",
                hindiDevanagari = "फ़",
                teluguScript = "ఫ",
                punjabiScript = "ਫ਼",
                malayalamScript = "ഫ",
                description = "Labio-dental fricative"
            ),
            MappingEntry(
                englishRoman = "za",
                hindiDevanagari = "ज़",
                teluguScript = "జ",
                punjabiScript = "ਜ਼",
                malayalamScript = "ജ",
                description = "Voiced alveolar fricative"
            ),
            MappingEntry(
                englishRoman = "xa",
                hindiDevanagari = "क्ष",
                teluguScript = "క్ష",
                punjabiScript = "ਕ੍ਸ਼",
                malayalamScript = "ക്ഷ",
                description = "Ksha (compound)"
            ),
            MappingEntry(
                englishRoman = "qa",
                hindiDevanagari = "क़",
                teluguScript = "క",
                punjabiScript = "ਕ਼",
                malayalamScript = "ക",
                description = "Uvular stop"
            )
        )
    }

    /**
     * Returns mappings for numbers and special symbols
     */
    fun getSpecialSymbols(): List<MappingEntry> {
        return listOf(            
            // Punctuation
            MappingEntry(".", ".", ".", ".", "."),
            MappingEntry(",", ",", ",", ",", ","),
            MappingEntry("?", "?", "?", "?", "?"),
            MappingEntry("!", "!", "!", "!", "!"),
            MappingEntry("-", "-", "-", "-", "-"),
            MappingEntry("(", "(", "(", "(", "("),
            MappingEntry(")", ")", ")", ")", ")"),
            MappingEntry(":", ":", ":", ":", ":"),
            MappingEntry(";", ";", ";", ";", ";"),
            MappingEntry("\"", "\"", "\"", "\"", "\""),
            MappingEntry("'", "'", "'", "'", "'"),
            MappingEntry("@", "@", "@", "@", "@"),
            MappingEntry("#", "#", "#", "#", "#"),
            MappingEntry("$", "$", "$", "$", "$"),
            MappingEntry("%", "%", "%", "%", "%"),
            MappingEntry("&", "&", "&", "&", "&"),
            MappingEntry("*", "*", "*", "*", "*"),
            MappingEntry("+", "+", "+", "+", "+"),
            MappingEntry("=", "=", "=", "=", "="),
            MappingEntry("/", "/", "/", "/", "/"),
            MappingEntry("\\", "\\", "\\", "\\", "\\"),
            MappingEntry("<", "<", "<", "<", "<"),
            MappingEntry(">", ">", ">", ">", ">")
        )
    }

    /**
     * Returns mappings for common English words that don't follow strict phonetic rules
     */
    fun getCommonEnglishWords(): List<MappingEntry> {
        return listOf(
            MappingEntry("the", "द", "ద"),
            MappingEntry("and", "और", "మరియు"),
            MappingEntry("is", "है", "ఉంది"),
            MappingEntry("to", "टू", "టు"),
            MappingEntry("of", "ऑफ", "ఆఫ్"),
            MappingEntry("in", "इन", "ఇన్"),
            MappingEntry("you", "यू", "యూ"),
            MappingEntry("that", "दैट", "దట్"),
            MappingEntry("it", "इट", "ఇట్"),
            MappingEntry("he", "ही", "హీ"),
            MappingEntry("was", "वॉज़", "వాజ్"),
            MappingEntry("for", "फॉर", "ఫర్"),
            MappingEntry("on", "ऑन", "ఆన్"),
            MappingEntry("are", "आर", "ఆర్"),
            MappingEntry("with", "विथ", "విత్"),
            MappingEntry("as", "एज़", "యాజ్"),
            MappingEntry("I", "मैं", "నేను"),
            MappingEntry("his", "हिज़", "హిజ్"),
            MappingEntry("they", "दे", "దే"),
            MappingEntry("be", "बी", "బీ"),
            MappingEntry("at", "एट", "ఎట్"),
            MappingEntry("one", "वन", "వన్"),
            MappingEntry("have", "हैव", "హావ్"),
            MappingEntry("this", "दिस", "దిస్"),
            MappingEntry("from", "फ्रॉम", "ఫ్రమ్"),
            MappingEntry("or", "या", "లేదా"),
            MappingEntry("had", "हैड", "హాడ్"),
            MappingEntry("by", "बाय", "బై"),
            MappingEntry("word", "वर्ड", "వర్డ్"),
            MappingEntry("but", "बट", "బట్"),
            MappingEntry("not", "नॉट", "నాట్"),
            MappingEntry("what", "वॉट", "వాట్"),
            MappingEntry("all", "ऑल", "ఆల్"),
            MappingEntry("were", "वर", "వర్"),
            MappingEntry("we", "वी", "వీ"),
            MappingEntry("when", "व्हेन", "వెన్"),
            MappingEntry("your", "योर", "యువర్"),
            MappingEntry("can", "कैन", "కాన్"),
            MappingEntry("said", "सैड", "సెడ్"),
            MappingEntry("there", "देयर", "దేర్"),
            MappingEntry("use", "यूज़", "యూజ్"),
            MappingEntry("an", "एन", "యాన్"),
            MappingEntry("each", "ईच", "ఈచ్"),
            MappingEntry("which", "विच", "విచ్"),
            MappingEntry("she", "शी", "షీ"),
            MappingEntry("do", "डू", "డూ"),
            MappingEntry("how", "हाउ", "హౌ"),
            MappingEntry("their", "देयर", "దేర్"),
            MappingEntry("if", "इफ", "ఇఫ్"),
            MappingEntry("will", "विल", "విల్"),
            MappingEntry("up", "अप", "అప్"),
            MappingEntry("other", "अदर", "అదర్"),
            MappingEntry("about", "अबाउट", "అబౌట్"),
            MappingEntry("out", "आउट", "అవుట్"),
            MappingEntry("many", "मेनी", "మెనీ"),
            MappingEntry("then", "देन", "దెన్"),
            MappingEntry("them", "देम", "దెమ్"),
            MappingEntry("these", "दीज़", "దీజ్"),
            MappingEntry("so", "सो", "సో"),
            MappingEntry("some", "सम", "సమ్"),
            MappingEntry("her", "हर", "హర్"),
            MappingEntry("would", "वुड", "వుడ్"),
            MappingEntry("make", "मेक", "మేక్"),
            MappingEntry("like", "लाइक", "లైక్"),
            MappingEntry("him", "हिम", "హిమ్"),
            MappingEntry("into", "इन्टू", "ఇంటూ"),
            MappingEntry("time", "टाइम", "టైమ్"),
            MappingEntry("has", "हैज़", "హ్యాజ్"),
            MappingEntry("look", "लुक", "లుక్"),
            MappingEntry("two", "टू", "టూ"),
            MappingEntry("more", "मोर", "మోర్"),
            MappingEntry("write", "राइट", "రైట్"),
            MappingEntry("go", "गो", "గో"),
            MappingEntry("see", "सी", "సీ"),
            MappingEntry("number", "नंबर", "నెంబర్"),
            MappingEntry("no", "नो", "నో"),
            MappingEntry("way", "वे", "వే"),
            MappingEntry("could", "कुड", "కుడ్"),
            MappingEntry("people", "पीपल", "పీపుల్"),
            MappingEntry("my", "माय", "మై"),
            MappingEntry("than", "दैन", "దాన్"),
            MappingEntry("first", "फर्स्ट", "ఫస్ట్"),
            MappingEntry("water", "वॉटर", "వాటర్"),
            MappingEntry("been", "बीन", "బీన్"),
            MappingEntry("call", "कॉल", "కాల్"),
            MappingEntry("who", "हू", "హూ"),
            MappingEntry("oil", "ऑइल", "ఆయిల్"),
            MappingEntry("its", "इट्स", "ఇట్స్"),
            MappingEntry("now", "नाउ", "నౌ"),
            MappingEntry("find", "फाइंड", "ఫైండ్"),
            MappingEntry("hello", "नमस्ते", "నమస్కారం"),
            MappingEntry("thank", "थैंक", "ధన్యవాదాలు"),
            MappingEntry("thanks", "थैंक्स", "ధన్యవాదాలు"),
            MappingEntry("please", "प्लीज़", "దయచేసి"),
            MappingEntry("sorry", "सॉरी", "క్షమించండి"),
            MappingEntry("yes", "हाँ", "అవును"),
            MappingEntry("ok", "ओके", "సరే"),
            
            // Street/Shop Sign Common Words
            MappingEntry("shop", "शॉप", "షాప్", "ਸ਼ਾਪ", "ഷോപ്പ്", "ஷாப்"),
            MappingEntry("store", "स्टोर", "స్టోర్", "ਸਟੋਰ", "സ്റ്റോർ", "ஸ்டோர்"),
            MappingEntry("open", "खुला", "తెరిచారు", "ਖੁੱਲ੍ਹਾ", "തുറന്നു", "திறந்து"),
            MappingEntry("closed", "बंद", "మూసివేసాయి", "ਬੰਦ", "അടച്ചു", "மூடியது"),
            MappingEntry("hotel", "होटल", "హోటల్", "ਹੋਟਲ", "ഹോട്ടൽ", "ஹோட்டல்"),
            MappingEntry("restaurant", "रेस्टोरेंट", "రెస్టారెంట్", "ਰੈਸਟੋਰੈਂਟ", "റെസ്റ്റോറന്റ്", "ரெஸ்டாரண்ட்"),
            MappingEntry("cafe", "कैफे", "కేఫ్", "ਕੈਫ਼ੇ", "കഫേ", "கஃபே"),
            MappingEntry("bank", "बैंक", "బ్యాంక్", "ਬੈਂਕ", "ബാങ്ക്", "வங்கி"),
            MappingEntry("atm", "एटीएम", "ఏటీఎం", "ਏਟੀਐਮ", "എടിഎം", "ஏடிஎம்"),
            MappingEntry("hospital", "अस्पताल", "ఆసుపత్రి", "ਹਸਪਤਾਲ", "ആശുപത്രി", "மருத்துவமனை"),
            MappingEntry("clinic", "क्लिनिक", "క్లినిక్", "ਕਲੀਨਿਕ", "ക്ലിനിക്", "கிளினிக்"),
            MappingEntry("pharmacy", "फार्मेसी", "ఫార్మసీ", "ਫਾਰਮੇਸੀ", "ഫാർമസി", "மருந்தகம்"),
            MappingEntry("medical", "मेडिकल", "మెడికల్", "ਮੈਡੀਕਲ", "മെഡിക്കൽ", "மருத்துவ"),
            MappingEntry("market", "बाज़ार", "మార్కెట్", "ਬਾਜ਼ਾਰ", "മാർക്കറ്റ്", "சந்தை"),
            MappingEntry("mall", "मॉल", "మాల్", "ਮਾਲ", "മാൾ", "மால்"),
            MappingEntry("parking", "पार्किंग", "పార్కింగ్", "ਪਾਰਕਿੰਗ", "പാർക്കിംഗ്", "பார்க்கிங்"),
            MappingEntry("entry", "प्रवेश", "ప్రవేశం", "ਐਂਟਰੀ", "എൻട്രി", "நுழைவு"),
            MappingEntry("exit", "निकास", "నిష్క్రమణ", "ਐਗਜ਼ਿਟ", "എക്സിറ്റ്", "வெளியேறு"),
            MappingEntry("entrance", "प्रवेश", "ప్రవేశద్వారం", "ਪ੍ਰਵੇਸ਼", "പ്രവേശനം", "நுழைவாயில்"),
            MappingEntry("school", "स्कूल", "పాఠశాల", "ਸਕੂਲ", "സ്കൂൾ", "பள்ளி"),
            MappingEntry("college", "कॉलेज", "కళాశాల", "ਕਾਲਜ", "കോളേജ്", "கல்லூரி"),
            MappingEntry("office", "ऑफिस", "కార్యాలయం", "ਦਫ਼ਤਰ", "ഓഫീസ്", "அலுவலகம்"),
            MappingEntry("police", "पुलिस", "పోలీసు", "ਪੁਲਿਸ", "പോലീസ്", "காவல்துறை"),
            MappingEntry("station", "स्टेशन", "స్టేషన్", "ਸਟੇਸ਼ਨ", "സ്റ്റേഷൻ", "நிலையம்"),
            MappingEntry("bus", "बस", "బస్", "ਬੱਸ", "ബസ്", "பேருந்து"),
            MappingEntry("taxi", "टैक्सी", "టాక్సీ", "ਟੈਕਸੀ", "ടാക്സി", "டாக்ஸி"),
            MappingEntry("petrol", "पेट्रोल", "పెట్రోల్", "ਪੈਟਰੋਲ", "പെട്രോൾ", "பெட்ரோல்"),
            MappingEntry("gas", "गैस", "గ్యాస్", "ਗੈਸ", "ഗ്യാസ്", "எரிவாயு"),
            MappingEntry("food", "खाना", "ఆహారం", "ਖਾਣਾ", "ഭക്ഷണം", "உணவு"),
            MappingEntry("drink", "पीना", "పానీయం", "ਪੀਣਾ", "പാനീയം", "பானம்"),
            MappingEntry("tea", "चाय", "టీ", "ਚਾਹ", "ചായ", "தேநீர்"),
            MappingEntry("coffee", "कॉफी", "కాఫీ", "ਕਾਫ਼ੀ", "കോഫി", "காபி"),
            MappingEntry("breakfast", "नाश्ता", "అల్పాహారం", "ਨਾਸ਼ਤਾ", "പ്രഭാതഭക്ഷണം", "காலை உணவு"),
            MappingEntry("lunch", "दोपहर का खाना", "మధ్యాహ్న భోజనం", "ਦੁਪਹਿਰ ਦਾ ਖਾਣਾ", "ഉച്ചഭക്ഷണം", "மதிய உணவு"),
            MappingEntry("dinner", "रात का खाना", "రాత్రి భోజనం", "ਰਾਤ ਦਾ ਖਾਣਾ", "അത്താഴം", "இரவு உணவு"),
            MappingEntry("bakery", "बेकरी", "బేకరీ", "ਬੇਕਰੀ", "ബേക്കറി", "பேக்கரி"),
            MappingEntry("grocery", "किराना", "కిరాణా", "ਕਿਰਾਨਾ", "പലചരക്ക്", "மளிகை"),
            MappingEntry("vegetable", "सब्जी", "కూరగాయ", "ਸਬਜ਼ੀ", "പച്ചക്കറി", "காய்கறி"),
            MappingEntry("fruit", "फल", "పండు", "ਫਲ", "പഴം", "பழம்"),
            MappingEntry("meat", "मांस", "మాంసం", "ਮੀਟ", "മാംസം", "இறைச்சி"),
            MappingEntry("fish", "मछली", "చేప", "ਮੱਛੀ", "മത്സ്യം", "மீன்"),
            MappingEntry("chicken", "चिकन", "చికెన్", "ਚਿਕਨ", "ചിക്കൻ", "கோழி"),
            MappingEntry("price", "कीमत", "ధర", "ਕੀਮਤ", "വില", "விலை"),
            MappingEntry("sale", "बिक्री", "అమ్మకం", "ਵਿਕਰੀ", "വില്പന", "விற்பனை"),
            MappingEntry("discount", "छूट", "డిస్కౌంట్", "ਛੋਟ", "കിഴിവ്", "தள்ளுபடி"),
            MappingEntry("offer", "ऑफर", "ఆఫర్", "ਪੇਸ਼ਕਸ਼", "ഓഫർ", "சலுகை"),
            MappingEntry("free", "मुफ्त", "ఉచితం", "ਮੁਫ਼ਤ", "സൗജന്യം", "இலவசம்"),
            MappingEntry("new", "नया", "కొత్త", "ਨਵਾਂ", "പുതിയ", "புதிய"),
            MappingEntry("old", "पुराना", "పాత", "ਪੁਰਾਣਾ", "പഴയ", "பழைய"),
            MappingEntry("fresh", "ताज़ा", "తాజా", "ਤਾਜ਼ਾ", "പുതിയ", "புதிய"),
            MappingEntry("hot", "गर्म", "వేడి", "ਗਰਮ", "ചൂട്", "சூடான"),
            MappingEntry("cold", "ठंडा", "చల్లని", "ਠੰਡਾ", "തണുത്ത", "குளிர்"),
            MappingEntry("toilet", "शौचालय", "టాయిలెట్", "ਟਾਇਲਟ", "ടോയ്ലറ്റ്", "கழிப்பறை"),
            MappingEntry("washroom", "वॉशरूम", "వాష్రూమ్", "ਵਾਸ਼ਰੂਮ", "വാഷ്റൂം", "கழிவறை"),
            MappingEntry("bathroom", "बाथरूम", "బాత్రూమ్", "ਬਾਥਰੂਮ", "ബാത്ത്റൂം", "குளியலறை"),
            MappingEntry("temple", "मंदिर", "ఆలయం", "ਮੰਦਰ", "ക്ഷേത്രം", "கோயில்"),
            MappingEntry("church", "चर्च", "చర్చి", "ਚਰਚ", "പള്ളി", "தேவாலயம்"),
            MappingEntry("mosque", "मस्जिद", "మసీదు", "ਮਸਜਿਦ", "പള്ളി", "மசூதி"),
            MappingEntry("salon", "सैलून", "సెలూన్", "ਸੈਲੂਨ", "സലൂൺ", "சலூன்"),
            MappingEntry("barber", "नाई", "క్షురకుడు", "ਨਾਈ", "നാപിത്", "முடிதிருத்துபவர்"),
            MappingEntry("laundry", "धोबी", "లాండ్రీ", "ਧੋਬੀ", "അലക്കുകാരൻ", "சலவை"),
            MappingEntry("tailor", "दर्जी", "దర్జీ", "ਦਰਜ਼ੀ", "തയ്യൽക്കാരൻ", "தையல்காரர்"),
            MappingEntry("repair", "मरम्मत", "రిపేర్", "ਮੁਰੰਮਤ", "റിപ്പയർ", "பழுதுபார்"),
            MappingEntry("service", "सेवा", "సేవ", "ਸੇਵਾ", "സേവനം", "சேவை"),
            MappingEntry("center", "केंद्र", "కేంద్రం", "ਕੇਂਦਰ", "കേന്ദ്രം", "மையம்"),
            MappingEntry("reception", "रिसेप्शन", "రిసెప్షన్", "ਰਿਸੈਪਸ਼ਨ", "റിസപ്ഷൻ", "வரவேற்பு"),
            MappingEntry("counter", "काउंटर", "కౌంటర్", "ਕਾਊਂਟਰ", "കൗണ്ടർ", "கவுண்டர்"),
            MappingEntry("cash", "नकद", "నగదు", "ਨਕਦ", "പണം", "பணம்"),
            MappingEntry("card", "कार्ड", "కార్డు", "ਕਾਰਡ", "കാർഡ്", "அட்டை"),
            MappingEntry("payment", "भुगतान", "చెల్లింపు", "ਭੁਗਤਾਨ", "പണമടയ്ക്കൽ", "கட்டணம்")
        )
    }

    /**
     * Returns all mapped entries combined, generating all consonant-vowel combinations
     */
    fun getAllMappings(): List<MappingEntry> {
        val vowels = getVowelMappings()
        val consonants = getConsonantMappings()
        val specialSymbols = getSpecialSymbols()
        val commonWords = getCommonEnglishWords()
        
        val computedMappings = mutableListOf<MappingEntry>()
        
        // Add common words first (highest priority)
        computedMappings.addAll(commonWords)
        
        // Add independent vowels
        computedMappings.addAll(vowels)
        
        // Add special symbols
        computedMappings.addAll(specialSymbols)
        
        // Add consonants and their combinations
        consonants.forEach { consonant ->
            // Base consonant (with 'a') - e.g., "ka" -> "क"
            computedMappings.add(consonant)
            
            // Generate combinations if the consonant ends with 'a' (which implies it's a base form)
            if (consonant.englishRoman.endsWith("a")) {
                val baseRoman = consonant.englishRoman.dropLast(1)
                if (baseRoman.isNotEmpty()) {
                    // Add Halant form (without 'a') - e.g., "k" -> "क्"
                    computedMappings.add(
                        MappingEntry(
                            englishRoman = baseRoman,
                            hindiDevanagari = consonant.hindiDevanagari + "्",
                            teluguScript = consonant.teluguScript + "్",
                            description = "Halant form of ${consonant.englishRoman}"
                        )
                    )
                    
                    // Add combinations with other vowels
                    vowels.forEach { vowel ->
                        // Skip 'a' as it's the base form already added
                        if (vowel.englishRoman != "a") {
                            val combinedRoman = baseRoman + vowel.englishRoman
                            
                            // Ensure we have matras for the vowel
                            if (vowel.hindiMatra != null && vowel.teluguMatra != null) {
                                computedMappings.add(
                                    MappingEntry(
                                        englishRoman = combinedRoman,
                                        hindiDevanagari = consonant.hindiDevanagari + vowel.hindiMatra,
                                        teluguScript = consonant.teluguScript + vowel.teluguMatra,
                                        description = "Combined ${consonant.englishRoman} + ${vowel.englishRoman}"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Generate Uppercase variants for single letters
        // e.g. if "a" -> "अ", then "A" -> "अ" (unless "A" is already defined)
        val existingRomans = computedMappings.map { it.englishRoman }.toSet()
        val uppercaseMappings = mutableListOf<MappingEntry>()
        
        computedMappings.forEach { entry ->
            if (entry.englishRoman.length == 1 && entry.englishRoman[0].isLowerCase()) {
                val upperRoman = entry.englishRoman.uppercase()
                if (!existingRomans.contains(upperRoman)) {
                    uppercaseMappings.add(entry.copy(englishRoman = upperRoman))
                }
            }
        }
        computedMappings.addAll(uppercaseMappings)
        
        return computedMappings
    }
}
