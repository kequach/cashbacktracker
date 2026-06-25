package com.cashbacktracker.data.export

import com.cashbacktracker.data.model.BankAccount
import com.cashbacktracker.data.model.CashbackDevice
import com.cashbacktracker.data.model.CashbackEntry
import com.cashbacktracker.data.model.CashbackStatus
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
                    bankAccountId = 7,
                    deviceId = 9,
                    notes = "Bon hochladen",
                    status = CashbackStatus.PAID,
                    createdAt = 0,
                    updatedAt = 0,
                ),
            ),
        )

        assertTrue(csv.startsWith("\"Cashback-Link\";\"Produkt\";\"Einloesestart\""))
        assertTrue(csv.contains("\"Test Produkt\""))
        assertTrue(csv.contains("\"2,49 EUR\""))
        assertTrue(csv.contains("\"Pixel Test\""))
        assertTrue(csv.contains("\"Ueberwiesen\""))
    }
}
