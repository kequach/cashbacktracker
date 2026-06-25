package com.cashbacktracker.ui

import android.animation.ValueAnimator
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
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
                    onTogglePaid = viewModel::togglePaid,
                    onMilestonesEnabledChange = viewModel::setMilestoneCelebrationsEnabled,
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
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp),
        )
    }

    if (showExportWarning) {
        AlertDialog(
            onDismissRequest = { showExportWarning = false },
            title = { Text("Unverschluesselter CSV-Export") },
            text = {
                Text(
                    "Der CSV-Export enthaelt Notizen, IBANs und Kontoinhaber lesbar im Klartext. " +
                        "Speichere die Datei nur an einem vertrauenswuerdigen Ort.",
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
    onSave: () -> Unit,
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
                Text("100% Cashback: Der Kaufpreis ist zugleich die erwartete Erstattung.")
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
    onTogglePaid: (Long, Boolean) -> Unit,
    onMilestonesEnabledChange: (Boolean) -> Unit,
    onExportClick: () -> Unit,
) {
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
                OutlinedButton(onClick = onExportClick) {
                    Text("CSV")
                }
            }
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
            item { Text("Noch keine Cashback-Eintraege.") }
        }

        items(uiState.cashbacks, key = { it.id }) { cashback ->
            CashbackEntryListItem(
                cashback = cashback,
                bankAccounts = uiState.bankAccounts,
                devices = uiState.devices,
                onTogglePaid = onTogglePaid,
            )
            HorizontalDivider()
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
    onSave: () -> Unit,
) {
    var showRedemptionRangePicker by remember { mutableStateOf(false) }
    val suggestions = remember(form.cashbackUrl, form.productName, uiState.cashbacks) {
        uiState.cashbacks
            .filter { cashback ->
                val urlQuery = form.cashbackUrl.trim()
                val productQuery = form.productName.trim()
                (urlQuery.length >= 4 && cashback.cashbackUrl.contains(urlQuery, ignoreCase = true)) ||
                    (productQuery.length >= 2 && cashback.productName.contains(productQuery, ignoreCase = true))
            }
            .distinctBy { it.cashbackUrl to it.productName }
            .take(6)
    }
    val usedBankAccountIds = remember(form.cashbackUrl, form.productName, uiState.cashbacks) {
        uiState.cashbacks
            .filter { it.matchesCashbackAction(form) }
            .mapNotNull { it.bankAccountId }
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
                suggestions = suggestions,
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
                label = "Cashback / Produkt",
                suggestions = suggestions,
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
                label = "IBAN",
                selectedId = form.bankAccountId,
                options = uiState.bankAccounts.map {
                    SelectionOption(
                        id = it.id,
                        label = it.nickname,
                        isWarning = it.id in usedBankAccountIds,
                    )
                },
                onSelect = { id -> onFormChange { it.copy(bankAccountId = id) } },
            )
            SelectionField(
                label = "Geraet",
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
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text("Speichern")
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
    var expanded by remember(value, suggestions) { mutableStateOf(suggestions.isNotEmpty()) }
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            modifier = Modifier.fillMaxWidth(),
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
        Text("Einloesezeitraum", style = MaterialTheme.typography.labelLarge)
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
            Text("Einloesezeitraum per Kalender waehlen")
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
                Text("Uebernehmen")
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
            title = { Text("Einloesezeitraum") },
            headline = null,
        )
    }
}

@Composable
private fun CashbackEntryListItem(
    cashback: CashbackEntry,
    bankAccounts: List<BankAccount>,
    devices: List<CashbackDevice>,
    onTogglePaid: (Long, Boolean) -> Unit,
) {
    val isPaid = cashback.status == CashbackStatus.PAID
    val bankAccountName = bankAccounts.firstOrNull { it.id == cashback.bankAccountId }?.nickname ?: "-"
    val deviceName = devices.firstOrNull { it.id == cashback.deviceId }?.name ?: "-"
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        colors = ListItemDefaults.colors(
            containerColor = if (isPaid) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
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
                Text(
                    text = "IBAN: $bankAccountName | Geraet: $deviceName",
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
        leadingContent = if (isPaid) {
            {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
            }
        } else {
            null
        },
        trailingContent = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = cashback.status.label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(onClick = { onTogglePaid(cashback.id, isPaid) }) {
                    Text(if (isPaid) "Zurueck" else "Ueberwiesen")
                }
            }
        },
    )
}

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
            text = "Bankdaten und Geraete",
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
            Text("Geraet", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = form.name,
                onValueChange = { value -> onChange { it.copy(name = value) } },
                label = { Text("Geraetename") },
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
                Text("Geraet speichern")
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
            Text("Geraete: ${uiState.devices.joinToString { it.name }.ifBlank { "-" }}")
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
                text = selectedOption?.label ?: "Nicht ausgewaehlt",
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
                text = { Text("Nicht ausgewaehlt") },
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
                                    text = "Bereits fuer diese Aktion verwendet",
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
            TransientCelebrationCard(
                eventKey = "milestone-$milestoneMinor",
                title = "Meilenstein erreicht",
                body = "Du hast ${MoneyFormatter.formatPlainMinor(milestoneMinor)} an Erstattungen erreicht.",
                onFinished = onMilestoneFinished,
            )
        }
        if (celebrationEvent != null) {
            TransientCelebrationCard(
                eventKey = "celebration-${celebrationEvent.id}",
                title = when (celebrationEvent.kind) {
                    CelebrationKind.CREATED -> "Cashback angelegt"
                    CelebrationKind.PAID -> "Cashback ueberwiesen"
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
private fun TransientCelebrationCard(
    eventKey: String,
    title: String,
    body: String,
    onFinished: () -> Unit,
) {
    LaunchedEffect(eventKey) {
        delay(1_800)
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
