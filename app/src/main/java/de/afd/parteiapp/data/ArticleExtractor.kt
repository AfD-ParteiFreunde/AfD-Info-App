package de.afd.parteiapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException

object ArticleExtractor {

    suspend fun load(url: String): ReaderContent = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", Http.USER_AGENT)
            .build()
        val html = Http.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string() ?: throw IOException("Empty body")
        }

        val doc = Jsoup.parse(html, url)
        val title = doc.selectFirst("meta[property=og:title]")?.attr("content")
            ?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("h1")?.text()?.takeIf { it.isNotBlank() }
            ?: doc.title()
        val image = doc.selectFirst("meta[property=og:image]")?.attr("content")
        val siteName = doc.selectFirst("meta[property=og:site_name]")?.attr("content").orEmpty()

        val container = doc.selectFirst("article") ?: doc.selectFirst("main") ?: doc.body()
        container.select(
            "script, style, nav, aside, form, iframe, noscript, figure, " +
                "[class*=advertisement], [class*=newsletter], [class*=related], [class*=comment]"
        ).remove()

        val paragraphs = container.select("p")
            .map { it.text().trim() }
            .filter { it.length >= 60 }
            .distinct()
            .take(80)

        val description = doc.selectFirst("meta[property=og:description]")?.attr("content")
            ?: doc.selectFirst("meta[name=description]")?.attr("content")

        ReaderContent(
            title = title,
            imageUrl = image,
            paragraphs = paragraphs.ifEmpty { listOfNotNull(description) },
            siteName = siteName,
        )
    }
}
