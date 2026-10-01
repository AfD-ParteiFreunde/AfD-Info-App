# AfD App (Android, nativ)

Eine native Android-App (Kotlin + Jetpack Compose) für Parteifreunde: Nachrichten-Aggregator, Wahlkampf-Termine & Umfragen, Veranstaltungskarte, Spenden, Bundestags- und Landesverbands-Kontakte sowie das Wahlprogramm mit Volltextsuche.

**Kein Web-Wrapper** – alle Inhalte werden nativ geladen, geparst und gerendert. Artikeltexte werden per Jsoup auf dem Gerät extrahiert, die Veranstaltungskarte nutzt Leaflet in einem eigenen Asset, das Wahlprogramm wird als echtes PDF gerendert und durchsucht.

> Private, inoffizielle App. Kein offizielles Angebot der AfD, ihrer Gliederungen oder Fraktionen. Details siehe Abschnitt *Rechtliches / Hinweise*.

## Screenshots

> Noch keine Screenshots eingecheckt. Aufnahmen unter `docs/screenshots/`
> ablegen (siehe `docs/screenshots/README.md`) und hier eine Tabelle ergänzen.

## Auf einen Blick

- **Nachrichten** – 9 RSS-Quellen (AfD, AfD-Fraktion, AfD TV, NIUS, Junge Freiheit, Apollo News, Tichys Einblick, COMPACT, Deutschlandkurier), Quellen-Filter, nativer Lese-Modus, Teilen.
- **Wahlkampf** – Countdown, Live-Wahltermine (wahlrecht.de), Live-Umfragen (Bundestag) und Landtagsumfragen mit **anklickbarer Deutschlandkarte** (AfD-Werte pro Bundesland).
- **Veranstaltungskarte** – Leaflet-Karte mit Event-Pins, PLZ-Suche, Anbieterwahl (Esri/OSM/Wikimedia/Apple), Zoom-Gesten.
- **Kontakte** – alle AfD-MdB (Live-Stammdaten + offizielle Porträts), Landesverbände mit E-Mail/Telefon/Adresse/Social-Icons inkl. Landtagsfraktionen.
- **Wahlprogramm** – das echte PDF, gerendert auf dem Gerät, mit **Volltextsuche**, Themen-Buttons (Migration, Asyl, Sicherheit, …) die zur Seite springen und die Treffer **gelb markieren**.
- **Mehr** – Sprache (DE/EN), Liquid-Glass-Optik umschaltbar, Quellenverwaltung, Datenschutz.

## Build & Installation

Voraussetzungen: JDK 17+, Android SDK (compileSdk 37), adb.

```bash
git clone <repo-url> && cd AfD-App
./gradlew :app:assembleDebug          # Gradle-Wrapper liegt bei
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Funktionen

- **Startanimation (~3 s)**: Offizielles AfD-Logo (weiße Wortmarke + roter Pfeil, Quelle afd.de; `drawable-nodpi/afd_logo.png`) fällt von oben in die Bildmitte, zoomt kurz auf (1,08×) und rastet ein. Unten am Bildschirmrand: deutsche Flagge (Vektor, `drawable/ic_german_flag.xml`) über dem Slogan „Wir lieben Deutschland." in Barlow Condensed Bold Italic. Glow-Puls, Tippen überspringt. Dauer exakt auf 3,0 s getaktet.
- **Nachrichten**: RSS-Aggregation mit OkHttp + XmlPullParser (RSS 2.0 & Atom), Quellen-Chips mit Original-Logos der Verlage, Pull-to-Refresh, Bild-Thumbnails (Coil), In-Memory/SharedPreferences-Cache, Teilen. Quellen: AfD-Fraktion, AfD, **AfD TV (YouTube-Videos mit Play-Overlay)**, NIUS, Junge Freiheit, Apollo News, Tichys Einblick, COMPACT, Deutschlandkurier.
- **Lese-Modus (nativ)**: Artikel-Text wird auf dem Gerät extrahiert und in Compose gerendert; Original-Link per Button.
- **Wahlkampf (live)**: Countdown zur nächsten Wahl; Wahltermine werden bei jedem Öffnen direkt von wahlrecht.de geladen (Jsoup-Scraping) und lokal gecacht; fällt das aus, greift der letzte Cache, sonst der mitgelieferte Stand (`assets/elections.json`). „voraussichtlich"-Markierung, Refresh-Button, Live-Status. Dazu der live gescrapte Terminkalender der Fraktion (`afdbundestag.de/veranstaltungen/`).
- **Unterstützen**: Tabs für Spenden und Shop. Spenden mit Landesverbands-Bankverbindungen (BW, Bayern, Berlin, NRW), Kopierfunktion, Steuer- und Rechtshinweisen. Shop-Bereich „AfD-Fanshop" (afd-fanshop.de → wir-lieben-deutschland.de) mit Live-Produkthighlights (Scraping, Fallback: mitgelieferter Auszug in `assets/merch.json`).
- **Kontakte (live)**: Alle AfD-MdB. Beim Öffnen aktualisiert die App die Mandatsdaten direkt aus den offiziellen Bundestag-Open-Data-Stammdaten (XML im ZIP, on-device geparst, 72-h-Cache) – inkl. Wahlkreis/Landesliste, Bundesland, Fraktionsvorstand-Rollen und **offiziellen Bundestag-Porträts** (147/150; Rest: Wikimedia/Fallback-Initialen). Suche + Bundesland-Filter, Refresh-Button. Landesverbände-Tab mit **offiziellen Landesverbands-Logos** (16 Landesverbände, Quellen: Wikimedia Commons + Landesverbands-Websites) und den offiziellen Websites aller 16 Landesverbände.
- **Mehr**: Sprache (Deutsch/Englisch, sofort umschaltbar), Quellen an/aus, Links, **Social Media (X, YouTube, Instagram, Facebook)**, Cache leeren, Über/Disclaimer.

## Build & Installation

Voraussetzungen: JDK 17+ (getestet mit Temurin 25 / OpenJDK 26), Android SDK (compileSdk 36, build-tools vorhanden), adb.

```bash
cd AfD-App
./gradlew :app:assembleDebug          # Gradle-Wrapper liegt bei (9.6.1)
./gradlew :app:installDebug           # auf angeschlossenes Gerät (USB-Debugging)
# oder direkt:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Gebaut mit AGP 9.4.1 (Built-in-Kotlin, Kotlin 2.2.10), compileSdk 37.2, Compose BOM 2026.09.00. Build und Lauf auf Android 15 verifiziert (RSS-Inhalte, Bilder, Kontaktdatenbank, Sprachumschaltung).

