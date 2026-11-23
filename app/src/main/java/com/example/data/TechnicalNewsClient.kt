package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.concurrent.TimeUnit

data class TechNewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val category: String, // "Electronics", "Embedded", "Power & Industrial", "Tech"
    val link: String,
    val publishedAt: String
)

/**
 * Fetches live technical news articles via public RSS/Atom feeds for Electronics Mechanic trainees
 * (Electronics, Embedded/Microcontrollers, Power & Industrial Systems, and General Engineering Tech).
 */
class TechnicalNewsClient {

    private data class FeedSpec(
        val url: String,
        val source: String,
        val category: String
    )

    private val feeds = listOf(
        FeedSpec(
            url = "https://hackaday.com/blog/feed/",
            source = "Hackaday",
            category = "Embedded"
        ),
        FeedSpec(
            url = "https://www.electronicsweekly.com/feed/",
            source = "Electronics Weekly",
            category = "Electronics"
        ),
        FeedSpec(
            url = "https://blog.arduino.cc/feed/",
            source = "Arduino Blog",
            category = "Embedded"
        ),
        FeedSpec(
            url = "https://spectrum.ieee.org/feeds/feed.rss",
            source = "IEEE Spectrum",
            category = "Power & Industrial"
        )
    )

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun fetchLiveNews(): ApiResult<List<TechNewsItem>> = withContext(Dispatchers.IO) {
        val collected = mutableListOf<TechNewsItem>()
        var lastError: String? = null

        for (feed in feeds) {
            try {
                val request = Request.Builder()
                    .url(feed.url)
                    .get()
                    .header("User-Agent", "ITI-Ranni-EM-Android/2.2")
                    .header("Accept", "application/rss+xml, application/xml, text/xml")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        if (body.isNotBlank()) {
                            collected.addAll(parseRssOrAtom(body, feed).take(7))
                        }
                    } else {
                        lastError = "HTTP ${response.code}"
                    }
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "Network error"
            }
        }

        if (collected.isEmpty()) {
            ApiResult.Error(
                "Unable to load live technical news right now (${lastError ?: "Check internet connection"}). Tap Refresh to try again."
            )
        } else {
            ApiResult.Success(collected.distinctBy { it.title.lowercase() })
        }
    }

    private fun parseRssOrAtom(xml: String, feed: FeedSpec): List<TechNewsItem> {
        val results = mutableListOf<TechNewsItem>()
        try {
            val factory = XmlPullParserFactory.newInstance().apply {
                isNamespaceAware = false
            }
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var inItem = false
            var currentTag = ""
            var title = ""
            var description = ""
            var link = ""
            var pubDate = ""

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name.orEmpty().lowercase()
                        currentTag = name
                        if (name == "item" || name == "entry") {
                            inItem = true
                            title = ""
                            description = ""
                            link = ""
                            pubDate = ""
                        } else if (inItem && name == "link") {
                            val href = parser.getAttributeValue(null, "href")
                            if (!href.isNullOrBlank()) {
                                link = href.trim()
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        if (inItem) {
                            val text = parser.text.orEmpty()
                            when (currentTag) {
                                "title" -> title += text
                                "description", "summary", "content", "content:encoded" -> {
                                    if (description.length < 360) description += text
                                }
                                "link" -> if (link.isBlank()) link += text
                                "pubdate", "published", "updated", "dc:date" -> {
                                    if (pubDate.isBlank()) pubDate += text
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val name = parser.name.orEmpty().lowercase()
                        if (name == "item" || name == "entry") {
                            val cleanTitle = stripHtml(title).trim()
                            val cleanDesc = stripHtml(description).trim().take(200)
                            val cleanLink = link.trim()
                            if (cleanTitle.isNotBlank() && cleanLink.startsWith("http")) {
                                results.add(
                                    TechNewsItem(
                                        id = "${feed.source}_${cleanTitle.hashCode()}",
                                        title = cleanTitle,
                                        summary = cleanDesc.ifBlank { "Tap to read full article on ${feed.source}." },
                                        source = feed.source,
                                        category = inferCategory(cleanTitle, cleanDesc, feed.category),
                                        link = cleanLink,
                                        publishedAt = cleanDate(pubDate)
                                    )
                                )
                            }
                            inItem = false
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
            // Return whatever parsed cleanly before malformed tag
        }
        return results
    }

    private fun inferCategory(title: String, desc: String, defaultCat: String): String {
        val text = "$title $desc".lowercase()
        return when {
            text.contains("esp32") || text.contains("esp8266") || text.contains("arduino") ||
                text.contains("stm32") || text.contains("microcontroller") || text.contains("fpga") ||
                text.contains("firmware") || text.contains("raspberry pi") || text.contains("embedded") -> "Embedded"
            text.contains("power") || text.contains("solar") || text.contains("inverter") ||
                text.contains("motor") || text.contains("plc") || text.contains("industrial") ||
                text.contains("grid") || text.contains("battery") || text.contains("ev ") -> "Power & Industrial"
            text.contains("pcb") || text.contains("circuit") || text.contains("oscilloscope") ||
                text.contains("transistor") || text.contains("mosfet") || text.contains("sensor") ||
                text.contains("ic ") || text.contains("semiconductor") || text.contains("chip") -> "Electronics"
            else -> defaultCat
        }
    }

    private fun stripHtml(raw: String): String {
        return raw
            .replace(Regex("<[^>]*>"), " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#8217;", "'")
            .replace("&#8211;", "–")
            .replace("&#8220;", "\"")
            .replace("&#8221;", "\"")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
    }

    private fun cleanDate(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return "Live"
        return trimmed.substringBefore(" +").substringBefore(" GMT").take(22)
    }
}
