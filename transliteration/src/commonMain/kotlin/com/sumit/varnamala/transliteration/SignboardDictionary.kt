package com.sumit.varnamala.transliteration

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Curated spellings for romanized words that rules get wrong, keyed by lowercase word and then
 * by target language code: `{"station": {"hi": "स्टेशन", "ta": "ஸ்டேஷன்"}}`.
 */
class SignboardDictionary private constructor(
    private val entries: Map<String, Map<String, String>>,
) {
    val size: Int get() = entries.size

    fun lookup(word: String, language: Language): String? =
        entries[word.lowercase()]?.get(language.code)

    /** Entry for any language written in [script], used when the exact language is missing. */
    internal fun lookupByScript(word: String, script: Script): Pair<String, Language>? {
        val translations = entries[word.lowercase()] ?: return null
        return translations.entries.firstNotNullOfOrNull { (code, value) ->
            Language.fromCode(code)?.takeIf { it.script == script }?.let { value to it }
        }
    }

    fun contains(word: String): Boolean = word.lowercase() in entries

    companion object {
        val EMPTY = SignboardDictionary(emptyMap())

        /**
         * Parses the JSON format above. Entries containing control characters are skipped:
         * they come from corrupted data and would render as garbage.
         */
        fun fromJson(json: String): SignboardDictionary {
            val root = Json.parseToJsonElement(json).jsonObject
            val entries = HashMap<String, Map<String, String>>(root.size)
            for ((word, value) in root) {
                val translations = (value as? JsonObject) ?: continue
                val clean = translations.mapNotNull { (language, spelling) ->
                    val text = (spelling as? JsonPrimitive)?.takeIf { it.isString }?.content
                    text?.takeIf { it.isNotBlank() && it.none { ch -> ch.code < 0x20 } }?.let { language to it }
                }.toMap()
                if (clean.isNotEmpty()) entries[word.lowercase()] = clean
            }
            return SignboardDictionary(entries)
        }
    }
}
