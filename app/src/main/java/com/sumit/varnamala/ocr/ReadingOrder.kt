package com.sumit.varnamala.ocr

import kotlin.math.max
import kotlin.math.min

/**
 * Orders text boxes the way a person reads them, for any layout: single lines, multi-column
 * pages, signboards with a title and several panels, and mixed scenes.
 *
 * Uses recursive XY-cut: split the boxes into bands at the widest empty horizontal gap, or if
 * there is none, into columns at the widest empty vertical gap, and recurse. Boxes that cannot
 * be cut apart are read top to bottom in row bands, left to right (right to left for [rtl]).
 */
object ReadingOrder {
    /** A plain rectangle, so the ordering can be tested without Android. */
    data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        fun width() = right - left
        fun height() = bottom - top
        fun centerY() = (top + bottom) / 2
    }

    fun <T> sort(items: List<T>, rtl: Boolean = false, rect: (T) -> Box): List<T> {
        if (items.size <= 1) return items
        val lineSize = items.map { min(rect(it).width(), rect(it).height()) }.sorted()
            .let { it[it.size / 2] }.coerceAtLeast(1)
        return cut(items, rect, rtl, lineSize, depth = 0)
    }

    private fun <T> cut(items: List<T>, rect: (T) -> Box, rtl: Boolean, lineSize: Int, depth: Int): List<T> {
        if (items.size <= 1 || depth > MAX_DEPTH) return items
        // Text columns first: when aligned lines would otherwise be read across the page, a person
        // reads one whole column and then the next. A price list (wide label, narrow value) is not
        // this case, so those are still read row by row below.
        textColumns(items, rect, lineSize)?.let { columns ->
            val ordered = if (rtl) columns.reversed() else columns
            return ordered.flatMap { cut(it, rect, rtl, lineSize, depth + 1) }
        }
        // Rows first: a gap of half a line height separates stacked blocks.
        splitAtGaps(items, rect, vertical = false, minGap = (lineSize * ROW_GAP).toInt().coerceAtLeast(2))?.let { bands ->
            // After each band, check whether what is left is a set of text columns (a title above
            // two columns, say) so the columns are not read row by row.
            val ordered = ArrayList<T>()
            for (index in bands.indices) {
                val rest = bands.drop(index).flatten()
                val columns = if (index > 0) textColumns(rest, rect, lineSize) else null
                if (columns != null) {
                    (if (rtl) columns.reversed() else columns).forEach { ordered += cut(it, rect, rtl, lineSize, depth + 1) }
                    break
                }
                ordered += cut(bands[index], rect, rtl, lineSize, depth + 1)
            }
            return ordered
        }
        // Then columns: a wider gap is needed so a wide space inside a line is not a column.
        splitAtGaps(items, rect, vertical = true, minGap = (lineSize * COLUMN_GAP).toInt().coerceAtLeast(4))?.let { columns ->
            val ordered = if (rtl) columns.reversed() else columns
            return ordered.flatMap { cut(it, rect, rtl, lineSize, depth + 1) }
        }
        return readLines(items, rect, rtl, lineSize)
    }

    /**
     * The columns of [items] when they look like parallel text columns: two to four groups
     * separated by a clear gutter, each with several lines and a similar width.
     */
    private fun <T> textColumns(items: List<T>, rect: (T) -> Box, lineSize: Int): List<List<T>>? {
        val columns = splitAtGaps(items, rect, vertical = true, minGap = (lineSize * COLUMN_GAP).toInt().coerceAtLeast(4))
            ?: return null
        if (columns.size !in 2..MAX_COLUMNS || columns.any { it.size < 2 }) return null
        val widths = columns.map { column -> column.maxOf { rect(it).right } - column.minOf { rect(it).left } }
        if (widths.max().toFloat() / widths.min().coerceAtLeast(1) > MAX_WIDTH_RATIO) return null
        // Columns of one text start at the same height; unrelated boxes scattered around a scene do not.
        val tops = columns.map { column -> column.minOf { rect(it).top } }
        if (tops.max() - tops.min() > lineSize * 2) return null
        return columns
    }

    /** Splits at every empty gap of at least [minGap] along one axis; null when there is none. */
    private fun <T> splitAtGaps(items: List<T>, rect: (T) -> Box, vertical: Boolean, minGap: Int): List<List<T>>? {
        fun start(r: Box) = if (vertical) r.left else r.top
        fun end(r: Box) = if (vertical) r.right else r.bottom
        val sorted = items.sortedBy { start(rect(it)) }
        val groups = mutableListOf<MutableList<T>>()
        var reach = Int.MIN_VALUE
        for (item in sorted) {
            val r = rect(item)
            if (groups.isEmpty() || start(r) - reach >= minGap) groups += mutableListOf<T>()
            groups.last() += item
            reach = if (groups.last().size == 1) end(r) else max(reach, end(r))
        }
        return groups.takeIf { it.size > 1 }
    }

    /** Fallback for interleaved boxes: group by vertical overlap into rows, then order each row. */
    private fun <T> readLines(items: List<T>, rect: (T) -> Box, rtl: Boolean, lineSize: Int): List<T> {
        val rows = mutableListOf<MutableList<T>>()
        val rowCenters = mutableListOf<Int>()
        for (item in items.sortedBy { rect(it).centerY() }) {
            val center = rect(item).centerY()
            val index = rowCenters.indexOfLast { kotlin.math.abs(it - center) <= lineSize / 2 }
            if (index >= 0) {
                rows[index] += item
            } else {
                rows += mutableListOf(item)
                rowCenters += center
            }
        }
        return rows.flatMap { row -> if (rtl) row.sortedByDescending { rect(it).left } else row.sortedBy { rect(it).left } }
    }

    private const val ROW_GAP = 0.5f
    private const val COLUMN_GAP = 1.0f
    private const val MAX_DEPTH = 12
    private const val MAX_COLUMNS = 4
    private const val MAX_WIDTH_RATIO = 2.5f
}
