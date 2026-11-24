package io.github.dogukantufan00.kesem.ui.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.data.local.entity.DebtEntity
import io.github.dogukantufan00.kesem.data.local.entity.DebtPaymentEntity
import io.github.dogukantufan00.kesem.model.AccountWithBalance
import io.github.dogukantufan00.kesem.model.DebtType
import io.github.dogukantufan00.kesem.model.DebtWithSummary
import io.github.dogukantufan00.kesem.ui.components.AmountText
import io.github.dogukantufan00.kesem.ui.components.KesemCard
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailSheet(
    item: DebtWithSummary,
    payments: List<DebtPaymentEntity>,
    activeAccounts: List<AccountWithBalance>,
    hideBalances: Boolean,
    openPaymentDialogInitially: Boolean = false,
    onRecordPayment: (amountKurus: Long, dateMillis: Long, accountId: Long?, note: String) -> Unit,
    onDeletePayment: (Long) -> Unit,
    onEdit: (DebtEntity) -> Unit,
    onToggleSettled: (Long, Boolean) -> Unit,
    onDeleteDebt: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val kesemColors = LocalKesemColors.current
    val debt = item.debt
    val isLent = debt.type == DebtType.LENT
    val accent = if (isLent) kesemColors.income else kesemColors.expense
    val progress = if (debt.totalAmountKurus > 0L) {
        (item.paidAmountKurus.toDouble() / debt.totalAmountKurus.toDouble()).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    var showPaymentDialog by remember(debt.id, openPaymentDialogInitially) {
        mutableStateOf(openPaymentDialogInitially && !debt.isSettled)
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isLent) "Alacak" else "Borç",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )
                    Text(
                        text = debt.personName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = { onEdit(debt) }) {
                    Text("Düzenle")
                }
            }

            KesemCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp)
            ) {
                Text(
                    text = if (debt.isSettled) "Tamamlandı" else "Kalan Tutar",
                    style = MaterialTheme.typography.labelMedium,
                    color = kesemColors.subtleText
                )
                Spacer(modifier = Modifier.height(4.dp))
                AmountText(
                    amountKurus = item.remainingAmountKurus,
                    hidden = hideBalances,
                    color = accent,
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (progress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceAtLeast(0.02f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(accent)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ödenen: ${MoneyFormatter.formatKurus(item.paidAmountKurus, hidden = hideBalances)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = kesemColors.subtleText
                    )
                    Text(
                        text = "Toplam: ${MoneyFormatter.formatKurus(debt.totalAmountKurus, hidden = hideBalances)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = kesemColors.subtleText
                    )
                }
                if (debt.dueDateMillis != null || debt.note.isNotBlank()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = kesemColors.cardBorder
                    )
                    if (debt.dueDateMillis != null) {
                        Text(
                            text = "Vade: ${MoneyFormatter.formatDateHeader(debt.dueDateMillis)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = kesemColors.subtleText
                        )
                    }
                    if (debt.note.isNotBlank()) {
                        Text(
                            text = debt.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (!debt.isSettled) {
                Button(
                    onClick = { showPaymentDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isLent) "Tahsilat Ekle" else "Kısmi / Tam Ödeme Ekle")
                }
            }

            // Payment History
            Text(
                text = "Ödeme Geçmişi",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (payments.isEmpty()) {
                Text(
                    text = "Henüz ödeme kaydı yok.",
                    style = MaterialTheme.typography.bodySmall,
                    color = kesemColors.subtleText
                )
            } else {
                KesemCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    payments.forEachIndexed { index, payment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                AmountText(
                                    amountKurus = payment.amountKurus,
                                    hidden = hideBalances,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val sub = buildList {
                                    add(MoneyFormatter.formatShortDate(payment.dateMillis))
                                    if (payment.note.isNotBlank()) add(payment.note)
                                }.joinToString(" · ")
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = kesemColors.subtleText
                                )
                            }
                            IconButton(onClick = { onDeletePayment(payment.id) }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Ödemeyi sil",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (index < payments.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                color = kesemColors.cardBorder
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onToggleSettled(debt.id, !debt.isSettled) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (debt.isSettled) "Yeniden Aç" else "Kapandı İşaretle")
                }
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Kaydı Sil")
                }
            }
        }
    }

    if (showPaymentDialog) {
        RecordPaymentDialog(
            isLent = isLent,
            remainingKurus = item.remainingAmountKurus,
            activeAccounts = activeAccounts,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amountKurus, accountId, note ->
                onRecordPayment(amountKurus, System.currentTimeMillis(), accountId, note)
                showPaymentDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Kaydı Sil") },
            text = { Text("\"${debt.personName}\" kaydı ve ödeme geçmişi silinecek.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteDebt(debt.id)
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
private fun RecordPaymentDialog(
    isLent: Boolean,
    remainingKurus: Long,
    activeAccounts: List<AccountWithBalance>,
    onDismiss: () -> Unit,
    onConfirm: (amountKurus: Long, accountId: Long?, note: String) -> Unit
) {
    var amountText by remember {
        mutableStateOf(
            if (remainingKurus > 0L) MoneyFormatter.formatInputFromKurus(remainingKurus) else ""
        )
    }
    var note by remember { mutableStateOf("") }
    var reflectInAccount by remember(activeAccounts) { mutableStateOf(activeAccounts.isNotEmpty()) }
    var selectedAccountId by remember(activeAccounts) {
        mutableLongStateOf(activeAccounts.firstOrNull()?.account?.id ?: 0L)
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isLent) "Tahsilat Ekle" else "Ödeme Ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        error = null
                    },
                    label = { Text("Tutar (₺)") },
                    suffix = { Text("₺") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (remainingKurus > 0L) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            amountText = MoneyFormatter.formatInputFromKurus(remainingKurus)
                            error = null
                        },
                        label = { Text("Kalanı doldur (${MoneyFormatter.formatKurus(remainingKurus)})") }
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Not (İsteğe bağlı)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (activeAccounts.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Hesaba yansıt",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = reflectInAccount,
                            onCheckedChange = { reflectInAccount = it }
                        )
                    }
                    if (reflectInAccount) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(activeAccounts, key = { it.account.id }) { item ->
                                val acc = item.account
                                FilterChip(
                                    selected = selectedAccountId == acc.id,
                                    onClick = { selectedAccountId = acc.id },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(acc.colorHex))
                                        )
                                    },
                                    label = { Text(acc.name) }
                                )
                            }
                        }
                    }
                }
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
                    val parsed = MoneyFormatter.parseAmountToKurus(amountText, allowZero = false)
                    if (parsed == null) {
                        error = "Geçerli bir tutar girin."
                    } else {
                        val accId = if (reflectInAccount && selectedAccountId != 0L) {
                            selectedAccountId
                        } else {
                            null
                        }
                        onConfirm(parsed, accId, note)
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
