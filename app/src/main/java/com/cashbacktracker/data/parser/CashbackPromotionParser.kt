package com.cashbacktracker.data.parser

import com.cashbacktracker.data.model.DateRange
import com.cashbacktracker.data.model.ParsedCashbackPromotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
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
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
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
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125 Mobile Safari/537.36 CashbackTracker/0.1"
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

        document.select("s, strike, del").remove()
        val text = buildString {
            append(document.body().text())
            appendLine()
            document.select("meta[content]").forEach { meta ->
                append(meta.attr("content"))
                appendLine()
            }
            document.select("img[alt], [aria-label], [title]").forEach { element ->
                append(element.attr("alt"))
                appendLine()
                append(element.attr("aria-label"))
                appendLine()
                append(element.attr("title"))
                appendLine()
            }
            document.select("script").forEach { script ->
                append(script.data())
                appendLine()
            }
        }
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
        val parsedUrl = runCatching { URL(url) }.getOrNull() ?: return null
        val hostTitle = hostTitle(parsedUrl.host)
        val pathTitle = parsedUrl.path
            .trim('/')
            .split("/")
            .asReversed()
            .mapNotNull(::slugTitle)
            .firstOrNull()

        return when {
            hostTitle != null && pathTitle != null && shouldCombineHostAndPath(parsedUrl.host, hostTitle, pathTitle) ->
                "$hostTitle $pathTitle"

            pathTitle != null -> pathTitle
            else -> hostTitle
        }
    }

    private fun hostTitle(host: String): String? {
        val label = host
            .removePrefix("www.")
            .split(".")
            .dropLast(1)
            .lastOrNull()
            ?: return null
        return slugTitle(label)
    }

    private fun shouldCombineHostAndPath(host: String, hostTitle: String, pathTitle: String): Boolean {
        if (host.contains("dealdoktor", ignoreCase = true)) return false
        if (pathTitle.contains(hostTitle, ignoreCase = true)) return false
        return pathTitle.length <= SHORT_PATH_TITLE_MAX_LENGTH
    }

    private fun slugTitle(slug: String): String? {
        val decoded = runCatching {
            URLDecoder.decode(slug, StandardCharsets.UTF_8.name())
        }.getOrDefault(slug)
        return decoded
            .replace("_", " ")
            .replace("-", " ")
            .replace(Regex("""(?i)\b(gratistesten|gratis-testen)\b"""), " ")
            .replace(Regex("""(?i)\bbis\s+zu\s+\d+\s*(?:e|euro)\b.*$"""), " ")
            .replace(
                Regex(
                    """(?i)\b(www|de|en|deutschland|welcome|user|deal|deals|gratis|testen|aktion|aktionen|cashback|geld|zurueck|zurueckerhalten|sichern)\b""",
                ),
                " ",
            )
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

    private companion object {
        const val SHORT_PATH_TITLE_MAX_LENGTH = 32
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
    private const val RANGE_SEPARATOR =
        """(?:-|–|—|\bbis\b(?:\s+(?:einschlie(?:ß|ss)lich|zum|zu|spätestens|spaetestens))?|\bto\b)"""

    private val numericRange = Regex(
        pattern = """(?i)\b(\d{1,2})\.(\d{1,2})\.(\d{2,4})?(?:\s*,?\s*\d{1,2}:\d{2}\s*uhr)?\s*$RANGE_SEPARATOR\s*(?:zum\s+)?(\d{1,2})\.(\d{1,2})\.(\d{2,4})\b""",
    )
    private val isoRange = Regex(
        pattern = """(?i)\b(\d{4})-(\d{2})-(\d{2})(?:T\d{2}:\d{2}(?::\d{2})?)?\s*$RANGE_SEPARATOR\s*(\d{4})-(\d{2})-(\d{2})\b""",
    )
    private val untilDate = Regex(
        pattern = """(?i)\b(?:bis|einl(?:ö|oe)seschluss(?:\s+ist)?(?:\s+der)?|teilnahmeschluss|einsendeschluss|hochladen\s+bis|einreichen\s+bis|teilnehmen\s+bis)\s*(?:zum\s+|zum\s+einsenden\s+|einschlie(?:ß|ss)lich\s+|spätestens\s+|spaetestens\s+)?(\d{1,2})\.(\d{1,2})\.(\d{2,4})\b""",
    )
    private val fromDate = Regex(
        pattern = """(?i)\b(?:ab(?:\s+dem)?|vom|von|start(?:et)?(?:\s+ist)?(?:\s+am)?|beginnt(?:\s+am)?|läuft\s+(?:ab|vom)|laeuft\s+(?:ab|vom))\s*(?:am\s+|dem\s+)?(\d{1,2})\.(\d{1,2})\.(\d{2,4})\b""",
    )
    private val numericDate = Regex("""\b(\d{1,2})\.(\d{1,2})\.(\d{2,4})\b""")
    private val isoDate = Regex("""\b(\d{4})-(\d{2})-(\d{2})\b""")

    fun extract(text: String): DateRange? {
        likelyPromotionText(text).forEach { candidate ->
            isoRange.find(candidate)?.let { match ->
                return DateRange(
                    start = createIsoDate(match.groupValues[1], match.groupValues[2], match.groupValues[3]),
                    end = createIsoDate(match.groupValues[4], match.groupValues[5], match.groupValues[6]),
                )
            }

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
            .mapNotNull { match -> createDate(match) }
            .plus(
                isoDate.findAll(text)
                    .mapNotNull { match ->
                        createIsoDate(match.groupValues[1], match.groupValues[2], match.groupValues[3])
                    },
            )
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

    private fun createIsoDate(year: String, month: String, day: String): LocalDate? =
        runCatching {
            LocalDate.of(year.toInt(), month.toInt(), day.toInt())
        }.getOrNull()

    private fun normalizeYear(value: String): Int {
        val number = value.toInt()
        return if (number < 100) 2000 + number else number
    }

    private fun likelyPromotionText(text: String): List<String> {
        val lower = text.lowercase(Locale.GERMANY)
        val keywords = listOf(
            "aktionszeitraum",
            "aktioszeitraum",
            "kaufzeitraum",
            "teilnahmezeitraum",
            "einreichungszeitraum",
            "aktionspackung",
            "gültig für käufe",
            "gueltig fuer kaeufe",
            "gültig für den kauf",
            "gueltig fuer den kauf",
            "käufe vom",
            "kaeufe vom",
            "kauf vom",
            "belegdatum",
            "kassenbon",
            "bon hochladen",
            "hochladen",
            "einreichen",
            "teilnehmen",
            "einl\u00F6seschluss",
            "einloeseschluss",
            "einsendeschluss",
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
