package de.afd.parteiapp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File

data class PartyPoll(
    val party: String,
    val percent: Double,
)

data class PollResult(
    val institute: String,
    val published: String,
    val period: String,
    val sample: String,
    val parties: List<PartyPoll>,
    val fetchedAt: Long,
)

object PollsRepository {

    private const val LIVE_URL = "https://www.wahlrecht.de/umfragen/index.htm"

    fun loadBest(context: Context): PollResult? = readCache(context)

    suspend fun refresh(context: Context): PollResult? = withContext(Dispatchers.IO) {
        try {
            val poll = parse(Http.get(LIVE_URL)) ?: throw IllegalStateException("no poll parsed")
            writeCache(context, poll)
            poll.copy(fetchedAt = System.currentTimeMillis())
        } catch (t: Throwable) {
            readCache(context)
        }
    }

    fun parse(html: String): PollResult? {
        val doc = Jsoup.parse(html, LIVE_URL)
        val table = doc.selectFirst("table.wilko") ?: return null
        val rows = table.select("tr")
        val header = rows.firstOrNull()?.select("th, td") ?: return null

        val instituteCols = header.indices.filter { index ->
            val href = header[index].selectFirst("a")?.attr("href").orEmpty()
            href.isNotBlank() && !href.contains("news") && !href.contains("bundestagswahl")
        }
        if (instituteCols.size < 3) return null
        val institutes = instituteCols.map { header[it].text().trim() }

        val publishedRow = rows.getOrNull(1)?.select("th, td")
        val published = instituteCols.map { publishedRow?.getOrNull(it)?.text().orEmpty().trim() }

        val erhebungRow = rows.lastOrNull()?.select("th, td")
        val erhebung = instituteCols.map { erhebungRow?.getOrNull(it)?.text().orEmpty().trim() }

        val partyNames = mutableListOf<String>()
        val partyValues = mutableListOf<DoubleArray>()
        rows.forEach { row ->
            val cells = row.select("th, td")
            val label = cells.firstOrNull()?.text()?.trim().orEmpty()
            if (label.isBlank() || label in setOf("Institut", "Veröffentl.", "Erhebung")) return@forEach
            val values = DoubleArray(instituteCols.size) { index ->
                parsePercent(cells.getOrNull(instituteCols[index])?.text().orEmpty()) ?: Double.NaN
            }
            if (values.all { it.isNaN() }) return@forEach
            partyNames += label
            partyValues += values
        }
        if (partyNames.isEmpty()) return null

        val polls = institutes.indices.map { column ->
            val parties = partyNames.mapIndexedNotNull { rowIndex, name ->
                val value = partyValues[rowIndex][column]
                if (value.isNaN()) null else PartyPoll(name, value)
            }
            PollResult(
                institute = institutes[column],
                published = published.getOrNull(column).orEmpty(),
                period = parsePeriod(erhebung.getOrNull(column).orEmpty()),
                sample = parseSample(erhebung.getOrNull(column).orEmpty()),
                parties = parties,
                fetchedAt = 0L,
            )
        }

        val dated = polls.mapNotNull { poll ->
            runCatching {
                java.time.LocalDate.parse(poll.published, java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            }.getOrNull()?.let { it to poll }
        }
        val pool = if (dated.isEmpty()) {
            polls
        } else {
            val newest = dated.maxOf { it.first }
            dated.filter { it.first >= newest.minusDays(10) }.map { it.second }
        }

        return pool
            .filter { poll -> poll.parties.any { it.party.equals("AfD", true) } }
            .maxByOrNull { poll -> poll.parties.first { it.party.equals("AfD", true) }.percent }
    }

    private fun parsePercent(text: String): Double? {
        val match = Regex("\\d+(?:[.,]\\d+)?").find(text) ?: return null
        return match.value.replace(',', '.').toDoubleOrNull()
    }

    private fun parsePeriod(text: String): String =
        Regex("\\d{1,2}\\.\\d{1,2}\\s*[–-]\\s*\\d{1,2}\\.\\d{1,2}\\.?")
            .find(text)?.value?.replace(" ", "").orEmpty()

    private fun parseSample(text: String): String =
        Regex("\\d{1,3}(?:\\.\\d{3})+|\\b\\d{3,4}\\b").find(text)?.value.orEmpty()

    private fun cacheFile(context: Context) = File(context.filesDir, "polls_live.json")

    private fun readCache(context: Context): PollResult? {
        val file = cacheFile(context)
        if (!file.exists()) return null
        return runCatching {
            val root = JSONObject(file.readText())
            val array = root.optJSONArray("parties") ?: JSONArray()
            val parties = (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                PartyPoll(obj.optString("party"), obj.optDouble("percent"))
            }
            if (parties.isEmpty()) return null
            PollResult(
                institute = root.optString("institute"),
                published = root.optString("published"),
                period = root.optString("period"),
                sample = root.optString("sample"),
                parties = parties,
                fetchedAt = root.optLong("fetchedAt"),
            )
        }.getOrNull()
    }

    private fun writeCache(context: Context, poll: PollResult) {
        val array = JSONArray()
        poll.parties.forEach { party ->
            array.put(
                JSONObject().apply {
                    put("party", party.party)
                    put("percent", party.percent)
                }
            )
        }
        val root = JSONObject().apply {
            put("institute", poll.institute)
            put("published", poll.published)
            put("period", poll.period)
            put("sample", poll.sample)
            put("parties", array)
            put("fetchedAt", System.currentTimeMillis())
        }
        cacheFile(context).writeText(root.toString())
    }
}
