package com.cashbacktracker.data.export

import com.cashbacktracker.data.model.BankAccount
import com.cashbacktracker.data.model.CashbackDevice
import com.cashbacktracker.data.model.CashbackEntry
import com.cashbacktracker.data.util.MoneyFormatter

class ExportService {
    fun createCashbackCsv(
        bankAccounts: List<BankAccount>,
        devices: List<CashbackDevice>,
        cashbacks: List<CashbackEntry>,
    ): String {
        val header = listOf(
            "Cashback-Link",
            "Produkt",
            "Einloesestart",
            "Einloeseende",
            "Kaufpreis",
            "IBAN-Spitzname",
            "IBAN",
            "Kontoinhaber",
            "Geraet",
            "Status",
            "Notizen",
        )
        val rows = cashbacks.map { cashback ->
            val bankAccount = bankAccounts.firstOrNull { it.id == cashback.bankAccountId }
            val device = devices.firstOrNull { it.id == cashback.deviceId }
            listOf(
                cashback.cashbackUrl,
                cashback.productName,
                cashback.redemptionStart?.toString().orEmpty(),
                cashback.redemptionEnd?.toString().orEmpty(),
                MoneyFormatter.formatPlainMinor(cashback.purchasePriceMinor, cashback.currency),
                bankAccount?.nickname.orEmpty(),
                bankAccount?.iban.orEmpty(),
                bankAccount?.accountHolder.orEmpty(),
                device?.name.orEmpty(),
                cashback.status.label,
                cashback.notes,
            )
        }

        return (listOf(header) + rows)
            .joinToString(separator = "\r\n", postfix = "\r\n") { row ->
                row.joinToString(separator = ";") { it.toCsvCell() }
            }
    }

    private fun String.toCsvCell(): String {
        val escaped = replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
