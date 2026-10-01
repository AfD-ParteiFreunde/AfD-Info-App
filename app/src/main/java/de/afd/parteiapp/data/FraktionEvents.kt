package de.afd.parteiapp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File

data class FraktionEvent(
    val title: String,
    val datetime: String,
    val venue: String,
    val url: String,
    val imageUrl: String?,
)

data class EventsResult(
    val events: List<FraktionEvent>,
    val live: Boolean,
    val fetchedAt: Long,
    val error: String? = null,
)

object FraktionEventsRepository {

    private const val LIVE_URL = Links.FRAKTION_EVENTS

    fun loadBest(context: Context): EventsResult {
        val file = cacheFile(context)
        if (!file.exists()) return EventsResult(emptyList(), live = false, fetchedAt = 0L)
        return runCatching {
            val root = JSONObject(file.readText())
            val array = root.optJSONArray("events") ?: JSONArray()
            val events = (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                FraktionEvent(
                    title = obj.optString("title"),
                    datetime = obj.optString("datetime"),
                    venue = obj.optString("venue"),
                    url = obj.optString("url"),
                    imageUrl = obj.optString("imageUrl").takeIf { it.isNotBlank() },
                )
            }
            EventsResult(events, live = false, fetchedAt = root.optLong("fetchedAt"))
        }.getOrDefault(EventsResult(emptyList(), live = false, fetchedAt = 0L))
    }

    suspend fun refresh(context: Context): EventsResult = withContext(Dispatchers.IO) {
        try {
            val html = Http.get(LIVE_URL)
            val events = parse(html)
            if (events.isEmpty()) throw IllegalStateException("No events parsed")
            saveCache(context, events)
            EventsResult(events, live = true, fetchedAt = System.currentTimeMillis())
        } catch (t: Throwable) {
            loadBest(context).copy(error = t.message ?: "error")
        }
    }

    fun parse(html: String): List<FraktionEvent> {
        val doc = Jsoup.parse(html, LIVE_URL)
        return doc.select("article.tribe-events-calendar-list__event").mapNotNull { element ->
            val link = element.selectFirst(".tribe-events-calendar-list__event-title-link")
                ?: return@mapNotNull null
            val url = link.absUrl("href").ifBlank { link.attr("href") }
            val title = link.text().trim()
            if (title.isBlank() || url.isBlank()) return@mapNotNull null
            val datetime = element.selectFirst(".tribe-events-calendar-list__event-datetime")
                ?.text()?.trim().orEmpty()
            val venueTitle = element.selectFirst(".tribe-events-calendar-list__event-venue-title")
                ?.text()?.trim().orEmpty()
            val venueAddress = element.selectFirst(".tribe-events-calendar-list__event-venue-address")
                ?.text()?.trim().orEmpty()
            val venue = listOf(venueTitle, venueAddress)
                .filter { it.isNotBlank() }
                .joinToString(", ")
                .take(180)
            val image = element.selectFirst("img.tribe-events-calendar-list__event-featured-image")
                ?.let { img ->
                    img.attr("data-lazy-src").takeIf { it.startsWith("http") }
                        ?: largestFromSrcset(img.attr("data-lazy-srcset").ifBlank { img.attr("srcset") })
                }
            FraktionEvent(
                title = title,
                datetime = datetime,
                venue = venue,
                url = url,
                imageUrl = image,
            )
        }.take(12)
    }

    private fun largestFromSrcset(srcset: String): String? =
        srcset.split(',')
            .mapNotNull { part ->
                val bits = part.trim().split(Regex("\\s+"))
                val url = bits.getOrNull(0) ?: return@mapNotNull null
                if (!url.startsWith("http")) return@mapNotNull null
                val width = bits.getOrNull(1)?.removeSuffix("w")?.toIntOrNull() ?: 0
                url to width
            }
            .maxByOrNull { it.second }
            ?.first

    private fun cacheFile(context: Context) = File(context.filesDir, "events_live.json")

    private fun saveCache(context: Context, events: List<FraktionEvent>) {
        val array = JSONArray()
        events.forEach { event ->
            array.put(
                JSONObject().apply {
                    put("title", event.title)
                    put("datetime", event.datetime)
                    put("venue", event.venue)
                    put("url", event.url)
                    put("imageUrl", event.imageUrl ?: "")
                }
            )
        }
        val root = JSONObject().apply {
            put("fetchedAt", System.currentTimeMillis())
            put("events", array)
        }
        cacheFile(context).writeText(root.toString())
    }
}
