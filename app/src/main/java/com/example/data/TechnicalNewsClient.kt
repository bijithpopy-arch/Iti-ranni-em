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
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchElectronicsNews(): List<TechNewsItem> = withContext(Dispatchers.IO) {
        val liveItems = try {
            val req = Request.Builder()
                .url("https://www.electronicsforu.com/feed")
                .header("User-Agent", "ITIRanniEM/2.6")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) emptyList()
                else parseRss(resp.body?.string().orEmpty())
            }
        } catch (_: Exception) {
            emptyList()
        }
        if (liveItems.isNotEmpty()) liveItems + curatedBilingualBriefs() else curatedBilingualBriefs()
    }

    private fun parseRss(xml: String): List<TechNewsItem> {
        if (xml.isBlank()) return emptyList()
        val items = mutableListOf<TechNewsItem>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var event = parser.eventType
            var insideItem = false
            var title = ""
            var desc = ""
            var link = ""
            var pubDate = ""
            while (event != XmlPullParser.END_DOCUMENT && items.size < 6) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val tag = parser.name.orEmpty().lowercase()
                        if (tag == "item") {
                            insideItem = true
                            title = ""
                            desc = ""
                            link = ""
                            pubDate = ""
                        } else if (insideItem) {
                            when (tag) {
                                "title" -> title = parser.nextText().trim()
                                "description" -> desc = stripHtml(parser.nextText()).take(220)
                                "link" -> link = parser.nextText().trim()
                                "pubdate" -> pubDate = parser.nextText().trim().take(16)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("item", ignoreCase = true) && insideItem) {
                            insideItem = false
                            if (title.isNotBlank()) {
                                val topicMl = autoMalayalamTopicTag(title)
                                items.add(
                                    TechNewsItem(
                                        id = "RSS-${items.size + 1}",
                                        titleEn = title,
                                        titleMl = "$topicMl — $title",
                                        summaryEn = desc.ifBlank { "Latest electronics & embedded industry update." },
                                        summaryMl = "ഇലക്ട്രോണിക്സ് വ്യവസായത്തിലെ പുതിയ വാർത്ത: ${desc.ifBlank { title }}",
                                        category = "Live Feed",
                                        source = "Electronics For You",
                                        publishedDate = pubDate,
                                        url = link
                                    )
                                )
                            }
                        }
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return items
    }

    private fun autoMalayalamTopicTag(title: String): String {
        val lower = title.lowercase()
        return when {
            "semiconductor" in lower || "chip" in lower || "ic" in lower -> "സെമികണ്ടക്ടർ & ചിപ്പ് സാങ്കേതികവിദ്യ"
            "ev" in lower || "battery" in lower || "solar" in lower || "power" in lower -> "പവർ ഇലക്ട്രോണിക്സ് & EV"
            "esp" in lower || "arduino" in lower || "iot" in lower || "sensor" in lower -> "എംബഡഡ് സിസ്റ്റംസ് & IoT"
            "ai" in lower || "robot" in lower || "automation" in lower -> "ഓട്ടോമേഷൻ & റോബോട്ടിക്സ്"
            else -> "ഇലക്ട്രോണിക്സ് ടെക് അപ്ഡേറ്റ്"
        }
    }

    private fun stripHtml(raw: String): String =
        raw.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()

    fun curatedBilingualBriefs(): List<TechNewsItem> = listOf(
        TechNewsItem(
            id = "CUR-1",
            titleEn = "GaN & SiC Power Electronics Transforming Compact SMPS & EV Chargers",
            titleMl = "കോംപാക്റ്റ് SMPS & EV ചാർജറുകളിൽ വിപ്ലവം സൃഷ്ടിക്കുന്ന GaN, SiC പവർ ഇലക്ട്രോണിക്സ്",
            summaryEn = "Gallium Nitride (GaN) and Silicon Carbide MOSFETs run at higher switching frequencies with up to 40% lower heat loss than traditional silicon.",
            summaryMl = "സാധാരണ സിലിക്കൺ ട്രാൻസിസ്റ്ററുകളേക്കാൾ ഉയർന്ന സ്വിച്ചിംഗ് ഫ്രീക്വൻസിയിലും 40% കുറവ് താപനഷ്ടത്തിലും പ്രവർത്തിക്കുന്ന GaN, SiC MOSFET-കൾ ആധുനിക SMPS, EV ചാർജറുകളിൽ വ്യാപകമാകുന്നു.",
            category = "Power Electronics",
            source = "EM Lab Brief",
            publishedDate = "NCVT Trade Focus"
        ),
        TechNewsItem(
            id = "CUR-2",
            titleEn = "ESP32-C6 & Wi-Fi 6 IoT Modules for Smart Workshop Automation",
            titleMl = "സ്മാർട്ട് വർക്ക്ഷോപ്പ് ഓട്ടോമേഷനായി ESP32-C6 & Wi-Fi 6 IoT മൊഡ്യൂളുകൾ",
            summaryEn = "RISC-V based microcontrollers with built-in Wi-Fi 6, Zigbee, and Matter make industrial sensor telemetry faster and ultra-low power.",
            summaryMl = "ബിൽറ്റ്-ഇൻ Wi-Fi 6, Zigbee സപ്പോർട്ടുള്ള RISC-V മൈക്രോകൺട്രോളറുകൾ വ്യവസായ സെൻസറുകളെയും RFID അറ്റൻഡൻസ് സംവിധാനങ്ങളെയും കൂടുതൽ കാര്യക്ഷമമാക്കുന്നു.",
            category = "Embedded & IoT",
            source = "ESP Lab Update",
            publishedDate = "Trade Practical"
        ),
        TechNewsItem(
            id = "CUR-3",
            titleEn = "India Semiconductor Mission Boosts PCB Assembly & SMD Repair Careers",
            titleMl = "ഇന്ത്യ സെമികണ്ടക്ടർ മിഷൻ: PCB അസംബ്ലി & SMD റിപ്പയർ മേഖലയിൽ വൻ തൊഴിലവസരങ്ങൾ",
            summaryEn = "New OSAT chip packaging plants across India increase demand for NCVT Electronics Mechanics skilled in BGA rework and oscilloscope diagnostics.",
            summaryMl = "ഇന്ത്യയിൽ പുതിയ ചിപ്പ് പാക്കേജിംഗ് പ്ലാന്റുകൾ വരുന്നതോടെ SMD സോൾഡറിംഗ്, ഓസിലോസ്കോപ്പ് ടെസ്റ്റിംഗ് പരിചയമുള്ള ഇലക്ട്രോണിക്സ് മെക്കാനിക് ട്രെയിനികൾക്ക് വലിയ സാധ്യതകൾ തുറക്കുന്നു.",
            category = "Semiconductors",
            source = "Skill India / DGT",
            publishedDate = "Career Watch"
        ),
        TechNewsItem(
            id = "CUR-4",
            titleEn = "Solar Hybrid Inverters & LiFePO4 BMS Testing Standards",
            titleMl = "സോളാർ ഹൈബ്രിഡ് ഇൻവെർട്ടറുകളും LiFePO4 ബാറ്ററി BMS ടെസ്റ്റിംഗ് രീതികളും",
            summaryEn = "Learn how active cell balancing ICs protect lithium packs and why thermal cutoff sensors are critical in inverter servicing.",
            summaryMl = "ആക്റ്റീവ് സെൽ ബാലൻസിംഗ് IC-കൾ ലിഥിയം ബാറ്ററികളെ സംരക്ഷിക്കുന്നതെങ്ങനെയെന്നും ഇൻവെർട്ടർ സർവീസിംഗിൽ തെർമൽ സെൻസറുകളുടെ പ്രാധാന്യവും അറിയുക.",
            category = "Renewable & BMS",
            source = "Govt. ITI Ranni",
            publishedDate = "Lab Reference"
        )
    )
}
