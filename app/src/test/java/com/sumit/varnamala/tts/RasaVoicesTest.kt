package com.sumit.varnamala.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RasaVoicesTest {
    @Test fun `every voice has its own speaker id inside the model range`() {
        val ids = RasaVoices.all.map { it.speakerId }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it in 0 until RasaTtsEngine.SPEAKERS })
    }

    @Test fun `voices are numbered per gender starting at one`() {
        assertEquals((1..RasaVoices.female.size).toList(), RasaVoices.female.map { it.number })
        assertEquals((1..RasaVoices.male.size).toList(), RasaVoices.male.map { it.number })
    }

    @Test fun `an unknown speaker falls back to the default voice`() {
        assertEquals(RasaVoices.default, RasaVoices.find(999))
    }

    @Test fun `speaking styles stay inside the model's style range`() {
        assertTrue(RasaVoices.Style.entries.all { it.emotionId in 0 until RasaTtsEngine.EMOTIONS })
        assertEquals(RasaVoices.Style.NEUTRAL, RasaVoices.Style.fromId(31))
    }
}
