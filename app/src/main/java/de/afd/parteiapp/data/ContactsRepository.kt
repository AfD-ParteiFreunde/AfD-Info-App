package de.afd.parteiapp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class FraktionInfo(
    val name: String,
    val address: String,
    val phone: String,
    val email: String,
    val website: String,
)

data class ContactMember(
    val name: String,
    val lastName: String,
    val firstName: String,
    val role: String,
    val email: String,
    val land: String,
    val mandate: String,
    val beruf: String,
    val photo: String,
    val socials: Map<String, String> = emptyMap(),
)

data class Landesverband(
    val name: String,
    val website: String,
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val fraktionName: String = "",
    val fraktionEmail: String = "",
    val fraktionPhone: String = "",
    val fraktionAddress: String = "",
    val fraktionWebsite: String = "",
    val facebook: String = "",
    val instagram: String = "",
    val x: String = "",
    val youtube: String = "",
    val telegram: String = "",
    val tiktok: String = "",
) {
    val socials: List<Pair<String, String>>
        get() = listOf(
            "Facebook" to facebook,
            "Instagram" to instagram,
            "X" to x,
            "YouTube" to youtube,
            "Telegram" to telegram,
            "TikTok" to tiktok,
        ).filter { it.second.isNotBlank() }
}

data class ContactsData(
    val updated: String,
    val source: String,
    val fraktion: FraktionInfo,
    val members: List<ContactMember>,
    val landesverbaende: List<Landesverband>,
)

data class ContactsResult(
    val data: ContactsData,
    val fetchedAt: Long,
    val live: Boolean,
    val error: String? = null,
)

object ContactsRepository {

    private val EMPTY = ContactsData(
        updated = "",
        source = "",
        fraktion = FraktionInfo("", "", "", "", ""),
        members = emptyList(),
        landesverbaende = emptyList(),
    )

    fun load(context: Context): ContactsData = runCatching { parseAsset(context) }
        .getOrDefault(EMPTY)

