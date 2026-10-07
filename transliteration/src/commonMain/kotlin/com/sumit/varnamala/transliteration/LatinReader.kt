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
import com.sumit.varnamala.transliteration.Iscii.MA
import com.sumit.varnamala.transliteration.Iscii.NA
import com.sumit.varnamala.transliteration.Iscii.NGA
import com.sumit.varnamala.transliteration.Iscii.NNA
import com.sumit.varnamala.transliteration.Iscii.NYA
import com.sumit.varnamala.transliteration.Iscii.PA
import com.sumit.varnamala.transliteration.Iscii.PHA
import com.sumit.varnamala.transliteration.Iscii.RA
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

/**
 * Reads one romanized word into tokens. Accepts casual spelling ("namaste", "shukriya",
 * "station") as well as ISO 15919 / IAST letters ("rāṣṭra"), so text romanized by this
 * library reads back exactly.
 *
 * Casual romanization is ambiguous (t may be त or ट, i may be इ or ई); the rules pick the most
 * common reading for the target language. A learned model is needed to do better.
 */
internal object LatinReader {

    private data class Letter(val code: Int, val nukta: Boolean = false)

    /** A consonant spelling, possibly a cluster such as ksh → क्ष. */
    private class Consonant(vararg val letters: Letter)

    private fun c(code: Int, nukta: Boolean = false) = Consonant(Letter(code, nukta))
    private fun cluster(vararg codes: Int) = Consonant(*codes.map { Letter(it) }.toTypedArray())

    /** Longest spelling first. */
    private val CONSONANTS: List<Pair<String, Consonant>> = listOf(
        "tion" to cluster(SHA, NA),
        "sion" to cluster(SHA, NA),
        "ksh" to cluster(KA, SSA),
        "chh" to c(CHA),
        "shh" to c(SSA),
        "jny" to cluster(JA, NYA),
        "kh" to c(KHA), "gh" to c(GHA), "ch" to c(CA), "jh" to c(JHA),
        "th" to c(THA), "dh" to c(DHA), "ph" to c(PHA), "bh" to c(BHA),
        "sh" to c(SHA), "ck" to c(KA),
        // ISO 15919 / IAST
        "ṭh" to c(TTHA), "ḍh" to c(DDHA), "ṛh" to c(DDHA, nukta = true), "k͟h" to c(KHA, nukta = true),
        "k" to c(KA), "g" to c(GA), "j" to c(JA), "t" to c(TA), "d" to c(DA), "n" to c(NA),
        "p" to c(PA), "b" to c(BA), "m" to c(MA), "y" to c(YA), "r" to c(RA), "l" to c(LA),
        "v" to c(VA), "w" to c(VA), "s" to c(SA), "h" to c(HA),
        "f" to c(PHA, nukta = true), "z" to c(JA, nukta = true), "q" to c(KA, nukta = true),
        "x" to cluster(KA, SA),
        "ṭ" to c(TTA), "ḍ" to c(DDA), "ṇ" to c(NNA), "ṅ" to c(NGA), "ñ" to c(NYA),
        "ś" to c(SHA), "ṣ" to c(SSA), "ḷ" to c(LLA), "ṛ" to c(DDA, nukta = true),
        "ġ" to c(GA, nukta = true), "ẏ" to c(YA, nukta = true), "ṟ" to c(RA, nukta = true),
        "ṉ" to c(NA, nukta = true), "ḻ" to c(LLA, nukta = true), "ç" to c(SA),
    )

    /** Vowel spellings; e/o depend on the target language (see [Language.shortEO]). */
    private fun vowels(language: Language): List<Pair<String, Int>> {
        val e = if (language.shortEO) Iscii.SHORT_E else Iscii.E
        val o = if (language.shortEO) Iscii.SHORT_O else Iscii.O
        return listOf(
            "aa" to Iscii.AA, "ai" to Iscii.AI, "au" to Iscii.AU, "ou" to Iscii.AU,
            "ee" to Iscii.II, "ii" to Iscii.II, "oo" to Iscii.UU, "uu" to Iscii.UU,
            "ae" to Iscii.E, "oa" to Iscii.O,
            "r̥̄" to Iscii.VOCALIC_RR, "r̥" to Iscii.VOCALIC_R, "l̥" to Iscii.VOCALIC_L,
            "ā" to Iscii.AA, "ī" to Iscii.II, "ū" to Iscii.UU, "ē" to Iscii.E, "ō" to Iscii.O,
            "a" to Iscii.A, "i" to Iscii.I, "u" to Iscii.U, "e" to e, "o" to o,
        )
    }

    /** Nasals followed by one of these become an anusvara: hindi → हिंदी, amba → अंबा. */
    private val ANUSVARA_TRIGGERS = setOf(
        KA, KHA, GA, GHA, CA, CHA, JA, JHA, TTA, TTHA, DDA, DDHA, TA, THA, DA, DHA,
        PA, PHA, BA, BHA, SA, SHA, SSA,
    )

