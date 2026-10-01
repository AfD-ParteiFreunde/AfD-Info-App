package de.afd.parteiapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.regex.Pattern

data class LandtagFetch(
    val name: String,
    val parliament: String,
    val source: String,
    val members: List<LandtagMember>,
)

object LandtagRemote {

    private fun nfc(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFC)

    private fun unescape(s: String): String {
        var out = s
        val m = Pattern.compile("&#x([0-9A-Fa-f]+);").matcher(out)
        val sb = StringBuilder()
        var last = 0
        while (m.find()) {
            sb.append(out, last, m.start())
            sb.append(m.group(1)?.toInt(16)?.toChar() ?: ' ')
            last = m.end()
        }
        sb.append(out, last, out.length)
        out = sb.toString()
        val m2 = Pattern.compile("&#(\\d+);").matcher(out)
        val sb2 = StringBuilder()
        var last2 = 0
        while (m2.find()) {
            sb2.append(out, last2, m2.start())
            sb2.append(m2.group(1)?.toInt()?.toChar() ?: ' ')
            last2 = m2.end()
        }
        sb2.append(out, last2, out.length)
        return sb2.toString()
            .replace("&amp;", "&")
            .replace("&ouml;", "ö").replace("&auml;", "ä").replace("&uuml;", "ü")
            .replace("&Ouml;", "Ö").replace("&Auml;", "Ä").replace("&Uuml;", "Ü")
            .replace("&szlig;", "ß")
    }

    private fun clean(s: String): String =
        unescape(nfc(s)).replace("\u00a0", " ").replace(Regex("\\s+"), " ").trim()

    private val TITLE = Regex("^(Dr|Prof|Med|Vet|Jur|Rer|H\\.c)\\.?\\s*", RegexOption.IGNORE_CASE)

    private fun stripTitles(s: String): String {
        var out = s
        var changed = true
        while (changed) {
            changed = false
            val t = TITLE.find(out)
            if (t != null && t.range.first == 0) {
                out = out.substring(t.range.last + 1).trim()
                changed = true
            }
        }
        return out
    }

    private fun splitFirstLast(full: String): Pair<String, String> {
        val parts = full.split(" ").filter { it.isNotBlank() }
        if (parts.isEmpty()) return "" to ""
        if (parts.size == 1) return "" to parts[0]
        return parts.first() to parts.last()
    }

    private fun make(state: String, fullRaw: String, role: String, photo: String, email: String = "", website: String = ""): LandtagMember {
        val full = clean(fullRaw)
        val plain = stripTitles(full)
        val (first, last) = splitFirstLast(plain)
        return LandtagMember(
            name = full,
            firstName = first,
            lastName = last,
            role = clean(role).let { if (it.equals("Mitglied des Landtags", true) || it.equals("Fraktionsmitglied", true) || it == "&nbsp;") "" else it },
            email = email,
            website = website,
            land = state,
            photo = photo,
        )
    }

    private fun resolve(base: String, url: String): String = when {
        url.startsWith("http") -> url
        url.startsWith("//") -> "https:$url"
        url.startsWith("/") -> base + url
        else -> base + "/" + url
    }

    private fun distinctByName(members: List<LandtagMember>): List<LandtagMember> {
        val seen = HashSet<String>()
        return members.filter { seen.add(normalizeKey(it.name)) }
    }

