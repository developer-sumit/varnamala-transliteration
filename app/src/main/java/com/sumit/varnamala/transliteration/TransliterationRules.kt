package com.sumit.varnamala.transliteration

/**
 * Comprehensive transliteration rules for all major Indian languages.
 * Handles proper consonant-vowel combinations with matras.
 */
object TransliterationRules {

    // Hindi/Devaganari Script
    object Hindi {
        // Independent vowels
        val vowels = mapOf(
            "a" to "अ", "aa" to "आ", "i" to "इ", "ii" to "ई", "ee" to "ई",
            "u" to "उ", "uu" to "ऊ", "oo" to "ऊ",
            "e" to "ए", "ai" to "ऐ", "ay" to "ऐ",
            "o" to "ओ", "au" to "औ", "aw" to "औ",
            "ri" to "ऋ", "lri" to "ऌ"
        )

        // Vowel matras (dependent forms)
        val matras = mapOf(
            "aa" to "ा", "a" to "", // अ is inherent, no matra needed
            "i" to "ि", "ii" to "ी", "ee" to "ी",
            "u" to "ु", "uu" to "ू", "oo" to "ू",
            "e" to "े", "ai" to "ै", "ay" to "ै",
            "o" to "ो", "au" to "ौ", "aw" to "ौ",
            "ri" to "ृ", "lri" to "ॢ"
        )

        // Consonants (with inherent 'a')
        val consonants = mapOf(
            "ka" to "क", "kha" to "ख", "ga" to "ग", "gha" to "घ", "nga" to "ङ",
            "cha" to "च", "chha" to "छ", "ja" to "ज", "jha" to "झ", "nja" to "ञ",
            "ta" to "ट", "tha" to "ठ", "da" to "ड", "dha" to "ढ", "na" to "ण",
            "tta" to "त", "ttha" to "थ", "dda" to "द", "ddha" to "ध", "nna" to "न",
            "pa" to "प", "pha" to "फ", "ba" to "ब", "bha" to "भ", "ma" to "म",
            "ya" to "य", "ra" to "र", "la" to "ल", "va" to "व", "wa" to "व",
            "sha" to "श", "shha" to "ष", "ssa" to "स", "sa" to "स", "ha" to "ह",
            "ksha" to "क्ष", "tra" to "त्र", "jnya" to "ज्ञ"
        )

        // Pure consonants (without 'a')
        val pureConsonants = mapOf(
            "k" to "क", "kh" to "ख", "g" to "ग", "gh" to "घ", "ng" to "ङ",
            "ch" to "च", "chh" to "छ", "j" to "ज", "jh" to "झ", "nj" to "ञ",
            "t" to "ट", "th" to "ठ", "d" to "ड", "dh" to "ढ", "n" to "ण",
            "tt" to "त", "tth" to "थ", "dd" to "द", "ddh" to "ध", "nn" to "न",
            "p" to "प", "ph" to "फ", "b" to "ब", "bh" to "भ", "m" to "म",
            "y" to "य", "r" to "र", "l" to "ल", "v" to "व", "w" to "व",
            "sh" to "श", "shh" to "ष", "ss" to "स", "s" to "स", "h" to "ह"
        )

        val halant = "्"
        val anusvara = "ं"
        val visarga = "ः"
        val chandrabindu = "ँ"

        val specialSymbols = mapOf(
            "m" to "ं",  // anusvara when at end or before consonant
            "h" to "ः",  // visarga when at end
            "n" to "ँ"   // chandrabindu in some contexts
        )

        val numbers = mapOf(
            "0" to "०", "1" to "१", "2" to "२", "3" to "३", "4" to "४",
            "5" to "५", "6" to "६", "7" to "७", "8" to "८", "9" to "९"
        )
    }

    // Bengali Script
    object Bengali {
        val vowels = mapOf(
            "a" to "অ", "aa" to "আ", "i" to "ই", "ii" to "ঈ", "ee" to "ঈ",
            "u" to "উ", "uu" to "ঊ", "oo" to "ঊ",
            "e" to "এ", "ai" to "ঐ", "ay" to "ঐ",
            "o" to "ও", "au" to "ঔ", "aw" to "ঔ",
            "ri" to "ঋ"
        )

        val matras = mapOf(
            "aa" to "া", "a" to "",
            "i" to "ি", "ii" to "ী", "ee" to "ী",
            "u" to "ু", "uu" to "ূ", "oo" to "ূ",
            "e" to "ে", "ai" to "ৈ", "ay" to "ৈ",
            "o" to "ো", "au" to "ৌ", "aw" to "ৌ",
            "ri" to "ৃ"
        )

