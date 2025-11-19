package com.sumit.varnamala.utils

object Transliterator {

    /**
     * Language codes for supported languages
     */
    object Languages {
        const val ENGLISH = "en"
        const val HINDI = "hi"
        const val PUNJABI = "pa"
        const val BENGALI = "bn"
        const val GUJARATI = "gu"
        const val TAMIL = "ta"
        const val TELUGU = "te"
        
        val ALL = listOf(ENGLISH, HINDI, PUNJABI)
        
        fun getDisplayName(code: String): String = when (code) {
            ENGLISH -> "English"
            HINDI -> "Hindi"
            PUNJABI -> "Punjabi"
            BENGALI -> "Bengali"
            GUJARATI -> "Gujarati"
            TAMIL -> "Tamil"
            TELUGU -> "Telugu"
            else -> code.uppercase()
        }
    }

    /**
     * Transliterates text from one script to another based on the source and target languages
     */
    fun transliterate(
        text: String,
        sourceLanguage: String?,
        targetLanguage: String = Languages.ENGLISH
    ): String {
        if (text.isBlank() || sourceLanguage == null || sourceLanguage == targetLanguage) {
            return text
        }

        // First convert to Latin (English) if needed, then to target language
        val latinText = when (sourceLanguage.lowercase()) {
            "hindi", "hi", "devanagari" -> hindiToEnglish(text)
            "bengali", "bn" -> bengaliToEnglish(text)
            "gujarati", "gu" -> gujaratiToEnglish(text)
            "punjabi", "pa" -> punjabiToEnglish(text)
            "tamil", "ta" -> tamilToEnglish(text)
            "telugu", "te" -> teluguToEnglish(text)
            "en", "english" -> text
            else -> text
        }
        
        // If target is English (Latin), we're done
        if (targetLanguage.lowercase() == "en" || targetLanguage.lowercase() == "english") {
            return latinText
        }
        
        // Otherwise, convert from Latin to target language
        return when (targetLanguage.lowercase()) {
            "hindi", "hi", "devanagari" -> englishToHindi(latinText)
            "punjabi", "pa" -> englishToPunjabi(latinText)
            else -> latinText // Default to Latin if target language not supported
        }
    }

    private fun hindiToEnglish(text: String): String {
        val hindiToEnglishMap = mapOf(
            // Vowels
            'अ' to "a", 'आ' to "aa", 'इ' to "i", 'ई' to "ii", 'उ' to "u", 'ऊ' to "uu",
            'ऋ' to "ri", 'ए' to "e", 'ऐ' to "ai", 'ओ' to "o", 'औ' to "au",

            // Consonants
            'क' to "ka", 'ख' to "kha", 'ग' to "ga", 'घ' to "gha", 'ङ' to "nga",
            'च' to "cha", 'छ' to "chha", 'ज' to "ja", 'झ' to "jha", 'ञ' to "nya",
            'ट' to "ta", 'ठ' to "tha", 'ड' to "da", 'ढ' to "dha", 'ण' to "na",
            'त' to "ta", 'थ' to "tha", 'द' to "da", 'ध' to "dha", 'न' to "na",
            'प' to "pa", 'फ' to "pha", 'ब' to "ba", 'भ' to "bha", 'म' to "ma",
            'य' to "ya", 'र' to "ra", 'ल' to "la", 'व' to "va", 'श' to "sha",
            'ष' to "sha", 'स' to "sa", 'ह' to "ha",

            // Matras
            'ा' to "aa", 'ि' to "i", 'ी' to "ii", 'ु' to "u", 'ू' to "uu",
            'ृ' to "ri", 'े' to "e", 'ै' to "ai", 'ो' to "o", 'ौ' to "au",
            '्' to "", // Halant (virama)

            // Numbers
            '०' to "0", '१' to "1", '२' to "2", '३' to "3", '४' to "4",
            '५' to "5", '६' to "6", '७' to "7", '८' to "8", '९' to "9"
        )

        return text.map { char ->
            hindiToEnglishMap[char] ?: char.toString()
        }.joinToString("")
    }

