package com.cashbacktracker.data.model

import java.time.LocalDate

enum class CashbackStatus(val label: String) {
    PLANNED("Geplant"),
    SUBMITTED("Eingereicht"),
    PAID("Ueberwiesen"),
}

data class BankAccount(
    val id: Long,
    val nickname: String,
    val accountHolder: String,
    val iban: String,
)

data class CashbackDevice(
    val id: Long,
    val name: String,
    val notes: String,
)

data class CashbackEntry(
    val id: Long,
    val cashbackUrl: String,
    val productName: String,
    val redemptionStart: LocalDate?,
    val redemptionEnd: LocalDate?,
    val purchasePriceMinor: Long,
    val currency: String,
    val bankAccountId: Long?,
    val deviceId: Long?,
    val notes: String,
    val status: CashbackStatus,
    val createdAt: Long,
    val updatedAt: Long,
)

data class CashbackOverview(
    val entries: List<CashbackEntry>,
) {
    val paidTotalMinor: Long = entries
        .asSequence()
        .filter { it.status == CashbackStatus.PAID }
        .map { it.purchasePriceMinor }
        .sum()
}

data class MilestoneSettings(
    val celebrationsEnabled: Boolean,
    val shownMilestonesMinor: Set<Long>,
)

data class ParsedCashbackPromotion(
    val productName: String? = null,
    val redemptionStart: LocalDate? = null,
    val redemptionEnd: LocalDate? = null,
)

data class DateRange(
    val start: LocalDate?,
    val end: LocalDate?,
)
