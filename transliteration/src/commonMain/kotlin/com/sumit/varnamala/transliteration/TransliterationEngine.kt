package com.sumit.varnamala.transliteration

import com.sumit.varnamala.transliteration.Token.Kind

/**
 * Converts text between the scripts of [Language]: the nine Brahmic scripts, Latin, and Urdu.
 *
 * Text is split into runs by script, so mixed input ("मैं going home") converts each part from
 * its own script. Spaces, punctuation, and ASCII digits pass through unchanged.
 *
 * Pure Kotlin with no platform dependencies, and thread-safe: one instance can be shared.
 */
class TransliterationEngine(
    private val dictionary: SignboardDictionary = SignboardDictionary.EMPTY,
) {

    /**
     * Transliterates [text] into the script of [target].
     *
     * @param source language of the input when known. It refines conventions within a script
     *   (Sanskrit keeps inherent vowels that Hindi drops); the script itself is always detected
     *   from the text.
     * @param scheme how Latin output is spelled when [target] is [Language.ENGLISH].
     */
    fun transliterate(
        text: String,
        target: Language,
        source: Language? = null,
        scheme: RomanizationScheme = RomanizationScheme.READABLE,
    ): String {
        if (text.isEmpty()) return text
        val out = StringBuilder(text.length)
        for (run in runs(text)) {
            out.append(if (run.script == null) convertNeutral(run.text, target) else convertRun(run, target, source, scheme))
        }
        return out.toString()
    }

    /**
     * Code-based overload for callers holding language codes ("hi", "tam", "bengali").
     * Unsupported targets return the text unchanged; unsupported sources are ignored.
     */
    fun transliterate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String? = null,
        scheme: RomanizationScheme = RomanizationScheme.READABLE,
    ): String {
        val target = Language.fromCode(targetLanguage)?.takeUnless { targetLanguage.equals("unknown", true) }
            ?: return text
        return transliterate(text, target, Language.fromCode(sourceLanguage), scheme)
    }

    // ---- Runs ----

    private data class Run(val text: String, val script: Script?)

    private fun runs(text: String): List<Run> {
        val runs = mutableListOf<Run>()
        val current = StringBuilder()
        var currentScript: Script? = null

        fun flush() {
            if (current.isNotEmpty()) runs += Run(current.toString(), currentScript)
            current.clear()
        }

        for (char in text) {
            val script = when {
                // Joiners and combining marks belong to the run they appear in.
                (char.code == 0x200C || char.code == 0x200D) && currentScript?.isBrahmic == true -> currentScript
                Script.isLatinCombiningMark(char) && currentScript == Script.LATIN -> currentScript
                else -> Script.of(char) // dandas stay with neutral text and are converted there
            }
            if (script != currentScript) {
                flush()
                currentScript = script
            }
            current.append(char)
        }
        flush()
        return runs
    }

    /** Spaces, punctuation, and digits; only the shared dandas are adapted to the target. */
    private fun convertNeutral(text: String, target: Language): String {
        if (text.none { it.code == Script.DANDA || it.code == Script.DOUBLE_DANDA }) return text
        val tokens = text.map {
            when (it.code) {
                Script.DANDA -> Token(Kind.DANDA, 1)
                Script.DOUBLE_DANDA -> Token(Kind.DANDA, 2)
                else -> Token(Kind.OTHER, text = it.toString())
            }
        }
        return write(tokens, target, Language.HINDI, RomanizationScheme.READABLE)
    }

    private fun convertRun(run: Run, target: Language, hint: Language?, scheme: RomanizationScheme): String {
        val script = run.script!!
        val source = resolveSource(run, hint)

        if (script == target.script && !needsRewrite(source, target)) return run.text

        if (script == Script.LATIN) return fromLatin(run.text, target, scheme)

        var tokens = when (script) {
            Script.URDU -> UrduCodec.read(run.text)
            else -> IndicReader.read(run.text, script)
        }
        tokens = adjustInherentVowels(tokens, source, target)
        return write(tokens, target, source, scheme)
    }

    private fun resolveSource(run: Run, hint: Language?): Language {
        val script = run.script!!
        if (script == Script.BENGALI && ('ৰ' in run.text || 'ৱ' in run.text)) return Language.ASSAMESE
        return hint?.takeIf { it.script == script } ?: Language.defaultFor(script)
    }

    /** Bengali and Assamese share a script but spell ra and wa differently. */
    private fun needsRewrite(source: Language, target: Language): Boolean =
        target.script == Script.BENGALI && source != target

    private fun fromLatin(word: String, target: Language, scheme: RomanizationScheme): String {
        if (target.script == Script.LATIN) return word
        dictionary.lookup(word, target)?.let { return it }
        dictionary.lookupByScript(word, target.script)?.let { (spelling, _) -> return spelling }
        if (target == Language.URDU) {
            dictionary.lookup(word, Language.HINDI)?.let {
                return UrduCodec.write(IndicReader.read(it, Script.DEVANAGARI))
            }
        }
        return write(LatinReader.read(word, target), target, Language.ENGLISH, scheme)
    }

    /**
     * Carries pronunciation across the schwa boundary: Hindi राम (rām) becomes Tamil ராம், not
     * ராம (rāma), and Tamil ராம் becomes Hindi राम without a visible virama.
     */
    private fun adjustInherentVowels(tokens: List<Token>, source: Language, target: Language): List<Token> {
        if (source.deletesSchwa && target.shortEO) {
            val silent = SchwaDeletion.silentSchwas(tokens)
            if (silent.isEmpty()) return tokens
            return buildList {
                tokens.forEachIndexed { index, token ->
                    add(token)
                    if (index in silent) add(Token(Kind.VIRAMA))
                }
            }
        }
        if (source.writesFinalVirama && target.script.isIndoAryan && !target.writesFinalVirama) {
            return tokens.filterIndexed { index, token ->
                !(token.kind == Kind.VIRAMA && index == tokens.lastIndex && index > 0 && tokens[index - 1].isConsonant)
            }
        }
        return tokens
    }

    private fun write(tokens: List<Token>, target: Language, source: Language, scheme: RomanizationScheme): String =
        when (target.script) {
            Script.LATIN -> LatinWriter.write(tokens, source, scheme)
            Script.URDU -> UrduCodec.write(tokens)
            else -> IndicWriter.write(tokens, target)
        }
}
