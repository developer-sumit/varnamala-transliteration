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
}
