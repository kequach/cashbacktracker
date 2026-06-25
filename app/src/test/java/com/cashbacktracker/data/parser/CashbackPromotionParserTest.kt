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
    fun parsesRealisticCheezItPromotionPeriod() {
        val parser = CashbackPromotionParser(
            fetcher = object : PromotionPageFetcher {
                override suspend fun fetch(url: String): String = error("Not used")
            },
        )

        val parsed = parser.parseHtml(
            html = """
                <html>
                    <head><title>Cheez-It</title></head>
                    <body>
                        <h1>Cheez-It</h1>
                        <p>Aktionszeitraum: 01.09.25 - 30.06.26</p>
                        <p>
                            Gültig für Käufe vom 01.09.2025 bis 30.06.2026.
                            Kassenbon fotografieren und bis 30.06.2026 hochladen.
                        </p>
                    </body>
                </html>
            """.trimIndent(),
        )

        assertEquals("Cheez-It", parsed.productName)
        assertEquals(LocalDate.of(2025, 9, 1), parsed.redemptionStart)
        assertEquals(LocalDate.of(2026, 6, 30), parsed.redemptionEnd)
    }

    @Test
    fun parsesDateRangeFromEmbeddedScriptData() {
        val parser = CashbackPromotionParser(
            fetcher = object : PromotionPageFetcher {
                override suspend fun fetch(url: String): String = error("Not used")
            },
        )

        val parsed = parser.parseHtml(
            html = """
                <html>
                    <head>
                        <meta property="og:title" content="Antikal WC Gel testen">
                    </head>
                    <body>
                        <script>
                            window.__ACTION__ = {
                                kaufzeitraum: { start: "2026-06-23", end: "2026-07-31" }
                            };
                        </script>
                    </body>
                </html>
            """.trimIndent(),
        )

        assertEquals("Antikal WC Gel testen", parsed.productName)
        assertEquals(LocalDate.of(2026, 6, 23), parsed.redemptionStart)
        assertEquals(LocalDate.of(2026, 7, 31), parsed.redemptionEnd)
    }

    @Test
    fun ignoresStruckThroughOutdatedDates() {
        val parser = CashbackPromotionParser(
            fetcher = object : PromotionPageFetcher {
                override suspend fun fetch(url: String): String = error("Not used")
            },
        )

        val parsed = parser.parseHtml(
            html = """
                <html>
                    <body>
                        <h1>Whiskas risikolos testen</h1>
                        <p>
                            Die Aktion startet am <s>04.01.2026</s> 01.04.2026
                            und soll bis 30.06.2026 dauern.
                        </p>
                    </body>
                </html>
            """.trimIndent(),
        )

        assertEquals(LocalDate.of(2026, 4, 1), parsed.redemptionStart)
        assertEquals(LocalDate.of(2026, 6, 30), parsed.redemptionEnd)
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

    @Test
    fun derivesCampaignTitleFromOfficialHostAndPathWhenPageFetchFails() {
        val parser = CashbackPromotionParser(
            fetcher = object : PromotionPageFetcher {
                override suspend fun fetch(url: String): String = error("Not used")
            },
        )

        val parsed = parser.parseUrlFallback("https://tetesept-aktion.de/melatoninspray")

        assertEquals("Tetesept Melatoninspray", parsed.productName)
    }

    @Test
    fun derivesCampaignTitleFromOfficialHostWhenPathIsGeneric() {
        val parser = CashbackPromotionParser(
            fetcher = object : PromotionPageFetcher {
                override suspend fun fetch(url: String): String = error("Not used")
            },
        )

        val parsed = parser.parseUrlFallback("https://purrorpass.whiskas.de/aktion/de-de/welcome/")

        assertEquals("Whiskas", parsed.productName)
    }
}