        val consonants = mapOf(
            "ka" to "ক", "kha" to "খ", "ga" to "গ", "gha" to "ঘ", "nga" to "ঙ",
            "cha" to "চ", "chha" to "ছ", "ja" to "জ", "jha" to "ঝ", "nja" to "ঞ",
            "ta" to "ট", "tha" to "ঠ", "da" to "ড", "dha" to "ঢ", "na" to "ণ",
            "tta" to "ত", "ttha" to "থ", "dda" to "দ", "ddha" to "ধ", "nna" to "ন",
            "pa" to "প", "pha" to "ফ", "ba" to "ব", "bha" to "ভ", "ma" to "ম",
            "ya" to "য", "ra" to "র", "la" to "ল", "va" to "ব", "wa" to "ও",
            "sha" to "শ", "shha" to "ষ", "sa" to "স", "ha" to "হ"
        )

        val pureConsonants = mapOf(
            "k" to "ক", "kh" to "খ", "g" to "গ", "gh" to "ঘ", "ng" to "ঙ",
            "ch" to "চ", "chh" to "ছ", "j" to "জ", "jh" to "ঝ", "nj" to "ঞ",
            "t" to "ট", "th" to "ঠ", "d" to "ড", "dh" to "ঢ", "n" to "ণ",
            "tt" to "ত", "tth" to "থ", "dd" to "দ", "ddh" to "ধ", "nn" to "ন",
            "p" to "প", "ph" to "ফ", "b" to "ব", "bh" to "ভ", "m" to "ম",
            "y" to "য", "r" to "র", "l" to "ল", "v" to "ব", "w" to "ও",
            "sh" to "শ", "shh" to "ষ", "s" to "স", "h" to "হ"
        )

        val halant = "্"
        val anusvara = "ং"
        val visarga = "ঃ"
        val chandrabindu = "ঁ"
    }

    // Tamil Script
    object Tamil {
        val vowels = mapOf(
            "a" to "அ", "aa" to "ஆ", "i" to "இ", "ii" to "ஈ", "ee" to "ஈ",
            "u" to "உ", "uu" to "ஊ", "oo" to "ஊ",
            "e" to "எ", "ee" to "ஏ", "ai" to "ஐ", "ay" to "ஐ",
            "o" to "ஒ", "oo" to "ஓ", "au" to "ஔ", "aw" to "ஔ"
        )

        val matras = mapOf(
            "aa" to "ா", "a" to "",
            "i" to "ி", "ii" to "ீ", "ee" to "ீ",
            "u" to "ு", "uu" to "ூ", "oo" to "ூ",
            "e" to "ெ", "ee" to "ே", "ai" to "ை", "ay" to "ை",
            "o" to "ொ", "oo" to "ோ", "au" to "ௌ", "aw" to "ௌ"
        )

        val consonants = mapOf(
            "ka" to "க", "nga" to "ங",
            "cha" to "ச", "ja" to "ஜ", "nja" to "ஞ",
            "ta" to "ட", "na" to "ண",
            "tta" to "த", "nna" to "ந",
            "pa" to "ப", "ma" to "ம",
            "ya" to "ய", "ra" to "ர", "la" to "ல", "va" to "வ", "zha" to "ழ",
            "La" to "ள", "Ra" to "ற", "na" to "ன",
            "sa" to "ஸ", "sha" to "ஷ", "ha" to "ஹ"
        )

        val pureConsonants = mapOf(
            "k" to "க", "ng" to "ங",
            "ch" to "ச", "j" to "ஜ", "nj" to "ஞ",
            "t" to "ட", "n" to "ண",
            "tt" to "த", "nn" to "ந",
            "p" to "ப", "m" to "ம",
            "y" to "ய", "r" to "ர", "l" to "ல", "v" to "வ", "zh" to "ழ",
            "L" to "ள", "R" to "ற",
            "s" to "ஸ", "sh" to "ஷ", "h" to "ஹ"
        )

        val halant = "்"
        val anusvara = "ஂ"
        val visarga = "ஃ"
    }

