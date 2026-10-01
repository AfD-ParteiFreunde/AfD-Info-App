package de.afd.parteiapp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class PageHit(val page: Int, val snippet: String)

data class WahlprogrammData(
    val pdfFile: File,
    val pages: List<String>,
    val source: String,
    val fetchedAt: Long,
    val live: Boolean,
)

object WahlprogrammRepository {

    private const val ASSET_PDF = "wahlprogramm.pdf"
    private const val ASSET_PAGES = "wahlprogramm_pages.json"
    private const val CACHE_PDF = "wahlprogramm_live.pdf"
    private const val CACHE_PAGES = "wahlprogramm_live_pages.json"
    private const val CACHE_META = "wahlprogramm_meta.json"

    private const val INDEX = "https://www.afd.de/wahlprogramm25/"

    fun loadBest(context: Context): WahlprogrammData {
        val pagesFile = File(context.filesDir, CACHE_PAGES)
        val pdfFile = File(context.filesDir, CACHE_PDF)
        if (pagesFile.exists() && pdfFile.exists()) {
            val pages = runCatching { readPages(pagesFile.readText()) }.getOrNull()
            if (!pages.isNullOrEmpty()) {
                val meta = runCatching {
                    org.json.JSONObject(File(context.filesDir, CACHE_META).readText())
                }.getOrNull()
                return WahlprogrammData(
                    pdfFile = pdfFile,
                    pages = pages,
                    source = "afd.de",
                    fetchedAt = meta?.optLong("fetchedAt") ?: 0L,
                    live = true,
                )
            }
        }
        return bundled(context)
    }

    fun bundled(context: Context): WahlprogrammData {
        val pdf = File(context.cacheDir, "wahlprogramm_bundled.pdf")
        if (!pdf.exists() || pdf.length() == 0L) {
            runCatching {
                context.assets.open(ASSET_PDF).use { input ->
                    pdf.outputStream().use { input.copyTo(it) }
                }
            }
        }
        val pages = runCatching {
            context.assets.open(ASSET_PAGES).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrDefault("").let { readPages(it) }
        return WahlprogrammData(pdf, pages, "afd.de (Offline)", 0L, live = false)
    }

    /**
     * Downloads the newest programme PDF and extracts its per-page text
     * so topic buttons / search can jump to the right page.
     */
    suspend fun refresh(context: Context): WahlprogrammData = withContext(Dispatchers.IO) {
        val current = loadBest(context)
        val pdfUrl = runCatching { findPdf() }.getOrNull() ?: return@withContext current
        val bytes = runCatching { Http.getBytes(pdfUrl) }.getOrNull() ?: return@withContext current
        if (bytes.size < 50_000) return@withContext current

        val pdfFile = File(context.filesDir, CACHE_PDF)
        runCatching { pdfFile.writeBytes(bytes) }.onFailure { return@withContext current }

        val pages = runCatching { extractPages(context, pdfFile) }.getOrNull()
        if (pages.isNullOrEmpty()) return@withContext current

        runCatching {
            File(context.filesDir, CACHE_PAGES).writeText(JSONArray(pages).toString())
            File(context.filesDir, CACHE_META).writeText(
                org.json.JSONObject()
                    .put("url", pdfUrl)
                    .put("fetchedAt", System.currentTimeMillis())
                    .toString()
            )
        }
        WahlprogrammData(pdfFile, pages, "afd.de", System.currentTimeMillis(), live = true)
    }

    /** Headline topic buttons shown on top of the reader. */
    val TOPICS: Map<String, String> = linkedMapOf(
        "Migration" to "Migration",
        "Asyl" to "Asyl",
        "Sicherheit" to "Innere Sicherheit",
        "Religion" to "Religion",
        "Digitalisierung" to "Digitalisierung",
        "Wirtschaft" to "Wirtschaft",
        "Energie" to "Energie",
        "Familie" to "Familie",
        "Bildung" to "Bildung",
        "Gesundheit" to "Gesundheit",
        "Europa" to "EU",
        "Steuern" to "Steuern",
        "Klima" to "Klima",
    )

    fun search(data: WahlprogrammData, query: String): List<PageHit> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        val hits = mutableListOf<PageHit>()
        data.pages.forEachIndexed { index, page ->
            val at = page.indexOf(q, ignoreCase = true)
            if (at >= 0) {
                val start = (at - 90).coerceAtLeast(0)
                val end = (at + q.length + 120).coerceAtMost(page.length)
                val snippet = (if (start > 0) "… " else "") +
                    page.substring(start, end).trim() + " …"
                hits += PageHit(index, snippet)
            }
        }
        return hits
    }

    private suspend fun findPdf(): String? {
        val html = Http.get(INDEX)
        val matches = Regex("""https://www\.afd\.de/wp-content/uploads/[^"']+\.pdf""")
            .findAll(html)
            .map { it.value.replace("&amp;", "&") }
            .filter {
                it.contains("wahlprogramm", ignoreCase = true) ||
                    it.contains("Wahlprogramm", ignoreCase = true)
            }
            .toList()
        return matches.firstOrNull {
            !it.contains("Kernforderungen", ignoreCase = true) &&
                !it.contains("Flugblatt", ignoreCase = true)
        } ?: matches.firstOrNull()
    }

    private fun extractPages(context: Context, pdf: File): List<String> {
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context.applicationContext)
        val doc = com.tom_roush.pdfbox.pdmodel.PDDocument.load(pdf)
        val stripper = com.tom_roush.pdfbox.text.PDFTextStripper().apply { sortByPosition = true }
        val out = ArrayList<String>(doc.numberOfPages)
        for (i in 1..doc.numberOfPages) {
            stripper.startPage = i
            stripper.endPage = i
            val text = runCatching { stripper.getText(doc) }.getOrDefault("")
            out += text.replace(Regex("\\s+"), " ").trim()
        }
        doc.close()
        return out
    }

    private fun readPages(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        val array = runCatching {
            val trimmed = json.trim()
            if (trimmed.startsWith("{")) {
                JSONObject(trimmed).optJSONArray("pages") ?: JSONArray()
            } else {
                JSONArray(trimmed)
            }
        }.getOrElse { return emptyList() }
        return (0 until array.length()).map { array.optString(it) }
    }
}
