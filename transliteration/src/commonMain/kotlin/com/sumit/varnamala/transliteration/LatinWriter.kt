package com.sumit.varnamala.transliteration

import com.sumit.varnamala.transliteration.Iscii.BA
import com.sumit.varnamala.transliteration.Iscii.BHA
import com.sumit.varnamala.transliteration.Iscii.CA
import com.sumit.varnamala.transliteration.Iscii.CHA
import com.sumit.varnamala.transliteration.Iscii.DA
import com.sumit.varnamala.transliteration.Iscii.DDA
import com.sumit.varnamala.transliteration.Iscii.DDHA
import com.sumit.varnamala.transliteration.Iscii.DHA
import com.sumit.varnamala.transliteration.Iscii.GA
import com.sumit.varnamala.transliteration.Iscii.GHA
import com.sumit.varnamala.transliteration.Iscii.HA
import com.sumit.varnamala.transliteration.Iscii.JA
import com.sumit.varnamala.transliteration.Iscii.JHA
import com.sumit.varnamala.transliteration.Iscii.KA
import com.sumit.varnamala.transliteration.Iscii.KHA
import com.sumit.varnamala.transliteration.Iscii.LA
import com.sumit.varnamala.transliteration.Iscii.LLA
import com.sumit.varnamala.transliteration.Iscii.LLLA
import com.sumit.varnamala.transliteration.Iscii.MA
import com.sumit.varnamala.transliteration.Iscii.NA
import com.sumit.varnamala.transliteration.Iscii.NGA
import com.sumit.varnamala.transliteration.Iscii.NNA
import com.sumit.varnamala.transliteration.Iscii.NNNA
import com.sumit.varnamala.transliteration.Iscii.NYA
import com.sumit.varnamala.transliteration.Iscii.PA
import com.sumit.varnamala.transliteration.Iscii.PHA
import com.sumit.varnamala.transliteration.Iscii.RA
import com.sumit.varnamala.transliteration.Iscii.RRA
import com.sumit.varnamala.transliteration.Iscii.SA
import com.sumit.varnamala.transliteration.Iscii.SHA
import com.sumit.varnamala.transliteration.Iscii.SSA
import com.sumit.varnamala.transliteration.Iscii.TA
import com.sumit.varnamala.transliteration.Iscii.THA
import com.sumit.varnamala.transliteration.Iscii.TTA
import com.sumit.varnamala.transliteration.Iscii.TTHA
import com.sumit.varnamala.transliteration.Iscii.VA
import com.sumit.varnamala.transliteration.Iscii.YA
import com.sumit.varnamala.transliteration.Token.Kind

/** How native text is written in Latin letters. */
enum class RomanizationScheme {
    /** Plain ASCII as people spell Indian words: नमस्ते → namaste, कमला → kamla. Lossy. */
    READABLE,

    /** ISO 15919 with diacritics: नमस्ते → namastē. Lossless and reads back exactly. */
    ISO_15919,
}

internal object LatinWriter {

    fun write(tokens: List<Token>, source: Language, scheme: RomanizationScheme): String {
        val iso = scheme == RomanizationScheme.ISO_15919
        val silent = if (!iso && source.deletesSchwa) SchwaDeletion.silentSchwas(tokens) else emptySet()
        val out = StringBuilder()

        tokens.forEachIndexed { index, token ->
            val next = tokens.getOrNull(index + 1)
            when (token.kind) {
                Kind.CONSONANT -> {
                    out.append(consonant(token, iso, source, index == 0 || tokens[index - 1].kind == Kind.OTHER))
                    val hasInherentVowel = next == null ||
                        (next.kind != Kind.SIGN && next.kind != Kind.VIRAMA)
                    if (hasInherentVowel && index !in silent) out.append('a')
                }
                Kind.VOWEL -> out.append(vowel(token.code, iso))
                Kind.SIGN -> out.append(vowel(Iscii.vowelFor(token.code), iso))
                Kind.VIRAMA -> Unit
                Kind.ANUSVARA -> out.append(
                    when {
                        iso -> "ṁ"
                        next?.isConsonant == true && next.code in PA..MA -> "m"
                        next == null && !source.deletesSchwa -> "m"
                        else -> "n"
                    }
                )
                Kind.CANDRABINDU -> out.append(if (iso) "m̐" else "n")
                Kind.VISARGA -> out.append(if (iso) "ḥ" else "h")
                Kind.AVAGRAHA -> out.append(if (iso) "’" else "")
                Kind.DIGIT -> out.append('0' + token.code)
                Kind.DANDA -> out.append(if (token.code == 2) ".." else ".")
                Kind.OTHER -> out.append(token.text)
            }
        }
        return out.toString()
    }