    private fun normalizeKey(s: String): String {
        var t = clean(s).lowercase()
            .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        return t.replace(Regex("[^a-z ]"), " ").replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Attempt to fetch the live AfD fraction member list for one state.
     * Returns null if the state has no supported live source or parsing yields too few members.
     */
    suspend fun fetch(state: String, parliament: String, urls: List<String>): LandtagFetch? = withContext(Dispatchers.IO) {
        for (url in urls) {
            val html = runCatching { Http.get(url) }.getOrNull() ?: continue
            val members = runCatching { parse(state, url, html) }.getOrNull() ?: continue
            if (members.size >= 2) {
                return@withContext LandtagFetch(state, parliament, url, distinctByName(members))
            }
        }
        null
    }

    private fun parse(state: String, url: String, html: String): List<LandtagMember> = when (state) {
        "Baden-Württemberg" -> parseBw(html)
        "Bayern" -> parseBy(html)
        "Berlin" -> parseBerlin(html)
        "Brandenburg" -> parseBrandenburg(html)
        "Bremen" -> parseBremen(html)
        "Hamburg" -> parseHamburg(html)
        "Hessen" -> parseHessen(html)
        "Mecklenburg-Vorpommern" -> parseMv(html)
        "Niedersachsen" -> parseNi(html)
        "Nordrhein-Westfalen" -> parseNrw(html)
        "Rheinland-Pfalz" -> parseRlp(html)
        "Saarland" -> parseSaarland(html)
        "Sachsen" -> parseSachsen(html)
        "Sachsen-Anhalt" -> parseSachsenAnhalt(html)
        "Thüringen" -> parseThueringen(html)
        else -> emptyList()
    }

    // --- Per-state parsers ---------------------------------------------------

    private fun parseBw(html: String): List<LandtagMember> {
        val out = mutableListOf<LandtagMember>()
        val name = Regex("<h4 class=\"et_pb_module_header\">\\s*([^<]+?)\\s*</h4>")
        val img = Regex("<img[^>]*(?:data-wpfc-original-src|src)=\"(https://afd-fraktion-bw\\.de/wp-content/uploads/[^\"]+)\"")
        val names = name.findAll(html).map { clean(it.groupValues[1]) }.toList()
        val photos = img.findAll(html).map { it.groupValues[1] }.filter { !it.contains("blank") }.distinct().toList()
        names.forEachIndexed { i, n ->
            out += make("Baden-Württemberg", n, "", photos.getOrElse(i) { "" })
        }
        return out
    }

    private fun parseBy(html: String): List<LandtagMember> {
        val out = mutableListOf<LandtagMember>()
        // person card: image with title="Fraktion N" then person-name / person-title
        val card = Regex(
            "<img[^>]*class=\"person-img[^\"]*\"[^>]*src=\"([^\"]+)\"[^>]*>.*?<span class=\"person-name\">([^<]+)</span>\\s*<span class=\"person-title\">([^<]*)</span>",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        card.findAll(html).forEach { m ->
            out += make("Bayern", m.groupValues[2], m.groupValues[3], m.groupValues[1])
        }
        if (out.isEmpty()) {
            val name = Regex("<span class=\"person-name\">([^<]+)</span>")
            val role = Regex("<span class=\"person-title\">([^<]*)</span>")
            val img = Regex("<img[^>]*class=\"person-img[^\"]*\"[^>]*src=\"([^\"]+)\"")
            val names = name.findAll(html).map { it.groupValues[1] }.toList()
            val roles = role.findAll(html).map { it.groupValues[1] }.toList()
            val photos = img.findAll(html).map { it.groupValues[1] }.toList()
            names.forEachIndexed { i, n -> out += make("Bayern", n, roles.getOrElse(i) { "" }, photos.getOrElse(i) { "" }) }
        }
        return out
    }

    private fun parseBerlin(html: String): List<LandtagMember> {
        val out = mutableListOf<LandtagMember>()
        val card = Regex(
            "attachment-large size-large[^>]*src=\"(https://afd-fraktion\\.berlin/wp-content/uploads/[^\"]+)\".*?<h3 class=\"elementor-heading-title elementor-size-default\">\\s*([^<]+?)\\s*</h3>(?:.*?<p class=\"p1\">([^<]*(?:<br\\s*/?>[^<]*)*)</p>)?",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        card.findAll(html).forEach { m ->
            out += make("Berlin", stripTitles(m.groupValues[2]), m.groupValues[3].replace(Regex("<br\\s*/?>"), " "), m.groupValues[1])
        }
        return out
    }

    private fun parseBrandenburg(html: String): List<LandtagMember> {
        val out = mutableListOf<LandtagMember>()
        val card = Regex(
            "<div class=\"profile-box\".*?<img src=\"(/media_fast/[^\"]+)\"[^>]*class=\"foto\".*?<h6>\\s*([^<]+?)\\s*</h6>",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        card.findAll(html).forEach { m ->
            out += make("Brandenburg", m.groupValues[2], "", resolve("https://www.landtag.brandenburg.de", m.groupValues[1]))
        }
        return out
    }

    private fun parseBremen(html: String): List<LandtagMember> {
        // Bürgerschaft AfD member(s); best-effort single member
        val name = Regex("Lichtenfeld", RegexOption.IGNORE_CASE)
        if (!name.containsMatchIn(html)) return emptyList()
        val photo = Regex("(/abgdb/fotos/[^\"']+)").find(html)?.groupValues?.get(1)
        return listOf(make("Bremen", "Sven Lichtenfeld", "", photo?.let { resolve("https://www.bremische-buergerschaft.de", it) } ?: ""))
    }

    private fun parseHamburg(html: String): List<LandtagMember> {
        val out = mutableListOf<LandtagMember>()
        val card = Regex(
            "src=\"(https://www\\.hamburgische-buergerschaft\\.de/resource/image/[^\"]+)\"[^>]*>.*?alt=\"([^\"]+)\"",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        card.findAll(html).forEach { m ->
            val n = clean(m.groupValues[2])
            if (n.length in 4..60 && !n.contains("Logo", true)) out += make("Hamburg", n, "", m.groupValues[1])
        }
        return out
    }

    private fun parseHessen(html: String): List<LandtagMember> {
        val out = mutableListOf<LandtagMember>()
        val card = Regex(
            "img-fluid[^>]*src=\"(/sites/default/files/styles/[^\"]+)\".*?field--name-name field--type-name[^>]*>\\s*([^<]+?)\\s*</div>",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        card.findAll(html).forEach { m ->
            out += make("Hessen", m.groupValues[2], "", resolve("https://hessischer-landtag.de", m.groupValues[1]))
        }
        return out
    }

    private fun parseMv(html: String): List<LandtagMember> {
        val out = mutableListOf<LandtagMember>()
        val col = Regex(
            "src=\"(https://afd-fraktion-mv\\.de/wp-content/uploads/[^\"]+)\".*?<h2 class=\"elementor-heading-title elementor-size-default\">([^<]+)</h2>.*?<h2 class=\"elementor-heading-title elementor-size-default\">([^<]+)</h2>(?:.*?<h5 class=\"elementor-heading-title elementor-size-default\">([^<]+)</h5>)?",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        col.findAll(html).forEach { m ->
            val first = clean(m.groupValues[2]).split(" ").firstOrNull() ?: ""
            val surname = clean(m.groupValues[3]).split(" ").firstOrNull() ?: ""
            val full = if (first.isNotBlank() && surname.isNotBlank()) {
                first.lowercase().replaceFirstChar { it.uppercase() } + " " + surname.lowercase().replaceFirstChar { it.uppercase() }
            } else clean(m.groupValues[2]) + " " + clean(m.groupValues[3])
            out += make("Mecklenburg-Vorpommern", full, m.groupValues[4], m.groupValues[1])
        }
        return out
    }

    private fun parseNi(html: String): List<LandtagMember> {
        val out = mutableListOf<LandtagMember>()
        val img = Regex("<img alt=\"([^\"]+)\" src=\"(/typo3temp/[^\"]+)\"")
        img.findAll(html).forEach { m ->
            val n = clean(m.groupValues[1])
            if (n.length in 4..60 && n.contains(" ")) {
                out += make("Niedersachsen", n, "", resolve("https://www.landtag-niedersachsen.de", m.groupValues[2]))
            }
        }
        return out
    }

    private fun parseNrw(html: String): List<LandtagMember> {
        val out = mutableListOf<LandtagMember>()
        val row = Regex("abgeordnetendetail\\.html\\?k=(\\d+)\"[^>]*>([^<]+)</a>(?:.*?<td>([^<]*(?:Fraktionsvorsitzender|Parl\\.|Stellv\\.|Fraktionsvorstand)[^<]*)</td>)?", setOf(RegexOption.DOT_MATCHES_ALL))
        row.findAll(html).forEach { m ->
            val raw = clean(m.groupValues[2])
            val full = if (raw.contains(",")) {
                val parts = raw.split(",").map { it.trim() }
                val last = stripTitles(parts[0])
                val first = parts.getOrElse(1) { "" }
                "$first $last".trim()
            } else stripTitles(raw)
            val role = m.groupValues[3].substringBefore(";")
            out += make("Nordrhein-Westfalen", full, role, "")
        }
        return out
    }

    private fun parseRlp(html: String): List<LandtagMember> {
        val out = mutableListOf<LandtagMember>()
        val link = Regex("href=\"(https://landtag-rlp\\.de/de/parlament/abgeordnete/abgeordnetensuche/[^\"]+)\"[^>]*>\\s*([^<]+?)\\s*</a>")
        link.findAll(html).forEach { m ->
            val n = clean(m.groupValues[2])
            if (n.length in 4..60 && n.contains(" ") && !n.contains("Abgeordnetensuche", true)) {
                out += make("Rheinland-Pfalz", n, "", "")
            }
        }
        return out
    }

    private fun parseSaarland(html: String): List<LandtagMember> {
        val out = mutableListOf<LandtagMember>()
        val card = Regex("<h4 class=\"ProfileName\">\\s*([^<]+?)\\s*</h4>")
        val img = Regex("<img src=\"(/media/[^\"?]+\\.(?:jpg|jpeg|png))")
        val names = card.findAll(html).map { clean(it.groupValues[1]) }.toList()
        val photos = img.findAll(html).map { it.groupValues[1] }.filter { !it.contains("logo", true) }.toList()
        names.forEachIndexed { i, n -> out += make("Saarland", n, "", photos.getOrElse(i) { "" }.let { if (it.isBlank()) "" else resolve("https://www.landtag-saar.de", it) }) }
        return out
    }

    private fun parseSachsen(html: String): List<LandtagMember> {
        val out = mutableListOf<LandtagMember>()
        val card = Regex(
            "src=\"(https://afd-fraktion-sachsen\\.de/wp-content/uploads/[^\"]+)\".*?<h3>([^<]+)</h3>\\s*<div>(?:<div>)?([^<]*)",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        card.findAll(html).forEach { m ->
            val n = clean(m.groupValues[2])
            if (!n.contains("Grundsatzerklärung", true)) out += make("Sachsen", stripTitles(n), m.groupValues[3].replace("\u00a0", ""), m.groupValues[1])
        }
        return out
    }

    private fun parseSachsenAnhalt(html: String): List<LandtagMember> {
        val out = mutableListOf<LandtagMember>()
        val card = Regex(
            "src=\"(https://afdfraktion-lsa\\.de/wp-content/uploads/[^\"]+)\".*?<h3 class=\"elementor-heading-title elementor-size-default\">\\s*([^<]+?)\\s*</h3>(?:.*?<p>([^<]+)</p>)?",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        card.findAll(html).forEach { m ->
            out += make("Sachsen-Anhalt", stripTitles(m.groupValues[2]), m.groupValues[3], m.groupValues[1])
        }
        return out
    }

    private fun parseThueringen(html: String): List<LandtagMember> {
        val out = mutableListOf<LandtagMember>()
        // scope to AfD accordion panel if present
        val start = html.indexOf("id=\"sect-fraktion-afd\"")
        val scope = if (start >= 0) html.substring(start, minOf(html.length, start + 400_000)) else html
        val img = Regex("<img data-src=\"(/fileadmin/_processed_/[^\"]+)\"[^>]*alt=\"([^\"]+)\"")
        img.findAll(scope).forEach { m ->
            val n = clean(m.groupValues[2])
            if (n.length in 4..60 && n.contains(" ")) {
                out += make("Thüringen", n, "", resolve("https://www.thueringer-landtag.de", m.groupValues[1]))
            }
        }
        return out
    }

    /** Per-state candidate URLs (primary fraction page, then fallbacks). */
    val SOURCES: Map<String, List<String>> = mapOf(
        "Baden-Württemberg" to listOf("https://afd-fraktion-bw.de/abgeordnete/"),
        "Bayern" to listOf("https://www.afd-landtag.bayern/fraktion/"),
        "Berlin" to listOf("https://afd-fraktion.berlin/unsere-abgeordneten/"),
        "Brandenburg" to listOf("https://www.landtag.brandenburg.de/de/abgeordnete_-_fraktionen/fraktionen/afd-fraktion/25206"),
        "Bremen" to listOf("https://www.bremische-buergerschaft.de/abgeordnete/fraktionen"),
        "Hamburg" to listOf("https://www.hamburgische-buergerschaft.de/ueber-uns/abgeordneten-uebersicht"),
        "Hessen" to listOf("https://hessischer-landtag.de/fraktion/afd"),
        "Mecklenburg-Vorpommern" to listOf("https://afd-fraktion-mv.de/"),
        "Niedersachsen" to listOf("https://www.landtag-niedersachsen.de/fraktion-der-afd"),
        "Nordrhein-Westfalen" to listOf("https://www.landtag.nrw.de/home/der-landtag/abgeordnete-und--fraktionen/die-abgeordneten/abgeordnetensuche/suche-nach-fraktionen/fraktionsliste.html?fraktion=AfD"),
        "Rheinland-Pfalz" to listOf("https://landtag-rlp.de/de/parlament/fraktionen/afd-fraktion.htm"),
        "Saarland" to listOf("https://www.landtag-saar.de/abgeordnete-und-fraktionen/fraktionen/afd"),
        "Sachsen" to listOf("https://afd-fraktion-sachsen.de/abgeordnete-seit-2024/"),
        "Sachsen-Anhalt" to listOf("https://afdfraktion-lsa.de/abgeordnete"),
        "Thüringen" to listOf("https://www.thueringer-landtag.de/abgeordnete/abgeordnete-fraktionen-sitzordnung"),
    )
}
