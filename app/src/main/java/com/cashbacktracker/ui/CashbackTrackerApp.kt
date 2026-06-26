package com.cashbacktracker.ui

import android.animation.ValueAnimator
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cashbacktracker.data.model.BankAccount
import com.cashbacktracker.data.model.CashbackDevice
import com.cashbacktracker.data.model.CashbackEntry
import com.cashbacktracker.data.model.CashbackStatus
import com.cashbacktracker.data.util.DateInput
import com.cashbacktracker.data.util.MoneyFormatter
import com.cashbacktracker.viewmodel.AppTab
import com.cashbacktracker.viewmodel.BankAccountFormState
import com.cashbacktracker.viewmodel.CashbackFormState
import com.cashbacktracker.viewmodel.CelebrationEvent
import com.cashbacktracker.viewmodel.CelebrationKind
import com.cashbacktracker.viewmodel.DeviceFormState
import com.cashbacktracker.viewmodel.MainUiState
import com.cashbacktracker.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val PaidStatusContainer = Color(0xFFDDF6E8)
private val PaidStatusContent = Color(0xFF14532D)

@Composable
fun CashbackTrackerApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cashbackForm by viewModel.cashbackForm.collectAsStateWithLifecycle()
    val bankAccountForm by viewModel.bankAccountForm.collectAsStateWithLifecycle()
    val deviceForm by viewModel.deviceForm.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExportWarning by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val csv = viewModel.createExportCsv()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(csv.toByteArray(Charsets.UTF_8))
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val csv = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        .orEmpty()
                }
                viewModel.importCashbackCsv(csv)
            }
        }
    }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = uiState.selectedTab == AppTab.INPUT,
                        onClick = { viewModel.selectTab(AppTab.INPUT) },
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                        label = { Text("Eingabe") },
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == AppTab.DATA,
                        onClick = { viewModel.selectTab(AppTab.DATA) },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Daten") },
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == AppTab.MASTER_DATA,
                        onClick = { viewModel.selectTab(AppTab.MASTER_DATA) },
                        icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                        label = { Text("Stammdaten") },
                    )
                }
            },
        ) { padding ->
            when (uiState.selectedTab) {
                AppTab.INPUT -> CashbackInputScreen(
                    uiState = uiState,
                    form = cashbackForm,
                    modifier = Modifier.padding(padding),
                    onFormChange = viewModel::updateCashbackForm,
                    onAnalyzeUrl = viewModel::analyzeCurrentCashbackUrl,
                    onApplySuggestion = viewModel::applyCashbackSuggestion,
                    onSave = viewModel::saveCashback,
                )

                AppTab.DATA -> CashbackDataScreen(
                uiState = uiState,
                modifier = Modifier.padding(padding),
                onStatusChange = viewModel::updateCashbackStatus,
                onMilestonesEnabledChange = viewModel::setMilestoneCelebrationsEnabled,
                onImportClick = {
                    importLauncher.launch(
                        arrayOf(
                            "text/*",
                            "text/csv",
                            "application/vnd.ms-excel",
                            "application/octet-stream",
                        ),
                    )
                },
                onExportClick = { showExportWarning = true },
                )

                AppTab.MASTER_DATA -> MasterDataScreen(
                    uiState = uiState,
                    bankAccountForm = bankAccountForm,
                    deviceForm = deviceForm,
                    modifier = Modifier.padding(padding),
                    onBankAccountFormChange = viewModel::updateBankAccountForm,
                    onDeviceFormChange = viewModel::updateDeviceForm,
                    onSaveBankAccount = viewModel::saveBankAccount,
                    onSaveDevice = viewModel::saveDevice,
                )
            }
        }

        CelebrationOverlay(
            milestoneMinor = uiState.milestoneToShowMinor,
            celebrationEvent = uiState.celebrationEvent,
            onMilestoneFinished = viewModel::dismissMilestone,
            onCelebrationFinished = viewModel::dismissCelebration,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
        )
    }

    if (showExportWarning) {
        AlertDialog(
            onDismissRequest = { showExportWarning = false },
            title = { Text("Unverschlüsselter CSV-Export") },
            text = {
                Text(
                    "Der CSV-Export enthält Notizen, IBANs und Kontoinhaber lesbar im Klartext. " +
                        "Speichere die Datei nur an einem vertrauenswürdigen Ort.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExportWarning = false
                        exportLauncher.launch("cashback-export.csv")
                    },
                ) {
                    Text("Exportieren")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportWarning = false }) {
                    Text("Abbrechen")
                }
            },
        )
    }

}

