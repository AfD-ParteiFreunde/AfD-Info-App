package de.afd.parteiapp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import java.time.LocalDate

data class ElectionDate(
    val id: String,
    val name: String,
    val type: String,
    val region: String,
    val dateIso: String,
    val period: String,
    val confirmed: Boolean,
) {
    val date: LocalDate?
        get() = if (dateIso.isBlank()) null else runCatching { LocalDate.parse(dateIso) }.getOrNull()
}

data class ElectionsResult(
    val elections: List<ElectionDate>,
    val live: Boolean,
    val fetchedAt: Long,
    val error: String? = null,
)

fun List<ElectionDate>.upcoming(today: LocalDate = LocalDate.now()): List<ElectionDate> =
    filter { election ->
        val date = election.date
        if (date != null) {
            !date.isBefore(today)
        } else {
            val periodYear = Regex("(\\d{4})").find(election.period)?.groupValues?.get(1)?.toIntOrNull()
            periodYear == null || periodYear >= today.year
        }
    }.sortedWith(compareBy({ it.date == null }, { it.date }))

object ElectionsRepository {

    private const val LIVE_URL = "https://www.wahlrecht.de/termine.htm"

    private val dateRegex = Regex(
        "(\\d{1,2})\\.\\s*(Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember)\\s+(\\d{4})"
    )
    private val periodRegex = Regex("^(Frühjahr|Sommer|Herbst|Winter)\\b")

    private fun String.isAllDigits() = isNotEmpty() && all { it.isDigit() }

    private val organKeywords = listOf(
        "Bundestag", "Bundespräsident", "Europäisches Parlament", "Landtag", "Bürgerschaft",
        "Abgeordnetenhaus", "Kreistag", "Stadträt", "Gemeinde", "Stadtvertretung",
        "Stadtverordneten", "Bezirks", "Beirat", "Orts", "Samtgemeinde",
        "Regionalversammlung", "Regionalverband", "Volkskammer",
    )
    private val months = listOf(
        "Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember",
    )

