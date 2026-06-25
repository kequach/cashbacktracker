package com.cashbacktracker.data.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyFormatterTest {
    @Test
    fun parsesGermanMoneyInputToMinorUnits() {
        assertEquals(123_45L, MoneyFormatter.parseMinor("123,45 EUR"))
        assertEquals(1_234_00L, MoneyFormatter.parseMinor("1.234"))
        assertEquals(999L, MoneyFormatter.parseMinor("9,99"))
    }

    @Test
    fun returnsNullForBlankOrInvalidMoney() {
        assertNull(MoneyFormatter.parseMinor(""))
        assertNull(MoneyFormatter.parseMinor("abc"))
    }
}
