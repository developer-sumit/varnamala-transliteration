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
 * Writes tokens in one of the nine Brahmic scripts, substituting the closest available letter
 * where the target script lacks one (Tamil has no voiced or aspirated stops, Bengali no va,
 * and so on) and following each script's spelling conventions.
 */
internal object IndicWriter {

    fun write(tokens: List<Token>, language: Language): String {
        val script = language.script
        require(script.isBrahmic)
        return Writer(tokens, language).run()
    }

    private class Writer(private val tokens: List<Token>, private val language: Language) {
        private val script = language.script
        private val assigned = IndicTables.assigned.getValue(script)
        private val out = StringBuilder()
        private var index = 0

        private fun has(offset: Int) = offset in assigned.indices && assigned[offset]
        private fun emit(offset: Int) = out.append((script.blockStart + offset).toChar())

        private fun peek(delta: Int): Token? = tokens.getOrNull(index + delta)

        fun run(): String {
            while (index < tokens.size) {
                val token = tokens[index]
                when (token.kind) {
                    Kind.CONSONANT -> consonant(token)
                    Kind.VOWEL -> vowel(token.code)
                    Kind.SIGN -> sign(token.code)
                    Kind.VIRAMA -> virama()
                    Kind.ANUSVARA -> nasal(candrabindu = false)
                    Kind.CANDRABINDU -> nasal(candrabindu = true)
                    Kind.VISARGA -> if (has(Iscii.VISARGA)) emit(Iscii.VISARGA) else { emit(HA); emit(Iscii.VIRAMA) }
                    Kind.AVAGRAHA -> if (has(Iscii.AVAGRAHA)) emit(Iscii.AVAGRAHA)
                    Kind.DIGIT -> emit(Iscii.DIGIT_ZERO + token.code)
                    Kind.DANDA -> out.append(
                        if (script.isIndoAryan && script != Script.GUJARATI) {
                            if (token.code == 2) "॥" else "।"
                        } else if (token.code == 2) ".." else "."
                    )
                    Kind.OTHER -> out.append(token.text)
                }
                index++
            }
            return out.toString()
        }

        // ---- Consonants ----

        private fun consonant(token: Token) {
            val next = peek(1)
            val nextNext = peek(2)

            // Gurmukhi marks a doubled consonant with addak (ੱ) instead of a conjunct.
            if (script == Script.GURMUKHI && next?.kind == Kind.VIRAMA && nextNext?.isConsonant == true &&
                nextNext.code == token.code && nextNext.nukta == token.nukta
            ) {
                emit(0x71)
                index++ // skip the virama; the second consonant is written normally
                return
            }

            // Word-final dead consonants with a dedicated letter form.
            if (next?.kind == Kind.VIRAMA && index + 1 == tokens.lastIndex) {
                if (script == Script.MALAYALAM) {
                    chillu(token)?.let { emit(it); index++; return }
                    if (token.code == MA) { emit(Iscii.ANUSVARA); index++; return } // മരം
                }
                if (script == Script.BENGALI && token.code == TA && !token.nukta) {
                    emit(0x4E) // khanda ta ৎ
                    index++
                    return
                }
            }
            writeConsonant(token)
        }

        private fun chillu(token: Token): Int? = when {
            token.code == NNA -> 0x7A
            token.code == NA -> 0x7B
            token.code == RA && !token.nukta -> 0x7C
            token.code == LA -> 0x7D
            token.code == LLA && !token.nukta -> 0x7E
            token.code == KA -> 0x7F
            else -> null
        }

        private fun writeConsonant(token: Token) {
            val code = token.code
            when (script) {
                Script.TAMIL -> return tamilConsonant(token)
                Script.GURMUKHI -> when {
                    code == SHA || code == SSA -> { emit(SA); emit(Iscii.NUKTA); return }
                    code == LLA -> { emit(LA); emit(Iscii.NUKTA); return }
                    code == DDA && token.nukta -> { emit(0x5C); return } // ੜ
                    code == DDHA && token.nukta -> { emit(0x5C); emit(HA); return }
                }
                Script.BENGALI -> {
                    val assamese = language == Language.ASSAMESE
                    when {
                        code == RA && assamese && !token.nukta -> { emit(0x70); return } // ৰ
                        code == VA && assamese && !afterVirama() -> { emit(0x71); return } // ৱ
                        code == YA && (token.nukta || followsVowel()) -> { emit(YA); emit(Iscii.NUKTA); return } // য়
                    }
                }
                Script.ORIYA -> when {
                    code == YA && (token.nukta || followsVowel()) -> { emit(0x5F); return } // ୟ
                }
                else -> Unit
            }

            // Dravidian letters written with a nukta elsewhere (ṉ ṟ ḻ) have their own offsets.
            if (token.nukta) {
                val standalone = when (code) {
                    NA -> NNNA
                    RA -> RRA
                    LLA -> LLLA
                    else -> null
                }
                // Devanagari ऩ ऱ ऴ are precomposed in NFC, unlike its other nukta letters.
                if (standalone != null && (has(standalone) || script == Script.DEVANAGARI)) {
                    emit(standalone)
                    return
                }
            }

            val resolved = resolveConsonant(code)
            emit(resolved)
            if (token.nukta && resolved == code && nuktaAllowed(code)) emit(Iscii.NUKTA)
        }