    private fun bengaliToEnglish(text: String): String {
        val bengaliToEnglishMap = mapOf(
            // Vowels
            'অ' to "a", 'আ' to "aa", 'ই' to "i", 'ঈ' to "ii", 'উ' to "u", 'ঊ' to "uu",
            'ঋ' to "ri", 'এ' to "e", 'ঐ' to "ai", 'ও' to "o", 'ঔ' to "au",

            // Consonants
            'ক' to "ka", 'খ' to "kha", 'গ' to "ga", 'ঘ' to "gha", 'ঙ' to "nga",
            'চ' to "cha", 'ছ' to "chha", 'জ' to "ja", 'ঝ' to "jha", 'ঞ' to "nya",
            'ট' to "ta", 'ঠ' to "tha", 'ড' to "da", 'ঢ' to "dha", 'ণ' to "na",
            'ত' to "ta", 'থ' to "tha", 'দ' to "da", 'ধ' to "dha", 'ন' to "na",
            'প' to "pa", 'ফ' to "pha", 'ব' to "ba", 'ভ' to "bha", 'ম' to "ma",
            'য' to "ya", 'র' to "ra", 'ল' to "la", 'শ' to "sha", 'ষ' to "sha",
            'স' to "sa", 'হ' to "ha",

            // Matras
            'া' to "aa", 'ি' to "i", 'ী' to "ii", 'ু' to "u", 'ূ' to "uu",
            'ৃ' to "ri", 'ে' to "e", 'ৈ' to "ai", 'ো' to "o", 'ৌ' to "au",
            '্' to "", // Halant

            // Numbers
            '০' to "0", '১' to "1", '২' to "2", '৩' to "3", '৪' to "4",
            '৫' to "5", '৬' to "6", '৭' to "7", '৮' to "8", '৯' to "9"
        )

        return text.map { char ->
            bengaliToEnglishMap[char] ?: char.toString()
        }.joinToString("")
    }

    private fun gujaratiToEnglish(text: String): String {
        val gujaratiToEnglishMap = mapOf(
            // Vowels
            'અ' to "a", 'આ' to "aa", 'ઇ' to "i", 'ઈ' to "ii", 'ઉ' to "u", 'ઊ' to "uu",
            'ઋ' to "ri", 'એ' to "e", 'ઐ' to "ai", 'ઓ' to "o", 'ઔ' to "au",

            // Consonants
            'ક' to "ka", 'ખ' to "kha", 'ગ' to "ga", 'ઘ' to "gha", 'ઙ' to "nga",
            'ચ' to "cha", 'છ' to "chha", 'જ' to "ja", 'ઝ' to "jha", 'ઞ' to "nya",
            'ટ' to "ta", 'ઠ' to "tha", 'ડ' to "da", 'ઢ' to "dha", 'ણ' to "na",
            'ત' to "ta", 'થ' to "tha", 'દ' to "da", 'ધ' to "dha", 'ન' to "na",
            'પ' to "pa", 'ફ' to "pha", 'બ' to "ba", 'ભ' to "bha", 'મ' to "ma",
            'ય' to "ya", 'ર' to "ra", 'લ' to "la", 'વ' to "va", 'શ' to "sha",
            'ષ' to "sha", 'સ' to "sa", 'હ' to "ha",

            // Matras
            'ા' to "aa", 'િ' to "i", 'ી' to "ii", 'ુ' to "u", 'ૂ' to "uu",
            'ૃ' to "ri", 'ે' to "e", 'ૈ' to "ai", 'ો' to "o", 'ૌ' to "au",
            '્' to "", // Halant

            // Numbers
            '૦' to "0", '૧' to "1", '૨' to "2", '૩' to "3", '૪' to "4",
            '૫' to "5", '૬' to "6", '૭' to "7", '૮' to "8", '૯' to "9"
        )

        return text.map { char ->
            gujaratiToEnglishMap[char] ?: char.toString()
        }.joinToString("")
    }

