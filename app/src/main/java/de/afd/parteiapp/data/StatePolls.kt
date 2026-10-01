package de.afd.parteiapp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class StatePoll(
    val land: String,
    val afd: Double,
    val type: String,
    val date: String,
    val next: String,
)

data class StatePollsData(
    val updated: String,
    val source: String,
    val polls: List<StatePoll>,
)

object StatePollsRepository {

    private const val ASSET = "state_polls.json"
    private const val CACHE = "state_polls_live.json"
    private const val API = "https://api.dawum.de/"
    private const val AFD_ID = "7"

    // dawum Parliament "Shortcut" -> our canonical state name
    private val DAWUM_SHORTCUT = mapOf(
        "Baden-Württemberg" to "Baden-Württemberg",
        "Bayern" to "Bayern",
        "Berlin" to "Berlin",
        "Brandenburg" to "Brandenburg",
        "Bremen" to "Bremen",
        "Hamburg" to "Hamburg",
        "Hessen" to "Hessen",
        "Mecklenburg-Vorpommern" to "Mecklenburg-Vorpommern",
        "Niedersachsen" to "Niedersachsen",
        "Nordrhein-Westfalen (NRW)" to "Nordrhein-Westfalen",
        "Rheinland-Pfalz" to "Rheinland-Pfalz",
        "Saarland" to "Saarland",
        "Sachsen" to "Sachsen",
        "Sachsen-Anhalt" to "Sachsen-Anhalt",
        "Schleswig-Holstein" to "Schleswig-Holstein",
        "Thüringen" to "Thüringen",
    )

    fun loadBest(context: Context): StatePollsData {
        val bundled = loadAsset(context)
        val live = readCache(context) ?: return bundled
        return merge(bundled, live)
    }

    fun loadAsset(context: Context): StatePollsData {
        val text = runCatching {
            context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull() ?: return StatePollsData("", "", emptyList())
        return parseJson(text)
    }

    /** Fetches the newest AfD poll per state from api.dawum.de, caches it. */
    suspend fun refresh(context: Context): StatePollsData = withContext(Dispatchers.IO) {
        val bundled = loadAsset(context)
        val scraped = runCatching { fetchLive() }.getOrDefault(emptyList())
        if (scraped.isEmpty()) return@withContext bundled
        val live = StatePollsData(
            updated = today(),
            source = "dawum.de / wahlrecht.de",
            polls = scraped,
        )
        writeCache(context, live)
        merge(bundled, live)
    }

    private suspend fun fetchLive(): List<StatePoll> {
        val root = JSONObject(Http.get(API))
        val parliaments = root.optJSONObject("Parliaments") ?: return emptyList()
        val surveys = root.optJSONObject("Surveys") ?: return emptyList()

        // parliament id -> our canonical name (only the 16 state parliaments)
        val idToName = HashMap<String, String>()
        parliaments.keys().forEach { pid ->
            val shortcut = parliaments.getJSONObject(pid).optString("Shortcut")
            DAWUM_SHORTCUT[shortcut]?.let { idToName[pid] = it }
        }

        // newest survey (with an AfD value) per parliament
        val best = HashMap<String, Pair<String, Double>>()
        surveys.keys().forEach { sid ->
            val s = surveys.getJSONObject(sid)
            val pid = s.optString("Parliament_ID")
            if (pid !in idToName) return@forEach
            val results = s.optJSONObject("Results") ?: return@forEach
            if (!results.has(AFD_ID)) return@forEach
            val afd = results.optDouble(AFD_ID, Double.NaN)
            if (afd.isNaN()) return@forEach
            val date = s.optString("Date")
            val current = best[pid]
            if (current == null || date > current.first) best[pid] = date to afd
        }

        return best.map { (pid, value) ->
            StatePoll(
                land = idToName.getValue(pid),
                afd = value.second,
                type = "poll",
                date = toGermanDate(value.first),
                next = "",
            )
        }
    }

    private fun merge(bundled: StatePollsData, live: StatePollsData): StatePollsData {
        val byLand = live.polls.associateBy { it.land }
        val merged = bundled.polls.map { base ->
            val fresh = byLand[base.land]
            when {
                // official election result always wins
                base.type == "election" -> base
                fresh != null && isNewer(fresh.date, base.date) ->
                    base.copy(afd = fresh.afd, date = fresh.date, type = "poll")
                else -> base
            }
        }
        val known = merged.map { it.land }.toSet()
        val extra = live.polls.filter { it.land !in known && it.afd > 0 }
        return StatePollsData(
            updated = live.updated,
            source = live.source,
            polls = merged + extra,
        )
    }

    private fun isNewer(candidate: String, existing: String): Boolean {
        val a = parseDate(candidate) ?: return false
        val b = parseDate(existing) ?: return true
        return a.isAfter(b)
    }

    private fun parseDate(text: String): java.time.LocalDate? =
        runCatching {
            java.time.LocalDate.parse(
                text,
                java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            )
        }.getOrNull()

    private fun toGermanDate(iso: String): String =
        runCatching {
            java.time.LocalDate.parse(iso).format(
                java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            )
        }.getOrDefault(iso)

    private fun today(): String =
        java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))

    private fun parseJson(text: String): StatePollsData {
        val root = JSONObject(text)
        val array = root.optJSONArray("polls") ?: return StatePollsData("", "", emptyList())
        val polls = buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    StatePoll(
                        land = o.optString("land"),
                        afd = o.optDouble("afd", 0.0),
                        type = o.optString("type", "poll"),
                        date = o.optString("date"),
                        next = o.optString("next"),
                    )
                )
            }
        }
        return StatePollsData(
            updated = root.optString("updated"),
            source = root.optString("source"),
            polls = polls,
        )
    }

    private fun cacheFile(context: Context) = File(context.filesDir, CACHE)

    private fun readCache(context: Context): StatePollsData? {
        val file = cacheFile(context)
        if (!file.exists()) return null
        return runCatching { parseJson(file.readText()) }.getOrNull()
    }

    private fun writeCache(context: Context, data: StatePollsData) {
        runCatching {
            val array = JSONArray()
            data.polls.forEach { p ->
                array.put(
                    JSONObject().apply {
                        put("land", p.land)
                        put("afd", p.afd)
                        put("type", p.type)
                        put("date", p.date)
                        put("next", p.next)
                    }
                )
            }
            val root = JSONObject().apply {
                put("updated", data.updated)
                put("source", data.source)
                put("polls", array)
            }
            cacheFile(context).writeText(root.toString())
        }
    }
}
