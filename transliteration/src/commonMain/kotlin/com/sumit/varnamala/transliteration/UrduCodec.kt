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

/**
 * Urdu (Perso-Arabic script) reader and writer.
 *
 * Urdu normally leaves short vowels unwritten, so reading it is approximate: کتاب becomes
 * "कताब" rather than "किताब" unless the text carries diacritics. The most frequent function
 * words are looked up in [COMMON_WORDS] because the letter rules get them wrong.
 */
internal object UrduCodec {

    // ---- Reading ----

    private val CONSONANTS: Map<Char, Pair<Int, Boolean>> = mapOf(
        'ب' to (BA to false), 'پ' to (PA to false), 'ت' to (TA to false), 'ٹ' to (TTA to false),
        'ث' to (SA to false), 'ج' to (JA to false), 'چ' to (CA to false), 'ح' to (HA to false),
        'خ' to (KHA to true), 'د' to (DA to false), 'ڈ' to (DDA to false), 'ذ' to (JA to true),
        'ر' to (RA to false), 'ڑ' to (DDA to true), 'ز' to (JA to true), 'ژ' to (JA to true),
        'س' to (SA to false), 'ش' to (SHA to false), 'ص' to (SA to false), 'ض' to (JA to true),
        'ط' to (TA to false), 'ظ' to (JA to true), 'غ' to (GA to true), 'ف' to (PHA to true),
        'ق' to (KA to true), 'ک' to (KA to false), 'ك' to (KA to false), 'گ' to (GA to false),
        'ل' to (LA to false), 'م' to (MA to false), 'ن' to (NA to false), 'ۃ' to (TA to false),
    )

    private val ASPIRATED = mapOf(
        KA to KHA, GA to GHA, CA to CHA, JA to JHA, TTA to TTHA, DDA to DDHA,
        TA to THA, DA to DHA, PA to PHA, BA to BHA,
    )

    private const val ALEF = 'ا'
    private const val ALEF_MADDA = 'آ'
    private const val WAW = 'و'
    private const val WAW_HAMZA = 'ؤ'
    private const val YE = 'ی'
    private const val ARABIC_YE = 'ي'
    private const val ALEF_MAKSURA = 'ى'
    private const val BARI_YE = 'ے'
    private const val YE_HAMZA = 'ئ'
    private const val HAMZA = 'ء'
    private const val AIN = 'ع'
    private const val NOON_GHUNNA = 'ں'
    private const val DO_CHASHMI = 'ھ'
    private const val HE = 'ہ'
    private const val ARABIC_HE = 'ه'
    private const val FATHA = 'َ'
    private const val DAMMA = 'ُ'
    private const val KASRA = 'ِ'
    private const val SHADDA = 'ّ'
    private const val SUKUN = 'ْ'
    private const val TANWEEN_FATH = 'ً'
    private const val SUPERSCRIPT_ALEF = 'ٰ'

    private fun isYe(c: Char?) = c == YE || c == ARABIC_YE || c == ALEF_MAKSURA
    private fun isVowelLetter(c: Char?) =
        c == ALEF || c == ALEF_MADDA || isYe(c) || c == BARI_YE || c == WAW || c == AIN

    /** Frequent words whose spelling the letter rules cannot recover, as Devanagari. */
    private val COMMON_WORDS = mapOf(
        "یہ" to "यह", "وہ" to "वह", "کہ" to "कि", "ہے" to "है", "ہیں" to "हैं", "میں" to "में",
        "نہیں" to "नहीं", "اور" to "और", "کیا" to "क्या", "ہو" to "हो", "یہاں" to "यहाँ",
        "وہاں" to "वहाँ", "بہت" to "बहुत", "کچھ" to "कुछ", "کیوں" to "क्यों", "کون" to "कौन",
        "لیکن" to "लेकिन", "پھر" to "फिर", "اس" to "इस", "ان" to "इन", "اُس" to "उस", "اُن" to "उन",
        "تم" to "तुम", "ہم" to "हम", "آپ" to "आप", "کب" to "कब", "جب" to "जब", "تب" to "तब",
        "مجھے" to "मुझे", "تمہیں" to "तुम्हें", "ہمیں" to "हमें", "انہیں" to "उन्हें",
    )

