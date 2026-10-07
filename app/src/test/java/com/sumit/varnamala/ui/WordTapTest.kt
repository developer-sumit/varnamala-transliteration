package com.sumit.varnamala.ui

import com.sumit.varnamala.ui.components.speechLanguage
import com.sumit.varnamala.ui.components.wordAt
import org.junit.Assert.assertEquals
import org.junit.Test

class WordTapTest {
    @Test fun `a tap picks the word under it`() {
        assertEquals("NEW", wordAt("NEW DELHI STATION", 0.05f))
        assertEquals("DELHI", wordAt("NEW DELHI STATION", 0.4f))
        assertEquals("STATION", wordAt("NEW DELHI STATION", 0.95f))
    }

    @Test fun `a single word line is returned whole`() {
        assertEquals("EXIT", wordAt("  EXIT ", 0.5f))
    }

    @Test fun `the speech language follows the main language of a mixed result`() {
        assertEquals("hi", speechLanguage("mixed_hi_en"))
        assertEquals("ta", speechLanguage("ta"))
        assertEquals("en", speechLanguage("unknown"))
        assertEquals("en", speechLanguage(null))
        assertEquals("en", speechLanguage("mixed_multi"))
    }
}
