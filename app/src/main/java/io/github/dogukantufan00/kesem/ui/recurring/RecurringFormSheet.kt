package io.github.dogukantufan00.kesem.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.data.local.SeedData
import io.github.dogukantufan00.kesem.data.local.entity.RecurringRuleEntity
import io.github.dogukantufan00.kesem.model.RecurrenceFrequency
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.ui.theme.adaptiveAccent
import io.github.dogukantufan00.kesem.util.CategoryIconRegistry
import io.github.dogukantufan00.kesem.util.MoneyFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurringFormSheet(
    editingRule: RecurringRuleEntity?,
    uiState: RecurringUiState,
    onDismiss: () -> Unit,
    onSave: (
        editingRule: RecurringRuleEntity?,
        title: String,
        type: TransactionType,
        amountKurus: Long,
        accountId: Long,
        categoryId: Long?,
        frequency: RecurrenceFrequency,
        dayOfMonth: Int,
        nextDueDateEpochDay: Long,
        note: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val kesemColors = LocalKesemColors.current
    val isEditing = editingRule != null

    var txType by remember(editingRule) {
        mutableStateOf(editingRule?.type ?: TransactionType.EXPENSE)
    }
    var title by remember(editingRule) {
        mutableStateOf(editingRule?.title ?: "")
    }
    var amountText by remember(editingRule) {
        mutableStateOf(
            if (editingRule != null) MoneyFormatter.formatInputFromKurus(editingRule.amountKurus) else ""
        )
    }
    var selectedAccountId by remember(editingRule, uiState.activeAccounts) {
        mutableLongStateOf(
            editingRule?.accountId ?: uiState.activeAccounts.firstOrNull()?.account?.id ?: 0L
        )
    }

    val categories = if (txType == TransactionType.INCOME) {
        uiState.incomeCategories
    } else {
        uiState.expenseCategories
    }
    var selectedCategoryId by remember(editingRule) {
        mutableStateOf(editingRule?.categoryId)
    }

    LaunchedEffect(txType, categories) {
        if (selectedCategoryId == null || categories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = categories.firstOrNull()?.id
        }
    }

    var frequency by remember(editingRule) {
        mutableStateOf(editingRule?.frequency ?: RecurrenceFrequency.MONTHLY)
    }
    var nextDueDate by remember(editingRule) {
        mutableStateOf(
            if (editingRule != null) {
                LocalDate.ofEpochDay(editingRule.nextDueDateEpochDay)
            } else {
                LocalDate.now()
            }
        )
    }
    var note by remember(editingRule) {
        mutableStateOf(editingRule?.note ?: "")
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isEditing) "Düzenli İşlemi Düzenle" else "Yeni Düzenli İşlem",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(TransactionType.INCOME to "Gelir", TransactionType.EXPENSE to "Gider")
                options.forEachIndexed { index, (type, label) ->
                    SegmentedButton(
                        selected = txType == type,
                        onClick = { txType = type },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        )
                    ) {
                        Text(label)
                    }
                }
            }

            if (!isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Hızlı Seçim",
                        style = MaterialTheme.typography.labelMedium,
                        color = kesemColors.subtleText
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SeedData.POPULAR_SUBSCRIPTIONS) { preset ->
                            FilterChip(
                                selected = title == preset,
                                onClick = {
                                    title = preset
                                    if (preset == "Maaş") {
                                        txType = TransactionType.INCOME
                                    } else {
                                        txType = TransactionType.EXPENSE
                                        val matchName = when (preset) {
                                            "Netflix", "Spotify", "YouTube", "Amazon Prime", "iCloud" -> "Abonelikler"
                                            "Kira" -> "Kira & Ev"
                                            "İnternet", "Telefon", "Elektrik", "Doğalgaz", "Su" -> "Faturalar"
                                            else -> null
                                        }
                                        if (matchName != null) {
                                            uiState.expenseCategories
                                                .firstOrNull { it.name == matchName }
                                                ?.let { selectedCategoryId = it.id }
                                        }
                                    }
                                    errorMessage = null
                                },
                                label = { Text(preset) }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    errorMessage = null
                },
                label = { Text("Başlık") },
                placeholder = { Text("Örn. Netflix, Kira, Maaş") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("Tutar (₺)") },
                placeholder = { Text("0,00") },
                suffix = { Text("₺") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Account
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Hesap",
                    style = MaterialTheme.typography.labelMedium,
                    color = kesemColors.subtleText
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.activeAccounts, key = { it.account.id }) { item ->
                        val acc = item.account
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(acc.colorHex))
                                )
                            },
                            label = { Text(acc.name) }
                        )
                    }
                }
            }

            // Category
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Kategori",
                    style = MaterialTheme.typography.labelMedium,
                    color = kesemColors.subtleText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryIconRegistry.iconFor(cat.iconKey),
                                    contentDescription = null,
                                    tint = Color(cat.colorHex).adaptiveAccent(kesemColors.isDark),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }

            // Frequency
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Tekrar Sıklığı",
                    style = MaterialTheme.typography.labelMedium,
                    color = kesemColors.subtleText
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val freqs = listOf(
                        RecurrenceFrequency.WEEKLY to "Haftalık",
                        RecurrenceFrequency.MONTHLY to "Aylık",
                        RecurrenceFrequency.YEARLY to "Yıllık"
                    )
                    freqs.forEachIndexed { index, (freq, label) ->
                        SegmentedButton(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = freqs.size
                            )
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            // Next Due Date
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val formatted = nextDueDate.format(
                    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr-TR"))
                )
                val today = LocalDate.now()
                Text(
                    text = "Sonraki İşlem Tarihi",
                    style = MaterialTheme.typography.labelMedium,
                    color = kesemColors.subtleText
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = nextDueDate == today,
                        onClick = { nextDueDate = today },
                        label = { Text("Bugün") }
                    )
                    FilterChip(
                        selected = nextDueDate != today,
                        onClick = { showDatePicker = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        label = { Text(if (nextDueDate == today) "Tarih Seç" else formatted) }
                    )
                }
                if (frequency != RecurrenceFrequency.WEEKLY) {
                    val clampedDay = nextDueDate.dayOfMonth.coerceIn(1, 28)
                    Text(
                        text = "Tekrarlama günü: her dönemin $clampedDay. günü",
                        style = MaterialTheme.typography.bodySmall,
                        color = kesemColors.subtleText
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Not (İsteğe bağlı)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val trimmedTitle = title.trim()
                    if (trimmedTitle.isEmpty()) {
                        errorMessage = "Lütfen bir başlık girin."
                        return@Button
                    }
                    val parsedKurus = MoneyFormatter.parseAmountToKurus(amountText, allowZero = false)
                    if (parsedKurus == null) {
                        errorMessage = "Sıfırdan büyük geçerli bir tutar girin."
                        return@Button
                    }
                    if (selectedAccountId == 0L) {
                        errorMessage = "Lütfen bir hesap seçin."
                        return@Button
                    }

                    onSave(
                        editingRule,
                        trimmedTitle,
                        txType,
                        parsedKurus,
                        selectedAccountId,
                        selectedCategoryId,
                        frequency,
                        nextDueDate.dayOfMonth.coerceIn(1, 28),
                        nextDueDate.toEpochDay(),
                        note
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (isEditing) "Güncelle" else "Kaydet")
            }
        }
    }

    if (showDatePicker) {
        val initialUtcMillis = nextDueDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val utc = pickerState.selectedDateMillis
                        if (utc != null) {
                            nextDueDate = Instant.ofEpochMilli(utc)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Seç")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("İptal")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
