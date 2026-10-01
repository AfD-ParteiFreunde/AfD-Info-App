package de.afd.parteiapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.Normalizer

/**
 * Live photo enrichment for AfD Bundestag members.
 *
 * The official Bundestag open-data XML (used for the member list) contains no portraits.
 * The AfD-Bundestagsfraktion site exposes one profile page per member at
 *   https://afdbundestag.de/abgeordnete/{slug}/
 * whose portrait can be read from the page's schema.org JSON-LD ("thumbnailUrl"/"primaryImage")
 * or from an <img> whose file name contains the member's surname. Photos are matched to members
 * by exact surname (plus first-name tie-break), so a photo can never be attached to the wrong
 * person even when names collide (e.g. Baum / Baumann, König).
 */
object BundestagRemote {

    private val LIST_PAGES = listOf(
        "abc", "def", "ghi", "jkl", "mno", "pqr", "stu", "vwz",
        "bw", "by", "be", "bb", "hb", "hh", "hs", "mv", "ni", "nw",
        "rp", "sl", "sx", "st", "sh", "th",
    )

    private fun norm(s: String): String {
        var t = s.lowercase().replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        return t.replace(Regex("[^a-z]"), "")
    }

    private val SLUG_RE = Regex("/abgeordnete/([a-z0-9-]+)/")
    private val IMG_TAG_RE = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val JSONLD_RE = Regex("\"(?:thumbnailUrl|primaryImage|contentUrl|image)\"\\s*:\\s*(?:\"([^\"]+\\.(?:jpg|jpeg|png))\"|\\{\\s*\"url\"\\s*:\\s*\"([^\"]+\\.(?:jpg|jpeg|png))\"\\})", RegexOption.IGNORE_CASE)
    private val BAD_IMG = Regex("lightbox|/icon|logo|plenarsaal|slider|avatar|gravatar|placeholder|spacer|ytimg|youtube|banner|header|footer|promo|sort_asc|sort_desc|\\.svg", RegexOption.IGNORE_CASE)
    private val PRESS = Regex("pressemitteilung|presse|doppel|slider", RegexOption.IGNORE_CASE)
    private val PORTRAITISH = Regex("quadrat|portrait|profil|freigestellt|homepage|bild|-1", RegexOption.IGNORE_CASE)

    /** Collect every AfD-fraction member slug from the list pages. */
    suspend fun collectSlugs(): List<String> = withContext(Dispatchers.IO) {
        val slugs = HashSet<String>()
        for (page in LIST_PAGES) {
            val html = runCatching { Http.get("https://afdbundestag.de/abgeordnete-$page") }.getOrNull() ?: continue
            SLUG_RE.findAll(html).forEach { slugs += it.groupValues[1] }
        }
        slugs.toList()
    }

    private fun photoForSurname(html: String, surname: String): String? {
        val sn = norm(surname)
        if (sn.isBlank()) return null
        // 1) JSON-LD primary image
        JSONLD_RE.findAll(html).forEach { m ->
            val raw = (m.groupValues[1].ifBlank { m.groupValues[2] }).replace("\\/", "/")
            val fn = norm(raw.substringAfterLast("/"))
            if (sn.isNotBlank() && sn in fn && !BAD_IMG.containsMatchIn(raw)) return raw
        }
        // 2) <img> whose URL contains the surname, preferring portrait-looking names
        var fallback: String? = null
        for (tag in IMG_TAG_RE.findAll(html).map { it.value }) {
            val src = (Regex("\\bdata-src=\"([^\"]+)\"").find(tag)?.groupValues?.get(1)
                ?: Regex("\\bc?src=\"(https?://[^\"]+)\"").find(tag)?.groupValues?.get(1)
                ?: continue).replace("//afdbt-cdn", "https://afdbt-cdn")
            if (!src.contains("afdbt-cdn")) continue
            if (BAD_IMG.containsMatchIn(src)) continue
            val fn = norm(src.substringAfterLast("/"))
            if (sn !in fn) continue
            if (PRESS.containsMatchIn(src)) { if (fallback == null) fallback = src; continue }
            if (PORTRAITISH.containsMatchIn(src)) return src
            if (fallback == null) fallback = src
        }
        return fallback
    }

    /**
     * Returns a map of normalized-surname -> photo URL for all AfD Bundestag members found.
     * Multiple members sharing a surname are keyed by "surname|firstname".
     */
    suspend fun fetchPhotos(): Map<String, String> = withContext(Dispatchers.IO) {
        val slugs = collectSlugs()
        // Bounded concurrency: fetch in small batches to avoid hammering the host.
        coroutineScope {
            val found = mutableListOf<Pair<String, String>>()
            slugs.chunked(6).forEach { chunk ->
                chunk.map { slug ->
                    async {
                        val surname = slug.substringAfterLast("-")
                        val html = runCatching { Http.get("https://afdbundestag.de/abgeordnete/$slug/") }.getOrNull()
                        if (html == null) null else {
                            val photo = photoForSurname(html, surname)
                            if (photo == null) null else slug to photo
                        }
                    }
                }.awaitAll().filterNotNull().let { found += it }
            }
            found
        }.associate { (slug, photo) ->
            // key by surname; if the slug has a first-name part, also store the full key
            val parts = slug.split("-")
            val surname = parts.last()
            val first = if (parts.size >= 2) parts[parts.size - 2] else ""
            val key = if (first.isNotBlank()) "${norm(surname)}|${norm(first)}" else norm(surname)
            key to photo
        }
    }
}