    private fun punjabiToEnglish(text: String): String {
        val punjabiToEnglishMap = mapOf(
            // Vowels
            'ਅ' to "a", 'ਆ' to "aa", 'ਇ' to "i", 'ਈ' to "ii", 'ਉ' to "u", 'ਊ' to "uu",
            'ਏ' to "e", 'ਐ' to "ai", 'ਓ' to "o", 'ਔ' to "au",

            // Consonants
            'ਕ' to "ka", 'ਖ' to "kha", 'ਗ' to "ga", 'ਘ' to "gha", 'ਙ' to "nga",
            'ਚ' to "cha", 'ਛ' to "chha", 'ਜ' to "ja", 'ਝ' to "jha", 'ਞ' to "nya",
            'ਟ' to "ta", 'ਠ' to "tha", 'ਡ' to "da", 'ਢ' to "dha", 'ਣ' to "na",
            'ਤ' to "ta", 'ਥ' to "tha", 'ਦ' to "da", 'ਧ' to "dha", 'ਨ' to "na",
            'ਪ' to "pa", 'ਫ' to "pha", 'ਬ' to "ba", 'ਭ' to "bha", 'ਮ' to "ma",
            'ਯ' to "ya", 'ਰ' to "ra", 'ਲ' to "la", 'ਵ' to "va",
            'ਸ' to "sa", 'ਹ' to "ha",

            // Matras
            'ਾ' to "aa", 'ਿ' to "i", 'ੀ' to "ii", 'ੁ' to "u", 'ੂ' to "uu",
            'ੇ' to "e", 'ੈ' to "ai", 'ੋ' to "o", 'ੌ' to "au",
            '੍' to "", // Halant

            // Numbers
            '੦' to "0", '੧' to "1", '੨' to "2", '੩' to "3", '੪' to "4",
            '੫' to "5", '੬' to "6", '੭' to "7", '੮' to "8", '੯' to "9"
        )

        return text.map { char ->
            punjabiToEnglishMap[char] ?: char.toString()
        }.joinToString("")
    }

    private fun tamilToEnglish(text: String): String {
        val tamilToEnglishMap = mapOf(
            // Vowels
            'அ' to "a", 'ஆ' to "aa", 'இ' to "i", 'ஈ' to "ii", 'உ' to "u", 'ஊ' to "uu",
            'எ' to "e", 'ஏ' to "ee", 'ஐ' to "ai", 'ஒ' to "o", 'ஓ' to "oo", 'ஔ' to "au",

            // Consonants
            'க' to "ka", 'ங' to "nga", 'ச' to "cha", 'ஞ' to "nya", 'ட' to "ta",
            'ண' to "na", 'த' to "tha", 'ந' to "na", 'ப' to "pa", 'ம' to "ma",
            'ய' to "ya", 'ர' to "ra", 'ல' to "la", 'வ' to "va", 'ழ' to "zha",
            'ள' to "la", 'ற' to "ra", 'ன' to "na",

            // Matras
            'ா' to "aa", 'ி' to "i", 'ீ' to "ii", 'ு' to "u", 'ூ' to "uu",
            'ெ' to "e", 'ே' to "ee", 'ை' to "ai", 'ொ' to "o", 'ோ' to "oo", 'ௌ' to "au",
            '்' to "", // Halant

            // Numbers
            '௦' to "0", '௧' to "1", '௨' to "2", '௩' to "3", '௪' to "4",
            '௫' to "5", '௬' to "6", '௭' to "7", '௮' to "8", '௯' to "9"
        )

        return text.map { char ->
            tamilToEnglishMap[char] ?: char.toString()
        }.joinToString("")
    }

    private fun teluguToEnglish(text: String): String {
        val teluguToEnglishMap = mapOf(
            // Vowels
            'అ' to "a", 'ఆ' to "aa", 'ఇ' to "i", 'ఈ' to "ii", 'ఉ' to "u", 'ఊ' to "uu",
            'ఋ' to "ri", 'ఎ' to "e", 'ఏ' to "ee", 'ఐ' to "ai", 'ఒ' to "o", 'ఓ' to "oo", 'ఔ' to "au",

            // Consonants
            'క' to "ka", 'ఖ' to "kha", 'గ' to "ga", 'ఘ' to "gha", 'ఙ' to "nga",
            'చ' to "cha", 'ఛ' to "chha", 'జ' to "ja", 'ఝ' to "jha", 'ఞ' to "nya",
            'ట' to "ta", 'ఠ' to "tha", 'డ' to "da", 'ఢ' to "dha", 'ణ' to "na",
            'త' to "ta", 'థ' to "tha", 'ద' to "da", 'ధ' to "dha", 'న' to "na",
            'ప' to "pa", 'ఫ' to "pha", 'బ' to "ba", 'భ' to "bha", 'మ' to "ma",
            'య' to "ya", 'ర' to "ra", 'ల' to "la", 'వ' to "va", 'శ' to "sha",
            'ష' to "sha", 'స' to "sa", 'హ' to "ha",

            // Matras
            'ా' to "aa", 'ి' to "i", 'ీ' to "ii", 'ు' to "u", 'ూ' to "uu",
            'ృ' to "ri", 'ె' to "e", 'ే' to "ee", 'ై' to "ai", 'ొ' to "o", 'ో' to "oo", 'ౌ' to "au",
            '్' to "", // Halant

            // Numbers
            '౦' to "0", '౧' to "1", '౨' to "2", '౩' to "3", '౪' to "4",
            '౫' to "5", '౬' to "6", '౭' to "7", '౮' to "8", '౯' to "9"
        )

        return text.map { char ->
            teluguToEnglishMap[char] ?: char.toString()
        }.joinToString("")
    }

