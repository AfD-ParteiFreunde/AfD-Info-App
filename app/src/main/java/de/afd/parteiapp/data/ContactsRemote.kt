package de.afd.parteiapp.data

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

object ContactsRemote {

    private const val ZIP_URL = "https://www.bundestag.de/resource/blob/472878/MdB-Stammdaten.zip"

    private val states = mapOf(
        "BW" to "Baden-Württemberg", "BWG" to "Baden-Württemberg",
        "BY" to "Bayern", "BYB" to "Bayern", "BAY" to "Bayern",
        "BE" to "Berlin", "BER" to "Berlin",
        "BB" to "Brandenburg", "BR" to "Brandenburg", "BRA" to "Brandenburg",
        "HB" to "Bremen", "BRE" to "Bremen",
        "HH" to "Hamburg", "HAM" to "Hamburg",
        "HE" to "Hessen", "HES" to "Hessen",
        "MV" to "Mecklenburg-Vorpommern", "MVP" to "Mecklenburg-Vorpommern",
        "NI" to "Niedersachsen", "NDS" to "Niedersachsen",
        "NW" to "Nordrhein-Westfalen", "NRW" to "Nordrhein-Westfalen",
        "RP" to "Rheinland-Pfalz", "RLP" to "Rheinland-Pfalz", "RPL" to "Rheinland-Pfalz",
        "SL" to "Saarland", "SRL" to "Saarland",
        "SN" to "Sachsen", "SAC" to "Sachsen", "SAX" to "Sachsen",
        "ST" to "Sachsen-Anhalt", "SAN" to "Sachsen-Anhalt",
        "SH" to "Schleswig-Holstein", "SHL" to "Schleswig-Holstein",
        "TH" to "Thüringen", "THU" to "Thüringen", "THUE" to "Thüringen",
    )

    suspend fun fetch(roles: Map<String, String>): List<ContactMember> = withContext(Dispatchers.IO) {
        val bytes = Http.getBytes(ZIP_URL)
        val xml = openXml(bytes) ?: throw IOException("MDB_STAMMDATEN.XML not found in archive")
        val members = xml.use { parse(it, roles) }
        if (members.size < 100) throw IOException("Unexpected member count: ${members.size}")
        members.sortedBy { translit(it.lastName) }
    }

    private fun openXml(zipBytes: ByteArray): InputStream? {
        val zip = ZipInputStream(ByteArrayInputStream(zipBytes))
        var entry = zip.nextEntry
        while (entry != null) {
            if (entry.name.equals("MDB_STAMMDATEN.XML", ignoreCase = true)) return zip
            entry = zip.nextEntry
        }
        return null
    }

    private class NameAcc {
        var nachname = ""
        var vorname = ""
        var praefix = ""
        var adel = ""
        var ortszusatz = ""
        var anredeTitel = ""
        var akadTitel = ""
        var bis = ""
    }

    private class InstAcc {
        var insart = ""
        var inslang = ""
        var bis = ""
        var fkt = ""
    }

    private class WpAcc {
        var wp = ""
        var bis = ""
        var mandatsart = ""
        var wkrNummer = ""
        var wkrName = ""
        var wkrLand = ""
        var liste = ""
        val institutions = mutableListOf<InstAcc>()
    }

