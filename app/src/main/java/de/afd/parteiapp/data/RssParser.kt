package de.afd.parteiapp.data

import android.text.Html
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object RssParser {

    private val imgRegex = Regex("<img[^>]+src=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
    private val tagRegex = Regex("<[^>]+>")
    private val spaceRegex = Regex("\\s+")
    private val parenRegex = Regex("\\(.*\\)")
    private val ytThumbRegex = Regex("""youtube\.com/v/([A-Za-z0-9_-]{6,})""")
    private val ytLinkRegex = Regex("""youtube\.com/watch\?v=([A-Za-z0-9_-]{6,})""")

    private val dateFormatters = listOf(
        DateTimeFormatter.RFC_1123_DATE_TIME,
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ISO_ZONED_DATE_TIME,
    )

    fun parse(stream: InputStream, source: FeedSource): List<Article> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, null)

        val articles = mutableListOf<Article>()
        var builder: ItemBuilder? = null
        var text = StringBuilder()
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name?.lowercase(Locale.ROOT) ?: ""
                    if (name == "item" || name == "entry") {
                        builder = ItemBuilder()
                    }
                    builder?.onStart(parser, name)
                    text = StringBuilder()
                }
                XmlPullParser.TEXT -> text.append(parser.text)
                XmlPullParser.END_TAG -> {
                    val name = parser.name?.lowercase(Locale.ROOT) ?: ""
                    val value = text.toString().trim()
                    if (name == "item" || name == "entry") {
                        builder?.let { articles += it.build(source) }
                        builder = null
                    } else if (value.isNotEmpty()) {
                        builder?.onText(name, value)
                    }
                    text = StringBuilder()
                }
            }
            event = parser.next()
        }

        return articles
            .filter { it.link.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.link }
            .sortedByDescending { it.publishedAt }
    }

    private class ItemBuilder {
        private var title = ""
        private var link = ""
        private var description = ""
        private var content = ""
        private var image: String? = null
        private var publishedAt = 0L

        fun onStart(parser: XmlPullParser, name: String) {
            when {
                name == "link" -> {
                    val rel = parser.getAttributeValue(null, "rel")
                    val href = parser.getAttributeValue(null, "href")
                    if (href != null && (rel == null || rel == "alternate") && link.isBlank()) {
                        link = href
                    }
                }
                name == "enclosure" || name.endsWith("content") || name.endsWith("thumbnail") -> {
                    val url = parser.getAttributeValue(null, "url")
                    if (url != null && image == null && url.startsWith("http")) image = url
                }
            }
        }

        fun onText(name: String, value: String) {
            when {
                name == "title" -> if (title.isBlank()) title = clean(value)
                name == "link" -> if (link.isBlank() && value.startsWith("http")) link = value
                name == "guid" || name == "id" -> if (link.isBlank() && value.startsWith("http")) link = value
                name == "description" || name.endsWith(":description") -> if (description.isBlank()) description = value
                name == "summary" -> if (description.isBlank()) description = value
                name == "encoded" -> if (content.isBlank()) content = value
                name == "pubdate" || name == "published" || name == "updated" || name == "date" -> {
                    if (publishedAt == 0L) publishedAt = parseDate(value)
                }
            }
        }

        fun build(source: FeedSource): Article {
            val rich = content.ifBlank { description }
            val summary = clean(rich)
            val img = image ?: imgRegex.find(rich)?.groupValues?.get(1)
            return Article(
                title = title,
                link = link,
                summary = summary.take(400),
                imageUrl = normalizeImageUrl(img) ?: youTubeThumbFor(link),
                publishedAt = publishedAt,
                sourceId = source.id,
                sourceName = source.name,
                sourceColor = source.color,
            )
        }
    }

    fun normalizeImageUrl(url: String?): String? {
        if (url.isNullOrBlank()) return url
        val videoId = ytThumbRegex.find(url)?.groupValues?.get(1) ?: return url
        return "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
    }

    fun youTubeThumbFor(link: String): String? =
        ytLinkRegex.find(link)?.groupValues?.get(1)
            ?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }

    fun clean(raw: String): String {
        val withoutTags = tagRegex.replace(raw, " ")
        val decoded = Html.fromHtml(withoutTags, Html.FROM_HTML_MODE_LEGACY).toString()
        return spaceRegex.replace(decoded, " ").trim()
    }

    fun parseDate(raw: String): Long {
        var value = raw.trim().replace(parenRegex, "").trim()
        if (value.endsWith(" UT")) value = value.removeSuffix(" UT") + " GMT"
        for (formatter in dateFormatters) {
            runCatching { return OffsetDateTime.parse(value, formatter).toInstant().toEpochMilli() }
        }
        runCatching {
            return ZonedDateTime.parse(
                value,
                DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH),
            ).toInstant().toEpochMilli()
        }
        runCatching { return Instant.parse(value).toEpochMilli() }
        runCatching {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                .toInstant(ZoneOffset.UTC).toEpochMilli()
        }
        runCatching {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                .toInstant(ZoneOffset.UTC).toEpochMilli()
        }
        return 0L
    }
}