    /**
     * Get supported target languages for a given source language
     */
    fun getSupportedTargetLanguages(sourceLanguage: String?): List<String> {
        if (sourceLanguage == null) return emptyList()
        
        // Currently we support conversion from any language to English, Hindi and Punjabi
        return listOf(Languages.ENGLISH, Languages.HINDI, Languages.PUNJABI)
    }
    
    /**
     * Converts English text to Hindi (Devanagari)
     */
    private fun englishToHindi(text: String): String {
        // Prepare a more comprehensive mapping for transliteration
        val englishToHindiMap = mapOf(
            // Vowels
            "a" to "अ", "aa" to "आ", "i" to "इ", "ee" to "ई", "ii" to "ई", 
            "u" to "उ", "oo" to "ऊ", "uu" to "ऊ", "ri" to "ऋ", 
            "e" to "ए", "ai" to "ऐ", "o" to "ओ", "au" to "औ", "ou" to "औ",
            
            // Consonants with vowel combinations
            "ka" to "क", "kaa" to "का", "ki" to "कि", "kee" to "की", "ku" to "कु", "koo" to "कू", 
            "ke" to "के", "kai" to "कै", "ko" to "को", "kau" to "कौ",
            
            "kha" to "ख", "khaa" to "खा", "khi" to "खि", "khee" to "खी", "khu" to "खु", "khoo" to "खू",
            "khe" to "खे", "khai" to "खै", "kho" to "खो", "khau" to "खौ",
            
            "ga" to "ग", "gaa" to "गा", "gi" to "गि", "gee" to "गी", "gu" to "गु", "goo" to "गू",
            "ge" to "गे", "gai" to "गै", "go" to "गो", "gau" to "गौ",
            
            "gha" to "घ", "ghaa" to "घा", "ghi" to "घि", "ghee" to "घी", "ghu" to "घु", "ghoo" to "घू",
            "ghe" to "घे", "ghai" to "घै", "gho" to "घो", "ghau" to "घौ",
            
            "nga" to "ङ",
            
            "cha" to "च", "chaa" to "चा", "chi" to "चि", "chee" to "ची", "chu" to "चु", "choo" to "चू",
            "che" to "चे", "chai" to "चै", "cho" to "चो", "chau" to "चौ",
            
            "chha" to "छ", "chhaa" to "छा", "chhi" to "छि", "chhee" to "छी", "chhu" to "छु", "chhoo" to "छू",
            "chhe" to "छे", "chhai" to "छै", "chho" to "छो", "chhau" to "छौ",
            
            "ja" to "ज", "jaa" to "जा", "ji" to "जि", "jee" to "जी", "ju" to "जु", "joo" to "जू",
            "je" to "जे", "jai" to "जै", "jo" to "जो", "jau" to "जौ",
            
            "jha" to "झ", "jhaa" to "झा", "jhi" to "झि", "jhee" to "झी", "jhu" to "झु", "jhoo" to "झू",
            "jhe" to "झे", "jhai" to "झै", "jho" to "झो", "jhau" to "झौ",
            
            "nya" to "ञ",
            
            "ta" to "त", "taa" to "ता", "ti" to "ति", "tee" to "ती", "tu" to "तु", "too" to "तू",
            "te" to "ते", "tai" to "तै", "to" to "तो", "tau" to "तौ",
            
            "tha" to "थ", "thaa" to "था", "thi" to "थि", "thee" to "थी", "thu" to "थु", "thoo" to "थू",
            "the" to "थे", "thai" to "थै", "tho" to "थो", "thau" to "थौ",
            
            "da" to "द", "daa" to "दा", "di" to "दि", "dee" to "दी", "du" to "दु", "doo" to "दू",
            "de" to "दे", "dai" to "दै", "do" to "दो", "dau" to "दौ",
            
            "dha" to "ध", "dhaa" to "धा", "dhi" to "धि", "dhee" to "धी", "dhu" to "धु", "dhoo" to "धू",
            "dhe" to "धे", "dhai" to "धै", "dho" to "धो", "dhau" to "धौ",
            
            "na" to "न", "naa" to "ना", "ni" to "नि", "nee" to "नी", "nu" to "नु", "noo" to "नू",
            "ne" to "ने", "nai" to "नै", "no" to "नो", "nau" to "नौ",
            
            "pa" to "प", "paa" to "पा", "pi" to "पि", "pee" to "पी", "pu" to "पु", "poo" to "पू",
            "pe" to "पे", "pai" to "पै", "po" to "पो", "pau" to "पौ",
            
            "fa" to "फ", "faa" to "फा", "fi" to "फि", "fee" to "फी", "fu" to "फु", "foo" to "फू",
            "fe" to "फे", "fai" to "फै", "fo" to "फो", "fau" to "फौ",
            
            "pha" to "फ", "phaa" to "फा", "phi" to "फि", "phee" to "फी", "phu" to "फु", "phoo" to "फू",
            "phe" to "फे", "phai" to "फै", "pho" to "फो", "phau" to "फौ",
            
            "ba" to "ब", "baa" to "बा", "bi" to "बि", "bee" to "बी", "bu" to "बु", "boo" to "बू",
            "be" to "बे", "bai" to "बै", "bo" to "बो", "bau" to "बौ",
            
            "bha" to "भ", "bhaa" to "भा", "bhi" to "भि", "bhee" to "भी", "bhu" to "भु", "bhoo" to "भू",
            "bhe" to "भे", "bhai" to "भै", "bho" to "भो", "bhau" to "भौ",
            
            "ma" to "म", "maa" to "मा", "mi" to "मि", "mee" to "मी", "mu" to "मु", "moo" to "मू",
            "me" to "मे", "mai" to "मै", "mo" to "मो", "mau" to "मौ",
            
            "ya" to "य", "yaa" to "या", "yi" to "यि", "yee" to "यी", "yu" to "यु", "yoo" to "यू",
            "ye" to "ये", "yai" to "यै", "yo" to "यो", "yau" to "यौ",
            
            "ra" to "र", "raa" to "रा", "ri" to "रि", "ree" to "री", "ru" to "रु", "roo" to "रू",
            "re" to "रे", "rai" to "रै", "ro" to "रो", "rau" to "रौ",
            
            "la" to "ल", "laa" to "ला", "li" to "लि", "lee" to "ली", "lu" to "लु", "loo" to "लू",
            "le" to "ले", "lai" to "लै", "lo" to "लो", "lau" to "लौ",
            
            "va" to "व", "vaa" to "वा", "vi" to "वि", "vee" to "वी", "vu" to "वु", "voo" to "वू",
            "ve" to "वे", "vai" to "वै", "vo" to "वो", "vau" to "वौ",
            
            "wa" to "व", "waa" to "वा", "wi" to "वि", "wee" to "वी", "wu" to "वु", "woo" to "वू",
            "we" to "वे", "wai" to "वै", "wo" to "वो", "wau" to "वौ",
            
            "sha" to "श", "shaa" to "शा", "shi" to "शि", "shee" to "शी", "shu" to "शु", "shoo" to "शू",
            "she" to "शे", "shai" to "शै", "sho" to "शो", "shau" to "शौ",
            
            "sa" to "स", "saa" to "सा", "si" to "सि", "see" to "सी", "su" to "सु", "soo" to "सू",
            "se" to "से", "sai" to "सै", "so" to "सो", "sau" to "सौ",
            
            "ha" to "ह", "haa" to "हा", "hi" to "हि", "hee" to "ही", "hu" to "हु", "hoo" to "हू",
            "he" to "हे", "hai" to "है", "ho" to "हो", "hau" to "हौ",
            
            // Common half forms
            "k" to "क्", "kh" to "ख्", "g" to "ग्", "gh" to "घ्", "ch" to "च्",
            "chh" to "छ्", "j" to "ज्", "jh" to "झ्", "t" to "त्", "th" to "थ्",
            "d" to "द्", "dh" to "ध्", "n" to "न्", "p" to "प्", "ph" to "फ्",
            "f" to "फ्", "b" to "ब्", "bh" to "भ्", "m" to "म्", "y" to "य्",
            "r" to "र्", "l" to "ल्", "v" to "व्", "w" to "व्", "sh" to "श्",
            "s" to "स्", "h" to "ह्",
            
            // Numbers
            "0" to "०", "1" to "१", "2" to "२", "3" to "३", "4" to "४",
            "5" to "५", "6" to "६", "7" to "७", "8" to "८", "9" to "९",
            
            // Common words
            "namaste" to "नमस्ते", "aap" to "आप", "kaise" to "कैसे", "hain" to "हैं",
            "dhanyavad" to "धन्यवाद", "shubh" to "शुभ", "prem" to "प्रेम"
        )
        
        // Handle spaces and word boundaries
        val words = text.lowercase().split(" ")
        return words.joinToString(" ") { word ->
            var result = word
            
            // Try to match whole words first
            if (englishToHindiMap.containsKey(word)) {
                return@joinToString englishToHindiMap[word] ?: word
            }
            
            // Otherwise do token substitution from longest to shortest
            val sortedMappings = englishToHindiMap.entries.sortedByDescending { it.key.length }
            for ((english, hindi) in sortedMappings) {
                result = result.replace(english, hindi)
            }
            
            result
        }
    }
    
