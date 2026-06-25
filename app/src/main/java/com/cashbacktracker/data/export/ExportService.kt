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
            "Kaufkonto-Spitzname",
            "Kaufkonto-IBAN",
            "Kaufkonto-Kontoinhaber",
            "Auszahlungskonto-Spitzname",
            "Auszahlungskonto-IBAN",
            "Auszahlungskonto-Kontoinhaber",
            "Geraet",
            "Status",
            "Notizen",
        )
        val rows = cashbacks.map { cashback ->
            val purchaseBankAccount = bankAccounts.firstOrNull { it.id == cashback.purchaseBankAccountId }
            val payoutBankAccount = bankAccounts.firstOrNull { it.id == cashback.payoutBankAccountId }
            val device = devices.firstOrNull { it.id == cashback.deviceId }
            listOf(
                cashback.cashbackUrl,
                cashback.productName,
                cashback.redemptionStart?.toString().orEmpty(),
                cashback.redemptionEnd?.toString().orEmpty(),
                MoneyFormatter.formatPlainMinor(cashback.purchasePriceMinor, cashback.currency),
                purchaseBankAccount?.nickname.orEmpty(),
                purchaseBankAccount?.iban.orEmpty(),
                purchaseBankAccount?.accountHolder.orEmpty(),
                payoutBankAccount?.nickname.orEmpty(),
                payoutBankAccount?.iban.orEmpty(),
                payoutBankAccount?.accountHolder.orEmpty(),
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
