package com.cashbacktracker.data.export

import com.cashbacktracker.data.model.BankAccount
import com.cashbacktracker.data.model.CashbackDevice
import com.cashbacktracker.data.model.CashbackEntry
import com.cashbacktracker.data.model.CashbackStatus
import com.cashbacktracker.data.util.DateInput
import com.cashbacktracker.data.util.MoneyFormatter
import java.time.LocalDate

class ExportService {
    fun createCashbackCsv(
        bankAccounts: List<BankAccount>,
        devices: List<CashbackDevice>,
        cashbacks: List<CashbackEntry>,
    ): String {
        val header = listOf(
            "Cashback-Link",
            "Produkt",
            "Einlösestart",
            "Einlöseende",
            "Kaufpreis",
            "Kaufkonto-Spitzname",
            "Kaufkonto-IBAN",
            "Kaufkonto-Kontoinhaber",
            "Auszahlungskonto-Spitzname",
            "Auszahlungskonto-IBAN",
            "Auszahlungskonto-Kontoinhaber",
            "Gerät",
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

    fun parseCashbackCsv(csv: String): List<CashbackCsvImportRow> {
        val rows = parseCsvRows(csv).filter { row -> row.any { it.isNotBlank() } }
        if (rows.isEmpty()) return emptyList()

        val headerIndices = rows.first()
            .map { it.trim() }
            .withIndex()
            .associate { (index, header) -> header.normalizedHeader() to index }

        return rows.drop(1).mapNotNull { row ->
            val cashbackUrl = row.cell(headerIndices, "Cashback-Link")
            val productName = row.cell(headerIndices, "Produkt")
            val purchasePriceMinor = MoneyFormatter.parseMinor(row.cell(headerIndices, "Kaufpreis"))
            if (cashbackUrl.isBlank() || productName.isBlank() || purchasePriceMinor == null) {
                return@mapNotNull null
            }

            CashbackCsvImportRow(
                cashbackUrl = cashbackUrl,
                productName = productName,
                redemptionStart = DateInput.parse(row.cell(headerIndices, "Einlösestart")),
                redemptionEnd = DateInput.parse(row.cell(headerIndices, "Einlöseende")),
                purchasePriceMinor = purchasePriceMinor,
                purchaseBankAccount = CashbackCsvBankAccount(
                    nickname = row.cell(headerIndices, "Kaufkonto-Spitzname"),
                    iban = row.cell(headerIndices, "Kaufkonto-IBAN"),
                    accountHolder = row.cell(headerIndices, "Kaufkonto-Kontoinhaber"),
                ).takeIf { it.hasData },
                payoutBankAccount = CashbackCsvBankAccount(
                    nickname = row.cell(headerIndices, "Auszahlungskonto-Spitzname"),
                    iban = row.cell(headerIndices, "Auszahlungskonto-IBAN"),
                    accountHolder = row.cell(headerIndices, "Auszahlungskonto-Kontoinhaber"),
                ).takeIf { it.hasData },
                deviceName = row.cell(headerIndices, "Gerät").ifBlank { null },
                status = row.cell(headerIndices, "Status").toCashbackStatus(),
                notes = row.cell(headerIndices, "Notizen"),
            )
        }
    }

    private fun String.toCsvCell(): String {
        val escaped = replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun parseCsvRows(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentCell = StringBuilder()
        var inQuotes = false
        var index = 0

        fun finishCell() {
            currentRow += currentCell.toString()
            currentCell.clear()
        }

        fun finishRow() {
            finishCell()
            rows += currentRow.toList()
            currentRow.clear()
        }

        while (index < csv.length) {
            when (val char = csv[index]) {
                '"' -> {
                    val nextIsQuote = inQuotes && index + 1 < csv.length && csv[index + 1] == '"'
                    if (nextIsQuote) {
                        currentCell.append('"')
                        index += 1
                    } else {
                        inQuotes = !inQuotes
                    }
                }

                ';' -> {
                    if (inQuotes) {
                        currentCell.append(char)
                    } else {
                        finishCell()
                    }
                }

                '\r', '\n' -> {
                    if (inQuotes) {
                        currentCell.append(char)
                    } else {
                        finishRow()
                        if (char == '\r' && index + 1 < csv.length && csv[index + 1] == '\n') {
                            index += 1
                        }
                    }
                }

                else -> currentCell.append(char)
            }
            index += 1
        }

        if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
            finishRow()
        }

        return rows
    }

    private fun List<String>.cell(headerIndices: Map<String, Int>, header: String): String {
        val index = headerIndices[header.normalizedHeader()] ?: return ""
        return getOrElse(index) { "" }.trim()
    }

    private fun String.normalizedHeader(): String =
        trim().removePrefix("\uFEFF").lowercase()

    private fun String.toCashbackStatus(): CashbackStatus {
        val normalized = trim().lowercase()
        return CashbackStatus.values().firstOrNull { status ->
            status.name.lowercase() == normalized || status.label.lowercase() == normalized
        } ?: when (normalized) {
            "ueberwiesen" -> CashbackStatus.PAID
            else -> CashbackStatus.PLANNED
        }
    }
}

data class CashbackCsvImportRow(
    val cashbackUrl: String,
    val productName: String,
    val redemptionStart: LocalDate?,
    val redemptionEnd: LocalDate?,
    val purchasePriceMinor: Long,
    val purchaseBankAccount: CashbackCsvBankAccount?,
    val payoutBankAccount: CashbackCsvBankAccount?,
    val deviceName: String?,
    val status: CashbackStatus,
    val notes: String,
)

data class CashbackCsvBankAccount(
    val nickname: String,
    val iban: String,
    val accountHolder: String,
) {
    val hasData: Boolean =
        nickname.isNotBlank() || iban.isNotBlank() || accountHolder.isNotBlank()
}
