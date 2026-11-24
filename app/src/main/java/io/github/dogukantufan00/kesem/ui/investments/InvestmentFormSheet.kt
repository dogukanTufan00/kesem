package io.github.dogukantufan00.kesem.ui.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.data.local.SeedData
import io.github.dogukantufan00.kesem.data.local.entity.InvestmentEntity
import io.github.dogukantufan00.kesem.model.InvestmentAssetType
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.util.InvestmentCalculator
import io.github.dogukantufan00.kesem.util.MoneyFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentFormSheet(
    editingInvestment: InvestmentEntity?,
    onDismiss: () -> Unit,
    onSave: (InvestmentEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val kesemColors = LocalKesemColors.current
    val isEditing = editingInvestment != null

    var assetType by remember(editingInvestment) {
        mutableStateOf(editingInvestment?.assetType ?: InvestmentAssetType.CURRENCY)
    }
    var symbol by remember(editingInvestment) {
        mutableStateOf(editingInvestment?.symbol ?: "")
    }
    var name by remember(editingInvestment) {
        mutableStateOf(editingInvestment?.name ?: "")
    }
    var quantityText by remember(editingInvestment) {
        mutableStateOf(
            editingInvestment?.let { InvestmentCalculator.formatDecimalInput(it.quantity) } ?: ""
        )
    }
    var avgCostText by remember(editingInvestment) {
        mutableStateOf(
            editingInvestment?.let { InvestmentCalculator.formatDecimalInput(it.averageCostTry) } ?: ""
        )
    }
    var currentPriceText by remember(editingInvestment) {
        mutableStateOf(
            editingInvestment?.let { InvestmentCalculator.formatDecimalInput(it.lastPriceTry) } ?: ""
        )
    }
    var includeInTotal by remember(editingInvestment) {
        mutableStateOf(editingInvestment?.includeInTotal ?: true)
    }
    var note by remember(editingInvestment) {
        mutableStateOf(editingInvestment?.note ?: "")
    }

    // Term Deposit fields
    var rateText by remember(editingInvestment) {
        mutableStateOf(
            editingInvestment?.annualInterestRate?.let { InvestmentCalculator.formatDecimalInput(it) } ?: ""
        )
    }
    var taxText by remember(editingInvestment) {
        mutableStateOf(
            editingInvestment?.withholdingTaxRate?.let { InvestmentCalculator.formatDecimalInput(it) } ?: "15"
        )
    }
    var startDate by remember(editingInvestment) {
        mutableStateOf(
            editingInvestment?.startEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()
        )
    }
    var maturityDate by remember(editingInvestment) {
        mutableStateOf(
            editingInvestment?.maturityEpochDay?.let { LocalDate.ofEpochDay(it) }
                ?: LocalDate.now().plusDays(32)
        )
    }
    var pickingStartDate by remember { mutableStateOf(false) }
    var pickingMaturityDate by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presets = remember(assetType) { SeedData.presetsFor(assetType) }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("tr-TR"))
    }

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
                text = if (isEditing) "Varlığı Düzenle" else "Yeni Yatırım Varlığı",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Asset Type Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(InvestmentAssetType.entries) { type ->
                    FilterChip(
                        selected = assetType == type,
                        onClick = {
                            if (!isEditing) {
                                assetType = type
                                symbol = if (type == InvestmentAssetType.TERM_DEPOSIT) "MEVDUAT" else ""
                                name = ""
                                errorMessage = null
                            }
                        },
                        enabled = !isEditing || assetType == type,
                        label = { Text(type.turkishLabel()) }
                    )
                }
            }

            if (assetType != InvestmentAssetType.TERM_DEPOSIT) {
                if (presets.isNotEmpty() && !isEditing) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Popüler Seçenekler",
                            style = MaterialTheme.typography.labelMedium,
                            color = kesemColors.subtleText
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(presets, key = { it.symbol }) { preset ->
                                FilterChip(
                                    selected = symbol.equals(preset.symbol, ignoreCase = true),
                                    onClick = {
                                        symbol = preset.symbol
                                        name = preset.name
                                        errorMessage = null
                                    },
                                    label = { Text("${preset.symbol} · ${preset.name}") }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = {
                            symbol = it.uppercase()
                            errorMessage = null
                        },
                        label = { Text("Sembol") },
                        placeholder = { Text("USD, THYAO...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.weight(0.42f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                        },
                        label = { Text("Varlık Adı") },
                        placeholder = { Text("Örn. Amerikan Doları") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.weight(0.58f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it
                        errorMessage = null
                    },
                    label = { Text("Adet / Miktar") },
                    placeholder = { Text("Örn. 100 veya 0,25") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = avgCostText,
                    onValueChange = {
                        avgCostText = it
                        errorMessage = null
                    },
                    label = { Text("Birim Alış Maliyeti (₺)") },
                    placeholder = { Text("Örn. 34,50") },
                    suffix = { Text("₺") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = currentPriceText,
                    onValueChange = {
                        currentPriceText = it
                        errorMessage = null
                    },
                    label = { Text("Güncel Birim Fiyat (₺ - İsteğe bağlı)") },
                    placeholder = { Text("Boşsa otomatik piyasa fiyatı çekilir") },
                    suffix = { Text("₺") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            } else {
                // Term Deposit Form
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Mevduat Adı") },
                    placeholder = { Text("Örn. Garanti 32 Günlük") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = avgCostText,
                    onValueChange = {
                        avgCostText = it
                        errorMessage = null
                    },
                    label = { Text("Anapara (₺)") },
                    placeholder = { Text("Örn. 50000") },
                    suffix = { Text("₺") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = {
                            rateText = it
                            errorMessage = null
                        },
                        label = { Text("Yıllık Faiz (%)") },
                        placeholder = { Text("45") },
                        suffix = { Text("%") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = taxText,
                        onValueChange = {
                            taxText = it
                            errorMessage = null
                        },
                        label = { Text("Stopaj (%)") },
                        placeholder = { Text("15") },
                        suffix = { Text("%") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Quick term chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Hızlı Vade Süresi",
                        style = MaterialTheme.typography.labelMedium,
                        color = kesemColors.subtleText
                    )
                    val currentDays = ChronoUnit.DAYS.between(startDate, maturityDate)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val termDaysList = listOf(32L, 45L, 92L, 181L, 365L)
                        items(termDaysList) { days ->
                            FilterChip(
                                selected = currentDays == days,
                                onClick = { maturityDate = startDate.plusDays(days) },
                                label = { Text("$days Gün") }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = true,
                        onClick = { pickingStartDate = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        label = { Text("Başlangıç: ${startDate.format(dateFormatter)}") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = true,
                        onClick = { pickingMaturityDate = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        label = { Text("Vade: ${maturityDate.format(dateFormatter)}") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Live Deposit Preview
                val previewPrincipal = InvestmentCalculator.parseFlexibleDecimal(avgCostText)
                val previewRate = InvestmentCalculator.parseFlexibleDecimal(rateText)
                val previewTax = InvestmentCalculator.parseFlexibleDecimal(taxText) ?: 15.0
                if (previewPrincipal != null && previewPrincipal > 0.0 && previewRate != null) {
                    val previewEntity = InvestmentEntity(
                        assetType = InvestmentAssetType.TERM_DEPOSIT,
                        symbol = "MEVDUAT",
                        name = name.ifBlank { "Mevduat" },
                        quantity = 1.0,
                        averageCostTry = previewPrincipal,
                        lastPriceTry = previewPrincipal,
                        annualInterestRate = previewRate,
                        withholdingTaxRate = previewTax,
                        startEpochDay = startDate.toEpochDay(),
                        maturityEpochDay = maturityDate.toEpochDay()
                    )
                    val previewVal = InvestmentCalculator.evaluate(previewEntity, startDate)
                    val projectedNet = previewVal.maturityProjectedNetInterestKurus ?: 0L
                    val totalDays = ChronoUnit.DAYS.between(startDate, maturityDate).coerceAtLeast(1L)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Vade Sonu Tahmini ($totalDays gün)",
                            style = MaterialTheme.typography.labelSmall,
                            color = kesemColors.subtleText
                        )
                        Text(
                            text = "Tahmini Net Getiri: +${MoneyFormatter.formatKurus(projectedNet)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = kesemColors.income
                        )
                    }
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Net varlığa dahil et",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Özet ekranındaki Yatırım toplamına eklenir",
                        style = MaterialTheme.typography.bodySmall,
                        color = kesemColors.subtleText
                    )
                }
                Switch(
                    checked = includeInTotal,
                    onCheckedChange = { includeInTotal = it }
                )
            }

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
                    val trimmedName = name.trim()
                    if (trimmedName.isEmpty()) {
                        errorMessage = "Lütfen bir varlık adı girin."
                        return@Button
                    }

                    if (assetType == InvestmentAssetType.TERM_DEPOSIT) {
                        val principal = InvestmentCalculator.parseFlexibleDecimal(avgCostText)
                        val rate = InvestmentCalculator.parseFlexibleDecimal(rateText)
                        val tax = if (taxText.isBlank()) {
                            15.0
                        } else {
                            InvestmentCalculator.parseFlexibleDecimal(taxText)
                        }
                        if (principal == null || principal <= 0.0) {
                            errorMessage = "Geçerli bir anapara tutarı girin."
                            return@Button
                        }
                        if (rate == null || rate < 0.0) {
                            errorMessage = "Geçerli bir faiz oranı girin."
                            return@Button
                        }
                        if (tax == null || tax !in 0.0..100.0) {
                            errorMessage = "Stopaj oranı 0 ile 100 arasında olmalı."
                            return@Button
                        }
                        if (!maturityDate.isAfter(startDate)) {
                            errorMessage = "Vade tarihi başlangıç tarihinden sonra olmalı."
                            return@Button
                        }

                        onSave(
                            (editingInvestment ?: InvestmentEntity(
                                assetType = InvestmentAssetType.TERM_DEPOSIT,
                                symbol = "MEVDUAT",
                                name = trimmedName,
                                quantity = 1.0,
                                averageCostTry = principal,
                                lastPriceTry = principal
                            )).copy(
                                assetType = InvestmentAssetType.TERM_DEPOSIT,
                                symbol = "MEVDUAT",
                                name = trimmedName,
                                quantity = 1.0,
                                averageCostTry = principal,
                                lastPriceTry = principal,
                                priceSource = "Mevduat",
                                isManualPrice = true,
                                includeInTotal = includeInTotal,
                                annualInterestRate = rate,
                                withholdingTaxRate = tax,
                                startEpochDay = startDate.toEpochDay(),
                                maturityEpochDay = maturityDate.toEpochDay(),
                                note = note.trim()
                            )
                        )
                    } else {
                        val cleanSymbol = symbol.trim().uppercase()
                        if (cleanSymbol.isEmpty()) {
                            errorMessage = "Lütfen bir sembol girin (örn. USD, THYAO)."
                            return@Button
                        }
                        val qty = InvestmentCalculator.parseFlexibleDecimal(quantityText)
                        if (qty == null || qty < 0.0) {
                            errorMessage = "Geçerli bir adet/miktar girin."
                            return@Button
                        }
                        val avgCost = if (avgCostText.isBlank()) {
                            0.0
                        } else {
                            InvestmentCalculator.parseFlexibleDecimal(avgCostText)
                        }
                        if (avgCost == null || avgCost < 0.0) {
                            errorMessage = "Geçerli bir birim maliyet girin."
                            return@Button
                        }
                        val enteredPrice = if (currentPriceText.isBlank()) {
                            null
                        } else {
                            InvestmentCalculator.parseFlexibleDecimal(currentPriceText)
                        }
                        if (currentPriceText.isNotBlank() && (enteredPrice == null || enteredPrice < 0.0)) {
                            errorMessage = "Geçerli bir güncel birim fiyat girin."
                            return@Button
                        }

                        val resolvedPrice = enteredPrice ?: avgCost
                        val priceWasModified = editingInvestment != null &&
                            enteredPrice != null &&
                            kotlin.math.abs(enteredPrice - editingInvestment.lastPriceTry) > 0.00001

                        onSave(
                            (editingInvestment ?: InvestmentEntity(
                                assetType = assetType,
                                symbol = cleanSymbol,
                                name = trimmedName,
                                quantity = qty,
                                averageCostTry = avgCost,
                                lastPriceTry = resolvedPrice
                            )).copy(
                                assetType = assetType,
                                symbol = cleanSymbol,
                                name = trimmedName,
                                quantity = qty,
                                averageCostTry = avgCost,
                                lastPriceTry = resolvedPrice,
                                priceSource = when {
                                    priceWasModified -> "Manuel"
                                    editingInvestment != null -> editingInvestment.priceSource
                                    enteredPrice != null -> "Manuel"
                                    else -> "Bekleniyor"
                                },
                                lastUpdatedMillis = when {
                                    priceWasModified -> System.currentTimeMillis()
                                    editingInvestment != null -> editingInvestment.lastUpdatedMillis
                                    else -> null
                                },
                                isManualPrice = if (priceWasModified) {
                                    true
                                } else {
                                    editingInvestment?.isManualPrice ?: false
                                },
                                includeInTotal = includeInTotal,
                                note = note.trim()
                            )
                        )
                    }
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

    if (pickingStartDate || pickingMaturityDate) {
        val current = if (pickingStartDate) startDate else maturityDate
        val initialUtcMillis = current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)

        DatePickerDialog(
            onDismissRequest = {
                pickingStartDate = false
                pickingMaturityDate = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val utc = pickerState.selectedDateMillis
                        if (utc != null) {
                            val picked = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                            if (pickingStartDate) {
                                startDate = picked
                                if (!maturityDate.isAfter(picked)) {
                                    maturityDate = picked.plusDays(32)
                                }
                            } else {
                                maturityDate = picked
                            }
                        }
                        pickingStartDate = false
                        pickingMaturityDate = false
                    }
                ) {
                    Text("Seç")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pickingStartDate = false
                        pickingMaturityDate = false
                    }
                ) {
                    Text("İptal")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