    private fun consonant(token: Token, iso: Boolean, source: Language, wordInitial: Boolean): String {
        if (token.nukta) {
            return when (token.code) {
                KA -> "q"
                KHA -> if (iso) "k͟h" else "kh"
                GA -> if (iso) "ġ" else "gh"
                JA -> "z"
                DDA -> if (iso) "ṛ" else "r"
                DDHA -> if (iso) "ṛh" else "rh"
                PHA -> "f"
                YA -> if (iso) "ẏ" else "y"
                NA -> if (iso) "ṉ" else "n"
                RA -> if (iso) "ṟ" else "r"
                LLA -> if (iso) "ḻ" else "zh"
                else -> consonant(token.copy(nukta = false), iso, source, wordInitial)
            }
        }
        // Bengali and Odia read word-initial য/ଯ as j.
        if (!iso && token.code == YA && wordInitial &&
            (source.script == Script.BENGALI || source.script == Script.ORIYA)
        ) return "j"
        return (if (iso) ISO_CONSONANTS else READABLE_CONSONANTS)[token.code] ?: ""
    }

    private fun vowel(code: Int, iso: Boolean): String =
        (if (iso) ISO_VOWELS else READABLE_VOWELS)[code] ?: ""

    private val ISO_CONSONANTS = mapOf(
        KA to "k", KHA to "kh", GA to "g", GHA to "gh", NGA to "ṅ",
        CA to "c", CHA to "ch", JA to "j", JHA to "jh", NYA to "ñ",
        TTA to "ṭ", TTHA to "ṭh", DDA to "ḍ", DDHA to "ḍh", NNA to "ṇ",
        TA to "t", THA to "th", DA to "d", DHA to "dh", NA to "n", NNNA to "ṉ",
        PA to "p", PHA to "ph", BA to "b", BHA to "bh", MA to "m",
        YA to "y", RA to "r", RRA to "ṟ", LA to "l", LLA to "ḷ", LLLA to "ḻ", VA to "v",
        SHA to "ś", SSA to "ṣ", SA to "s", HA to "h",
    )

    private val READABLE_CONSONANTS = mapOf(
        KA to "k", KHA to "kh", GA to "g", GHA to "gh", NGA to "n",
        CA to "ch", CHA to "chh", JA to "j", JHA to "jh", NYA to "n",
        TTA to "t", TTHA to "th", DDA to "d", DDHA to "dh", NNA to "n",
        TA to "t", THA to "th", DA to "d", DHA to "dh", NA to "n", NNNA to "n",
        PA to "p", PHA to "ph", BA to "b", BHA to "bh", MA to "m",
        YA to "y", RA to "r", RRA to "r", LA to "l", LLA to "l", LLLA to "zh", VA to "v",
        SHA to "sh", SSA to "sh", SA to "s", HA to "h",
    )

