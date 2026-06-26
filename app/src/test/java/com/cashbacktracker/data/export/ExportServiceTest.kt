package com.cashbacktracker.data.export

import com.cashbacktracker.data.model.BankAccount
import com.cashbacktracker.data.model.CashbackDevice
import com.cashbacktracker.data.model.CashbackEntry
import com.cashbacktracker.data.model.CashbackStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExportServiceTest {
    @Test
    fun exportsCashbacksAsSemicolonSeparatedCsv() {
        val csv = ExportService().createCashbackCsv(
            bankAccounts = listOf(
                BankAccount(
                    id = 7,
                    nickname = "Haushalt",
                    accountHolder = "Max Muster",
                    iban = "DE02120300000000202051",
                ),
                BankAccount(
                    id = 8,
                    nickname = "Cashback",
                    accountHolder = "Erika Muster",
                    iban = "DE02120300000000202052",
                ),
            ),
            devices = listOf(CashbackDevice(id = 9, name = "Pixel Test", notes = "")),
            cashbacks = listOf(
                CashbackEntry(
                    id = 1,
                    cashbackUrl = "https://example.test/cashback",
                    productName = "Test Produkt",
                    redemptionStart = LocalDate.of(2026, 6, 1),
                    redemptionEnd = LocalDate.of(2026, 6, 30),
                    purchasePriceMinor = 249,
                    currency = "EUR",
                    purchaseBankAccountId = 7,
                    payoutBankAccountId = 8,
                    deviceId = 9,
                    notes = "Bon hochladen",
                    status = CashbackStatus.PAID,
                    createdAt = 0,
                    updatedAt = 0,
                ),
            ),
        )

        assertTrue(csv.startsWith("\"Cashback-Link\";\"Produkt\";\"Einlösestart\""))
        assertTrue(csv.contains("\"Test Produkt\""))
        assertTrue(csv.contains("\"2,49 EUR\""))
        assertTrue(csv.contains("\"Haushalt\""))
        assertTrue(csv.contains("\"Cashback\""))
        assertTrue(csv.contains("\"Pixel Test\""))
        assertTrue(csv.contains("\"Überwiesen\""))
    }

    @Test
    fun importsCashbackCsvRows() {
        val csv = ExportService().createCashbackCsv(
            bankAccounts = listOf(
                BankAccount(
                    id = 7,
                    nickname = "Haushalt",
                    accountHolder = "Max Muster",
                    iban = "DE02120300000000202051",
                ),
                BankAccount(
                    id = 8,
                    nickname = "Cashback",
                    accountHolder = "Erika Muster",
                    iban = "DE02120300000000202052",
                ),
            ),
            devices = listOf(CashbackDevice(id = 9, name = "Pixel Test", notes = "")),
            cashbacks = listOf(
                CashbackEntry(
                    id = 1,
                    cashbackUrl = "https://example.test/cashback",
                    productName = "Test Produkt",
                    redemptionStart = LocalDate.of(2026, 6, 1),
                    redemptionEnd = LocalDate.of(2026, 6, 30),
                    purchasePriceMinor = 249,
                    currency = "EUR",
                    purchaseBankAccountId = 7,
                    payoutBankAccountId = 8,
                    deviceId = 9,
                    notes = "Bon hochladen; später prüfen",
                    status = CashbackStatus.PAID,
                    createdAt = 0,
                    updatedAt = 0,
                ),
            ),
        )

        val rows = ExportService().parseCashbackCsv(csv)

        assertEquals(1, rows.size)
        assertEquals("https://example.test/cashback", rows.single().cashbackUrl)
        assertEquals("Test Produkt", rows.single().productName)
        assertEquals(LocalDate.of(2026, 6, 1), rows.single().redemptionStart)
        assertEquals(LocalDate.of(2026, 6, 30), rows.single().redemptionEnd)
        assertEquals(249L, rows.single().purchasePriceMinor)
        assertEquals("Haushalt", rows.single().purchaseBankAccount?.nickname)
        assertEquals("Cashback", rows.single().payoutBankAccount?.nickname)
        assertEquals("Pixel Test", rows.single().deviceName)
        assertEquals(CashbackStatus.PAID, rows.single().status)
        assertEquals("Bon hochladen; später prüfen", rows.single().notes)
    }
}
