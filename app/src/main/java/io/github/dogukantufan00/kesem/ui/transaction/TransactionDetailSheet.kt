package io.github.dogukantufan00.kesem.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.model.TransactionWithDetails
import io.github.dogukantufan00.kesem.ui.components.AmountText
import io.github.dogukantufan00.kesem.ui.components.KesemCard
import io.github.dogukantufan00.kesem.ui.components.ReceiptThumbnail
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.ui.theme.adaptiveAccent
import io.github.dogukantufan00.kesem.ui.theme.adaptiveBadgeBackground
import io.github.dogukantufan00.kesem.util.CategoryIconRegistry
import io.github.dogukantufan00.kesem.util.MoneyFormatter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailSheet(
    item: TransactionWithDetails,
    hideBalances: Boolean,
    receiptFile: File?,
    onViewReceipt: (File) -> Unit,
    onEdit: (TransactionWithDetails) -> Unit,
    onDelete: (Long) -> Unit,
    onDeleteInstallmentGroup: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val kesemColors = LocalKesemColors.current
    val tx = item.transaction

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val typeLabel: String
    val amountColor: Color
    val badgeColor: Color
    val badgeBg: Color
    val signedKurus: Long
    val showPlus: Boolean

    when (tx.type) {
        TransactionType.EXPENSE -> {
            val rawColor = item.categoryColorHex?.let { Color(it) } ?: kesemColors.expense
            typeLabel = "Gider"
            amountColor = kesemColors.expense
            badgeColor = rawColor.adaptiveAccent(kesemColors.isDark)
            badgeBg = rawColor.adaptiveBadgeBackground(kesemColors.isDark)
            signedKurus = -tx.amountKurus
            showPlus = false
        }
        TransactionType.INCOME -> {
            val rawColor = item.categoryColorHex?.let { Color(it) } ?: kesemColors.income
            typeLabel = "Gelir"
            amountColor = kesemColors.income
            badgeColor = rawColor.adaptiveAccent(kesemColors.isDark)
            badgeBg = rawColor.adaptiveBadgeBackground(kesemColors.isDark)
            signedKurus = tx.amountKurus
            showPlus = true
        }
        TransactionType.TRANSFER -> {
            typeLabel = "Transfer"
            amountColor = kesemColors.transfer
            badgeColor = kesemColors.transfer
            badgeBg = kesemColors.transferContainer
            signedKurus = tx.amountKurus
            showPlus = false
        }
        TransactionType.ADJUSTMENT_IN -> {
            typeLabel = "Bakiye Düzeltme"
            amountColor = kesemColors.income
            badgeColor = kesemColors.income
            badgeBg = kesemColors.incomeContainer
            signedKurus = tx.amountKurus
            showPlus = true
        }
        TransactionType.ADJUSTMENT_OUT -> {
            typeLabel = "Bakiye Düzeltme"
            amountColor = kesemColors.expense
            badgeColor = kesemColors.expense
            badgeBg = kesemColors.expenseContainer
            signedKurus = -tx.amountKurus
            showPlus = false
        }
    }

    val isAdjustment = tx.type == TransactionType.ADJUSTMENT_IN || tx.type == TransactionType.ADJUSTMENT_OUT
    val titleText = when (tx.type) {
        TransactionType.TRANSFER -> "Hesaplar Arası Transfer"
        TransactionType.ADJUSTMENT_IN,
        TransactionType.ADJUSTMENT_OUT -> "Bakiye Düzeltme"
        else -> item.categoryName ?: "Kategorisiz"
    }
    val tags = MoneyFormatter.parseTags(tx.tagsCsv)
    val hasInstallments = !tx.installmentGroupId.isNullOrBlank() && (tx.installmentCount ?: 1) > 1

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (tx.type) {
                            TransactionType.TRANSFER -> Icons.Rounded.SwapHoriz
                            TransactionType.ADJUSTMENT_IN,
                            TransactionType.ADJUSTMENT_OUT -> Icons.Rounded.Tune
                            else -> CategoryIconRegistry.iconFor(item.categoryIconKey)
                        },
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = kesemColors.subtleText
                    )
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AmountText(
                amountKurus = signedKurus,
                hidden = hideBalances,
                showPlusSign = showPlus,
                color = amountColor,
                style = MaterialTheme.typography.displaySmall
            )

            KesemCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp)
            ) {
                DetailRow(
                    label = "Tarih",
                    value = MoneyFormatter.formatDateHeader(tx.dateMillis)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = kesemColors.cardBorder
                )
                if (tx.type == TransactionType.TRANSFER) {
                    DetailRow(label = "Kaynak", value = item.accountName)
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = kesemColors.cardBorder
                    )
                    DetailRow(label = "Hedef", value = item.toAccountName ?: "Hesap")
                } else {
                    DetailRow(label = "Hesap", value = item.accountName)
                }
                if (hasInstallments) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = kesemColors.cardBorder
                    )
                    DetailRow(
                        label = "Taksit",
                        value = "${tx.installmentIndex} / ${tx.installmentCount}"
                    )
                }
                if (tx.note.isNotBlank()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = kesemColors.cardBorder
                    )
                    DetailRow(label = "Not", value = tx.note)
                }
            }

            if (tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text("#$tag") }
                        )
                    }
                }
            }

            if (receiptFile != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReceiptThumbnail(
                        file = receiptFile,
                        onClick = { onViewReceipt(receiptFile) },
                        modifier = Modifier.size(64.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fiş Fotoğrafı",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Büyütmek için dokun",
                            style = MaterialTheme.typography.bodySmall,
                            color = kesemColors.subtleText
                        )
                    }
                    TextButton(onClick = { onViewReceipt(receiptFile) }) {
                        Text("Aç")
                    }
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
                if (!isAdjustment) {
                    Button(
                        onClick = { onEdit(item) },
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
    }

    if (showDeleteConfirm) {
        if (hasInstallments && tx.installmentGroupId != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Taksitli İşlemi Sil") },
                text = {
                    Text("Sadece bu taksiti mi (${tx.installmentIndex}/${tx.installmentCount}), yoksa tüm ${tx.installmentCount} taksitlik seriyi mi silmek istersin?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = false
                            onDeleteInstallmentGroup(tx.installmentGroupId)
                        }
                    ) {
                        Text(
                            "Tüm Taksitler (${tx.installmentCount})",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text("Vazgeç")
                        }
                        TextButton(
                            onClick = {
                                showDeleteConfirm = false
                                onDelete(tx.id)
                            }
                        ) {
                            Text("Sadece Bu Taksit")
                        }
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("İşlemi Sil") },
                text = { Text("Bu işlem kaydı kalıcı olarak silinecek ve ilgili hesap bakiyesi güncellenecek.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = false
                            onDelete(tx.id)
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
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalKesemColors.current.subtleText
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
