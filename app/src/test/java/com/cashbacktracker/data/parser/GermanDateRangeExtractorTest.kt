package com.cashbacktracker.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class GermanDateRangeExtractorTest {
    @Test
    fun extractsFullNumericRange() {
        val range = GermanDateRangeExtractor.extract("Einloesen vom 01.02.2026 bis 31.03.2026")

        assertEquals(LocalDate.of(2026, 2, 1), range?.start)
        assertEquals(LocalDate.of(2026, 3, 31), range?.end)
    }

    @Test
    fun borrowsEndYearForStartDateWithoutYear() {
        val range = GermanDateRangeExtractor.extract("Aktionszeitraum 01.02. bis 31.03.2026")

        assertEquals(LocalDate.of(2026, 2, 1), range?.start)
        assertEquals(LocalDate.of(2026, 3, 31), range?.end)
    }

    @Test
    fun extractsSingleUntilDateAsEndDate() {
        val range = GermanDateRangeExtractor.extract("Gratis testen bis 30.06.2026")

        assertNull(range?.start)
        assertEquals(LocalDate.of(2026, 6, 30), range?.end)
    }

    @Test
    fun extractsAnnouncementRangeWithMissingStartYear() {
        val range = GermanDateRangeExtractor.extract("Ankuendigung ab 22.06. bis 31.07.2026")

        assertEquals(LocalDate.of(2026, 6, 22), range?.start)
        assertEquals(LocalDate.of(2026, 7, 31), range?.end)
    }

    @Test
    fun returnsNullWhenNoDateIsPresent() {
        assertNull(GermanDateRangeExtractor.extract("Keine Datumsangabe"))
    }
}
