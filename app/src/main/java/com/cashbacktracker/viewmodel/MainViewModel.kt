package com.cashbacktracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cashbacktracker.AppContainer
import com.cashbacktracker.data.export.ExportService
import com.cashbacktracker.data.model.CashbackStatus
import com.cashbacktracker.data.parser.CashbackPromotionParser
import com.cashbacktracker.data.repository.BankAccountRepository
import com.cashbacktracker.data.repository.CashbackRepository
import com.cashbacktracker.data.repository.DeviceRepository
import com.cashbacktracker.data.repository.SettingsRepository
import com.cashbacktracker.data.util.DateInput
import com.cashbacktracker.data.util.MoneyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val cashbackRepository: CashbackRepository,
    private val bankAccountRepository: BankAccountRepository,
    private val deviceRepository: DeviceRepository,
    private val settingsRepository: SettingsRepository,
    private val parser: CashbackPromotionParser,
    private val exportService: ExportService,
) : ViewModel() {
    private val selectedTab = MutableStateFlow(AppTab.INPUT)
    private val message = MutableStateFlow<String?>(null)
    private val isParsing = MutableStateFlow(false)
    private val milestoneToShowMinor = MutableStateFlow<Long?>(null)
    private val celebrationEvent = MutableStateFlow<CelebrationEvent?>(null)

    val cashbackForm = MutableStateFlow(CashbackFormState())
    val bankAccountForm = MutableStateFlow(BankAccountFormState())
    val deviceForm = MutableStateFlow(DeviceFormState())

    private val repositorySnapshot = combine(
        cashbackRepository.cashbacks,
        bankAccountRepository.bankAccounts,
        deviceRepository.devices,
    ) { cashbacks, bankAccounts, devices ->
        RepositorySnapshot(cashbacks, bankAccounts, devices)
    }

    private val uiChrome = combine(
        selectedTab,
        message,
        isParsing,
        milestoneToShowMinor,
        celebrationEvent,
    ) { tab, userMessage, parsing, milestone, celebration ->
        UiChromeState(
            selectedTab = tab,
            message = userMessage,
            isParsing = parsing,
            milestoneToShowMinor = milestone,
            celebrationEvent = celebration,
        )
    }

    val uiState = combine(
        repositorySnapshot,
        settingsRepository.milestoneSettings,
        uiChrome,
    ) { snapshot, settings, chrome ->
        MainUiState(
            selectedTab = chrome.selectedTab,
            cashbacks = snapshot.cashbacks,
            bankAccounts = snapshot.bankAccounts,
            devices = snapshot.devices,
            milestoneSettings = settings,
            paidTotalMinor = snapshot.cashbacks
                .filter { it.status == CashbackStatus.PAID }
                .sumOf { it.purchasePriceMinor },
            message = chrome.message,
            isParsing = chrome.isParsing,
            milestoneToShowMinor = chrome.milestoneToShowMinor,
            celebrationEvent = chrome.celebrationEvent,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    init {
        viewModelScope.launch {
            combine(cashbackRepository.cashbacks, settingsRepository.milestoneSettings) { cashbacks, settings ->
                if (!settings.celebrationsEnabled) return@combine null
                val paidTotal = cashbacks
                    .filter { it.status == CashbackStatus.PAID }
                    .sumOf { it.purchasePriceMinor }

                MILESTONES_MINOR
                    .filter { it <= paidTotal }
                    .firstOrNull { it !in settings.shownMilestonesMinor }
            }.collect { milestone ->
                if (milestone != null) {
                    settingsRepository.markMilestoneShown(milestone)
                    milestoneToShowMinor.value = milestone
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        selectedTab.value = tab
    }

    fun updateCashbackForm(transform: (CashbackFormState) -> CashbackFormState) {
        cashbackForm.update(transform)
    }

    fun updateBankAccountForm(transform: (BankAccountFormState) -> BankAccountFormState) {
        bankAccountForm.update(transform)
    }

    fun updateDeviceForm(transform: (DeviceFormState) -> DeviceFormState) {
        deviceForm.update(transform)
    }

    fun analyzeCurrentCashbackUrl() {
        val url = cashbackForm.value.cashbackUrl.trim()
        if (!url.startsWith("https://", ignoreCase = true)) {
            message.value = "Bitte zuerst einen gueltigen HTTPS-Link einfuegen."
            return
        }

        viewModelScope.launch {
            isParsing.value = true
            message.value = null
            val fallback = parser.parseUrlFallback(url)
            runCatching { parser.parse(url) }
                .onSuccess { parsed ->
                    val hasPageData = parsed.productName != null ||
                        parsed.redemptionStart != null ||
                        parsed.redemptionEnd != null
                    cashbackForm.update { form ->
                        form.copy(
                            productName = parsed.productName
                                ?: fallback.productName
                                ?: form.productName,
                            redemptionStart = parsed.redemptionStart?.let(DateInput::format)
                                ?: form.redemptionStart,
                            redemptionEnd = parsed.redemptionEnd?.let(DateInput::format)
                                ?: form.redemptionEnd,
                        )
                    }
                    message.value = if (hasPageData) {
                        "URL wurde analysiert. Bitte Felder pruefen."
                    } else {
                        "Keine Aktionsdaten gefunden. Produkttitel wurde aus dem Link abgeleitet."
                    }
                }
                .onFailure {
                    cashbackForm.update { form ->
                        form.copy(productName = fallback.productName ?: form.productName)
                    }
                    message.value = if (fallback.productName != null) {
                        "Webseite konnte nicht gelesen werden. Produkttitel wurde aus dem Link abgeleitet."
                    } else {
                        "URL konnte nicht automatisch gelesen werden."
                    }
                }
            isParsing.value = false
        }
    }

    fun saveCashback(status: CashbackStatus) {
        val form = cashbackForm.value
        val purchasePriceMinor = MoneyFormatter.parseMinor(form.purchasePrice)
        val redemptionStart = DateInput.parse(form.redemptionStart)
        val redemptionEnd = DateInput.parse(form.redemptionEnd)
        if (form.cashbackUrl.isBlank() || form.productName.isBlank() || purchasePriceMinor == null) {
            message.value = "Link, Produkt und Kaufpreis sind Pflichtfelder."
            return
        }
        if ((form.redemptionStart.isNotBlank() && redemptionStart == null) ||
            (form.redemptionEnd.isNotBlank() && redemptionEnd == null)
        ) {
            message.value = "Datumsfelder bitte als YYYY-MM-DD eingeben."
            return
        }
        if (redemptionStart != null && redemptionEnd != null && redemptionStart.isAfter(redemptionEnd)) {
            message.value = "Startdatum darf nicht nach dem Enddatum liegen."
            return
        }

        viewModelScope.launch {
            cashbackRepository.addCashback(
                cashbackUrl = form.cashbackUrl,
                productName = form.productName,
                redemptionStart = redemptionStart,
                redemptionEnd = redemptionEnd,
                purchasePriceMinor = purchasePriceMinor,
                bankAccountId = form.bankAccountId,
                deviceId = form.deviceId,
                notes = form.notes,
                status = status,
            )
            cashbackForm.value = CashbackFormState()
            celebrationEvent.value = CelebrationEvent(
                id = System.currentTimeMillis(),
                kind = CelebrationKind.CREATED,
            )
        }
    }

    fun saveBankAccount() {
        val form = bankAccountForm.value
        if (form.accountHolder.isBlank() || form.iban.isBlank()) {
            message.value = "Name und IBAN sind Pflichtfelder."
            return
        }
        viewModelScope.launch {
            bankAccountRepository.addBankAccount(form.nickname, form.accountHolder, form.iban)
            bankAccountForm.value = BankAccountFormState()
            message.value = "Bankdaten wurden gespeichert."
        }
    }

    fun saveDevice() {
        val form = deviceForm.value
        if (form.name.isBlank()) {
            message.value = "Geraetename ist ein Pflichtfeld."
            return
        }
        viewModelScope.launch {
            deviceRepository.addDevice(form.name, form.notes)
            deviceForm.value = DeviceFormState()
            message.value = "Geraet wurde gespeichert."
        }
    }

    fun updateCashbackStatus(id: Long, currentStatus: CashbackStatus, newStatus: CashbackStatus) {
        if (currentStatus == newStatus) return

        viewModelScope.launch {
            cashbackRepository.setStatus(id = id, status = newStatus)
            if (newStatus == CashbackStatus.PAID) {
                celebrationEvent.value = CelebrationEvent(
                    id = System.currentTimeMillis(),
                    kind = CelebrationKind.PAID,
                )
            } else {
                message.value = "Status wurde aktualisiert."
            }
        }
    }

    fun applyCashbackSuggestion(entryId: Long) {
        val entry = uiState.value.cashbacks.firstOrNull { it.id == entryId } ?: return
        cashbackForm.update { form ->
            form.copy(
                cashbackUrl = entry.cashbackUrl,
                productName = entry.productName,
                redemptionStart = DateInput.format(entry.redemptionStart),
                redemptionEnd = DateInput.format(entry.redemptionEnd),
                purchasePrice = MoneyFormatter.formatPlainMinor(entry.purchasePriceMinor, entry.currency),
                notes = entry.notes,
                bankAccountId = null,
                deviceId = null,
            )
        }
        message.value = "Vorlage uebernommen. Bitte IBAN und Geraet waehlen."
    }

    fun setMilestoneCelebrationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMilestoneCelebrationsEnabled(enabled)
        }
    }

    fun dismissMilestone() {
        milestoneToShowMinor.value = null
    }

    fun dismissCelebration() {
        celebrationEvent.value = null
    }

    suspend fun createExportCsv(): String {
        val snapshot = repositorySnapshot.first()
        return exportService.createCashbackCsv(
            bankAccounts = snapshot.bankAccounts,
            devices = snapshot.devices,
            cashbacks = snapshot.cashbacks,
        )
    }

    fun clearMessage() {
        message.value = null
    }

    class Factory(
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                cashbackRepository = container.cashbackRepository,
                bankAccountRepository = container.bankAccountRepository,
                deviceRepository = container.deviceRepository,
                settingsRepository = container.settingsRepository,
                parser = container.parser,
                exportService = container.exportService,
            ) as T
        }
    }

    private companion object {
        val MILESTONES_MINOR = listOf(10_000L, 50_000L, 100_000L)
    }
}
