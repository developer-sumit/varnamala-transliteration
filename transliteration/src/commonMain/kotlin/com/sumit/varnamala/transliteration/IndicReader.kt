package com.sumit.varnamala.transliteration

import com.sumit.varnamala.transliteration.Iscii.A
import com.sumit.varnamala.transliteration.Iscii.CA
import com.sumit.varnamala.transliteration.Iscii.I
import com.sumit.varnamala.transliteration.Iscii.II
import com.sumit.varnamala.transliteration.Iscii.JA
import com.sumit.varnamala.transliteration.Iscii.KA
import com.sumit.varnamala.transliteration.Iscii.LA
import com.sumit.varnamala.transliteration.Iscii.LLA
import com.sumit.varnamala.transliteration.Iscii.MA
import com.sumit.varnamala.transliteration.Iscii.NA
import com.sumit.varnamala.transliteration.Iscii.NNA
import com.sumit.varnamala.transliteration.Iscii.O
import com.sumit.varnamala.transliteration.Iscii.RA
import com.sumit.varnamala.transliteration.Iscii.SA
import com.sumit.varnamala.transliteration.Iscii.SHA
import com.sumit.varnamala.transliteration.Iscii.TA
import com.sumit.varnamala.transliteration.Iscii.TTA
import com.sumit.varnamala.transliteration.Iscii.U
import com.sumit.varnamala.transliteration.Iscii.VA
import com.sumit.varnamala.transliteration.Iscii.YA
import com.sumit.varnamala.transliteration.Token.Kind

/** Reads text in one of the nine Brahmic scripts into script-neutral [Token]s. */
internal object IndicReader {
    private const val ZWNJ = 0x200C
    private const val ZWJ = 0x200D

    fun read(text: String, script: Script): List<Token> {
        require(script.isBrahmic)
        val base = script.blockStart
        val out = mutableListOf<Token>()
        var geminateNext = false
        var lastGeminated = false

        fun consonant(code: Int, nukta: Boolean = false) {
            val token = Token(Kind.CONSONANT, code, nukta)
            lastGeminated = geminateNext
            if (geminateNext) {
                out += token
                out += Token(Kind.VIRAMA)
                geminateNext = false
            }
            out += token
        }

        fun dead(code: Int, nukta: Boolean = false) {
            consonant(code, nukta)
            out += Token(Kind.VIRAMA)
        }

        fun token(kind: Kind, code: Int = 0) {
            out += Token(kind, code)
        }

        fun other(char: Char) {
            out += Token(Kind.OTHER, text = char.toString())
        }

        val expanded = buildString(text.length) {
            for (char in text) append(IndicTables.nuktaDecompositions[char] ?: char)
        }

        for (char in expanded) {
            val code = char.code
            if (code == ZWNJ || code == ZWJ) continue
            if (code == Script.DANDA || code == Script.DOUBLE_DANDA) {
                token(Kind.DANDA, if (code == Script.DANDA) 1 else 2)
                continue
            }
            val offset = code - base
            if (offset !in 0 until 0x80) {
                other(char)
                continue
            }

            // Script-specific characters whose offsets do not follow the shared layout.
            when (script) {
                Script.DEVANAGARI -> when (offset) {
                    0x00 -> { token(Kind.CANDRABINDU); continue }
                    0x04, 0x72 -> { token(Kind.VOWEL, A); continue }
                    in 0x51..0x54 -> continue
                    in 0x70..0x7F -> { other(char); continue }
                }
                Script.BENGALI -> when (offset) {
                    0x4E -> { dead(TA); continue } // khanda ta ৎ
                    0x70 -> { consonant(RA); continue } // Assamese ৰ
                    0x71 -> { consonant(VA); continue } // Assamese ৱ
                    0x00, in 0x72..0x7F -> { other(char); continue }
                }
                Script.GURMUKHI -> when (offset) {
                    0x01 -> { token(Kind.CANDRABINDU); continue }
                    0x70 -> { token(Kind.ANUSVARA); continue } // tippi
                    0x71 -> { geminateNext = true; continue } // addak
                    0x72 -> { token(Kind.VOWEL, I); continue } // iri carrier
                    0x73 -> { token(Kind.VOWEL, U); continue } // ura carrier
                    0x75 -> { token(Kind.VIRAMA); consonant(YA); continue } // yakash
                    0x51 -> continue
                    0x74, 0x76 -> { other(char); continue }
                }
                Script.ORIYA -> when (offset) {
                    0x71 -> { consonant(VA); continue }
                    0x70, in 0x72..0x7F -> { other(char); continue }
                }
                Script.TAMIL -> when (offset) {
                    0x29 -> { consonant(NA, nukta = true); continue } // ன
                    0x31 -> { consonant(RA, nukta = true); continue } // ற
                    0x34 -> { consonant(LLA, nukta = true); continue } // ழ
                    in 0x70..0x7F -> { other(char); continue }
                }
                Script.TELUGU -> when (offset) {
                    0x00 -> { token(Kind.CANDRABINDU); continue }
                    0x04 -> { token(Kind.ANUSVARA); continue }
                    0x31, 0x5A -> { consonant(RA, nukta = true); continue }
                    0x34 -> { consonant(LLA, nukta = true); continue }
                    0x58 -> { consonant(CA); continue }
                    0x59 -> { consonant(JA); continue }
                    0x5D -> { dead(NA); continue }
                    in 0x77..0x7F -> { other(char); continue }
                }
                Script.KANNADA -> when (offset) {
                    0x00 -> { token(Kind.CANDRABINDU); continue }
                    0x31 -> { consonant(RA, nukta = true); continue }
                    0x5D -> { dead(NA); continue }
                    0x71, 0x72 -> { token(Kind.VISARGA); continue }
                    0x73 -> { token(Kind.ANUSVARA); continue }
                    0x04, in 0x74..0x7F -> { other(char); continue }
                }
                Script.MALAYALAM -> when (offset) {
                    0x00, 0x04 -> { token(Kind.ANUSVARA); continue }
                    0x29 -> { consonant(NA, nukta = true); continue }
                    0x31 -> { consonant(RA, nukta = true); continue }
                    0x34 -> { consonant(LLA, nukta = true); continue }
                    0x3A -> { consonant(TTA); continue }
                    0x3B, 0x3C -> { token(Kind.VIRAMA); continue }
                    0x4E -> { dead(RA); continue } // dot reph
                    0x54 -> { dead(MA); continue } // chillus
                    0x55 -> { dead(YA); continue }
                    0x56 -> { dead(LLA, nukta = true); continue }
                    0x7A -> { dead(NNA); continue }
                    0x7B -> { dead(NA); continue }
                    0x7C -> { dead(RA); continue }
                    0x7D -> { dead(LA); continue }
                    0x7E -> { dead(LLA); continue }
                    0x7F -> { dead(KA); continue }
                    0x5F -> { token(Kind.VOWEL, II); continue }
                    0x4F, in 0x58..0x5E, in 0x70..0x79 -> { other(char); continue }
                }
                else -> Unit
            }

            when {
                Iscii.isConsonant(offset) -> consonant(offset)
                offset in 0x58..0x5F -> consonant(NUKTA_BASES[offset - 0x58], nukta = true)
                offset == Iscii.NUKTA -> attachNukta(out, script, lastGeminated)
                Iscii.isIndependentVowel(offset) -> token(Kind.VOWEL, offset)
                Iscii.isVowelSign(offset) -> addSign(out, offset)
                offset == Iscii.VIRAMA -> token(Kind.VIRAMA)
                offset == Iscii.CANDRABINDU -> token(Kind.CANDRABINDU)
                offset == Iscii.ANUSVARA -> token(Kind.ANUSVARA)
                offset == Iscii.VISARGA -> token(Kind.VISARGA)
                offset == Iscii.AVAGRAHA -> token(Kind.AVAGRAHA)
                offset == Iscii.OM -> { token(Kind.VOWEL, O); token(Kind.ANUSVARA) }
                offset in 0x66..0x6F -> token(Kind.DIGIT, offset - Iscii.DIGIT_ZERO)
                offset in 0x51..0x54 -> Unit // Vedic accents carry no letter value
                else -> other(char)
            }
        }
        return out
    }