    // Telugu Script
    object Telugu {
        val vowels = mapOf(
            "a" to "అ", "aa" to "ఆ", "i" to "ఇ", "ii" to "ఈ", "ee" to "ఈ",
            "u" to "ఉ", "uu" to "ఊ", "oo" to "ఊ",
            "e" to "ఎ", "ee" to "ఏ", "ai" to "ఐ", "ay" to "ఐ",
            "o" to "ఒ", "oo" to "ఓ", "au" to "ఔ", "aw" to "ఔ",
            "ri" to "ఋ", "rri" to "ౠ", "lri" to "ఌ"
        )

        val matras = mapOf(
            "aa" to "ా", "a" to "",
            "i" to "ి", "ii" to "ీ", "ee" to "ీ",
            "u" to "ు", "uu" to "ూ", "oo" to "ూ",
            "e" to "ె", "ee" to "ే", "ai" to "ై", "ay" to "ై",
            "o" to "ొ", "oo" to "ో", "au" to "ౌ", "aw" to "ౌ",
            "ri" to "ృ", "rri" to "ౄ", "lri" to "ౢ"
        )

        val consonants = mapOf(
            "ka" to "క", "kha" to "ఖ", "ga" to "గ", "gha" to "ఘ", "nga" to "ఙ",
            "cha" to "చ", "chha" to "ఛ", "ja" to "జ", "jha" to "ఝ", "nja" to "ఞ",
            "ta" to "ట", "tha" to "ఠ", "da" to "డ", "dha" to "ఢ", "na" to "ణ",
            "tta" to "త", "ttha" to "థ", "dda" to "ద", "ddha" to "ధ", "nna" to "న",
            "pa" to "ప", "pha" to "ఫ", "ba" to "బ", "bha" to "భ", "ma" to "మ",
            "ya" to "య", "ra" to "ర", "la" to "ల", "va" to "వ", "wa" to "వ",
            "sha" to "శ", "shha" to "ష", "sa" to "స", "ha" to "హ",
            "La" to "ళ", "Ra" to "ఱ"
        )

        val pureConsonants = mapOf(
            "k" to "క", "kh" to "ఖ", "g" to "గ", "gh" to "ఘ", "ng" to "ఙ",
            "ch" to "చ", "chh" to "ఛ", "j" to "జ", "jh" to "ఝ", "nj" to "ఞ",
            "t" to "ట", "th" to "ఠ", "d" to "డ", "dh" to "ఢ", "n" to "ణ",
            "tt" to "త", "tth" to "థ", "dd" to "ద", "ddh" to "ధ", "nn" to "న",
            "p" to "ప", "ph" to "ఫ", "b" to "బ", "bh" to "భ", "m" to "మ",
            "y" to "య", "r" to "ర", "l" to "ల", "v" to "వ", "w" to "వ",
            "sh" to "శ", "shh" to "ష", "s" to "స", "h" to "హ",
            "L" to "ళ", "R" to "ఱ"
        )

        val halant = "్"
        val anusvara = "ం"
        val visarga = "ః"
    }

    // Gujrati Script
    object Gujarati {
        val vowels = mapOf(
            "a" to "અ", "aa" to "આ", "i" to "ઇ", "ii" to "ઈ", "ee" to "ઈ",
            "u" to "ઉ", "uu" to "ઊ", "oo" to "ઊ",
            "e" to "એ", "ai" to "ઐ", "ay" to "ઐ",
            "o" to "ઓ", "au" to "ઔ", "aw" to "ઔ",
            "ri" to "ઋ"
        )

        val matras = mapOf(
            "aa" to "ા", "a" to "",
            "i" to "િ", "ii" to "ી", "ee" to "ી",
            "u" to "ુ", "uu" to "ૂ", "oo" to "ૂ",
            "e" to "ે", "ai" to "ૈ", "ay" to "ૈ",
            "o" to "ો", "au" to "ૌ", "aw" to "ૌ",
            "ri" to "ૃ"
        )

        val consonants = mapOf(
            "ka" to "ક", "kha" to "ખ", "ga" to "ગ", "gha" to "ઘ", "nga" to "ઙ",
            "cha" to "ચ", "chha" to "છ", "ja" to "જ", "jha" to "ઝ", "nja" to "ઞ",
            "ta" to "ટ", "tha" to "ઠ", "da" to "ડ", "dha" to "ઢ", "na" to "ણ",
            "tta" to "ત", "ttha" to "થ", "dda" to "દ", "ddha" to "ધ", "nna" to "ન",
            "pa" to "પ", "pha" to "ફ", "ba" to "બ", "bha" to "ભ", "ma" to "મ",
            "ya" to "ય", "ra" to "ર", "la" to "લ", "va" to "વ", "wa" to "વ",
            "sha" to "શ", "shha" to "ષ", "ssa" to "સ", "sa" to "સ", "ha" to "હ",
            "ksha" to "ક્ષ", "tra" to "ત્ર", "jnya" to "જ્ઞ"
        )

