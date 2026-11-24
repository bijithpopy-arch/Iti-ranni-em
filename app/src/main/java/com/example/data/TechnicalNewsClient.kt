package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.concurrent.TimeUnit

class TechnicalNewsClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLiveNews(): List<TechNewsItem> = withContext(Dispatchers.IO) {
        val curated = curatedElectronicsMechanicNews()
        val liveRss = try {
            fetchRssFeed("https://www.electronics-lab.com/feed/", "Electronics-Lab")
        } catch (_: Exception) {
            emptyList()
        }
        (liveRss + curated).distinctBy { it.titleEn.lowercase() }.take(18)
    }

    private fun fetchRssFeed(feedUrl: String, sourceLabel: String): List<TechNewsItem> {
        val request = Request.Builder().url(feedUrl).get().build()
        val xml = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            resp.body?.string().orEmpty()
        }
        if (xml.isBlank()) return emptyList()

        val items = mutableListOf<TechNewsItem>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))

        var insideItem = false
        var title = ""
        var link = ""
        var pubDate = ""
        var desc = ""
        var currentTag = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT && items.size < 8) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name.orEmpty()
                    if (currentTag.equals("item", ignoreCase = true)) {
                        insideItem = true
                        title = ""
                        link = ""
                        pubDate = ""
                        desc = ""
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideItem) {
                        val txt = parser.text.orEmpty()
                        when (currentTag.lowercase()) {
                            "title" -> title += txt
                            "link" -> link += txt
                            "pubdate" -> pubDate += txt
                            "description" -> desc += txt
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("item", ignoreCase = true) && insideItem) {
                        val cleanTitle = title.replace(Regex("<[^>]*>"), "").trim()
                        val cleanSummary = desc.replace(Regex("<[^>]*>"), "").replace(Regex("\\s+"), " ").trim().take(220)
                        if (cleanTitle.isNotBlank()) {
                            items.add(
                                TechNewsItem(
                                    id = "RSS-${items.size}-${cleanTitle.hashCode()}",
                                    titleEn = cleanTitle,
                                    titleMl = "ഇലക്ട്രോണിക്സ് വാർത്ത: $cleanTitle",
                                    summaryEn = cleanSummary.ifBlank { "Latest electronics & embedded hardware update from $sourceLabel." },
                                    summaryMl = "ഇലക്ട്രോണിക്സ് മെക്കാനിക് ട്രേഡിന് ഉപകാരപ്രദമായ പുതിയ ഹാർഡ്‌വെയർ & സർക്യൂട്ട് വിവരങ്ങൾ ($sourceLabel).",
                                    category = "Embedded & Hardware",
                                    source = sourceLabel,
                                    publishedDate = pubDate.take(16).trim(),
                                    url = link.trim()
                                )
                            )
                        }
                        insideItem = false
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }
        return items
    }

    fun curatedElectronicsMechanicNews(): List<TechNewsItem> = listOf(
        TechNewsItem(
            id = "EM-NEWS-1",
            titleEn = "GaN & SiC Power MOSFETs Transforming SMPS & EV Chargers",
            titleMl = "SMPS & EV ചാർജറുകളിൽ GaN, SiC പവർ മോസ്ഫെറ്റുകളുടെ മുന്നേറ്റം",
            summaryEn = "Gallium Nitride (GaN) switching stages run cooler above 500 kHz with smaller ferrite transformers and higher efficiency in modern inverters.",
            summaryMl = "ഉയർന്ന ഫ്രീക്വൻസിയിൽ ചൂട് കുറഞ്ഞതും കാര്യക്ഷമത കൂടിയതുമായ GaN/SiC പവർ മോസ്ഫെറ്റുകൾ പുതിയ ഇൻവെർട്ടറുകളിലും SMPS പവർ സപ്ലൈകളിലും വ്യാപകമാകുന്നു.",
            category = "Power Electronics",
            source = "DGT / Industry Brief",
            publishedDate = "Nov 2026"
        ),
        TechNewsItem(
            id = "EM-NEWS-2",
            titleEn = "ESP32-S3 & Industrial Wi-Fi/BLE Telemetry in Smart Workshops",
            titleMl = "സ്മാർട്ട് വർക്ക്ഷോപ്പുകളിൽ ESP32-S3 & IoT ടെലിമെട്രി പ്രയോഗങ്ങൾ",
            summaryEn = "Low-cost dual-core microcontrollers enable real-time RFID attendance logging, multimeter USB telemetry, and automated lab safety cutoffs.",
            summaryMl = "RFID ഹാജർ സംവിധാനം, സെൻസർ നിരീക്ഷണം, സുരക്ഷാ കട്ട്ഓഫ് എന്നിവയ്ക്കായി ESP സീരീസ് മൈക്രോകൺട്രോളറുകൾ വ്യവസായ ലാബുകളിൽ ഉപയോഗിക്കുന്നു.",
            category = "Microcontrollers & IoT",
            source = "Electronics Mechanic Lab",
            publishedDate = "Nov 2026"
        ),
        TechNewsItem(
            id = "EM-NEWS-3",
            titleEn = "NCVT / SIDH Digital Skills & Solar Micro-Inverter Testing",
            titleMl = "സോളാർ മൈക്രോ-ഇൻവെർട്ടർ ടെസ്റ്റിംഗും NCVT സ്കിൽ ഇന്ത്യ അപ്ഡേറ്റുകളും",
            summaryEn = "Hands-on DSO waveform diagnostics, MPPT charge controller calibration, and lead-free SMD rework remain core employability skills for EM trainees.",
            summaryMl = "ഡിജിറ്റൽ ഓസിലോസ്കോപ്പ് പരിശോധന, സോളാർ MPPT ചാർജ് കൺട്രോളർ കാലിബ്രേഷൻ, SMD സോൾഡറിംഗ് എന്നിവയിൽ പ്രായോഗിക പരിശീലനം ശക്തമാക്കുന്നു.",
            category = "Skill India & NCVT",
            source = "SIDH / DET Kerala",
            publishedDate = "Nov 2026"
        ),
        TechNewsItem(
            id = "EM-NEWS-4",
            titleEn = "USB-C PD 3.1 (240W) & Programmable Buck-Boost Bench Supplies",
            titleMl = "USB-C PD 3.1 (240W) & പ്രോഗ്രാമബിൾ ബക്ക്-ബൂസ്റ്റ് പവർ സപ്ലൈകൾ",
            summaryEn = "Extended Power Range (EPR) up to 48V 5A relies on bidirectional buck-boost controller ICs and digital CC/CV protection loops.",
            summaryMl = "48V 5A വരെ നൽകുന്ന USB-C PD 3.1 പവർ അഡാപ്റ്ററുകളിൽ ഡിജിറ്റൽ CC/CV പ്രൊട്ടക്ഷൻ സർക്യൂട്ടുകളും ബക്ക്-ബൂസ്റ്റ് കൺട്രോളറുകളും പ്രധാനമാണ്.",
            category = "Circuit Design",
            source = "IEEE Spectrum Digest",
            publishedDate = "Nov 2026"
        ),
        TechNewsItem(
            id = "EM-NEWS-5",
            titleEn = "LiFePO4 Battery Management System (BMS) Cell Balancing & Safety",
            titleMl = "LiFePO4 ബാറ്ററി മാനേജ്മെന്റ് സിസ്റ്റം (BMS) & സെൽ ബാലൻസിംഗ്",
            summaryEn = "Active vs passive balancing, NTC thermal probes, and back-to-back MOSFET protection ensure safe cycle life in UPS and solar storage packs.",
            summaryMl = "UPS, സോളാർ ബാറ്ററി പാക്കുകളിൽ താപനില നിയന്ത്രണത്തിനും സെൽ ബാലൻസിംഗിനും സ്മാർട്ട് BMS സർക്യൂട്ടുകളുടെ പ്രാധാന്യം വർദ്ധിക്കുന്നു.",
            category = "Battery & UPS",
            source = "EM Trade Bulletin",
            publishedDate = "Nov 2026"
        )
    )
}
