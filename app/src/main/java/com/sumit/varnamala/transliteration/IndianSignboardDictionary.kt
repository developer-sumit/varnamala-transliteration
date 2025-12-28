package com.sumit.varnamala.transliteration

import android.content.Context
import org.json.JSONObject

object IndianSignboardDictionary {
    private const val ASSET_PATH = "signboard_dictionary.json"

    @Volatile
    private var cached: Map<String, Map<String, String>>? = null

    fun init(context: Context) {
        if (cached != null) return
        synchronized(this) {
            if (cached != null) return
            cached = loadFromAssets(context)
        }
    }

    fun getTransliteration(word: String, targetLanguage: String): String? {
        return (cached ?: emptyMap())[word.lowercase()]?.get(targetLanguage)
    }

    fun hasWord(word: String): Boolean {
        return (cached ?: emptyMap()).containsKey(word.lowercase())
    }

    private fun loadFromAssets(context: Context): Map<String, Map<String, String>> {
        val jsonText = context.assets.open(ASSET_PATH).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(jsonText)

        val result = HashMap<String, Map<String, String>>(root.length())
        val keys = root.keys()
        while (keys.hasNext()) {
            val word = keys.next()
            val translationsObj = root.getJSONObject(word)

            val translations = HashMap<String, String>(translationsObj.length())
            val langKeys = translationsObj.keys()
            while (langKeys.hasNext()) {
                val lang = langKeys.next()
                translations[lang] = translationsObj.getString(lang)
            }
            result[word.lowercase()] = translations
        }
        return result
    }
}