## Projektstruktur

```
app/src/main/
  assets/news_feeds.json      # RSS-Quellen (Name, URL, Farbe, Standard an/aus)
  assets/contacts.json        # MdB-Datenbank (generiert aus Bundestag Open Data)
  assets/donations.json       # Bankverbindungen
  java/de/afd/parteiapp/
    data/    (RSS-Parser, Repositories, Extraktor, Prefs)
    ui/      (Splash, News, Reader, Kontakte, Spenden, Termine, Mehr, Theme)
  res/values/strings.xml      # Deutsch (Standard)
  res/values-en/strings.xml   # Englisch
```

## Kontaktdaten aktualisieren

Quelle: [Bundestag Open Data](https://www.bundestag.de/services/opendata) → „MdB-Stammdaten“ (XML). Der Generator liegt im Temp-Verzeichnis der Session; das Muster ist:

1. ZIP von bundestag.de laden, `MDB_STAMMDATEN.XML` entpacken.
2. Skript `make_contacts.py` anpassen/ausführen → `app/src/main/assets/contacts.json`.

E-Mail-Adressen werden aus dem Bundestag-Schema (vorname.nachname@bundestag.de) abgeleitet und sind unbestätigt – die App weist darauf hin.

## Nachrichtenquellen prüfen

Stand 26.09.2026 lieferten NIUS, Junge Freiheit, Apollo News, Tichys Einblick, COMPACT, Deutschlandkurier, die AfD und die AfD-Fraktion valides RSS. Hinweis: Der YouTube-Feed von AfD TV (`youtube.com/feeds/videos.xml`) antwortet nach vielen Abrufen zeitweise mit 404/500 (YouTube-Drosselung für anonyme Feed-Aufrufe) und erholt sich wieder. Die App verliert dabei keine bereits geladenen Videos: Artikel fehlgeschlagener Quellen bleiben im Cache erhalten und werden beim nächsten erfolgreichen Abruf aktualisiert.

## Schriften (Fonts)

Die App verwendet die AfD-CI-Schriften (Barlow + Barlow Condensed, wie auf afd.de) für die gesamte Oberfläche. Im Nachrichtenbereich trägt jede Quelle ihre eigene Typografie (Titel-/Textschrift des jeweiligen Mediums), damit sich die Feeds optisch unterscheiden.

| Quelle | Titel | Text | Herkunft |
|---|---|---|---|
| App/UI + AfD-Feed | Barlow Condensed | Barlow | afd.de (Avada-CI) |
| AfD-Fraktion | Barlow Condensed | Open Sans | afdbundestag.de |
| NIUS | Inter | Libre Franklin | Ersatz für Acumin Variable Concept / Trade Gothic Next (Adobe Typekit, kommerziell) |
| Junge Freiheit | EB Garamond | Open Sans | jungefreiheit.de |
| Apollo News | Anton | Mulish | apollo-news.net |
| Tichys Einblick | Gelasio | Gelasio | Ersatz für Georgia (Systemschrift, metrik-kompatibler Klon) |
| COMPACT | Montserrat | Inter | Ersatz für RBNo2.1 / Univers (kommerziell) |
| Deutschlandkurier | Source Serif 4 | Lato | deutschlandkurier.de |

37 statische TTF-Dateien liegen in `app/src/main/res/font/` (mit fontTools aus den Google-Fonts-Variablen instanziert, Gewichte 300–700). Alle gebündelten Schriften stehen unter OFL/Apache-Lizenz (Google Fonts) und sind damit frei verteilbar. Kommerzielle Originale (Adobe/Univers/RBNo2.1) sind bewusst durch freie, optisch nächstliegende Schnitte ersetzt; wer Lizenzen besitzt, kann die TTFs in `res/font/` einfach austauschen (Mapping in `ui/theme/Fonts.kt`).

## Live-Daten & Fallbacks

Die App zeigt keine rein statischen Daten: Nachrichten, Wahltermine, MdB-Kontakte und Shop-Highlights werden zur Laufzeit von den Quellen geholt und lokal gecacht. Jede Quelle hat eine Fallback-Kette (Live → Cache → mitgelieferter Stand), inkl. Statusanzeige und Stale-Hinweis bei fehlgeschlagenem Update.

| Bereich | Live-Quelle | Cache | Mitgelieferter Stand |
|---|---|---|---|
| Nachrichten | 8 RSS-Feeds (OkHttp + XmlPullParser) | SharedPreferences | – |
| Wahltermine | wahlrecht.de/termine.htm (Jsoup) | `files/elections_live.json` | `assets/elections.json` |
| Termine | afdbundestag.de/veranstaltungen/ (Jsoup) | `files/events_live.json` | – (Linkkarten als Fallback) |
| Kontakte | Bundestag Open Data MdB-Stammdaten (ZIP→XML, on-device geparst) | `files/contacts_live.json` (72 h) | `assets/contacts.json` |
| Shop | wir-lieben-deutschland.de (Jsoup) | – | `assets/merch.json` |

Kontakte ohne Gewähr: E-Mail-Adressen folgen dem Bundestag-Schema (vorname.nachname@bundestag.de) und sind unbestätigt – die App weist darauf hin.

## Shop

Die App verlinkt auf **www.wir-lieben-deutschland.de** (AfD-Fanshop, Alias `afd-fanshop.de`; Magento). Die Domain ist durch afd.de selbst bestätigt (`afd.de/werbemittel-kaufen/` verlinkt dorthin). Der Shop blockt Kommandozeilen-Clients per Cloudflare (Browser funktionieren), daher werden Produkt-Highlights best-effort live gelesen und sonst als mitgelieferter Auszug mit Stand-Datum gezeigt.

## Link-Check (vom Gerät aus verifiziert, 26.09.2026)

| App-Link | Ziel | Status |
|---|---|---|
| Nachrichten | 8 RSS-Feeds (AfD, AfD-Fraktion, NIUS, Junge Freiheit, Apollo, Tichy, COMPACT, Deutschlandkurier) | 200 – AfD-Feed jetzt standardmäßig aktiv |
| Wahltermine | wahlrecht.de/termine.htm | 200, Live-Scrape (33 Einträge) |
| Termine | afdbundestag.de/veranstaltungen/ | 200, Live-Scrape (10 Veranstaltungen mit Ort/Bild) |
| Kontakte | bundestag.de/abgeordnete + Open-Data-ZIP | 200 |
| Mitglied werden | afd.de/mitglied-werden/ | 200 (frühere 404-Route `/mitmachen/` korrigiert) |
| Spenden | spenden.afd.de | 200 (frühere Redirect-Route `/spenden/` korrigiert) |
| Programm | afd.de/grundsatzprogramm/ | 200 |
| Landesverbände | afd.de/partei/landesverbaende/ | 200 |
| Fraktion | afdbundestag.de | 200 |
| Shop | wir-lieben-deutschland.de | Browser ok; CLI blockt Cloudflare; von afd.de verlinkt |
| Landesverbands-Sites + Spenden-Deeplinks | 9 Websites, 4 `/spenden`-Seiten | 200 (afd-hamburg.de: Cloudflare-Challenge für CLI, im Browser ok) |

## Lizenz

Der Quellcode steht unter der **MIT-Lizenz** (siehe [`LICENSE`](LICENSE)) – frei nutzbar, veränderbar und weiterverbreitbar. Die MIT-Lizenz gilt **nur für den Quellcode**; gebündelte Assets (Partei-Logos, Fonts, PDF, Live-Inhalte) verbleiben bei den jeweiligen Rechteinhabern, siehe Hinweis am Ende der `LICENSE`.

## Rechtliches / Hinweise

- Private, inoffizielle App; kein Angebot der AfD, ihrer Gliederungen oder Fraktionen. Impressum/Schriftzug/CI gehören den jeweiligen Rechteinhabern; offizielle Logo-Assets ggf. durch die Parteivorlage ersetzen.
- RSS-Inhalte unterliegen dem Urheberrecht der jeweiligen Verlage; der Lese-Modus zeigt auf dem Gerät extrahierte Vorschau-Texte.
- Kontaktdaten: öffentlich zugängliche Mandatsdaten; bei Veröffentlichung/Einbindung außerhalb des privaten Gebrauchs DSGVO beachten.
- Bankverbindungen ohne Gewähr – vor Überweisung auf der Landesverbands-Website prüfen.
- `afd.de`-Pfade (`/spenden/`, `/mitmachen/`, `/veranstaltungen/`, `/grundsatzprogramm/`, `/landesverbaende/`) waren aus der Build-Umgebung nicht prüfbar; bei Bedarf in `EventsScreen.kt`, `MoreScreen.kt`, `DonateScreen.kt` korrigieren.