    /**
     * Converts English text to Punjabi (Gurmukhi)
     */
    private fun englishToPunjabi(text: String): String {
        // Prepare a more comprehensive mapping for transliteration
        val englishToPunjabiMap = mapOf(
            // Vowels
            "a" to "ਅ", "aa" to "ਆ", "i" to "ਇ", "ee" to "ਈ", "ii" to "ਈ", 
            "u" to "ਉ", "oo" to "ਊ", "uu" to "ਊ", 
            "e" to "ਏ", "ai" to "ਐ", "o" to "ਓ", "au" to "ਔ", "ou" to "ਔ",
            
            // Consonants with vowel combinations
            "ka" to "ਕ", "kaa" to "ਕਾ", "ki" to "ਕਿ", "kee" to "ਕੀ", "ku" to "ਕੁ", "koo" to "ਕੂ", 
            "ke" to "ਕੇ", "kai" to "ਕੈ", "ko" to "ਕੋ", "kau" to "ਕੌ",
            
            "kha" to "ਖ", "khaa" to "ਖਾ", "khi" to "ਖਿ", "khee" to "ਖੀ", "khu" to "ਖੁ", "khoo" to "ਖੂ",
            "khe" to "ਖੇ", "khai" to "ਖੈ", "kho" to "ਖੋ", "khau" to "ਖੌ",
            
            "ga" to "ਗ", "gaa" to "ਗਾ", "gi" to "ਗਿ", "gee" to "ਗੀ", "gu" to "ਗੁ", "goo" to "ਗੂ",
            "ge" to "ਗੇ", "gai" to "ਗੈ", "go" to "ਗੋ", "gau" to "ਗੌ",
            
            "gha" to "ਘ", "ghaa" to "ਘਾ", "ghi" to "ਘਿ", "ghee" to "ਘੀ", "ghu" to "ਘੁ", "ghoo" to "ਘੂ",
            "ghe" to "ਘੇ", "ghai" to "ਘੈ", "gho" to "ਘੋ", "ghau" to "ਘੌ",
            
            "nga" to "ਙ",
            
            "cha" to "ਚ", "chaa" to "ਚਾ", "chi" to "ਚਿ", "chee" to "ਚੀ", "chu" to "ਚੁ", "choo" to "ਚੂ",
            "che" to "ਚੇ", "chai" to "ਚੈ", "cho" to "ਚੋ", "chau" to "ਚੌ",
            
            "chha" to "ਛ", "chhaa" to "ਛਾ", "chhi" to "ਛਿ", "chhee" to "ਛੀ", "chhu" to "ਛੁ", "chhoo" to "ਛੂ",
            "chhe" to "ਛੇ", "chhai" to "ਛੈ", "chho" to "ਛੋ", "chhau" to "ਛੌ",
            
            "ja" to "ਜ", "jaa" to "ਜਾ", "ji" to "ਜਿ", "jee" to "ਜੀ", "ju" to "ਜੁ", "joo" to "ਜੂ",
            "je" to "ਜੇ", "jai" to "ਜੈ", "jo" to "ਜੋ", "jau" to "ਜੌ",
            
            "jha" to "ਝ", "jhaa" to "ਝਾ", "jhi" to "ਝਿ", "jhee" to "ਝੀ", "jhu" to "ਝੁ", "jhoo" to "ਝੂ",
            "jhe" to "ਝੇ", "jhai" to "ਝੈ", "jho" to "ਝੋ", "jhau" to "ਝੌ",
            
            "nya" to "ਞ",
            
            "ta" to "ਤ", "taa" to "ਤਾ", "ti" to "ਤਿ", "tee" to "ਤੀ", "tu" to "ਤੁ", "too" to "ਤੂ",
            "te" to "ਤੇ", "tai" to "ਤੈ", "to" to "ਤੋ", "tau" to "ਤੌ",
            
            "tha" to "ਥ", "thaa" to "ਥਾ", "thi" to "ਥਿ", "thee" to "ਥੀ", "thu" to "ਥੁ", "thoo" to "ਥੂ",
            "the" to "ਥੇ", "thai" to "ਥੈ", "tho" to "ਥੋ", "thau" to "ਥੌ",
            
            "da" to "ਦ", "daa" to "ਦਾ", "di" to "ਦਿ", "dee" to "ਦੀ", "du" to "ਦੁ", "doo" to "ਦੂ",
            "de" to "ਦੇ", "dai" to "ਦੈ", "do" to "ਦੋ", "dau" to "ਦੌ",
            
            "dha" to "ਧ", "dhaa" to "ਧਾ", "dhi" to "ਧਿ", "dhee" to "ਧੀ", "dhu" to "ਧੁ", "dhoo" to "ਧੂ",
            "dhe" to "ਧੇ", "dhai" to "ਧੈ", "dho" to "ਧੋ", "dhau" to "ਧੌ",
            
            "na" to "ਨ", "naa" to "ਨਾ", "ni" to "ਨਿ", "nee" to "ਨੀ", "nu" to "ਨੁ", "noo" to "ਨੂ",
            "ne" to "ਨੇ", "nai" to "ਨੈ", "no" to "ਨੋ", "nau" to "ਨੌ",
            
            "pa" to "ਪ", "paa" to "ਪਾ", "pi" to "ਪਿ", "pee" to "ਪੀ", "pu" to "ਪੁ", "poo" to "ਪੂ",
            "pe" to "ਪੇ", "pai" to "ਪੈ", "po" to "ਪੋ", "pau" to "ਪੌ",
            
            "fa" to "ਫ", "faa" to "ਫਾ", "fi" to "ਫਿ", "fee" to "ਫੀ", "fu" to "ਫੁ", "foo" to "ਫੂ",
            "fe" to "ਫੇ", "fai" to "ਫੈ", "fo" to "ਫੋ", "fau" to "ਫੌ",
            
            "pha" to "ਫ", "phaa" to "ਫਾ", "phi" to "ਫਿ", "phee" to "ਫੀ", "phu" to "ਫੁ", "phoo" to "ਫੂ",
            "phe" to "ਫੇ", "phai" to "ਫੈ", "pho" to "ਫੋ", "phau" to "ਫੌ",
            
            "ba" to "ਬ", "baa" to "ਬਾ", "bi" to "ਬਿ", "bee" to "ਬੀ", "bu" to "ਬੁ", "boo" to "ਬੂ",
            "be" to "ਬੇ", "bai" to "ਬੈ", "bo" to "ਬੋ", "bau" to "ਬੌ",
            
            "bha" to "ਭ", "bhaa" to "ਭਾ", "bhi" to "ਭਿ", "bhee" to "ਭੀ", "bhu" to "ਭੁ", "bhoo" to "ਭੂ",
            "bhe" to "ਭੇ", "bhai" to "ਭੈ", "bho" to "ਭੋ", "bhau" to "ਭੌ",
            
            "ma" to "ਮ", "maa" to "ਮਾ", "mi" to "ਮਿ", "mee" to "ਮੀ", "mu" to "ਮੁ", "moo" to "ਮੂ",
            "me" to "ਮੇ", "mai" to "ਮੈ", "mo" to "ਮੋ", "mau" to "ਮੌ",
            
            "ya" to "ਯ", "yaa" to "ਯਾ", "yi" to "ਯਿ", "yee" to "ਯੀ", "yu" to "ਯੁ", "yoo" to "ਯੂ",
            "ye" to "ਯੇ", "yai" to "ਯੈ", "yo" to "ਯੋ", "yau" to "ਯੌ",
            
            "ra" to "ਰ", "raa" to "ਰਾ", "ri" to "ਰਿ", "ree" to "ਰੀ", "ru" to "ਰੁ", "roo" to "ਰੂ",
            "re" to "ਰੇ", "rai" to "ਰੈ", "ro" to "ਰੋ", "rau" to "ਰੌ",
            
            "la" to "ਲ", "laa" to "ਲਾ", "li" to "ਲਿ", "lee" to "ਲੀ", "lu" to "ਲੁ", "loo" to "ਲੂ",
            "le" to "ਲੇ", "lai" to "ਲੈ", "lo" to "ਲੋ", "lau" to "ਲੌ",
            
            "va" to "ਵ", "vaa" to "ਵਾ", "vi" to "ਵਿ", "vee" to "ਵੀ", "vu" to "ਵੁ", "voo" to "ਵੂ",
            "ve" to "ਵੇ", "vai" to "ਵੈ", "vo" to "ਵੋ", "vau" to "ਵੌ",
            
            "wa" to "ਵ", "waa" to "ਵਾ", "wi" to "ਵਿ", "wee" to "ਵੀ", "wu" to "ਵੁ", "woo" to "ਵੂ",
            "we" to "ਵੇ", "wai" to "ਵੈ", "wo" to "ਵੋ", "wau" to "ਵੌ",
            
            "sa" to "ਸ", "saa" to "ਸਾ", "si" to "ਸਿ", "see" to "ਸੀ", "su" to "ਸੁ", "soo" to "ਸੂ",
            "se" to "ਸੇ", "sai" to "ਸੈ", "so" to "ਸੋ", "sau" to "ਸੌ",
            
            "ha" to "ਹ", "haa" to "ਹਾ", "hi" to "ਹਿ", "hee" to "ਹੀ", "hu" to "ਹੁ", "hoo" to "ਹੂ",
            "he" to "ਹੇ", "hai" to "ਹੈ", "ho" to "ਹੋ", "hau" to "ਹੌ",
            
            // Common half forms
            "k" to "ਕ੍", "kh" to "ਖ੍", "g" to "ਗ੍", "gh" to "ਘ੍", "ch" to "ਚ੍",
            "chh" to "ਛ੍", "j" to "ਜ੍", "jh" to "ਝ੍", "t" to "ਤ੍", "th" to "ਥ੍",
            "d" to "ਦ੍", "dh" to "ਧ੍", "n" to "ਨ੍", "p" to "ਪ੍", "ph" to "ਫ੍",
            "f" to "ਫ੍", "b" to "ਬ੍", "bh" to "ਭ੍", "m" to "ਮ੍", "y" to "ਯ੍",
            "r" to "ਰ੍", "l" to "ਲ੍", "v" to "ਵ੍", "w" to "ਵ੍", "s" to "ਸ੍", "h" to "ਹ੍",
            
            // Numbers
            "0" to "੦", "1" to "੧", "2" to "੨", "3" to "੩", "4" to "੪",
            "5" to "੫", "6" to "੬", "7" to "੭", "8" to "੮", "9" to "੯",
            
            // Common words
            "sat sri akal" to "ਸਤ ਸ੍ਰੀ ਅਕਾਲ", "guru" to "ਗੁਰੂ", "waheguru" to "ਵਾਹਿਗੁਰੂ",
            "punjab" to "ਪੰਜਾਬ", "sikh" to "ਸਿੱਖ", "khalsa" to "ਖਾਲਸਾ"
        )
        
        // Handle spaces and word boundaries
        val words = text.lowercase().split(" ")
        return words.joinToString(" ") { word ->
            var result = word
            
            // Try to match whole words first
            if (englishToPunjabiMap.containsKey(word)) {
                return@joinToString englishToPunjabiMap[word] ?: word
            }
            
            // Otherwise do token substitution from longest to shortest
            val sortedMappings = englishToPunjabiMap.entries.sortedByDescending { it.key.length }
            for ((english, punjabi) in sortedMappings) {
                result = result.replace(english, punjabi)
            }
            
            result
        }
    }
}