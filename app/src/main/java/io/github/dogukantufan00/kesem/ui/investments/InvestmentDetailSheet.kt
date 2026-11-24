package io.github.dogukantufan00.kesem.ui.investments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.data.local.entity.InvestmentEntity
import io.github.dogukantufan00.kesem.model.InvestmentAssetType
import io.github.dogukantufan00.kesem.model.InvestmentWithValuation
import io.github.dogukantufan00.kesem.ui.components.AmountText
import io.github.dogukantufan00.kesem.ui.components.KesemCard
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.util.InvestmentCalculator
import io.github.dogukantufan00.kesem.util.MoneyFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentDetailSheet(
    item: InvestmentWithValuation,
    hideBalances: Boolean,
    onAddBuyLot: (id: Long, buyQty: Double, buyUnitPriceTry: Double) -> Unit,
    onApplySellLot: (id: Long, sellQty: Double) -> Unit,
    onUpdateManualPrice: (id: Long, newPriceTry: Double) -> Unit,
    onEdit: (InvestmentEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val kesemColors = LocalKesemColors.current
    val inv = item.investment
    val valuation = item.valuation
    val isDeposit = inv.assetType == InvestmentAssetType.TERM_DEPOSIT

    var showBuyDialog by remember { mutableStateOf(false) }
    var showSellDialog by remember { mutableStateOf(false) }
    var showPriceDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val plColor = when {
        valuation.profitLossKurus > 0L -> kesemColors.income
        valuation.profitLossKurus < 0L -> kesemColors.expense
        else -> MaterialTheme.colorScheme.onSurface
    }
    val pctText = valuation.profitLossPercent?.let {
        val sign = if (it > 0) "+" else ""
        " ($sign${String.format(Locale.forLanguageTag("tr-TR"), "%.2f", it)}%)"
    } ?: ""

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${inv.assetType.turkishLabel()} · ${inv.symbol}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = inv.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column {
                Text(
                    text = "Güncel Değer",
                    style = MaterialTheme.typography.labelMedium,
                    color = kesemColors.subtleText
                )
                AmountText(
                    amountKurus = valuation.currentValueKurus,
                    hidden = hideBalances,
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (hideBalances) {
                        "K/Z: ••••••"
                    } else {
                        "K/Z: ${MoneyFormatter.formatKurus(valuation.profitLossKurus, showPlusSign = true)}$pctText"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = plColor
                )
            }

            KesemCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp)
            ) {
                if (isDeposit) {
                    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("tr-TR"))
                    val startText = inv.startEpochDay?.let { LocalDate.ofEpochDay(it).format(fmt) } ?: "—"
                    val maturityText = inv.maturityEpochDay?.let { LocalDate.ofEpochDay(it).format(fmt) } ?: "—"
                    DetailKvRow(
                        "Anapara",
                        MoneyFormatter.formatKurus(valuation.costBasisKurus, hidden = hideBalances)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = kesemColors.cardBorder)
                    DetailKvRow(
                        "Yıllık Faiz / Stopaj",
                        String.format(
                            Locale.forLanguageTag("tr-TR"),
                            "%%%.1f / %%%.1f",
                            inv.annualInterestRate ?: 0.0,
                            inv.withholdingTaxRate ?: 15.0
                        )
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = kesemColors.cardBorder)
                    DetailKvRow("Başlangıç / Vade", "$startText – $maturityText")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = kesemColors.cardBorder)
                    DetailKvRow("Kalan Gün", "${valuation.daysRemaining ?: 0} gün")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = kesemColors.cardBorder)
                    DetailKvRow(
                        "Vade Sonu Tahmini Net",
                        "+${
                            MoneyFormatter.formatKurus(
                                valuation.maturityProjectedNetInterestKurus ?: 0L,
                                hidden = hideBalances
                            )
                        }"
                    )
                } else {
                    DetailKvRow(
                        "Adet / Miktar",
                        if (hideBalances) "••" else InvestmentCalculator.formatQuantity(inv.quantity)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = kesemColors.cardBorder)
                    DetailKvRow(
                        "Ortalama Alış",
                        if (hideBalances) "•••" else InvestmentCalculator.formatUnitPriceTry(inv.averageCostTry)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = kesemColors.cardBorder)
                    DetailKvRow(
                        "Toplam Maliyet",
                        MoneyFormatter.formatKurus(valuation.costBasisKurus, hidden = hideBalances)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = kesemColors.cardBorder)
                    DetailKvRow(
                        "Güncel Birim Fiyat",
                        InvestmentCalculator.formatUnitPriceTry(inv.lastPriceTry)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = kesemColors.cardBorder)
                    val updatedAtText = inv.lastUpdatedMillis?.let { millis ->
                        val str = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.forLanguageTag("tr-TR")))
                        "${inv.priceSource} · $str"
                    } ?: inv.priceSource
                    DetailKvRow("Fiyat Kaynağı", updatedAtText)
                }

                if (inv.note.isNotBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = kesemColors.cardBorder)
                    DetailKvRow("Not", inv.note)
                }
            }

            if (!isDeposit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showBuyDialog = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Alım Ekle")
                    }
                    FilledTonalButton(
                        onClick = { showSellDialog = true },
                        enabled = inv.quantity > 0.0,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Satış Yap")
                    }
                }
                OutlinedButton(
                    onClick = { showPriceDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Birim Fiyatı Manuel Güncelle")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sil")
                }
                Button(
                    onClick = { onEdit(inv) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Düzenle")
                }
            }
        }
    }

    if (showBuyDialog) {
        BuyLotDialog(
            defaultPriceTry = inv.lastPriceTry,
            onDismiss = { showBuyDialog = false },
            onConfirm = { qty, price ->
                onAddBuyLot(inv.id, qty, price)
                showBuyDialog = false
            }
        )
    }

    if (showSellDialog) {
        SellLotDialog(
            maxQty = inv.quantity,
            onDismiss = { showSellDialog = false },
            onConfirm = { qty ->
                onApplySellLot(inv.id, qty)
                showSellDialog = false
            }
        )
    }

    if (showPriceDialog) {
        ManualPriceDialog(
            currentPriceTry = inv.lastPriceTry,
            onDismiss = { showPriceDialog = false },
            onConfirm = { price ->
                onUpdateManualPrice(inv.id, price)
                showPriceDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Varlığı Sil") },
            text = { Text("\"${inv.name}\" portföyden kalıcı olarak silinecek.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(inv.id)
                    }
                ) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun DetailKvRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = LocalKesemColors.current.subtleText
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BuyLotDialog(
    defaultPriceTry: Double,
    onDismiss: () -> Unit,
    onConfirm: (qty: Double, unitPriceTry: Double) -> Unit
) {
    var qtyText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf(InvestmentCalculator.formatDecimalInput(defaultPriceTry)) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alım Ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Ortalama alış maliyetin ağırlıklı ortalama ile otomatik güncellenir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalKesemColors.current.subtleText
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = {
                        qtyText = it
                        error = null
                    },
                    label = { Text("Alınan Adet / Miktar") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        error = null
                    },
                    label = { Text("Birim Alış Fiyatı (₺)") },
                    suffix = { Text("₺") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val q = InvestmentCalculator.parseFlexibleDecimal(qtyText)
                    val p = InvestmentCalculator.parseFlexibleDecimal(priceText)
                    if (q == null || q <= 0.0) {
                        error = "Sıfırdan büyük adet girin."
                    } else if (p == null || p < 0.0) {
                        error = "Geçerli bir birim fiyat girin."
                    } else {
                        onConfirm(q, p)
                    }
                }
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
private fun SellLotDialog(
    maxQty: Double,
    onDismiss: () -> Unit,
    onConfirm: (qty: Double) -> Unit
) {
    var qtyText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Satış Yap") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Mevcut miktar: ${InvestmentCalculator.formatQuantity(maxQty)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalKesemColors.current.subtleText
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = {
                        qtyText = it
                        error = null
                    },
                    label = { Text("Satılan Adet / Miktar") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val q = InvestmentCalculator.parseFlexibleDecimal(qtyText)
                    if (q == null || q <= 0.0) {
                        error = "Sıfırdan büyük miktar girin."
                    } else if (q > maxQty) {
                        error = "Mevcut miktardan fazla satış girilemez."
                    } else {
                        onConfirm(q)
                    }
                }
            ) {
                Text("Düş")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
private fun ManualPriceDialog(
    currentPriceTry: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var priceText by remember { mutableStateOf(InvestmentCalculator.formatDecimalInput(currentPriceTry)) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Birim Fiyatı Güncelle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TEFAS fonu veya özel varlıklar için güncel birim TL fiyatını girebilirsin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalKesemColors.current.subtleText
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        error = null
                    },
                    label = { Text("Yeni Birim Fiyat (₺)") },
                    suffix = { Text("₺") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val p = InvestmentCalculator.parseFlexibleDecimal(priceText)
                    if (p == null || p < 0.0) {
                        error = "Geçerli bir fiyat girin."
                    } else {
                        onConfirm(p)
                    }
                }
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