@Composable
private fun CashbackInputScreen(
    uiState: MainUiState,
    form: CashbackFormState,
    modifier: Modifier = Modifier,
    onFormChange: ((CashbackFormState) -> CashbackFormState) -> Unit,
    onAnalyzeUrl: () -> Unit,
    onApplySuggestion: (Long) -> Unit,
    onSave: (CashbackStatus) -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    text = "Cashback eingeben",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        item {
            CashbackForm(
                form = form,
                uiState = uiState,
                onFormChange = onFormChange,
                onAnalyzeUrl = onAnalyzeUrl,
                onApplySuggestion = onApplySuggestion,
                onSave = onSave,
            )
        }
    }
}

@Composable
private fun CashbackDataScreen(
    uiState: MainUiState,
    modifier: Modifier = Modifier,
    onStatusChange: (Long, CashbackStatus, CashbackStatus) -> Unit,
    onMilestonesEnabledChange: (Boolean) -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
) {
    val today = remember { LocalDate.now() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Alle Cashbacks",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("Erstattet: ${MoneyFormatter.formatMinor(uiState.paidTotalMinor)}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onImportClick) {
                        Text("Import")
                    }
                    OutlinedButton(onClick = onExportClick) {
                        Text("Export")
                    }
                }
            }
        }

        item {
            MilestoneProgressCard(
                paidTotalMinor = uiState.paidTotalMinor,
                milestonesMinor = uiState.milestonesMinor,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Meilenstein-Animationen")
                Switch(
                    checked = uiState.milestoneSettings.celebrationsEnabled,
                    onCheckedChange = onMilestonesEnabledChange,
                )
            }
        }

        if (uiState.cashbacks.isEmpty()) {
            item { Text("Noch keine Cashback-Einträge.") }
        }

        items(uiState.cashbacks, key = { it.id }) { cashback ->
            CashbackEntryListItem(
                cashback = cashback,
                bankAccounts = uiState.bankAccounts,
                devices = uiState.devices,
                today = today,
                onStatusChange = onStatusChange,
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun MilestoneProgressCard(
    paidTotalMinor: Long,
    milestonesMinor: List<Long>,
) {
    val sortedMilestones = milestonesMinor.sorted()
    if (sortedMilestones.isEmpty()) return

    val previousMilestone = sortedMilestones.lastOrNull { it <= paidTotalMinor } ?: 0L
    val nextMilestone = sortedMilestones.firstOrNull { it > paidTotalMinor }
    val targetProgress = if (nextMilestone == null) {
        1f
    } else {
        ((paidTotalMinor - previousMilestone).toFloat() / (nextMilestone - previousMilestone).toFloat())
            .coerceIn(0f, 1f)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 900),
        label = "milestone-progress",
    )

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Meilenstein-Fortschritt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (nextMilestone == null) {
                            "Alle Meilensteine erreicht"
                        } else {
                            "Nächster Meilenstein: ${MoneyFormatter.formatPlainMinor(nextMilestone)}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = MoneyFormatter.formatPlainMinor(paidTotalMinor),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth(),
            )
            if (nextMilestone != null) {
                Text(
                    text = "${MoneyFormatter.formatPlainMinor(nextMilestone - paidTotalMinor)} bis zum nächsten Meilenstein",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashbackForm(
    form: CashbackFormState,
    uiState: MainUiState,
    onFormChange: ((CashbackFormState) -> CashbackFormState) -> Unit,
    onAnalyzeUrl: () -> Unit,
    onApplySuggestion: (Long) -> Unit,
    onSave: (CashbackStatus) -> Unit,
) {
    var showRedemptionRangePicker by remember { mutableStateOf(false) }
    val linkSuggestions = remember(form.cashbackUrl, uiState.cashbacks) {
        uiState.cashbacks.recentUniqueSuggestions(
            query = form.cashbackUrl,
            valueSelector = CashbackEntry::cashbackUrl,
        )
    }
    val productSuggestions = remember(form.productName, uiState.cashbacks) {
        uiState.cashbacks.recentUniqueSuggestions(
            query = form.productName,
            valueSelector = CashbackEntry::productName,
        )
    }
    val usedPurchaseBankAccountIds = remember(form.cashbackUrl, form.productName, uiState.cashbacks) {
        uiState.cashbacks
            .filter { it.matchesCashbackAction(form) }
            .mapNotNull { it.purchaseBankAccountId }
            .toSet()
    }
    val usedPayoutBankAccountIds = remember(form.cashbackUrl, form.productName, uiState.cashbacks) {
        uiState.cashbacks
            .filter { it.matchesCashbackAction(form) }
            .mapNotNull { it.payoutBankAccountId }
            .toSet()
    }
    val usedDeviceIds = remember(form.cashbackUrl, form.productName, uiState.cashbacks) {
        uiState.cashbacks
            .filter { it.matchesCashbackAction(form) }
            .mapNotNull { it.deviceId }
            .toSet()
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Neues Cashback", style = MaterialTheme.typography.titleMedium)
            SuggestingTextField(
                value = form.cashbackUrl,
                onValueChange = { value -> onFormChange { it.copy(cashbackUrl = value) } },
                label = "Cashback-Link",
                suggestions = linkSuggestions,
                suggestionLabel = { it.cashbackUrl },
                onSuggestionClick = { onApplySuggestion(it.id) },
            )
            Button(
                onClick = onAnalyzeUrl,
                enabled = !uiState.isParsing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isParsing) "Analysiere..." else "URL analysieren")
            }
            SuggestingTextField(
                value = form.productName,
                onValueChange = { value -> onFormChange { it.copy(productName = value) } },
                label = "Produktname",
                suggestions = productSuggestions,
                suggestionLabel = { it.productName },
                onSuggestionClick = { onApplySuggestion(it.id) },
            )
            DateRangeInput(
                form = form,
                onFormChange = onFormChange,
                onOpenPicker = { showRedemptionRangePicker = true },
            )
            OutlinedTextField(
                value = form.purchasePrice,
                onValueChange = { value -> onFormChange { it.copy(purchasePrice = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Kaufpreis") },
                singleLine = true,
            )
            SelectionField(
                label = "Kaufkonto (IBAN)",
                selectedId = form.purchaseBankAccountId,
                options = uiState.bankAccounts.map {
                    SelectionOption(
                        id = it.id,
                        label = it.nickname,
                        isWarning = it.id in usedPurchaseBankAccountIds,
                    )
                },
                onSelect = { id -> onFormChange { it.copy(purchaseBankAccountId = id) } },
            )
            SelectionField(
                label = "Auszahlungskonto (IBAN)",
                selectedId = form.payoutBankAccountId,
                options = uiState.bankAccounts.map {
                    SelectionOption(
                        id = it.id,
                        label = it.nickname,
                        isWarning = it.id in usedPayoutBankAccountIds,
                    )
                },
                onSelect = { id -> onFormChange { it.copy(payoutBankAccountId = id) } },
            )
            SelectionField(
                label = "Gerät",
                selectedId = form.deviceId,
                options = uiState.devices.map {
                    SelectionOption(
                        id = it.id,
                        label = it.name,
                        isWarning = it.id in usedDeviceIds,
                    )
                },
                onSelect = { id -> onFormChange { it.copy(deviceId = id) } },
            )
            OutlinedTextField(
                value = form.notes,
                onValueChange = { value -> onFormChange { it.copy(notes = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notizen") },
                minLines = 2,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onSave(CashbackStatus.PLANNED) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Geplant speichern")
                }
                Button(
                    onClick = { onSave(CashbackStatus.SUBMITTED) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Eingereicht speichern")
                }
            }
        }
    }

    if (showRedemptionRangePicker) {
        CashbackDateRangeDialog(
            form = form,
            onDismiss = { showRedemptionRangePicker = false },
            onConfirm = { start, end ->
                onFormChange {
                    it.copy(
                        redemptionStart = DateInput.format(start),
                        redemptionEnd = DateInput.format(end),
                    )
                }
                showRedemptionRangePicker = false
            },
        )
    }
}

@Composable
private fun SuggestingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<CashbackEntry>,
    suggestionLabel: (CashbackEntry) -> String,
    onSuggestionClick: (CashbackEntry) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        expanded = true
                    }
                },
            label = { Text(label) },
            singleLine = true,
        )
        DropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(suggestionLabel(suggestion), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(suggestion.productName, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSuggestionClick(suggestion)
                    },
                )
            }
        }
    }
}