    fun read(word: String): List<Token> {
        COMMON_WORDS[word]?.let { return IndicReader.read(it, Script.DEVANAGARI) }

        val out = mutableListOf<Token>()
        var pending = false // last token is a consonant still waiting for a vowel

        fun consonant(code: Int, nukta: Boolean) {
            out += Token(Kind.CONSONANT, code, nukta)
            pending = true
        }

        fun sign(code: Int) {
            out += Token(Kind.SIGN, code)
            pending = false
        }

        fun vowel(code: Int) {
            out += Token(Kind.VOWEL, code)
            pending = false
        }

        var i = 0
        while (i < word.length) {
            val c = word[i]
            val next = word.getOrNull(i + 1)
            val atStart = out.isEmpty()
            val atEnd = i == word.lastIndex
            when {
                c in CONSONANTS -> CONSONANTS.getValue(c).let { (code, nukta) -> consonant(code, nukta) }
                c == DO_CHASHMI -> {
                    val last = out.lastOrNull()
                    val aspirated = last?.takeIf { it.isConsonant }?.let {
                        if (it.nukta && it.code == DDA) DDHA else ASPIRATED[it.code]
                    }
                    if (aspirated != null) out[out.lastIndex] = last.copy(code = aspirated)
                    else { if (pending) out += Token(Kind.VIRAMA); consonant(HA, false) }
                }
                c == HE || c == ARABIC_HE -> if (atEnd && pending) sign(Iscii.SIGN_AA) else consonant(HA, false)
                c == ALEF || c == AIN -> when {
                    atStart && next == WAW -> { vowel(Iscii.O); i++ }
                    atStart && isYe(next) -> { vowel(Iscii.E); i++ }
                    atStart && next == KASRA -> { vowel(Iscii.I); i++ }
                    atStart && next == DAMMA -> { vowel(Iscii.U); i++ }
                    atStart -> vowel(Iscii.A)
                    pending -> sign(Iscii.SIGN_AA)
                    c == ALEF -> vowel(Iscii.AA)
                }
                c == ALEF_MADDA -> if (pending) sign(Iscii.SIGN_AA) else vowel(Iscii.AA)
                c == WAW || c == WAW_HAMZA -> when {
                    c == WAW_HAMZA -> vowel(Iscii.O)
                    atStart -> consonant(VA, false)
                    pending && isVowelLetter(next) -> { out += Token(Kind.VIRAMA); consonant(VA, false) }
                    pending -> sign(Iscii.SIGN_O)
                    else -> consonant(VA, false)
                }
                isYe(c) -> when {
                    atStart -> consonant(YA, false)
                    pending && (next == ALEF || next == ALEF_MADDA) -> { out += Token(Kind.VIRAMA); consonant(YA, false) }
                    pending -> sign(Iscii.SIGN_II)
                    else -> consonant(YA, false)
                }
                c == BARI_YE -> if (pending) sign(Iscii.SIGN_E) else vowel(Iscii.E)
                c == YE_HAMZA -> when {
                    next == BARI_YE -> { vowel(Iscii.E); i++ }
                    isYe(next) -> { vowel(Iscii.II); i++ }
                    else -> vowel(Iscii.I)
                }
                c == NOON_GHUNNA -> { out += Token(Kind.CANDRABINDU); pending = false }
                c == FATHA -> pending = false
                c == KASRA -> if (pending) sign(Iscii.SIGN_I)
                c == DAMMA -> if (pending) sign(Iscii.SIGN_U)
                c == SUPERSCRIPT_ALEF -> if (pending) sign(Iscii.SIGN_AA)
                c == SUKUN -> if (pending) { out += Token(Kind.VIRAMA); pending = false }
                c == SHADDA -> out.lastOrNull()?.takeIf { it.isConsonant }?.let {
                    out.add(out.lastIndex, Token(Kind.VIRAMA))
                    out.add(out.lastIndex - 1, it)
                }
                c == TANWEEN_FATH -> { out += Token(Kind.ANUSVARA); pending = false }
                c == HAMZA -> Unit
                c.code in 0x06F0..0x06F9 -> out += Token(Kind.DIGIT, c.code - 0x06F0)
                c.code in 0x0660..0x0669 -> out += Token(Kind.DIGIT, c.code - 0x0660)
                c.code in 0x064B..0x065F -> Unit
                else -> { out += Token(Kind.OTHER, text = c.toString()); pending = false }
            }
            i++
        }
        joinFinalCluster(out)
        return out
    }

