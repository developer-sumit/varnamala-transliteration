package com.sumit.varnamala.data

data class MappingEntry(
    val englishRoman: String,          // Roman/English representation (e.g., "ka")
    val hindiDevanagari: String,       // Hindi in Devanagari script (e.g., "क")
    val teluguScript: String,          // Telugu script (e.g., "క")
    val punjabiScript: String? = null, // Punjabi/Gurmukhi script (e.g., "ਕ")
    val malayalamScript: String? = null, // Malayalam script (e.g., "ക")
    val hindiMatra: String? = null,    // Hindi matra/vowel modifier if applicable
    val teluguMatra: String? = null,   // Telugu matra/vowel modifier if applicable
    val punjabiMatra: String? = null,  // Punjabi matra/vowel modifier if applicable
    val malayalamMatra: String? = null, // Malayalam matra/vowel modifier if applicable
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
                description = "Inherent 'a' sound"
            ),
            MappingEntry(
                englishRoman = "aa",
                hindiDevanagari = "आ",
                teluguScript = "ఆ",
                punjabiScript = "ਆ",
                malayalamScript = "ആ",
                hindiMatra = "ा",
                teluguMatra = "ా",
                punjabiMatra = "ਾ",
                malayalamMatra = "ാ",
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
                description = "Unvoiced velar stop"
            ),
            MappingEntry(
                englishRoman = "kha",
                hindiDevanagari = "ख",
                teluguScript = "ఖ",
                punjabiScript = "ਖ",
                malayalamScript = "ഖ",
                description = "Aspirated unvoiced velar stop"
            ),
            MappingEntry(
                englishRoman = "ga",
                hindiDevanagari = "ग",
                teluguScript = "గ",
                punjabiScript = "ਗ",
                malayalamScript = "ഗ",
                description = "Voiced velar stop"
            ),
            MappingEntry(
                englishRoman = "gha",
                hindiDevanagari = "घ",
                teluguScript = "ఘ",
                punjabiScript = "ਘ",
                malayalamScript = "ഘ",
                description = "Aspirated voiced velar stop"
            ),
            MappingEntry(
                englishRoman = "nga",
                hindiDevanagari = "ङ",
                teluguScript = "ఙ",
                punjabiScript = "ਙ",
                malayalamScript = "ങ",
                description = "Nasal velar stop"
            ),

            // Palatal consonants (Chavya Varga)
            MappingEntry(
                englishRoman = "cha",
                hindiDevanagari = "च",
                teluguScript = "చ",
                punjabiScript = "ਚ",
                malayalamScript = "ച",
                description = "Unvoiced palatal stop"
            ),
            MappingEntry(
                englishRoman = "chha",
                hindiDevanagari = "छ",
                teluguScript = "ఛ",
                punjabiScript = "ਛ",
                malayalamScript = "ഛ",
                description = "Aspirated unvoiced palatal stop"
            ),
            MappingEntry(
                englishRoman = "ja",
                hindiDevanagari = "ज",
                teluguScript = "జ",
                punjabiScript = "ਜ",
                malayalamScript = "ജ",
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
            // Numbers
            MappingEntry("0", "०", "౦", "੦", "൦"),
            MappingEntry("1", "१", "౧", "੧", "൧"),
            MappingEntry("2", "२", "౨", "੨", "൨"),
            MappingEntry("3", "३", "౩", "੩", "൩"),
            MappingEntry("4", "४", "౪", "੪", "൪"),
            MappingEntry("5", "५", "౫", "੫", "൫"),
            MappingEntry("6", "६", "౬", "੬", "൬"),
            MappingEntry("7", "७", "౭", "੭", "൭"),
            MappingEntry("8", "८", "౮", "੮", "൮"),
            MappingEntry("9", "९", "౯", "੯", "൯"),
            
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
            MappingEntry("ok", "ओके", "సరే")
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

    /**
     * Find mapping by English Roman transliteration
     */
    fun getMappingByEnglish(englishRoman: String): MappingEntry? {
        return getAllMappings().find { it.englishRoman.equals(englishRoman, ignoreCase = true) }
    }

    /**
     * Find mapping by Hindi Devanagari script
     */
    fun getMappingByHindi(hindiDevanagari: String): MappingEntry? {
        return getAllMappings().find { it.hindiDevanagari == hindiDevanagari }
    }

    /**
     * Find mapping by Telugu script
     */
    fun getMappingByTelugu(teluguScript: String): MappingEntry? {
        return getAllMappings().find { it.teluguScript == teluguScript }
    }

    /**
     * Get all Hindi characters available in mappings
     */
    fun getAllHindiCharacters(): List<String> {
        return getAllMappings().map { it.hindiDevanagari }.distinct()
    }

    /**
     * Get all Telugu characters available in mappings
     */
    fun getAllTeluguCharacters(): List<String> {
        return getAllMappings().map { it.teluguScript }.distinct()
    }

    /**
     * Get all English Roman transliterations available in mappings
     */
    fun getAllEnglishTransliterations(): List<String> {
        return getAllMappings().map { it.englishRoman }.distinct()
    }
}
