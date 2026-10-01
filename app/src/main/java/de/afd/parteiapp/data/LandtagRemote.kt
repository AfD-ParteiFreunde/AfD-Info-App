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
        // Each member card is: portrait <img> (filename contains the person's name)
        // then two H2 headings (first name, last name) and an optional H5 role.
        // Anchoring on the portrait filename keeps brochure/section headings out.
        val col = Regex(
            "src=\"(https://afd-fraktion-mv\\.de/wp-content/uploads/[^\"]+?)-\\d+x\\d+\\.(?:png|jpg|jpeg|webp)\"[^>]*>" +
                ".*?<h2[^>]*elementor-heading-title[^>]*>([^<]+)</h2>\\s*</div>\\s*</div>\\s*<div[^>]*widget-heading" +
                ".*?<h2[^>]*elementor-heading-title[^>]*>([^<]+)</h2>" +
                "(?:.*?<h5[^>]*elementor-heading-title[^>]*>([^<]+)</h5>)?",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        val namePat = Regex("^[A-ZÄÖÜ][A-Za-zÄÖÜäöüß.\\- ]+$")
        val seen = HashSet<String>()
        col.findAll(html).forEach { m ->
            val a = clean(m.groupValues[2])
            val b = clean(m.groupValues[3])
            if (!namePat.matches(a) || !namePat.matches(b)) return@forEach
            if (a.any(Char::isDigit) || b.any(Char::isDigit)) return@forEach
            val imgName = normalizeKey(m.groupValues[1].substringAfterLast("/"))
            if (!imgName.contains(normalizeKey(b)) && !imgName.contains(normalizeKey(a))) return@forEach
            val full = titleCase(a) + " " + titleCase(b)
            if (!seen.add(full)) return@forEach
            out += make("Mecklenburg-Vorpommern", full, clean(m.groupValues[4]), m.groupValues[1])
        }
        return out
    }

    private fun titleCase(s: String): String =
        s.lowercase().split(" ").joinToString(" ") { part ->
            part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
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
        // Match each member link independently (no cross-row spanning), then look
        // for an optional role only within a small bounded window after the link.
        val link = Regex("abgeordnetendetail\\.html\\?k=(\\d+)\"[^>]*title=\"Zur Detailansicht von ([^\"]+)\"")
        val roleRegex = Regex("<td>([^<]*(?:Fraktionsvorsitzender|Parl\\.|Stellv\\.|Fraktionsvorstand)[^<]*)</td>")
        val seen = HashSet<String>()
        link.findAll(html).forEach { m ->
            val raw = clean(m.groupValues[2])
            val full = if (raw.contains(",")) {
                val parts = raw.split(",").map { it.trim() }
                val last = stripTitles(parts[0])
                val first = parts.getOrElse(1) { "" }
                "$first $last".trim()
            } else stripTitles(raw)
            if (full.isBlank() || !seen.add(full)) return@forEach
            val from = m.range.last
            val window = html.substring(from, (from + 600).coerceAtMost(html.length))
            val role = roleRegex.find(window)?.groupValues?.get(1)?.substringBefore(";")?.trim().orEmpty()
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
        // Members are listed as <h3>Name</h3>; some have no portrait, so parse the
        // names directly (not only those preceded by an image) and attach a role
        // when present.
        val nameRe = Regex("<h3>([^<]+)</h3>")
        val seen = HashSet<String>()
        nameRe.findAll(html).forEach { m ->
            val raw = clean(m.groupValues[1])
            if (raw.contains("Grundsatzerklärung", true)) return@forEach
            if (raw.length !in 4..60 || !raw.contains(" ")) return@forEach
            val name = stripTitles(raw)
            if (!seen.add(name)) return@forEach
            // role (optional) within a small window after the name
            val from = m.range.last
            val window = html.substring(from, (from + 200).coerceAtMost(html.length))
            val role = Regex("<div>(?:<div>)?([^<]+)").find(window)
                ?.groupValues?.get(1)?.replace("\u00a0", "")?.trim().orEmpty()
            out += make("Sachsen", name, role, "")
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
        // The page lists ALL parties. Scope strictly to the AfD section:
        // from id="sect-fraktion-afd" up to the next "sect-fraktion-<other>".
        val start = html.indexOf("id=\"sect-fraktion-afd\"")
        if (start < 0) return emptyList()
        val nextSection = Regex("id=\"sect-fraktion-(?!afd)[a-zA-Z0-9-]+\"")
            .find(html, start + 1)
        val end = nextSection?.range?.first ?: html.length
        val scope = html.substring(start, end)
        val img = Regex("<img data-src=\"(/fileadmin/_processed_/[^\"]+)\"[^>]*alt=\"([^\"]+)\"")
        val seen = HashSet<String>()
        img.findAll(scope).forEach { m ->
            val n = clean(m.groupValues[2])
            if (n.length in 4..60 && n.contains(" ") && seen.add(n)) {
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