    private val ISO_VOWELS = mapOf(
        Iscii.A to "a", 0x04 to "a", Iscii.AA to "ā", Iscii.I to "i", Iscii.II to "ī",
        Iscii.U to "u", Iscii.UU to "ū", Iscii.VOCALIC_R to "r̥", Iscii.VOCALIC_RR to "r̥̄",
        Iscii.VOCALIC_L to "l̥", Iscii.VOCALIC_LL to "l̥̄", Iscii.CANDRA_E to "ê",
        Iscii.SHORT_E to "e", Iscii.E to "ē", Iscii.AI to "ai", Iscii.CANDRA_O to "ô",
        Iscii.SHORT_O to "o", Iscii.O to "ō", Iscii.AU to "au",
    )

    private val READABLE_VOWELS = mapOf(
        Iscii.A to "a", 0x04 to "a", Iscii.AA to "a", Iscii.I to "i", Iscii.II to "i",
        Iscii.U to "u", Iscii.UU to "u", Iscii.VOCALIC_R to "ri", Iscii.VOCALIC_RR to "ri",
        Iscii.VOCALIC_L to "li", Iscii.VOCALIC_LL to "li", Iscii.CANDRA_E to "e",
        Iscii.SHORT_E to "e", Iscii.E to "e", Iscii.AI to "ai", Iscii.CANDRA_O to "o",
        Iscii.SHORT_O to "o", Iscii.O to "o", Iscii.AU to "au",
    )
}

/**
 * Finds inherent vowels that go silent in Hindi-type pronunciation (कमला → kamlā, राम → rām).
 * Scanning right to left, an inherent vowel is dropped at the end of a word (unless it follows
 * a conjunct), and medially when it sits between a vowel-bearing syllable and a consonant that
 * carries its own vowel.
 */
internal object SchwaDeletion {
    fun silentSchwas(tokens: List<Token>): Set<Int> {
        val silent = mutableSetOf<Int>()
        // Treat each run between OTHER tokens as a word.
        var start = 0
        while (start < tokens.size) {
            var end = start
            while (end < tokens.size && tokens[end].kind != Kind.OTHER && tokens[end].kind != Kind.DANDA) end++
            processWord(tokens, start, end, silent)
            start = end + 1
        }
        return silent
    }

    private fun processWord(tokens: List<Token>, start: Int, end: Int, silent: MutableSet<Int>) {
        fun hasInherent(i: Int): Boolean {
            if (i !in start until end || !tokens[i].isConsonant) return false
            val next = tokens.getOrNull(i + 1)
            return i + 1 >= end || (next!!.kind != Kind.SIGN && next.kind != Kind.VIRAMA)
        }

        val consonants = (start until end).filter { tokens[it].isConsonant }
        if (consonants.size < 2 && (start until end).none { tokens[it].isVowelBearing }) return

        // Word-final inherent vowel, unless it follows a conjunct (मित्र mitra).
        val last = consonants.lastOrNull() ?: return
        if (last == end - 1 && hasInherent(last)) {
            val afterConjunct = tokens.getOrNull(last - 1)?.kind == Kind.VIRAMA
            val onlySyllable = consonants.size == 1 && (start until end).none { tokens[it].kind == Kind.VOWEL }
            if (!afterConjunct && !onlySyllable) silent += last
        }

        // Medial: V C(a) C V → V C C V, scanning right to left.
        for (k in consonants.indices.reversed()) {
            val i = consonants[k]
            if (i == last || !hasInherent(i) || i in silent) continue
            val nextConsonant = consonants.getOrNull(k + 1) ?: continue
            if (nextConsonant != i + 1) continue // something (anusvara, vowel) intervenes
            val nextHasOwnVowel = tokens.getOrNull(nextConsonant + 1)?.kind == Kind.SIGN
            val previous = tokens.getOrNull(i - 1)
            val prevBearsVowel = previous != null && i - 1 >= start && when {
                previous.isVowelBearing -> true
                previous.isConsonant -> hasInherent(i - 1) && (i - 1) !in silent
                else -> false
            }
            if (nextHasOwnVowel && prevBearsVowel) silent += i
        }
    }
}