    /** Word-final clusters such as dost, gosht, waqt, dard are written without a vowel between. */
    private fun joinFinalCluster(out: MutableList<Token>) {
        if (out.size < 3) return
        val last = out[out.lastIndex]
        val beforeLast = out[out.lastIndex - 1]
        if (!last.isConsonant || !beforeLast.isConsonant) return
        val firstOk = beforeLast.code in setOf(SA, SHA, RA, NA) || (beforeLast.nukta && beforeLast.code in setOf(KA, KHA, PHA))
        val secondOk = last.code in setOf(TA, DA, TTA, KA)
        if (firstOk && secondOk) out.add(out.lastIndex, Token(Kind.VIRAMA))
    }

    // ---- Writing ----

    private val PLAIN = mapOf(
        KA to "ک", KHA to "کھ", GA to "گ", GHA to "گھ", NGA to "ن",
        CA to "چ", CHA to "چھ", JA to "ج", JHA to "جھ", NYA to "ن",
        TTA to "ٹ", TTHA to "ٹھ", DDA to "ڈ", DDHA to "ڈھ", NNA to "ن",
        TA to "ت", THA to "تھ", DA to "د", DHA to "دھ", NA to "ن", NNNA to "ن",
        PA to "پ", PHA to "پھ", BA to "ب", BHA to "بھ", MA to "م",
        YA to "ی", RA to "ر", RRA to "ر", LA to "ل", LLA to "ل", LLLA to "ل", VA to "و",
        SHA to "ش", SSA to "ش", SA to "س", HA to "ہ",
    )

    private val NUKTA = mapOf(
        KA to "ق", KHA to "خ", GA to "غ", JA to "ز", DDA to "ڑ", DDHA to "ڑھ", PHA to "ف",
    )

    fun write(tokens: List<Token>): String {
        val out = StringBuilder()
        tokens.forEachIndexed { index, token ->
            val next = tokens.getOrNull(index + 1)
            val wordEnd = next == null || next.kind == Kind.OTHER || next.kind == Kind.DANDA
            when (token.kind) {
                Kind.CONSONANT -> out.append(
                    (if (token.nukta) NUKTA[token.code] else null) ?: PLAIN[token.code] ?: ""
                )
                Kind.SIGN -> out.append(signText(token.code, wordEnd))
                Kind.VOWEL -> out.append(vowelText(token.code, wordEnd))
                Kind.ANUSVARA, Kind.CANDRABINDU -> out.append(
                    when {
                        wordEnd -> "ں"
                        next?.isConsonant == true && next.code in PA..MA -> "م"
                        else -> "ن"
                    }
                )
                Kind.VISARGA -> out.append("ہ")
                Kind.DIGIT -> out.append((0x06F0 + token.code).toChar())
                Kind.DANDA -> out.append("۔")
                Kind.OTHER -> out.append(token.text)
                Kind.VIRAMA, Kind.AVAGRAHA -> Unit
            }
        }
        return out.toString()
    }

    private fun signText(sign: Int, wordEnd: Boolean): String = when (sign) {
        Iscii.SIGN_AA -> "ا"
        Iscii.SIGN_II -> "ی"
        Iscii.SIGN_UU, Iscii.SIGN_O, Iscii.SIGN_SHORT_O, Iscii.SIGN_AU, Iscii.SIGN_CANDRA_O -> "و"
        Iscii.SIGN_E, Iscii.SIGN_SHORT_E, Iscii.SIGN_AI, Iscii.SIGN_CANDRA_E -> if (wordEnd) "ے" else "ی"
        Iscii.SIGN_VOCALIC_R, Iscii.SIGN_VOCALIC_RR -> "ر"
        else -> "" // short i/u are not written
    }

    private fun vowelText(vowel: Int, wordEnd: Boolean): String = when (vowel) {
        Iscii.AA -> "آ"
        Iscii.II -> "ای"
        Iscii.UU, Iscii.O, Iscii.SHORT_O, Iscii.AU, Iscii.CANDRA_O -> "او"
        Iscii.E, Iscii.SHORT_E, Iscii.AI, Iscii.CANDRA_E -> if (wordEnd) "اے" else "ای"
        Iscii.VOCALIC_R, Iscii.VOCALIC_RR -> "ر"
        else -> "ا"
    }
}