    private fun parse(input: InputStream, roles: Map<String, String>): List<ContactMember> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        val members = mutableListOf<ContactMember>()
        val stack = ArrayDeque<String>()
        var names = mutableListOf<NameAcc>()
        var periods = mutableListOf<WpAcc>()
        var currentName: NameAcc? = null
        var currentWp: WpAcc? = null
        var currentInst: InstAcc? = null
        var beruf = ""
        var text = StringBuilder()
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name ?: ""
                    stack.addLast(tag)
                    when (tag) {
                        "MDB" -> {
                            names = mutableListOf()
                            periods = mutableListOf()
                            beruf = ""
                            currentName = null
                            currentWp = null
                            currentInst = null
                        }
                        "NAME" -> {
                            if (stack.elementAtOrNull(stack.size - 2) == "NAMEN") currentName = NameAcc()
                        }
                        "WAHLPERIODE" -> {
                            if (stack.elementAtOrNull(stack.size - 2) == "WAHLPERIODEN") currentWp = WpAcc()
                        }
                        "INSTITUTION" -> currentInst = InstAcc()
                    }
                    text = StringBuilder()
                }

                XmlPullParser.TEXT -> text.append(parser.text)

                XmlPullParser.END_TAG -> {
                    val tag = parser.name ?: ""
                    val value = text.toString().trim()
                    val path = stack.joinToString("/")
                    when (tag) {
                        "NAME" -> {
                            currentName?.let { names += it }
                            currentName = null
                        }
                        "WAHLPERIODE" -> {
                            currentWp?.let { periods += it }
                            currentWp = null
                        }
                        "INSTITUTION" -> {
                            currentInst?.let { currentWp?.institutions?.add(it) }
                            currentInst = null
                        }
                        "MDB" -> parseMember(names, periods, beruf, roles)?.let { members += it }
                        else -> {
                            val inst = currentInst
                            val wp = currentWp
                            val name = currentName
                            when {
                                inst != null && path.endsWith("INSTITUTION/$tag") -> when (tag) {
                                    "INSART_LANG" -> inst.insart = value
                                    "INS_LANG" -> inst.inslang = value
                                    "MDBINS_BIS" -> inst.bis = value
                                    "FKT_LANG" -> inst.fkt = value
                                }
                                wp != null && path.endsWith("WAHLPERIODE/$tag") -> when (tag) {
                                    "WP" -> wp.wp = value
                                    "MDBWP_BIS" -> wp.bis = value
                                    "MANDATSART" -> wp.mandatsart = value
                                    "WKR_NUMMER" -> wp.wkrNummer = value
                                    "WKR_NAME" -> wp.wkrName = value
                                    "WKR_LAND" -> wp.wkrLand = value
                                    "LISTE" -> wp.liste = value
                                }
                                name != null && path.endsWith("NAME/$tag") -> when (tag) {
                                    "NACHNAME" -> name.nachname = value
                                    "VORNAME" -> name.vorname = value
                                    "PRAEFIX" -> name.praefix = value
                                    "ADEL" -> name.adel = value
                                    "ORTSZUSATZ" -> name.ortszusatz = value
                                    "ANREDE_TITEL" -> name.anredeTitel = value
                                    "AKAD_TITEL" -> name.akadTitel = value
                                    "HISTORIE_BIS" -> name.bis = value
                                }
                                path.endsWith("BIOGRAFISCHE_ANGABEN/BERUF") -> beruf = value
                            }
                        }
                    }
                    text = StringBuilder()
                    if (stack.isNotEmpty()) stack.removeLast()
                }
            }
            event = parser.next()
        }
        return members
    }

    private fun parseMember(
        names: List<NameAcc>,
        periods: List<WpAcc>,
        beruf: String,
        roles: Map<String, String>,
    ): ContactMember? {
        val wp = periods
            .filter { it.bis.isEmpty() }
            .maxByOrNull { it.wp.toIntOrNull() ?: 0 }
            ?: return null
        val isAfDFraktion = wp.institutions.any { inst ->
            inst.insart.contains("fraktion", true) &&
                (inst.inslang.contains("AfD", true) || inst.inslang.contains("Alternative für Deutschland", true)) &&
                inst.bis.isEmpty()
        }
        if (!isAfDFraktion) return null
        val name = names.lastOrNull { it.bis.isEmpty() } ?: names.lastOrNull() ?: return null
        if (name.nachname.isBlank()) return null

        val landCode = if (wp.mandatsart.contains("liste", true)) wp.liste else wp.wkrLand
        val land = states[landCode] ?: landCode
        val mandate = when {
            wp.mandatsart.contains("direkt", true) ->
                "Wahlkreis " + listOf(wp.wkrNummer, wp.wkrName).filter { it.isNotBlank() }.joinToString(" ")
            wp.mandatsart.contains("liste", true) ->
                "Landesliste " + (states[wp.liste] ?: wp.liste)
            else -> wp.mandatsart
        }

        val titel = name.akadTitel.ifBlank { name.anredeTitel }
        val display = listOf(titel, name.vorname, name.adel, name.praefix, name.nachname)
            .filter { it.isNotBlank() }
            .joinToString(" ") + if (name.ortszusatz.isNotBlank()) " " + name.ortszusatz else ""
        val fullPlain = listOf(name.vorname, name.adel, name.praefix, name.nachname)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        val fkt = wp.institutions.firstOrNull {
            it.insart.contains("fraktion", true) && it.fkt.isNotBlank()
        }?.fkt.orEmpty()
        val emailLocal = emailPart(name.vorname) + "." + emailPart(listOf(name.praefix, name.nachname).joinToString(""))

        return ContactMember(
            name = display,
            lastName = name.nachname,
            firstName = name.vorname,
            role = fkt.ifBlank { roles[display] ?: roles[fullPlain] ?: "" },
            email = if (emailLocal == ".") "" else "$emailLocal@bundestag.de",
            land = land,
            mandate = mandate,
            beruf = beruf,
            photo = "",
        )
    }

    private fun translit(value: String): String {
        val builder = StringBuilder()
        value.lowercase().forEach { char ->
            when (char) {
                'ä' -> builder.append("ae")
                'ö' -> builder.append("oe")
                'ü' -> builder.append("ue")
                'ß' -> builder.append("ss")
                else -> builder.append(char)
            }
        }
        return java.text.Normalizer.normalize(builder.toString(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
    }

    private fun emailPart(value: String): String = translit(value).replace(Regex("[^a-z]"), "")
}