        val pureConsonants = mapOf(
            "k" to "ક", "kh" to "ખ", "g" to "ગ", "gh" to "ઘ", "ng" to "ઙ",
            "ch" to "ચ", "chh" to "છ", "j" to "જ", "jh" to "ઝ", "nj" to "ઞ",
            "t" to "ટ", "th" to "ઠ", "d" to "ડ", "dh" to "ઢ", "n" to "ણ",
            "tt" to "ત", "tth" to "થ", "dd" to "દ", "ddh" to "ધ", "nn" to "ન",
            "p" to "પ", "ph" to "ફ", "b" to "બ", "bh" to "ભ", "m" to "મ",
            "y" to "ય", "r" to "ર", "l" to "લ", "v" to "વ", "w" to "વ",
            "sh" to "શ", "shh" to "ષ", "ss" to "સ", "s" to "સ", "h" to "હ"
        )

        val halant = "્"
    }

    // Kannada Script
    object Kannada {
        val vowels = mapOf(
            "a" to "ಅ", "aa" to "ಆ", "i" to "ಇ", "ii" to "ಈ", "ee" to "ಈ",
            "u" to "ಉ", "uu" to "ಊ", "oo" to "ಊ",
            "e" to "ಎ", "ee" to "ಏ", "ai" to "ಐ", "ay" to "ಐ",
            "o" to "ಒ", "oo" to "ಓ", "au" to "ಔ", "aw" to "ಔ",
            "ri" to "ಋ"
        )

        val matras = mapOf(
            "aa" to "ಾ", "a" to "",
            "i" to "ಿ", "ii" to "ೀ", "ee" to "ೀ",
            "u" to "ು", "uu" to "ೂ", "oo" to "ೂ",
            "e" to "ೆ", "ee" to "ೇ", "ai" to "ೈ", "ay" to "ೈ",
            "o" to "ೊ", "oo" to "ೋ", "au" to "ೌ", "aw" to "ೌ",
            "ri" to "ೃ"
        )

        val consonants = mapOf(
            "ka" to "ಕ", "kha" to "ಖ", "ga" to "ಗ", "gha" to "ಘ", "nga" to "ಙ",
            "cha" to "ಚ", "chha" to "ಛ", "ja" to "ಜ", "jha" to "ಝ", "nja" to "ಞ",
            "ta" to "ಟ", "tha" to "ಠ", "da" to "ಡ", "dha" to "ಢ", "na" to "ಣ",
            "tta" to "ತ", "ttha" to "ಥ", "dda" to "ದ", "ddha" to "ಧ", "nna" to "ನ",
            "pa" to "ಪ", "pha" to "ಫ", "ba" to "ಬ", "bha" to "ಭ", "ma" to "ಮ",
            "ya" to "ಯ", "ra" to "ರ", "la" to "ಲ", "va" to "ವ", "wa" to "ವ",
            "sha" to "ಶ", "shha" to "ಷ", "ssa" to "ಸ", "sa" to "ಸ", "ha" to "ಹ",
            "La" to "ಳ"
        )

        val pureConsonants = mapOf(
            "k" to "ಕ", "kh" to "ಖ", "g" to "ಗ", "gh" to "ಘ", "ng" to "ಙ",
            "ch" to "ಚ", "chh" to "ಛ", "j" to "ಜ", "jh" to "ಝ", "nj" to "ಞ",
            "t" to "ಟ", "th" to "ಠ", "d" to "ಡ", "dh" to "ಢ", "n" to "ಣ",
            "tt" to "ತ", "tth" to "ಥ", "dd" to "ದ", "ddh" to "ಧ", "nn" to "ನ",
            "p" to "ಪ", "ph" to "ಫ", "b" to "ಬ", "bh" to "ಭ", "m" to "ಮ",
            "y" to "ಯ", "r" to "ರ", "l" to "ಲ", "v" to "ವ", "w" to "ವ",
            "sh" to "ಶ", "shh" to "ಷ", "ss" to "ಸ", "s" to "ಸ", "h" to "ಹ",
            "L" to "ಳ"
        )

