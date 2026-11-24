package io.github.dogukantufan00.kesem.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.data.local.entity.RecurringRuleEntity
import io.github.dogukantufan00.kesem.model.RecurrenceFrequency
import io.github.dogukantufan00.kesem.model.RecurringRuleWithDetails
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.ui.components.AmountText
import io.github.dogukantufan00.kesem.ui.components.EmptyStateCard
import io.github.dogukantufan00.kesem.ui.components.KesemCard
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.ui.theme.adaptiveAccent
import io.github.dogukantufan00.kesem.ui.theme.adaptiveBadgeBackground
import io.github.dogukantufan00.kesem.util.CategoryIconRegistry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    uiState: RecurringUiState,
    onBack: () -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (RecurringRuleEntity) -> Unit,
    onExecuteRule: (Long) -> Unit,
    onToggleActive: (Long, Boolean) -> Unit,
    onDeleteRule: (Long) -> Unit
) {
    val kesemColors = LocalKesemColors.current
    var deletingRule by remember { mutableStateOf<RecurringRuleEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                title = {
                    Text(
                        text = "Düzenli İşlemler",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = onAddRule) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Yeni Kural"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Monthly Equivalent Summary Card
            item {
                KesemCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aylık Sabit Gider",
                                style = MaterialTheme.typography.labelMedium,
                                color = kesemColors.subtleText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AmountText(
                                amountKurus = uiState.monthlyFixedExpenseKurus,
                                hidden = uiState.hideBalances,
                                color = kesemColors.expense,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aylık Sabit Gelir",
                                style = MaterialTheme.typography.labelMedium,
                                color = kesemColors.subtleText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AmountText(
                                amountKurus = uiState.monthlyFixedIncomeKurus,
                                hidden = uiState.hideBalances,
                                color = kesemColors.income,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (uiState.rules.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Autorenew,
                        title = "Henüz düzenli işlem yok",
                        subtitle = "Maaş, kira, fatura veya aboneliklerini ekleyerek vadesi geldiğinde tek dokunuşla kaydedebilirsin.",
                        actionLabel = "Düzenli İşlem Ekle",
                        onAction = onAddRule
                    )
                }
            } else {
                items(uiState.rules, key = { it.rule.id }) { item ->
                    RecurringRuleCard(
                        item = item,
                        hideBalances = uiState.hideBalances,
                        onExecute = { onExecuteRule(item.rule.id) },
                        onToggleActive = { onToggleActive(item.rule.id, !item.rule.isActive) },
                        onEdit = { onEditRule(item.rule) },
                        onDelete = { deletingRule = item.rule }
                    )
                }
            }
        }
    }

    if (deletingRule != null) {
        val rule = deletingRule!!
        AlertDialog(
            onDismissRequest = { deletingRule = null },
            title = { Text("Kuralı Sil") },
            text = { Text("\"${rule.title}\" düzenli işlem kuralı silinecek. Geçmişte oluşturulan işlemler korunur.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRule(rule.id)
                        deletingRule = null
                    }
                ) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingRule = null }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
private fun RecurringRuleCard(
    item: RecurringRuleWithDetails,
    hideBalances: Boolean,
    onExecute: () -> Unit,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val kesemColors = LocalKesemColors.current
    val rule = item.rule
    val isIncome = rule.type == TransactionType.INCOME
    val amountColor = if (isIncome) kesemColors.income else kesemColors.expense
    val rawBadgeColor = item.categoryColorHex?.let { Color(it) } ?: amountColor
    val badgeColor = rawBadgeColor.adaptiveAccent(kesemColors.isDark)

    val today = LocalDate.now()
    val nextDate = LocalDate.ofEpochDay(rule.nextDueDateEpochDay)
    val daysDiff = ChronoUnit.DAYS.between(today, nextDate)
    val freqLabel = when (rule.frequency) {
        RecurrenceFrequency.WEEKLY -> "Haftalık"
        RecurrenceFrequency.MONTHLY -> "Aylık"
        RecurrenceFrequency.YEARLY -> "Yıllık"
    }
    val dateLabel = when {
        !rule.isActive -> "Duraklatıldı"
        daysDiff == 0L -> "Bugün"
        daysDiff < 0L -> "${-daysDiff} gün gecikti"
        daysDiff <= 7L -> "$daysDiff gün kaldı"
        else -> nextDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("tr-TR")))
    }

    KesemCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(rawBadgeColor.adaptiveBadgeBackground(kesemColors.isDark)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconRegistry.iconFor(item.categoryIconKey ?: "autorenew"),
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.accountName} · $freqLabel · $dateLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (rule.isActive && daysDiff <= 0L) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        kesemColors.subtleText
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            AmountText(
                amountKurus = rule.amountKurus,
                hidden = hideBalances,
                showPlusSign = isIncome,
                color = if (rule.isActive) amountColor else kesemColors.subtleText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = kesemColors.cardBorder)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleActive) {
                    Icon(
                        imageVector = if (rule.isActive) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (rule.isActive) "Duraklat" else "Etkinleştir",
                        tint = kesemColors.subtleText
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Düzenle",
                        tint = kesemColors.subtleText
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (rule.isActive) {
                FilledTonalButton(
                    onClick = onExecute,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Şimdi Kaydet")
                }
            }
        }
    }
}
