package com.sumit.varnamala

import com.sumit.varnamala.data.ocr.OcrLanguageCatalog
import com.sumit.varnamala.data.ocr.OcrModelCategory
import com.sumit.varnamala.models.ModelRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRegistryTest {
    @Test
    fun defaultSourceIsEnglishTessdataBestModel() {
        val model = OcrLanguageCatalog.sourceOrDefault(null)

        assertEquals("eng", model.code)
        assertEquals("eng.traineddata", model.trainedDataPath)
        assertTrue(model.downloadUrl.endsWith("/eng.traineddata"))
    }

    @Test
    fun appLanguageAliasesMapToTesseractCodes() {
        assertEquals("eng", ModelRegistry.toTesseractLanguageCode("en"))
        assertEquals("hin", ModelRegistry.toTesseractLanguageCode("hi"))
        assertEquals("ben", ModelRegistry.toTesseractLanguageCode("bn"))
        assertEquals("urd", ModelRegistry.toTesseractLanguageCode("ur"))
    }

    @Test
    fun specialModelsAreManageableButNotSelectableSources() {
        val osd = OcrLanguageCatalog.find("osd")
        val equ = OcrLanguageCatalog.find("equ")

        assertNotNull(osd)
        assertNotNull(equ)
        assertEquals(OcrModelCategory.SPECIAL, osd!!.category)
        assertFalse(osd.isSelectableSource)
        assertFalse(OcrLanguageCatalog.selectableSources.any { it.code == "equ" })
    }
}