        val halant = "್"
    }

    // Malayalam Script
    object Malayalam {
        val vowels = mapOf(
            "a" to "അ", "aa" to "ആ", "i" to "ഇ", "ii" to "ഈ", "ee" to "ഈ",
            "u" to "ഉ", "uu" to "ഊ", "oo" to "ഊ",
            "e" to "എ", "ee" to "ഏ", "ai" to "ഐ", "ay" to "ഐ",
            "o" to "ഒ", "oo" to "ഓ", "au" to "ഔ", "aw" to "ഔ",
            "ri" to "ഋ"
        )

        val matras = mapOf(
            "aa" to "ാ", "a" to "",
            "i" to "ി", "ii" to "ീ", "ee" to "ീ",
            "u" to "ു", "uu" to "ൂ", "oo" to "ൂ",
            "e" to "െ", "ee" to "േ", "ai" to "ൈ", "ay" to "ൈ",
            "o" to "ൊ", "oo" to "ോ", "au" to "ൗ", "aw" to "ൗ",
            "ri" to "ൃ"
        )

        val consonants = mapOf(
            "ka" to "ക", "kha" to "ഖ", "ga" to "ഗ", "gha" to "ഘ", "nga" to "ങ",
            "cha" to "ച", "chha" to "ഛ", "ja" to "ജ", "jha" to "ഝ", "nja" to "ഞ",
            "ta" to "ട", "tha" to "ഠ", "da" to "ഡ", "dha" to "ഢ", "na" to "ണ",
            "tta" to "ത", "ttha" to "ഥ", "dda" to "ദ", "ddha" to "ധ", "nna" to "ന",
            "pa" to "പ", "pha" to "ഫ", "ba" to "ബ", "bha" to "ഭ", "ma" to "മ",
            "ya" to "യ", "ra" to "ര", "la" to "ല", "va" to "വ", "wa" to "വ",
            "sha" to "ശ", "shha" to "ഷ", "ssa" to "സ", "sa" to "സ", "ha" to "ഹ"
        )

        val pureConsonants = mapOf(
            "k" to "ക", "kh" to "ഖ", "g" to "ഗ", "gh" to "ഘ", "ng" to "ങ",
            "ch" to "ച", "chh" to "ഛ", "j" to "ജ", "jh" to "ഝ", "nj" to "ഞ",
            "t" to "ട", "th" to "ഠ", "d" to "ഡ", "dh" to "ഢ", "n" to "ണ",
            "tt" to "ത", "tth" to "ഥ", "dd" to "ദ", "ddh" to "ധ", "nn" to "ന",
            "p" to "പ", "ph" to "ഫ", "b" to "ബ", "bh" to "ഭ", "m" to "മ",
            "y" to "യ", "r" to "ര", "l" to "ല", "v" to "വ", "w" to "വ",
            "sh" to "ശ", "shh" to "ഷ", "ss" to "സ", "s" to "സ", "h" to "ഹ"
        )

        val halant = "്"
    }

    // Punjabi/Gurmukhi Script
    object Punjabi {
        val vowels = mapOf(
            "a" to "ਅ", "aa" to "ਆ", "i" to "ਇ", "ii" to "ਈ", "ee" to "ਈ",
            "u" to "ਉ", "uu" to "ਊ", "oo" to "ਊ",
            "e" to "ਏ", "ai" to "ਐ", "ay" to "ਐ",
            "o" to "ਓ", "au" to "ਔ", "aw" to "ਔ"
        )

        val matras = mapOf(
            "aa" to "ਾ", "a" to "",
            "i" to "ਿ", "ii" to "ੀ", "ee" to "ੀ",
            "u" to "ੁ", "uu" to "ੂ", "oo" to "ੂ",
            "e" to "ੇ", "ai" to "ੈ", "ay" to "ੈ",
            "o" to "ੋ", "au" to "ੌ", "aw" to "ੌ"
        )

        val consonants = mapOf(
            "ka" to "ਕ", "kha" to "ਖ", "ga" to "ਗ", "gha" to "ਘ", "nga" to "ਙ",
            "cha" to "ਚ", "chha" to "ਛ", "ja" to "ਜ", "jha" to "ਝ", "nja" to "ਞ",
            "ta" to "ਟ", "tha" to "ਠ", "da" to "ਡ", "dha" to "ਢ", "na" to "ਣ",
            "tta" to "ਤ", "ttha" to "ਥ", "dda" to "ਦ", "ddha" to "ਧ", "nna" to "ਨ",
            "pa" to "ਪ", "pha" to "ਫ", "ba" to "ਬ", "bha" to "ਭ", "ma" to "ਮ",
            "ya" to "ਯ", "ra" to "ਰ", "la" to "ਲ", "va" to "ਵ", "wa" to "ਵ",
            "sha" to "ਸ਼", "sa" to "ਸ", "ha" to "ਹ"
        )

