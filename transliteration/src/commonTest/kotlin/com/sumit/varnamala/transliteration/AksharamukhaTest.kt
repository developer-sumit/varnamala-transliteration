package com.sumit.varnamala.transliteration

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class AksharamukhaTest {

    @Test
    fun testSupportedScriptsAndSchemes() {
        val scripts = Aksharamukha.supportedScripts()
        assertTrue(scripts.contains("Devanagari"))
        assertTrue(scripts.contains("Bengali"))
        assertTrue(scripts.contains("Tamil"))
        assertTrue(scripts.contains("Telugu"))
        assertTrue(scripts.contains("Kannada"))
        assertTrue(scripts.contains("Malayalam"))
        assertTrue(scripts.contains("Gujarati"))
        assertTrue(scripts.contains("Gurmukhi"))

        val roman = Aksharamukha.supportedRoman()
        assertTrue(roman.contains("IAST"))
        assertTrue(roman.contains("HK"))
        assertTrue(roman.contains("ISO15919"))

        val devaMap = Aksharamukha.getScriptMapping("Devanagari")
        assertTrue(devaMap != null && devaMap.isNotEmpty())
        assertEquals("क", devaMap?.get("k"))
    }

    @Test
    fun testDevanagariToBengali() {
        val result = Aksharamukha.process("Devanagari", "Bengali", "नमस्ते")
        assertEquals("নমস্তে", result)
    }

    @Test
    fun testDevanagariToTelugu() {
        val result = Aksharamukha.process("Devanagari", "Telugu", "नमस्ते")
        assertEquals("నమస్తే", result)
    }

    @Test
    fun testDevanagariToTamil() {
        val result = Aksharamukha.process("Devanagari", "Tamil", "नमस्ते")
        assertEquals("நமஸ்தே", result)
    }

    @Test
    fun testDevanagariToKannada() {
        val result = Aksharamukha.process("Devanagari", "Kannada", "नमस्ते")
        assertEquals("ನಮಸ್ತೇ", result)
    }

    @Test
    fun testDevanagariToMalayalam() {
        val result = Aksharamukha.process("Devanagari", "Malayalam", "नमस्ते")
        assertEquals("നമസ്തേ", result)
    }

    @Test
    fun testDevanagariToGujarati() {
        val result = Aksharamukha.process("Devanagari", "Gujarati", "नमस्ते")
        assertEquals("નમસ્તે", result)
    }

    @Test
    fun testDevanagariToOriya() {
        val result = Aksharamukha.process("Devanagari", "Oriya", "नमस्ते")
        assertEquals("ନମସ୍ତେ", result)
    }

    @Test
    fun testHarvardKyotoToDevanagari() {
        val result = Aksharamukha.process("HK", "Devanagari", "namaste")
        assertEquals("नमस्ते", result)

        val krishna = Aksharamukha.process("HK", "Devanagari", "kRSNa")
        assertEquals("कृष्ण", krishna)
    }

    @Test
    fun testDevanagariToIAST() {
        val result = Aksharamukha.process("Devanagari", "IAST", "नमस्ते")
        assertEquals("namaste", result)

        val krishna = Aksharamukha.process("Devanagari", "IAST", "कृष्ण")
        assertEquals("kṛṣṇa", krishna)
    }

    @Test
    fun testAutodetectSource() {
        val fromDevanagari = Aksharamukha.process("autodetect", "Telugu", "भारत")
        assertEquals("భారత", fromDevanagari)

        val fromRoman = Aksharamukha.process("autodetect", "Devanagari", "namaste")
        assertEquals("नमस्ते", fromRoman)
    }

    @Test
    fun testEngineIntegration() {
        val engine = TransliterationEngine()
        val result = engine.aksharamukha("नमस्ते", "Telugu")
        assertEquals("నమస్తే", result)
    }

    @Test
    fun testDevanagariToSinhala() {
        val result = Aksharamukha.process("Devanagari", "Sinhala", "नमस्ते")
        assertEquals("නමස්තේ", result)
    }

    @Test
    fun testSinhalaToDevanagari() {
        val result = Aksharamukha.process("Sinhala", "Devanagari", "නමස්තේ")
        assertEquals("नमस्ते", result)
    }

    @Test
    fun testDevanagariToEnglishAndReadable() {
        val toEn = Aksharamukha.process("Devanagari", "en", "नमस्ते")
        assertEquals("namaste", toEn)

        val toReadable = Aksharamukha.process("Devanagari", "Readable", "नमस्ते")
        assertEquals("namaste", toReadable)
    }

    @Test
    fun testEnglishToDevanagari() {
        val fromEn = Aksharamukha.process("en", "Devanagari", "namaste")
        assertEquals("नमस्ते", fromEn)

        val fromCapitalized = Aksharamukha.process("en", "Devanagari", "Namaste")
        assertEquals("नमस्ते", fromCapitalized)
    }

    @Test
    fun testLanguageCodeMapping() {
        val hiToTe = Aksharamukha.process("hi", "te", "नमस्ते")
        assertEquals("నమస్తే", hiToTe)

        val hiToEn = Aksharamukha.process("hi", "en", "नमस्ते")
        assertEquals("namaste", hiToEn)

        val enToHi = Aksharamukha.process("en", "hi", "namaste")
        assertEquals("नमस्ते", enToHi)
    }

    @Test
    fun testUrduTransliteration() {
        val toUrdu = Aksharamukha.process("Devanagari", "Urdu", "दोस्त")
        assertEquals("دوست", toUrdu)

        val fromUrdu = Aksharamukha.process("Urdu", "Devanagari", "دوست")
        assertEquals("दोस्त", fromUrdu)
    }

    @Test
    fun testEngineAksharamukhaFallbackForSinhala() {
        val engine = TransliterationEngine()
        val sinhalaResult = engine.transliterate("नमस्ते", "si")
        assertEquals("නමස්තේ", sinhalaResult)
    }
}