        /** Scripts use nukta letters only for the sounds their orthography actually writes with it. */
        private fun nuktaAllowed(code: Int): Boolean = when (script) {
            Script.DEVANAGARI -> true
            Script.GURMUKHI -> code == KHA || code == GA || code == JA || code == PHA
            Script.BENGALI, Script.ORIYA -> code == DDA || code == DDHA || code == YA
            Script.KANNADA -> code == JA || code == PHA
            else -> false
        }

        private fun resolveConsonant(code: Int): Int {
            if (has(code)) return code
            return CONSONANT_FALLBACKS[code]?.firstOrNull { has(it) } ?: code
        }

        private fun tamilConsonant(token: Token) {
            val code = token.code
            if (token.nukta) {
                when (code) {
                    PHA -> { emit(Iscii.VISARGA); emit(PA); return } // ஃப for f
                    NA -> { emit(NNNA); return }
                    RA -> { emit(RRA); return }
                    LLA -> { emit(LLLA); return }
                }
            }
            if (code == NA) {
                // ந starts words and precedes த; ன is used everywhere else.
                val beforeDental = peek(1)?.kind == Kind.VIRAMA && peek(2)?.let { it.isConsonant && it.code == TA } == true
                emit(if (index == 0 || beforeDental || previousIsBoundary()) NA else NNNA)
                return
            }
            emit(resolveConsonant(code))
        }

        private fun afterVirama() = tokens.getOrNull(index - 1)?.kind == Kind.VIRAMA

        /** True when this letter follows a pronounced vowel within the word. */
        private fun followsVowel(): Boolean {
            val previous = tokens.getOrNull(index - 1) ?: return false
            return previous.isConsonant || previous.isVowelBearing
        }

        private fun previousIsBoundary(): Boolean {
            val previous = tokens.getOrNull(index - 1) ?: return true
            return previous.kind == Kind.OTHER || previous.kind == Kind.DANDA
        }

        // ---- Vowels ----

        private fun vowel(code: Int) {
            val normalized = normalizeVowel(code)
            if (has(normalized)) {
                emit(normalized)
                return
            }
            when (normalized) {
                Iscii.VOCALIC_R, Iscii.VOCALIC_RR -> { emit(RA); emit(vocalicRVowelSign()) }
                Iscii.VOCALIC_L, Iscii.VOCALIC_LL -> { emit(LA); emit(Iscii.SIGN_I) }
                else -> VOWEL_FALLBACKS[normalized]?.firstOrNull { has(it) }?.let(::emit) ?: emit(Iscii.A)
            }
        }

        private fun sign(code: Int) {
            val normalized = normalizeSign(code)
            if (has(normalized)) {
                emit(normalized)
                return
            }
            when (normalized) {
                Iscii.SIGN_VOCALIC_R, Iscii.SIGN_VOCALIC_RR -> {
                    // कृ → க்ரு / ਕ੍ਰਿ
                    emit(Iscii.VIRAMA); emit(RA); emit(vocalicRVowelSign())
                }
                Iscii.SIGN_VOCALIC_L, Iscii.SIGN_VOCALIC_LL -> { emit(Iscii.VIRAMA); emit(LA); emit(Iscii.SIGN_I) }
                else -> SIGN_FALLBACKS[normalized]?.firstOrNull { has(it) }?.let(::emit)
            }
        }

        private fun vocalicRVowelSign() = if (script == Script.TAMIL || script == Script.MALAYALAM) Iscii.SIGN_U else Iscii.SIGN_I

        /** Indo-Aryan scripts have one e and one o; Dravidian short/long pairs collapse to them. */
        private fun normalizeVowel(code: Int): Int = if (script.isIndoAryan) when (code) {
            Iscii.SHORT_E, Iscii.CANDRA_E -> if (code == Iscii.CANDRA_E && has(code)) code else Iscii.E
            Iscii.SHORT_O, Iscii.CANDRA_O -> if (code == Iscii.CANDRA_O && has(code)) code else Iscii.O
            else -> code
        } else code

