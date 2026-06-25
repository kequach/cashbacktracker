package com.cashbacktracker.data.parser

import com.cashbacktracker.data.model.DateRange
import com.cashbacktracker.data.model.ParsedCashbackPromotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Locale

interface PromotionPageFetcher {
    suspend fun fetch(url: String): String
}

class HttpPromotionPageFetcher : PromotionPageFetcher {
    override suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml")
            connection.setRequestProperty("Accept-Language", "de-DE,de;q=0.9,en;q=0.6")
            if (connection.responseCode !in 200..299) {
                throw IOException("Unexpected HTTP response ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader().use { reader ->
                val builder = StringBuilder()
                val buffer = CharArray(BUFFER_SIZE)
                while (builder.length < MAX_CHARS) {
                    val read = reader.read(buffer, 0, minOf(buffer.size, MAX_CHARS - builder.length))
                    if (read == -1) break
                    builder.append(buffer, 0, read)
                }
                builder.toString()
            }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val BUFFER_SIZE = 8_192
        const val MAX_CHARS = 700_000
        const val TIMEOUT_MS = 12_000
        const val USER_AGENT = "Mozilla/5.0 CashbackTracker/0.1"
    }
}

class CashbackPromotionParser(
    private val fetcher: PromotionPageFetcher,
) {
    suspend fun parse(url: String): ParsedCashbackPromotion {
        val html = fetcher.fetch(url)
        return withContext(Dispatchers.Default) {
            parseHtml(html, url)
        }
    }

    fun parseHtml(html: String, baseUrl: String = ""): ParsedCashbackPromotion {
        val document = Jsoup.parse(html, baseUrl)
        val title = listOfNotNull(
            document.selectFirst("h1")?.text(),
            document.selectFirst("meta[property=og:title]")?.attr("content"),
            document.selectFirst("meta[name=title]")?.attr("content"),
            document.title(),
        )
            .mapNotNull { it.cleanTitle().takeIf(String::isNotBlank) }
            .firstOrNull()

        val text = document.body().text()
        val range = GermanDateRangeExtractor.extract(text)
        return ParsedCashbackPromotion(
            productName = title,
            redemptionStart = range?.start,
            redemptionEnd = range?.end,
        )
    }

    fun parseUrlFallback(url: String): ParsedCashbackPromotion =
        ParsedCashbackPromotion(productName = titleFromUrl(url))

    private fun titleFromUrl(url: String): String? {
        val path = runCatching { URL(url).path }.getOrNull().orEmpty()
        val slug = path.trim('/').substringAfterLast('/').takeIf { it.isNotBlank() }
            ?: return null
        return slug
            .replace("-", " ")
            .replace(Regex("""(?i)\bbis\s+zu\s+\d+\s*(?:e|euro)\b.*$"""), " ")
            .replace(Regex("""(?i)\b(gratis|testen|aktion|cashback|geld|zurueck|zurueckerhalten|sichern)\b"""), " ")
            .replace(Regex("""\b\d+\b"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.GERMANY) else char.toString()
                }
            }
            .takeIf { it.isNotBlank() }
    }

    private fun String.cleanTitle(): String = cleanCashbackTitle()
}

private fun String.cleanCashbackTitle(): String =
    replace(Regex("""\s+"""), " ")
        .replace(Regex("""(?i)^\s*ABGELAUFEN\s+"""), "")
        .replace(Regex("""[*_`]+"""), "")
        .replace(Regex("""(?i)\s*\|\s*DealDoktor.*$"""), "")
        .replace(Regex("""(?i)\s+[|-]\s+Cashback.*$"""), "")
        .replace(Regex("""(?i)\s+gratis\s+testen!?\s*$"""), "")
        .replace(Regex("""(?i)\s+100\s*%\s*cashback!?\s*$"""), "")
        .replace(Regex("""(?i)\s+geld\s+zur(?:ue|\u00FC)ck.*$"""), "")
        .trim()

object GermanDateRangeExtractor {
    private val numericRange = Regex(
        pattern = """(?i)\b(\d{1,2})\.(\d{1,2})\.(\d{2,4})?\s*(?:-|\u2013|\u2014|bis|to)\s*(\d{1,2})\.(\d{1,2})\.(\d{2,4})\b""",
    )
    private val untilDate = Regex(
        pattern = """(?i)\b(?:bis|einl(?:\u00F6|oe)seschluss(?:\s+ist)?(?:\s+der)?|teilnahmeschluss)\s*(?:zum\s+|zum\s+einsenden\s+)?(\d{1,2})\.(\d{1,2})\.(\d{2,4})\b""",
    )
    private val fromDate = Regex(
        pattern = """(?i)\b(?:ab|vom|von)\s*(\d{1,2})\.(\d{1,2})\.(\d{2,4})\b""",
    )
    private val numericDate = Regex("""\b(\d{1,2})\.(\d{1,2})\.(\d{2,4})\b""")

    fun extract(text: String): DateRange? {
        likelyPromotionText(text).forEach { candidate ->
            numericRange.find(candidate)?.let { match ->
                val endYear = normalizeYear(match.groupValues[6])
                val startYear = match.groupValues[3]
                    .takeIf { it.isNotBlank() }
                    ?.let(::normalizeYear)
                    ?: endYear
                return DateRange(
                    start = createDate(match.groupValues[1], match.groupValues[2], startYear),
                    end = createDate(match.groupValues[4], match.groupValues[5], endYear),
                )
            }

            val labeledStart = fromDate.find(candidate)?.let(::createDate)
            val labeledEnd = untilDate.find(candidate)?.let(::createDate)
            if (labeledStart != null || labeledEnd != null) {
                return DateRange(start = labeledStart, end = labeledEnd)
            }
        }

        val dates = numericDate.findAll(text)
            .mapNotNull { match ->
                createDate(match.groupValues[1], match.groupValues[2], normalizeYear(match.groupValues[3]))
            }
            .take(2)
            .toList()

        return when (dates.size) {
            0 -> null
            1 -> DateRange(start = null, end = dates.first())
            else -> DateRange(start = dates[0], end = dates[1])
        }
    }

    private fun createDate(match: MatchResult): LocalDate? =
        createDate(match.groupValues[1], match.groupValues[2], normalizeYear(match.groupValues[3]))

    private fun createDate(day: String, month: String, year: Int): LocalDate? =
        runCatching {
            LocalDate.of(year, month.toInt(), day.toInt())
        }.getOrNull()

    private fun normalizeYear(value: String): Int {
        val number = value.toInt()
        return if (number < 100) 2000 + number else number
    }

    private fun likelyPromotionText(text: String): List<String> {
        val lower = text.lowercase(Locale.GERMANY)
        val keywords = listOf(
            "aktionszeitraum",
            "einl\u00F6seschluss",
            "einloeseschluss",
            "teilnahmeschluss",
            "aktion findet",
            "aktion l\u00E4uft",
            "aktion laeuft",
            "gratis testen",
            "cashback",
        )
        val snippets = keywords.mapNotNull { keyword ->
            val index = lower.indexOf(keyword)
            if (index < 0) {
                null
            } else {
                text.substring(
                    startIndex = (index - SNIPPET_RADIUS).coerceAtLeast(0),
                    endIndex = (index + SNIPPET_RADIUS).coerceAtMost(text.length),
                )
            }
        }
        return snippets + text
    }

    private const val SNIPPET_RADIUS = 280
}