        val pureConsonants = mapOf(
            "k" to "ਕ", "kh" to "ਖ", "g" to "ਗ", "gh" to "ਘ", "ng" to "ਙ",
            "ch" to "ਚ", "chh" to "ਛ", "j" to "ਜ", "jh" to "ਝ", "nj" to "ਞ",
            "t" to "ਟ", "th" to "ਠ", "d" to "ਡ", "dh" to "ਢ", "n" to "ਣ",
            "tt" to "ਤ", "tth" to "ਥ", "dd" to "ਦ", "ddh" to "ਧ", "nn" to "ਨ",
            "p" to "ਪ", "ph" to "ਫ", "b" to "ਬ", "bh" to "ਭ", "m" to "ਮ",
            "y" to "ਯ", "r" to "ਰ", "l" to "ਲ", "v" to "ਵ", "w" to "ਵ",
            "sh" to "ਸ਼", "s" to "ਸ", "h" to "ਹ"
        )

        val halant = "੍"
    }

    // Odia Script
    object Odia {
        val vowels = mapOf(
            "a" to "ଅ", "aa" to "ଆ", "i" to "ଇ", "ii" to "ଈ", "ee" to "ଈ",
            "u" to "ଉ", "uu" to "ଊ", "oo" to "ଊ",
            "e" to "ଏ", "ai" to "ଐ", "ay" to "ଐ",
            "o" to "ଓ", "au" to "ଔ", "aw" to "ଔ",
            "ri" to "ଋ"
        )

        val matras = mapOf(
            "aa" to "ା", "a" to "",
            "i" to "ି", "ii" to "ୀ", "ee" to "ୀ",
            "u" to "ୁ", "uu" to "ୂ", "oo" to "ୂ",
            "e" to "େ", "ai" to "ୈ", "ay" to "ୈ",
            "o" to "ୋ", "au" to "ୌ", "aw" to "ୌ",
            "ri" to "ୃ"
        )

        val consonants = mapOf(
            "ka" to "କ", "kha" to "ଖ", "ga" to "ଗ", "gha" to "ଘ", "nga" to "ଙ",
            "cha" to "ଚ", "chha" to "ଛ", "ja" to "ଜ", "jha" to "ଝ", "nja" to "ଞ",
            "ta" to "ଟ", "tha" to "ଠ", "da" to "ଡ", "dha" to "ଢ", "na" to "ଣ",
            "tta" to "ତ", "ttha" to "ଥ", "dda" to "ଦ", "ddha" to "ଧ", "nna" to "ନ",
            "pa" to "ପ", "pha" to "ଫ", "ba" to "ବ", "bha" to "ଭ", "ma" to "ମ",
            "ya" to "ୟ", "ra" to "ର", "la" to "ଲ", "va" to "ବ", "wa" to "ବ",
            "sha" to "ଶ", "shha" to "ଷ", "ssa" to "ସ", "sa" to "ସ", "ha" to "ହ"
        )

        val pureConsonants = mapOf(
            "k" to "କ", "kh" to "ଖ", "g" to "ଗ", "gh" to "ଘ", "ng" to "ଙ",
            "ch" to "ଚ", "chh" to "ଛ", "j" to "ଜ", "jh" to "ଝ", "nj" to "ଞ",
            "t" to "ଟ", "th" to "ଠ", "d" to "ଡ", "dh" to "ଢ", "n" to "ଣ",
            "tt" to "ତ", "tth" to "ଥ", "dd" to "ଦ", "ddh" to "ଧ", "nn" to "ନ",
            "p" to "ପ", "ph" to "ଫ", "b" to "ବ", "bh" to "ଭ", "m" to "ମ",
            "y" to "ୟ", "r" to "ର", "l" to "ଲ", "v" to "ବ", "w" to "ବ",
            "sh" to "ଶ", "shh" to "ଷ", "ss" to "ସ", "s" to "ସ", "h" to "ହ"
        )

        val halant = "୍"
    }
}