        private fun normalizeSign(code: Int): Int = if (script.isIndoAryan) when (code) {
            Iscii.SIGN_SHORT_E, Iscii.SIGN_CANDRA_E -> if (code == Iscii.SIGN_CANDRA_E && has(code)) code else Iscii.SIGN_E
            Iscii.SIGN_SHORT_O, Iscii.SIGN_CANDRA_O -> if (code == Iscii.SIGN_CANDRA_O && has(code)) code else Iscii.SIGN_O
            else -> code
        } else code

        // ---- Virama and nasals ----

        private fun virama() {
            if (script == Script.GURMUKHI) {
                // Punjabi writes subjoined forms only for ra, va, ha, and ya; other clusters
                // are spelled with the inherent vowel.
                val next = peek(1)
                if (next?.isConsonant == true && next.code in GURMUKHI_SUBJOINED) emit(Iscii.VIRAMA)
                return
            }
            emit(Iscii.VIRAMA)
        }

        private fun nasal(candrabindu: Boolean) {
            val next = peek(1)?.takeIf { it.isConsonant }?.code
            when (script) {
                Script.TAMIL -> {
                    emit(Iscii.nasalFor(next))
                    emit(Iscii.VIRAMA)
                }
                Script.MALAYALAM -> if (next != null && Iscii.isStop(next)) {
                    emit(Iscii.nasalFor(next)); emit(Iscii.VIRAMA)
                } else emit(Iscii.ANUSVARA)
                Script.GURMUKHI -> emit(if (!candrabindu && afterShortVowel()) 0x70 else Iscii.ANUSVARA)
                else -> emit(if (candrabindu && has(Iscii.CANDRABINDU)) Iscii.CANDRABINDU else Iscii.ANUSVARA)
            }
        }

        /** Gurmukhi uses tippi after short vowels and bindi after long ones. */
        private fun afterShortVowel(): Boolean {
            val previous = tokens.getOrNull(index - 1) ?: return true
            return when (previous.kind) {
                Kind.CONSONANT -> true
                Kind.SIGN -> previous.code == Iscii.SIGN_I || previous.code == Iscii.SIGN_U
                Kind.VOWEL -> previous.code == Iscii.A || previous.code == Iscii.I || previous.code == Iscii.U
                else -> false
            }
        }

    }

    private val GURMUKHI_SUBJOINED = setOf(RA, VA, HA, YA)

    /** Nearest substitutes, most faithful first. */
    private val CONSONANT_FALLBACKS: Map<Int, List<Int>> = mapOf(
        KHA to listOf(KA), GA to listOf(KA), GHA to listOf(GA, KA),
        CHA to listOf(CA), JA to listOf(CA), JHA to listOf(JA, CA),
        TTHA to listOf(TTA), DDA to listOf(TTA), DDHA to listOf(DDA, TTA),
        THA to listOf(TA), DA to listOf(TA), DHA to listOf(DA, TA),
        PHA to listOf(PA), BA to listOf(PA), BHA to listOf(BA, PA),
        NGA to listOf(NA), NYA to listOf(NA), NNA to listOf(NA), NNNA to listOf(NA),
        VA to listOf(BA), SSA to listOf(SHA, SA), SHA to listOf(SA),
        LLA to listOf(LA), LLLA to listOf(LLA, LA), RRA to listOf(RA),
    )

    private val VOWEL_FALLBACKS: Map<Int, List<Int>> = mapOf(
        0x04 to listOf(Iscii.A),
        Iscii.CANDRA_E to listOf(Iscii.E, Iscii.SHORT_E),
        Iscii.SHORT_E to listOf(Iscii.E),
        Iscii.E to listOf(Iscii.SHORT_E),
        Iscii.CANDRA_O to listOf(Iscii.O, Iscii.SHORT_O),
        Iscii.SHORT_O to listOf(Iscii.O),
        Iscii.O to listOf(Iscii.SHORT_O),
    )

    private val SIGN_FALLBACKS: Map<Int, List<Int>> = mapOf(
        Iscii.SIGN_CANDRA_E to listOf(Iscii.SIGN_E, Iscii.SIGN_SHORT_E),
        Iscii.SIGN_SHORT_E to listOf(Iscii.SIGN_E),
        Iscii.SIGN_E to listOf(Iscii.SIGN_SHORT_E),
        Iscii.SIGN_CANDRA_O to listOf(Iscii.SIGN_O, Iscii.SIGN_SHORT_O),
        Iscii.SIGN_SHORT_O to listOf(Iscii.SIGN_O),
        Iscii.SIGN_O to listOf(Iscii.SIGN_SHORT_O),
    )
}