@Composable
private fun DateRangeInput(
    form: CashbackFormState,
    onFormChange: ((CashbackFormState) -> CashbackFormState) -> Unit,
    onOpenPicker: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Einlösezeitraum", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = form.redemptionStart,
                onValueChange = { value -> onFormChange { it.copy(redemptionStart = value) } },
                modifier = Modifier.weight(1f),
                label = { Text("Start") },
                singleLine = true,
                supportingText = { Text("YYYY-MM-DD") },
            )
            OutlinedTextField(
                value = form.redemptionEnd,
                onValueChange = { value -> onFormChange { it.copy(redemptionEnd = value) } },
                modifier = Modifier.weight(1f),
                label = { Text("Ende") },
                singleLine = true,
                supportingText = { Text("YYYY-MM-DD") },
            )
        }
        OutlinedButton(
            onClick = onOpenPicker,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Einlösezeitraum per Kalender wählen")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashbackDateRangeDialog(
    form: CashbackFormState,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate?, LocalDate?) -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = DateInput.parse(form.redemptionStart)?.toUtcMillis(),
        initialSelectedEndDateMillis = DateInput.parse(form.redemptionEnd)?.toUtcMillis(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        state.selectedStartDateMillis?.toLocalDate(),
                        state.selectedEndDateMillis?.toLocalDate(),
                    )
                },
            ) {
                Text("Übernehmen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
    ) {
        DateRangePicker(
            state = state,
            title = { Text("Einlösezeitraum") },
            headline = null,
        )
    }
}