    fun loadBundled(context: Context): List<ElectionDate> = runCatching {
        val text = context.assets.open("elections.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val root = JSONObject(text)
        val array = root.optJSONArray("elections") ?: JSONArray()
        (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            ElectionDate(
                id = obj.optString("id"),
                name = obj.optString("name"),
                type = obj.optString("type"),
                region = obj.optString("region"),
                dateIso = obj.optString("date"),
                period = obj.optString("period"),
                confirmed = obj.optBoolean("confirmed", false),
            )
        }
    }.getOrDefault(emptyList())

    fun loadBest(context: Context): ElectionsResult {
        val cached = readCache(context)
        if (cached != null) {
            return ElectionsResult(cached.first, live = false, fetchedAt = cached.second)
        }
        return ElectionsResult(loadBundled(context), live = false, fetchedAt = 0L)
    }

    suspend fun refresh(context: Context): ElectionsResult = withContext(Dispatchers.IO) {
        try {
            val scraped = scrape()
            if (scraped.size < 3) throw IllegalStateException("Too few election dates parsed: ${scraped.size}")
            writeCache(context, scraped)
            ElectionsResult(scraped, live = true, fetchedAt = System.currentTimeMillis())
        } catch (t: Throwable) {
            val cached = readCache(context)
            val elections = cached?.first ?: loadBundled(context)
            ElectionsResult(elections, live = false, fetchedAt = cached?.second ?: 0L, error = t.message ?: "error")
        }
    }

    private suspend fun scrape(): List<ElectionDate> {
        val html = Http.get(LIVE_URL)
        return parse(html)
    }

    fun parse(html: String): List<ElectionDate> {
        val doc = Jsoup.parse(html, LIVE_URL)
        val out = mutableListOf<ElectionDate>()
        var currentYear = ""

        doc.select("table tr").forEach { row ->
            val cells = row.select("td, th").map { cell ->
                cell.text().replace('\u00A0', ' ').replace("\u202F", " ").trim()
            }
            if (cells.size < 4) return@forEach

            val yearCell = cells.firstOrNull { it.matches(Regex("\\d{4}")) }
            val tbodyYear = Regex("termine-(\\d{4})")
                .find(row.parent()?.id().orEmpty())
                ?.groupValues?.get(1)
            if (yearCell != null) currentYear = yearCell
            val year = yearCell ?: tbodyYear ?: currentYear
            if (year.isBlank()) return@forEach
            currentYear = year

            val dateCell = cells.firstOrNull { dateRegex.containsMatchIn(it) }
            val periodCell = cells.firstOrNull { periodRegex.containsMatchIn(it) }
            val organ = cells.firstOrNull { cell ->
                organKeywords.any { cell.contains(it, true) }
            } ?: return@forEach
            val land = cells.firstOrNull { cell ->
                cell != yearCell && cell != dateCell && cell != periodCell && cell != organ &&
                    cell.length in 3..80 &&
                    !cell.matches(Regex("\\d+(\\.\\d+)? Jahre")) &&
                    !cell.isAllDigits() && !cell.equals("Jahr", true)
            }.orEmpty()

            val dateIso: String
            val period: String
            val dateMatch = dateCell?.let { dateRegex.find(it) }
            if (dateMatch != null) {
                val month = months.indexOf(dateMatch.groupValues[2]) + 1
                if (month <= 0) return@forEach
                dateIso = "%04d-%02d-%02d".format(
                    dateMatch.groupValues[3].toInt(),
                    month,
                    dateMatch.groupValues[1].toInt(),
                )
                period = ""
            } else if (periodCell != null) {
                dateIso = ""
                period = if (Regex("\\d{4}").containsMatchIn(periodCell)) periodCell else "$periodCell $year"
            } else {
                return@forEach
            }

            val type = when {
                organ.contains("Bundestag", true) || organ.contains("Bundesversammlung", true) ||
                    organ.contains("Bundespräsident", true) -> "bund"
                organ.contains("Europäisches Parlament", true) || organ.contains("Europawahl", true) -> "europa"
                organ.contains("Landtag", true) || organ.contains("Bürgerschaft", true) ||
                    organ.contains("Abgeordnetenhaus", true) -> "land"
                organ.contains("Kreistag", true) || organ.contains("Stadträt", true) ||
                    organ.contains("Gemeinde", true) || organ.contains("Stadtvertretung", true) ||
                    organ.contains("Stadtverordneten", true) || organ.contains("Bezirks", true) ||
                    organ.contains("Beirat", true) || organ.contains("Orts", true) ||
                    organ.contains("Samtgemeinde", true) || organ.contains("Regionalversammlung", true) ||
                    organ.contains("Regionalverband", true) -> "kommunal"
                else -> "land"
            }

            val name = when {
                organ.contains("Bundespräsident", true) -> "Wahl des Bundespräsidenten ($land)"
                type == "bund" -> "Bundestagswahl"
                type == "europa" -> "Europawahl"
                type == "kommunal" -> "Kommunalwahlen $land"
                organ.contains("Bürgerschaft", true) -> "Bürgerschaftswahl $land"
                organ.contains("Abgeordnetenhaus", true) -> "Abgeordnetenhauswahl $land"
                organ.contains("Landtag", true) -> "Landtagswahl $land"
                else -> "$organ $land".trim()
            }

            out += ElectionDate(
                id = "live-${name.hashCode()}-$dateIso$period",
                name = name,
                type = type,
                region = land,
                dateIso = dateIso,
                period = period,
                confirmed = dateIso.isNotBlank(),
            )
        }

        return out
            .distinctBy { it.name + it.dateIso + it.period }
            .sortedWith(compareBy({ it.date == null }, { it.date }))
    }

    private fun cacheFile(context: Context) = File(context.filesDir, "elections_live.json")

    private fun readCache(context: Context): Pair<List<ElectionDate>, Long>? {
        val file = cacheFile(context)
        if (!file.exists()) return null
        return runCatching {
            val root = JSONObject(file.readText())
            val array = root.optJSONArray("elections") ?: JSONArray()
            val elections = (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                ElectionDate(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    type = obj.optString("type"),
                    region = obj.optString("region"),
                    dateIso = obj.optString("date"),
                    period = obj.optString("period"),
                    confirmed = obj.optBoolean("confirmed", false),
                )
            }
            if (elections.isEmpty()) return null
            elections to root.optLong("fetchedAt")
        }.getOrNull()
    }

    private fun writeCache(context: Context, elections: List<ElectionDate>) {
        val array = JSONArray()
        elections.forEach { election ->
            array.put(
                JSONObject().apply {
                    put("id", election.id)
                    put("name", election.name)
                    put("type", election.type)
                    put("region", election.region)
                    put("date", election.dateIso)
                    put("period", election.period)
                    put("confirmed", election.confirmed)
                }
            )
        }
        val root = JSONObject().apply {
            put("fetchedAt", System.currentTimeMillis())
            put("elections", array)
        }
        cacheFile(context).writeText(root.toString())
    }
}