    private fun parseAsset(context: Context): ContactsData {
        val text = context.assets.open("contacts.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val root = JSONObject(text)

        val fraktionObj = root.optJSONObject("fraktion")
        val fraktion = FraktionInfo(
            name = fraktionObj?.optString("name").orEmpty(),
            address = fraktionObj?.optString("address").orEmpty(),
            phone = fraktionObj?.optString("phone").orEmpty(),
            email = fraktionObj?.optString("email").orEmpty(),
            website = fraktionObj?.optString("website").orEmpty(),
        )

        val landArray = root.optJSONArray("landesverbaende")
        val landesverbaende = if (landArray == null) emptyList() else {
            (0 until landArray.length()).map { index ->
                val obj = landArray.getJSONObject(index)
                Landesverband(
                    name = obj.optString("name"),
                    website = obj.optString("website"),
                    email = obj.optString("email"),
                    phone = obj.optString("phone"),
                    address = obj.optString("address"),
                    fraktionName = obj.optString("fraktionName"),
                    fraktionEmail = obj.optString("fraktionEmail"),
                    fraktionPhone = obj.optString("fraktionPhone"),
                    fraktionAddress = obj.optString("fraktionAddress"),
                    fraktionWebsite = obj.optString("fraktionWebsite"),
                    facebook = obj.optString("facebook"),
                    instagram = obj.optString("instagram"),
                    x = obj.optString("x"),
                    youtube = obj.optString("youtube"),
                    telegram = obj.optString("telegram"),
                    tiktok = obj.optString("tiktok"),
                )
            }
        }

        return ContactsData(
            updated = root.optString("updated"),
            source = root.optString("source"),
            fraktion = fraktion,
            members = parseMembers(root.optJSONArray("members") ?: JSONArray()),
            landesverbaende = landesverbaende,
        )
    }

    fun loadBest(context: Context): Pair<ContactsData, Long> {
        val asset = load(context)
        val photosByName = asset.members
            .filter { it.photo.isNotBlank() }
            .associate { it.name to it.photo }
        val socialsByName = asset.members
            .filter { it.socials.isNotEmpty() }
            .associate { it.name to it.socials }
        val file = cacheFile(context)
        if (!file.exists()) return asset to 0L
        return runCatching {
            val root = JSONObject(file.readText())
            val cached = parseMembers(root.optJSONArray("members") ?: JSONArray())
            if (cached.isEmpty()) return asset to 0L
            // Always backfill blank cached photos and socials from the bundled asset.
            val members = cached.map { member ->
                member.copy(
                    photo = member.photo.takeIf { it.isNotBlank() }
                        ?: photosByName[member.name].orEmpty(),
                    socials = if (member.socials.isNotEmpty()) member.socials
                    else socialsByName[member.name].orEmpty(),
                )
            }
            asset.copy(members = members) to root.optLong("fetchedAt")
        }.getOrDefault(asset to 0L)
    }

    suspend fun refresh(context: Context, force: Boolean): ContactsResult = withContext(Dispatchers.IO) {
        val (current, fetchedAt) = loadBest(context)
        val fresh = fetchedAt > 0L && System.currentTimeMillis() - fetchedAt < 72L * 60L * 60L * 1000L
        // Retry automatically if the cached set is missing photos, even when fresh.
        val missingPhotos = current.members.any { it.photo.isBlank() }
        if (!force && fresh && !missingPhotos) {
            // Cache is current — this is NOT an offline/failed state, so report it
            // as live; the UI then shows "updated <ago>" instead of a wrong "Offline".
            return@withContext ContactsResult(current, fetchedAt, live = true)
        }
        try {
            val seed = load(context).members
            val roles = seed.filter { it.role.isNotBlank() }.associate { it.name to it.role }
            // Photo fallback: live scrape -> fetched value -> bundled -> cached,
            // so a thin or failed photo scrape can never erase existing portraits.
            val seedPhotos = seed.filter { it.photo.isNotBlank() }.associate { it.name to it.photo }
            val cachedPhotos = current.members.filter { it.photo.isNotBlank() }.associate { it.name to it.photo }
            val seedSocials = seed.filter { it.socials.isNotEmpty() }.associate { it.name to it.socials }
            val cachedSocials = current.members.filter { it.socials.isNotEmpty() }.associate { it.name to it.socials }
            val fetched = ContactsRemote.fetch(roles)
            // Prefer official Bundestag portraits (working host); AfD-fraction photos
            // are a secondary/best-effort source.
            val officialPhotos = runCatching { BundestagRemote.fetchBundestagPortraits() }
                .getOrDefault(emptyMap())
            val livePhotos = runCatching { BundestagRemote.fetchPhotos() }.getOrDefault(emptyMap())
            val members = fetched.map { member ->
                val key = BundestagRemote.publicKey(member.firstName, member.lastName)
                val photo = officialPhotos[key]
                    ?: livePhotos[photoKey(member.lastName, member.firstName)]
                    ?: livePhotos[normalizeKey(member.lastName)]
                    ?: member.photo.takeIf { it.isNotBlank() }
                    ?: seedPhotos[member.name]?.takeIf { it.isNotBlank() }
                    ?: cachedPhotos[member.name]?.takeIf { it.isNotBlank() }
                    ?: ""
                val socials = seedSocials[member.name] ?: cachedSocials[member.name] ?: emptyMap()
                member.copy(photo = photo, socials = socials)
            }
            saveCache(context, members)
            ContactsResult(
                data = current.copy(members = members),
                fetchedAt = System.currentTimeMillis(),
                live = true,
            )
        } catch (t: Throwable) {
            // Keep previous data; do NOT advance fetchedAt so the next attempt
            // retries instead of being locked out for 72h.
            android.util.Log.w("ContactsRepo", "refresh failed", t)
            ContactsResult(current, fetchedAt, live = false, error = t.message ?: "error")
        }
    }

    private fun saveCache(context: Context, members: List<ContactMember>) {
        val array = JSONArray()
        members.forEach { member ->
            array.put(
                JSONObject().apply {
                    put("name", member.name)
                    put("firstName", member.firstName)
                    put("lastName", member.lastName)
                    put("role", member.role)
                    put("email", member.email)
                    put("land", member.land)
                    put("mandate", member.mandate)
                    put("beruf", member.beruf)
                    put("photo", member.photo)
                    if (member.socials.isNotEmpty()) {
                        put("socials", JSONObject(member.socials as Map<*, *>))
                    }
                }
            )
        }
        val root = JSONObject().apply {
            put("fetchedAt", System.currentTimeMillis())
            put("members", array)
        }
        cacheFile(context).writeText(root.toString())
    }

    private fun normalizeKey(s: String): String {
        var t = s.lowercase().replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
        t = java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        return t.replace(Regex("[^a-z]"), "")
    }

    private fun photoKey(lastName: String, firstName: String): String =
        normalizeKey(lastName) + "|" + normalizeKey(firstName)

    private fun parseMembers(array: JSONArray): List<ContactMember> =
        (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            ContactMember(
                name = obj.optString("name"),
                lastName = obj.optString("lastName"),
                firstName = obj.optString("firstName"),
                role = obj.optString("role"),
                email = obj.optString("email"),
                land = obj.optString("land"),
                mandate = obj.optString("mandate"),
                beruf = obj.optString("beruf"),
                photo = obj.optString("photo"),
                socials = parseSocials(obj.optJSONObject("socials")),
            )
        }

    private fun parseSocials(obj: JSONObject?): Map<String, String> {
        if (obj == null) return emptyMap()
        return buildMap {
            obj.keys().forEach { key ->
                val value = obj.optString(key)
                if (value.isNotBlank()) put(key, value)
            }
        }
    }

    private fun cacheFile(context: Context) = File(context.filesDir, "contacts_live.json")
}