    /** Consonants that the precomposed letters at offsets 0x58..0x5F add a nukta to. */
    private val NUKTA_BASES = intArrayOf(
        KA, Iscii.KHA, Iscii.GA, JA, Iscii.DDA, Iscii.DDHA, Iscii.PHA, YA,
    )

    private fun attachNukta(out: MutableList<Token>, script: Script, geminated: Boolean) {
        val index = out.indexOfLast { it.kind != Kind.VIRAMA }
        val last = out.getOrNull(index)?.takeIf { it.isConsonant } ?: return
        // Gurmukhi writes sha and lla as sa/la with a nukta.
        val updated = when {
            script == Script.GURMUKHI && last.code == SA -> last.copy(code = SHA)
            script == Script.GURMUKHI && last.code == LA -> last.copy(code = LLA)
            else -> last.copy(nukta = true)
        }
        out[index] = updated
        if (geminated && index >= 2 && out[index - 2].isConsonant) out[index - 2] = updated
    }

    /**
     * Adds a vowel sign, merging decomposed two-part signs (ே + ா → ோ) and signs written on a
     * vowel carrier (Gurmukhi ੲ + ਿ → ਇ).
     */
    private fun addSign(out: MutableList<Token>, sign: Int) {
        val last = out.lastOrNull()
        if (last?.kind == Kind.VOWEL) {
            out[out.lastIndex] = Token(Kind.VOWEL, Iscii.vowelFor(sign))
            return
        }
        if (last?.kind == Kind.SIGN) {
            val merged = when (sign) {
                Iscii.SIGN_AA -> when (last.code) {
                    Iscii.SIGN_SHORT_E -> Iscii.SIGN_SHORT_O
                    Iscii.SIGN_E -> Iscii.SIGN_O
                    else -> null
                }
                0x55 -> when (last.code) { // length mark
                    Iscii.SIGN_SHORT_E -> Iscii.SIGN_E
                    Iscii.SIGN_SHORT_O -> Iscii.SIGN_O
                    Iscii.SIGN_I -> Iscii.SIGN_II
                    else -> last.code
                }
                0x56 -> Iscii.SIGN_AI
                0x57 -> Iscii.SIGN_AU
                else -> null
            }
            if (merged != null) {
                out[out.lastIndex] = Token(Kind.SIGN, merged)
                return
            }
        }
        when (sign) {
            0x57 -> out += Token(Kind.SIGN, Iscii.SIGN_AU) // Malayalam writes au with the length mark alone
            0x55, 0x56 -> Unit
            0x3A, 0x3B, 0x4E, 0x4F -> out += Token(Kind.SIGN, Iscii.SIGN_E)
            else -> out += Token(Kind.SIGN, sign)
        }
    }
}
