package com.cashbacktracker.viewmodel

import com.cashbacktracker.data.model.BankAccount
import com.cashbacktracker.data.model.CashbackDevice
import com.cashbacktracker.data.model.CashbackEntry
import com.cashbacktracker.data.model.MilestoneSettings

enum class AppTab {
    INPUT,
    DATA,
    MASTER_DATA,
}

data class MainUiState(
    val selectedTab: AppTab = AppTab.INPUT,
    val cashbacks: List<CashbackEntry> = emptyList(),
    val bankAccounts: List<BankAccount> = emptyList(),
    val devices: List<CashbackDevice> = emptyList(),
    val milestoneSettings: MilestoneSettings = MilestoneSettings(
        celebrationsEnabled = true,
        shownMilestonesMinor = emptySet(),
    ),
    val paidTotalMinor: Long = 0,
    val message: String? = null,
    val isParsing: Boolean = false,
    val milestoneToShowMinor: Long? = null,
    val celebrationEvent: CelebrationEvent? = null,
)

data class RepositorySnapshot(
    val cashbacks: List<CashbackEntry>,
    val bankAccounts: List<BankAccount>,
    val devices: List<CashbackDevice>,
)

data class UiChromeState(
    val selectedTab: AppTab,
    val message: String?,
    val isParsing: Boolean,
    val milestoneToShowMinor: Long?,
    val celebrationEvent: CelebrationEvent?,
)

enum class CelebrationKind {
    CREATED,
    PAID,
}

data class CelebrationEvent(
    val id: Long,
    val kind: CelebrationKind,
)
