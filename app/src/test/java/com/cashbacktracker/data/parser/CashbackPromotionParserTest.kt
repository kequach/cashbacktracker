package com.cashbacktracker.data.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CashbackPromotionParserTest {
    @Test
    fun parsesTitleAndDateRangeFromHtml() {
        val parser = CashbackPromotionParser(
            fetcher = object : PromotionPageFetcher {
                override suspend fun fetch(url: String): String = error("Not used")
            },
        )

        val parsed = parser.parseHtml(
            html = """
                <html>
                    <head>
                        <meta property="og:title" content="Espresso Maschine | Cashback Aktion">
                    </head>
                    <body>Einloesen vom 01.02.2026 bis 31.03.2026.</body>
                </html>
            """.trimIndent(),
        )

        assertEquals("Espresso Maschine", parsed.productName)
        assertEquals(LocalDate.of(2026, 2, 1), parsed.redemptionStart)
        assertEquals(LocalDate.of(2026, 3, 31), parsed.redemptionEnd)
    }

    @Test
    fun parsesDealdoktorHeadingAndPromotionPeriod() {
        val parser = CashbackPromotionParser(
            fetcher = object : PromotionPageFetcher {
                override suspend fun fetch(url: String): String = error("Not used")
            },
        )

        val parsed = parser.parseHtml(
            html = """
                <html>
                    <body>
                        <h1>ABGELAUFEN Desperados Tropical Daiquiri gratis testen!</h1>
                        <h2>Desperados gratis testen: Aktionszeitraum</h2>
                        <p>
                            Die Desperados gratis testen Aktion findet im Zeitraum
                            vom 16.04.2026 bis 28.06.2026 statt.
                            Einloeseschluss ist der 05.07.2026.
                        </p>
                    </body>
                </html>
            """.trimIndent(),
        )

        assertEquals("Desperados Tropical Daiquiri", parsed.productName)
        assertEquals(LocalDate.of(2026, 4, 16), parsed.redemptionStart)
        assertEquals(LocalDate.of(2026, 6, 28), parsed.redemptionEnd)
    }

    @Test
    fun derivesReadableTitleFromPlainUrlWhenPageFetchFails() {
        val parser = CashbackPromotionParser(
            fetcher = object : PromotionPageFetcher {
                override suspend fun fetch(url: String): String = error("Not used")
            },
        )

        val parsed = parser.parseUrlFallback(
            "https://www.dealdoktor.de/user-deals/wasa-crunchy-bites-gratis-testen-bis-zu-219-e-zurueck/",
        )

        assertEquals("Wasa Crunchy Bites", parsed.productName)
    }
}
