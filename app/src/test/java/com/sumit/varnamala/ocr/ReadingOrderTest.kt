package com.sumit.varnamala.ocr

import com.sumit.varnamala.ocr.ReadingOrder.Box
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingOrderTest {
    private fun order(items: List<Pair<String, Box>>, rtl: Boolean = false) =
        ReadingOrder.sort(items, rtl) { it.second }.map { it.first }

    @Test
    fun `a single row reads left to right whatever order it arrives in`() {
        val items = listOf("c" to Box(400, 100, 500, 140), "a" to Box(0, 100, 100, 140), "b" to Box(200, 102, 300, 142))
        assertEquals(listOf("a", "b", "c"), order(items))
    }

    @Test
    fun `stacked lines read top to bottom`() {
        val items = listOf("2" to Box(0, 200, 400, 240), "3" to Box(0, 400, 400, 440), "1" to Box(0, 0, 400, 40))
        assertEquals(listOf("1", "2", "3"), order(items))
    }

    @Test
    fun `two columns read the whole left column before the right one`() {
        // Rows of the two columns line up, which a plain top-to-bottom sort would interleave.
        val items = listOf(
            "R1" to Box(700, 0, 1000, 40), "L1" to Box(0, 0, 300, 40),
            "R2" to Box(700, 100, 1000, 140), "L2" to Box(0, 100, 300, 140),
            "R3" to Box(700, 200, 1000, 240), "L3" to Box(0, 200, 300, 240),
        )
        assertEquals(listOf("L1", "L2", "L3", "R1", "R2", "R3"), order(items))
    }

    @Test
    fun `a title above two columns comes first`() {
        val items = listOf(
            "R1" to Box(700, 200, 1000, 240), "TITLE" to Box(0, 0, 1000, 80),
            "L1" to Box(0, 200, 300, 240), "L2" to Box(0, 300, 300, 340), "R2" to Box(700, 300, 1000, 340),
        )
        assertEquals(listOf("TITLE", "L1", "L2", "R1", "R2"), order(items))
    }

    @Test
    fun `right to left scripts read the right column first`() {
        val items = listOf("L" to Box(0, 0, 300, 40), "R" to Box(700, 0, 1000, 40), "L2" to Box(0, 100, 300, 140), "R2" to Box(700, 100, 1000, 140))
        assertEquals(listOf("R", "R2", "L", "L2"), order(items, rtl = true))
    }

    @Test
    fun `a scattered scene still gives a stable top to bottom order`() {
        val items = listOf(
            "shop" to Box(500, 20, 800, 70), "open" to Box(60, 400, 200, 440), "sale" to Box(100, 180, 260, 230),
            "phone" to Box(600, 600, 900, 650),
        )
        assertEquals(listOf("shop", "sale", "open", "phone"), order(items))
    }

    @Test
    fun `empty and single inputs are returned unchanged`() {
        assertEquals(emptyList<String>(), order(emptyList()))
        assertEquals(listOf("only"), order(listOf("only" to Box(0, 0, 10, 10))))
    }
}
