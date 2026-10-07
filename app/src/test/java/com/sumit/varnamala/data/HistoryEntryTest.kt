package com.sumit.varnamala.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryEntryTest {
    @Test fun `an entry survives a JSON round trip, including collection and picture`() {
        val entry = HistoryEntry(
            originalText = "स्टेशन", transliteratedText = "station", sourceLanguage = "hi", targetLanguage = "en",
            isFavorite = true, source = "camera", collection = "Goa trip", imageRef = "/data/thumbs/a.jpg",
        )
        assertEquals(entry, HistoryEntry.fromJson(entry.toJson()))
    }

    @Test fun `entries saved before collections existed still load`() {
        val old = org.json.JSONObject("""{"id":"1","originalText":"a","transliteratedText":"b","sourceLanguage":"en","targetLanguage":"hi","timestamp":5,"isFavorite":false,"source":"text_input"}""")
        val entry = HistoryEntry.fromJson(old)
        assertEquals("", entry.collection)
        assertEquals("", entry.imageRef)
    }
}
