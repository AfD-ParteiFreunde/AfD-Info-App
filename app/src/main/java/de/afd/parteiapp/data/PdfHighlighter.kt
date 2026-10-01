package de.afd.parteiapp.data

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.File

/**
 * Finds every occurrence of [query] across a PDF and returns, per page,
 * rectangles in NORMALISED page coordinates (0..1, origin top-left), so the
 * caller can scale them onto a rendered bitmap of any size.
 */
object PdfHighlighter {

    data class PageHighlight(val page: Int, val rects: List<FloatArray>)

    private var initialised = false

    fun compute(context: Context, pdfPath: String, query: String): List<PageHighlight> {
        val q = query.trim().lowercase()
        if (q.length < 4) return emptyList()
        return runCatching {
            if (!initialised) {
                PDFBoxResourceLoader.init(context.applicationContext)
                initialised = true
            }
            scan(pdfPath, q)
        }.getOrDefault(emptyList())
    }

    private fun scan(pdfPath: String, q: String): List<PageHighlight> {
        val doc = PDDocument.load(File(pdfPath))
        val result = mutableListOf<PageHighlight>()
        val chars = mutableListOf<TextPosition>()
        val buffer = StringBuilder()
        var currentPage = 0

        val stripper = object : PDFTextStripper() {
            private var index = -1

            override fun startPage(page: PDPage) {
                index++
                currentPage = index
                chars.clear()
                buffer.setLength(0)
            }

            override fun endPage(page: PDPage) {
                val stream = buffer.toString().lowercase()
                val rects = mutableListOf<FloatArray>()
                var from = 0
                while (true) {
                    val at = stream.indexOf(q, from)
                    if (at < 0) break
                    val end = at + q.length
                    if (at < chars.size && end <= chars.size) {
                        rects += normalise(chars.subList(at, end), page)
                    }
                    from = at + q.length
                }
                if (rects.isNotEmpty()) result += PageHighlight(currentPage, rects)
            }

            override fun writeString(text: String, textPositions: List<TextPosition>) {
                textPositions.forEach { tp ->
                    val c = tp.unicode
                    if (c.isNotEmpty()) {
                        buffer.append(c)
                        chars.add(tp)
                    }
                }
            }
        }
        stripper.sortByPosition = true
        try {
            stripper.getText(doc)
        } finally {
            runCatching { doc.close() }
        }
        return result
    }

    private fun normalise(slice: List<TextPosition>, page: PDPage): FloatArray {
        val pageW = page.mediaBox.width
        val pageH = page.mediaBox.height
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minTop = Float.MAX_VALUE
        var maxBottom = -Float.MAX_VALUE
        slice.forEach { tp ->
            minX = minOf(minX, tp.xDirAdj)
            maxX = maxOf(maxX, tp.xDirAdj + tp.widthDirAdj)
            // yDirAdj is the baseline measured from the TOP of the page, downward
            minTop = minOf(minTop, tp.yDirAdj - tp.heightDir)
            maxBottom = maxOf(maxBottom, tp.yDirAdj)
        }
        val left = (minX / pageW).coerceIn(0f, 1f)
        val right = (maxX / pageW).coerceIn(0f, 1f)
        val top = (minTop / pageH).coerceIn(0f, 1f)
        val bottom = (maxBottom / pageH).coerceIn(0f, 1f)
        return floatArrayOf(left, top, right, bottom)
    }
}