@Composable
private fun CashbackEntryListItem(
    cashback: CashbackEntry,
    bankAccounts: List<BankAccount>,
    devices: List<CashbackDevice>,
    today: LocalDate,
    onStatusChange: (Long, CashbackStatus, CashbackStatus) -> Unit,
) {
    val purchaseBankAccountName = bankAccounts
        .firstOrNull { it.id == cashback.purchaseBankAccountId }
        ?.nickname
        ?: "-"
    val payoutBankAccountName = bankAccounts
        .firstOrNull { it.id == cashback.payoutBankAccountId }
        ?.nickname
        ?: "-"
    val deviceName = devices.firstOrNull { it.id == cashback.deviceId }?.name ?: "-"
    val statusColors = cashback.status.listItemStatusColors()
    val periodMarker = cashback.periodMarker(today)
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onStatusChange(cashback.id, cashback.status, cashback.status.nextStatus())
            },
        colors = ListItemDefaults.colors(
            containerColor = statusColors.container,
            headlineColor = statusColors.content,
            supportingColor = statusColors.content,
            leadingIconColor = statusColors.content,
            trailingIconColor = statusColors.content,
        ),
        headlineContent = {
            Text(
                text = cashback.productName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(cashback.cashbackUrl, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "Zeitraum: ${formatDateRange(cashback)} | " +
                        "Cashback: ${MoneyFormatter.formatMinor(cashback.purchasePriceMinor)}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (periodMarker != null) {
                    PeriodMarkerChip(periodMarker)
                }
                Text(
                    text = "Kauf: $purchaseBankAccountName | Auszahlung: $payoutBankAccountName | Gerät: $deviceName",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (cashback.notes.isNotBlank()) {
                    Text(
                        text = "Notizen: ${cashback.notes}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        leadingContent = if (cashback.status == CashbackStatus.PAID) {
            {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
            }
        } else {
            null
        },
        trailingContent = {
            Text(
                text = cashback.status.label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun PeriodMarkerChip(marker: ActionPeriodMarker) {
    val containerColor = when (marker) {
        ActionPeriodMarker.NOT_STARTED -> MaterialTheme.colorScheme.tertiaryContainer
        ActionPeriodMarker.EXPIRED_NOT_SUBMITTED -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (marker) {
        ActionPeriodMarker.NOT_STARTED -> MaterialTheme.colorScheme.onTertiaryContainer
        ActionPeriodMarker.EXPIRED_NOT_SUBMITTED -> MaterialTheme.colorScheme.onErrorContainer
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = when (marker) {
                ActionPeriodMarker.NOT_STARTED -> "Noch nicht im Aktionszeitraum"
                ActionPeriodMarker.EXPIRED_NOT_SUBMITTED -> "Aktionszeitraum abgelaufen - noch nicht eingereicht"
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private enum class ActionPeriodMarker {
    NOT_STARTED,
    EXPIRED_NOT_SUBMITTED,
}

private fun CashbackEntry.periodMarker(today: LocalDate): ActionPeriodMarker? =
    when {
        redemptionStart != null && today.isBefore(redemptionStart) -> ActionPeriodMarker.NOT_STARTED
        status == CashbackStatus.PLANNED &&
            redemptionEnd != null &&
            today.isAfter(redemptionEnd) -> ActionPeriodMarker.EXPIRED_NOT_SUBMITTED
        else -> null
    }

private fun CashbackStatus.nextStatus(): CashbackStatus =
    when (this) {
        CashbackStatus.PLANNED -> CashbackStatus.SUBMITTED
        CashbackStatus.SUBMITTED -> CashbackStatus.PAID
        CashbackStatus.PAID -> CashbackStatus.PLANNED
    }

@Composable
private fun CashbackStatus.listItemStatusColors(): StatusListItemColors =
    when (this) {
        CashbackStatus.PLANNED -> StatusListItemColors(
            container = MaterialTheme.colorScheme.surface,
            content = MaterialTheme.colorScheme.onSurface,
        )

        CashbackStatus.SUBMITTED -> StatusListItemColors(
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
        )

        CashbackStatus.PAID -> StatusListItemColors(
            container = PaidStatusContainer,
            content = PaidStatusContent,
        )
    }

private data class StatusListItemColors(
    val container: Color,
    val content: Color,
)

@Composable
private fun MasterDataScreen(
    uiState: MainUiState,
    bankAccountForm: BankAccountFormState,
    deviceForm: DeviceFormState,
    modifier: Modifier = Modifier,
    onBankAccountFormChange: ((BankAccountFormState) -> BankAccountFormState) -> Unit,
    onDeviceFormChange: ((DeviceFormState) -> DeviceFormState) -> Unit,
    onSaveBankAccount: () -> Unit,
    onSaveDevice: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Bankdaten und Geräte",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        BankAccountForm(bankAccountForm, onBankAccountFormChange, onSaveBankAccount)
        DeviceForm(deviceForm, onDeviceFormChange, onSaveDevice)
        ExistingMasterData(uiState)
    }
}

@Composable
private fun BankAccountForm(
    form: BankAccountFormState,
    onChange: ((BankAccountFormState) -> BankAccountFormState) -> Unit,
    onSave: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Bankdaten", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = form.nickname,
                onValueChange = { value -> onChange { it.copy(nickname = value) } },
                label = { Text("Spitzname") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = form.accountHolder,
                onValueChange = { value -> onChange { it.copy(accountHolder = value) } },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = form.iban,
                onValueChange = { value -> onChange { it.copy(iban = value) } },
                label = { Text("IBAN") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text("Bankdaten speichern")
            }
        }
    }
}

@Composable
private fun DeviceForm(
    form: DeviceFormState,
    onChange: ((DeviceFormState) -> DeviceFormState) -> Unit,
    onSave: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Gerät", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = form.name,
                onValueChange = { value -> onChange { it.copy(name = value) } },
                label = { Text("Gerätename") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = form.notes,
                onValueChange = { value -> onChange { it.copy(notes = value) } },
                label = { Text("Notizen") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text("Gerät speichern")
            }
        }
    }
}

@Composable
private fun ExistingMasterData(uiState: MainUiState) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Gespeicherte Stammdaten", style = MaterialTheme.typography.titleMedium)
            Text("IBANs: ${uiState.bankAccounts.joinToString { it.nickname }.ifBlank { "-" }}")
            Text("Geräte: ${uiState.devices.joinToString { it.name }.ifBlank { "-" }}")
        }
    }
}

@Composable
private fun SelectionField(
    label: String,
    selectedId: Long?,
    options: List<SelectionOption>,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.firstOrNull { it.id == selectedId }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = selectedOption?.label ?: "Nicht ausgewählt",
                color = if (selectedOption?.isWarning == true) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Nicht ausgewählt") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = option.label,
                                color = if (option.isWarning) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            if (option.isWarning) {
                                Text(
                                    text = "Bereits für diese Aktion verwendet",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelect(option.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

private data class SelectionOption(
    val id: Long,
    val label: String,
    val isWarning: Boolean = false,
)

@Composable
private fun CelebrationOverlay(
    milestoneMinor: Long?,
    celebrationEvent: CelebrationEvent?,
    onMilestoneFinished: () -> Unit,
    onCelebrationFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (milestoneMinor == null && celebrationEvent == null) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (milestoneMinor != null) {
            MilestoneCelebrationCard(
                eventKey = "milestone-$milestoneMinor",
                amountMinor = milestoneMinor,
                onFinished = onMilestoneFinished,
            )
        }
        if (celebrationEvent != null) {
            TransientCelebrationCard(
                eventKey = "celebration-${celebrationEvent.id}",
                title = when (celebrationEvent.kind) {
                    CelebrationKind.CREATED -> "Cashback angelegt"
                    CelebrationKind.PAID -> "Cashback überwiesen"
                },
                body = when (celebrationEvent.kind) {
                    CelebrationKind.CREATED -> "Die Aktion ist gespeichert."
                    CelebrationKind.PAID -> "Diese Erstattung ist erledigt."
                },
                onFinished = onCelebrationFinished,
            )
        }
    }
}

@Composable
private fun MilestoneCelebrationCard(
    eventKey: String,
    amountMinor: Long,
    onFinished: () -> Unit,
) {
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()
    var targetProgress by remember(eventKey) { mutableStateOf(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(eventKey) {
        targetProgress = 1f
        delay(3_200)
        onFinished()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1_150),
        label = "milestone-celebration-progress",
    )

    val scale: Float
    val rotation: Float
    if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = eventKey)
        scale = transition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.24f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 560),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "milestone-scale",
        ).value
        rotation = transition.animateFloat(
            initialValue = -10f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 460),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "milestone-rotation",
        ).value
    } else {
        scale = 1f
        rotation = 0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = null,
                    modifier = Modifier
                        .scale(scale)
                        .rotate(rotation),
                )
                Column {
                    Text(
                        text = "Meilenstein erreicht",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${MoneyFormatter.formatPlainMinor(amountMinor)} Erstattungen geschafft.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TransientCelebrationCard(
    eventKey: String,
    title: String,
    body: String,
    onFinished: () -> Unit,
) {
    LaunchedEffect(eventKey) {
        delay(2_300)
        onFinished()
    }

    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()
    val scale: Float
    val rotation: Float
    if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = eventKey)
        scale = transition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.18f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 520),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "celebration-scale",
        ).value
        rotation = transition.animateFloat(
            initialValue = -7f,
            targetValue = 7f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 420),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "celebration-rotation",
        ).value
    } else {
        scale = 1f
        rotation = 0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Celebration,
                contentDescription = null,
                modifier = Modifier
                    .scale(scale)
                    .rotate(rotation),
            )
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun formatDateRange(cashback: CashbackEntry): String {
    val start = DateInput.format(cashback.redemptionStart).ifBlank { "-" }
    val end = DateInput.format(cashback.redemptionEnd).ifBlank { "-" }
    return "$start bis $end"
}

private fun List<CashbackEntry>.recentUniqueSuggestions(
    query: String,
    valueSelector: (CashbackEntry) -> String,
): List<CashbackEntry> {
    val trimmedQuery = query.trim()
    return asSequence()
        .filter { valueSelector(it).isNotBlank() }
        .filter { cashback ->
            trimmedQuery.isBlank() ||
                valueSelector(cashback).contains(trimmedQuery, ignoreCase = true)
        }
        .sortedWith(
            compareByDescending<CashbackEntry> { it.createdAt }
                .thenByDescending { it.id },
        )
        .distinctBy { valueSelector(it).normalizedActionKey() }
        .take(3)
        .toList()
}

private fun CashbackEntry.matchesCashbackAction(form: CashbackFormState): Boolean {
    val formUrl = form.cashbackUrl.normalizedActionKey()
    if (formUrl.isNotBlank()) {
        return cashbackUrl.normalizedActionKey() == formUrl
    }

    val formProductName = form.productName.normalizedActionKey()
    return formProductName.isNotBlank() && productName.normalizedActionKey() == formProductName
}

private fun String.normalizedActionKey(): String =
    trim()
        .trimEnd('/')
        .lowercase()

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