    fun read(word: String, language: Language): List<Token> {
        val text = word.lowercase()
        val vowels = vowels(language)
        val out = mutableListOf<Token>()
        var pendingConsonant = false
        var i = 0

        fun atEnd(position: Int) = position >= text.length

        while (i < text.length) {
            // Signs written with ISO letters.
            if (text.startsWith("m\u0310", i)) {
                out += Token(Kind.CANDRABINDU); pendingConsonant = false; i += 2; continue
            }
            when (text[i]) {
                'ṃ', 'ṁ' -> { out += Token(Kind.ANUSVARA); pendingConsonant = false; i++; continue }
                'ḥ' -> { out += Token(Kind.VISARGA); pendingConsonant = false; i++; continue }
                '\'', '’' -> { i++; continue }
            }

            val vowel = vowels.firstOrNull { text.startsWith(it.first, i) }
            if (vowel != null) {
                val end = i + vowel.first.length
                var code = vowel.second
                if (pendingConsonant) {
                    if (atEnd(end)) code = lengthenFinal(code, language)
                    Iscii.signFor(code)?.let { out += Token(Kind.SIGN, it) }
                    pendingConsonant = false
                } else {
                    out += Token(Kind.VOWEL, code)
                }
                i = end
                continue
            }

            // English final y after a consonant is a vowel: city → सिटी.
            if (text[i] == 'y' && pendingConsonant && atEnd(i + 1)) {
                out += Token(Kind.SIGN, Iscii.SIGN_II)
                pendingConsonant = false
                i++
                continue
            }

            val match = consonantAt(text, i, language)
            if (match == null) {
                if (pendingConsonant) closeConsonant(out, language, atWordEnd = false)
                pendingConsonant = false
                out += Token(Kind.OTHER, text = text.substring(i, i + 1))
                i++
                continue
            }
            val (length, consonant) = match
            val first = consonant.letters.first()
            if (pendingConsonant) {
                val previous = out.last()
                val previousIsNasal = !previous.nukta && (previous.code == NA || previous.code == MA)
                val hasVowelBefore = out.size >= 2
                if (previousIsNasal && hasVowelBefore && first.code in ANUSVARA_TRIGGERS && !first.nukta) {
                    out[out.lastIndex] = Token(Kind.ANUSVARA)
                } else if (!keepsSilentVowel(language, previous, first, hasVowelBefore, text, i + length, vowels)) {
                    out += Token(Kind.VIRAMA)
                }
            }
            consonant.letters.forEachIndexed { index, letter ->
                if (index > 0) out += Token(Kind.VIRAMA)
                out += Token(Kind.CONSONANT, letter.code, letter.nukta)
            }
            pendingConsonant = true
            // q is almost always followed by a silent-looking u: queen → क्वीन.
            i += length
            if (first.code == KA && first.nukta && text.startsWith("u", i) && i + 1 < text.length) {
                out[out.lastIndex] = Token(Kind.CONSONANT, KA)
                out += Token(Kind.VIRAMA)
                out += Token(Kind.CONSONANT, VA)
                i++
            }
        }
        if (pendingConsonant) closeConsonant(out, language, atWordEnd = true)
        return out
    }

    /**
     * Casual Hindi-type spelling omits vowels that are silent in speech: karna is करना and
     * kamla is कमला, not कर्ना. Before n or l followed by a vowel, keep the inherent vowel;
     * other clusters (dharma, mitra) stay conjuncts.
     */
    private fun keepsSilentVowel(
        language: Language,
        previous: Token,
        next: Letter,
        hasVowelBefore: Boolean,
        text: String,
        afterNext: Int,
        vowels: List<Pair<String, Int>>,
    ): Boolean {
        if (!language.deletesSchwa || !hasVowelBefore || next.nukta) return false
        if (next.code != NA && next.code != LA || previous.code == next.code) return false
        return vowels.any { text.startsWith(it.first, afterNext) }
    }

    private fun consonantAt(text: String, i: Int, language: Language): Pair<Int, Consonant>? {
        // Tamil and Malayalam romanize ழ/ഴ as zh.
        if (text.startsWith("zh", i) && (language == Language.TAMIL || language == Language.MALAYALAM)) {
            return 2 to c(LLA, nukta = true)
        }
        // English c: soft before e/i/y, hard elsewhere.
        if (text[i] == 'c' && !text.startsWith("ch", i)) {
            val next = text.getOrNull(i + 1)
            return 1 to c(if (next == 'e' || next == 'i' || next == 'y') SA else KA)
        }
        return CONSONANTS.firstOrNull { text.startsWith(it.first, i) }?.let { it.first.length to it.second }
    }

    private fun closeConsonant(out: MutableList<Token>, language: Language, atWordEnd: Boolean) {
        if (!atWordEnd || language.writesFinalVirama) out += Token(Kind.VIRAMA)
    }

    private fun lengthenFinal(code: Int, language: Language): Int = when (code) {
        Iscii.A -> if (language.finalALong) Iscii.AA else code
        Iscii.I -> if (language.finalIULong) Iscii.II else code
        Iscii.U -> if (language.finalIULong) Iscii.UU else code
        else -> code
    }
}
