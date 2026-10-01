package de.afd.parteiapp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class LandtagMember(
    val name: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val email: String,
    val website: String,
    val land: String,
    val photo: String,
)

data class LandtagState(
    val name: String,
    val parliament: String,
    val source: String,
    val members: List<LandtagMember>,
)

data class LandtagData(
    val updated: String,
    val source: String,
    val states: List<LandtagState>,
)

data class LandtagResult(
    val data: LandtagData,
    val fetchedAt: Long,
    val live: Boolean,
    val error: String? = null,
)

object LandtagRepository {

    private const val CACHE_DAYS = 7L

    fun load(context: Context): LandtagData =
        runCatching { parseAsset(assetText(context)) }
            .getOrDefault(LandtagData("", "", emptyList()))

    private fun assetText(context: Context): String =
        context.assets.open("landtag_members.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

    private fun parseAsset(text: String): LandtagData {
        val root = JSONObject(text)
        return LandtagData(
            updated = root.optString("updated"),
            source = root.optString("source"),
            states = parseStates(root.optJSONArray("states")),
        )
    }

    private fun parseStates(array: JSONArray?): List<LandtagState> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            LandtagState(
                name = obj.optString("name"),
                parliament = obj.optString("parliament"),
                source = obj.optString("source"),
                members = parseMembers(obj.optJSONArray("members")),
            )
        }
    }

    private fun parseMembers(array: JSONArray?): List<LandtagMember> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { m ->
            val mo = array.getJSONObject(m)
            LandtagMember(
                name = mo.optString("name"),
                firstName = mo.optString("firstName"),
                lastName = mo.optString("lastName"),
                role = mo.optString("role"),
                email = mo.optString("email"),
                website = mo.optString("website"),
                land = mo.optString("land"),
                photo = mo.optString("photo"),
            )
        }
    }

    private fun cacheFile(context: Context) = File(context.filesDir, "landtag_live.json")

    /** Bundled seed merged with any previously cached live data. */
    fun loadBest(context: Context): Pair<LandtagData, Long> {
        val asset = load(context)
        val file = cacheFile(context)
        if (!file.exists()) return asset to 0L
        return runCatching {
            val root = JSONObject(file.readText())
            val at = root.optLong("fetchedAt")
            val cachedStates = parseStates(root.optJSONArray("states")).associateBy { it.name }
            val merged = asset.states.map { st -> cachedStates[st.name] ?: st }
            asset.copy(states = merged) to at
        }.getOrDefault(asset to 0L)
    }

    suspend fun refresh(context: Context, force: Boolean): LandtagResult = withContext(Dispatchers.IO) {
        val (current, fetchedAt) = loadBest(context)
        val fresh = fetchedAt > 0L && System.currentTimeMillis() - fetchedAt < CACHE_DAYS * 24L * 60L * 60L * 1000L
        if (!force && fresh) {
            return@withContext LandtagResult(current, fetchedAt, live = false)
        }
        val asset = load(context)
        val assetByName = asset.states.associateBy { it.name }
        val updatedStates = mutableListOf<LandtagState>()
        var anyLive = false
        var lastError: String? = null
        for (st in asset.states) {
            val urls = LandtagRemote.SOURCES[st.name]
            val fetch = if (urls == null) null else runCatching {
                LandtagRemote.fetch(st.name, st.parliament, urls)
            }.getOrNull()
            if (fetch != null) {
                // preserve curated emails/websites from the seed by name
                val seedByName = assetByName[st.name]?.members?.associateBy { normalize(it.name) } ?: emptyMap()
                val merged = fetch.members.map { m ->
                    val seed = seedByName[normalize(m.name)]
                    if (seed != null) m.copy(
                        email = m.email.ifBlank { seed.email },
                        website = m.website.ifBlank { seed.website },
                        photo = m.photo.ifBlank { seed.photo },
                    ) else m
                }
                updatedStates += LandtagState(st.name, st.parliament, fetch.source, merged)
                anyLive = true
            } else {
                updatedStates += st
                lastError = lastError ?: "refresh failed"
            }
        }
        if (anyLive) {
            saveCache(context, updatedStates)
            val result = current.copy(states = updatedStates)
            return@withContext LandtagResult(result, System.currentTimeMillis(), live = true, error = lastError)
        }
        LandtagResult(current, fetchedAt, live = false, error = lastError ?: "error")
    }

    private fun normalize(s: String): String {
        var t = s.lowercase().replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
        t = java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        return t.replace(Regex("[^a-z ]"), " ").replace(Regex("\\s+"), " ").trim()
    }

    private fun saveCache(context: Context, states: List<LandtagState>) = runCatching {
        val statesArray = JSONArray()
        states.forEach { st ->
            val members = JSONArray()
            st.members.forEach { m ->
                members.put(
                    JSONObject().apply {
                        put("name", m.name)
                        put("firstName", m.firstName)
                        put("lastName", m.lastName)
                        put("role", m.role)
                        put("email", m.email)
                        put("website", m.website)
                        put("land", m.land)
                        put("photo", m.photo)
                    }
                )
            }
            statesArray.put(
                JSONObject().apply {
                    put("name", st.name)
                    put("parliament", st.parliament)
                    put("source", st.source)
                    put("members", members)
                }
            )
        }
        val root = JSONObject().apply {
            put("fetchedAt", System.currentTimeMillis())
            put("states", statesArray)
        }
        cacheFile(context).writeText(root.toString())
    }
}
