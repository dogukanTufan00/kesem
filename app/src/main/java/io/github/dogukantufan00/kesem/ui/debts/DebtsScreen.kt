package io.github.dogukantufan00.kesem.ui.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.model.DebtType
import io.github.dogukantufan00.kesem.model.DebtWithSummary
import io.github.dogukantufan00.kesem.ui.components.AmountText
import io.github.dogukantufan00.kesem.ui.components.EmptyStateCard
import io.github.dogukantufan00.kesem.ui.components.KesemCard
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.util.MoneyFormatter

private enum class DebtFilter(val label: String) {
    ALL("Tümü"),
    LENT("Alacaklarım"),
    BORROWED("Borçlarım"),
    SETTLED("Kapananlar")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    uiState: DebtsUiState,
    onBack: () -> Unit,
    onAddDebt: () -> Unit,
    onDebtClick: (DebtWithSummary) -> Unit,
    onQuickPaymentClick: (DebtWithSummary) -> Unit
) {
    val kesemColors = LocalKesemColors.current
    var selectedFilter by remember { mutableStateOf(DebtFilter.ALL) }

    val filtered = remember(uiState.debts, selectedFilter) {
        when (selectedFilter) {
            DebtFilter.ALL -> uiState.debts.filter { !it.debt.isSettled }
            DebtFilter.LENT -> uiState.debts.filter { !it.debt.isSettled && it.debt.type == DebtType.LENT }
            DebtFilter.BORROWED -> uiState.debts.filter { !it.debt.isSettled && it.debt.type == DebtType.BORROWED }
            DebtFilter.SETTLED -> uiState.debts.filter { it.debt.isSettled }
        }
    }

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
                        text = "Borç & Alacak",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = onAddDebt) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Yeni Kayıt"
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
            // Summary Card
            item {
                KesemCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Toplam Alacak",
                                style = MaterialTheme.typography.labelMedium,
                                color = kesemColors.subtleText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AmountText(
                                amountKurus = uiState.totalReceivablesKurus,
                                hidden = uiState.hideBalances,
                                color = kesemColors.income,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Toplam Borç",
                                style = MaterialTheme.typography.labelMedium,
                                color = kesemColors.subtleText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AmountText(
                                amountKurus = uiState.totalBorrowedKurus,
                                hidden = uiState.hideBalances,
                                color = kesemColors.expense,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DebtFilter.entries) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Handshake,
                        title = "Kayıt bulunamadı",
                        subtitle = if (selectedFilter == DebtFilter.SETTLED) {
                            "Henüz kapanmış bir borç veya alacak kaydı yok."
                        } else {
                            "Kişilere verdiğin veya aldığın borçları buradan takip edebilirsin."
                        },
                        actionLabel = if (selectedFilter != DebtFilter.SETTLED) "Borç / Alacak Ekle" else null,
                        onAction = if (selectedFilter != DebtFilter.SETTLED) onAddDebt else null
                    )
                }
            } else {
                items(filtered, key = { it.debt.id }) { item ->
                    DebtCard(
                        item = item,
                        hideBalances = uiState.hideBalances,
                        onClick = { onDebtClick(item) },
                        onQuickPayment = { onQuickPaymentClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtCard(
    item: DebtWithSummary,
    hideBalances: Boolean,
    onClick: () -> Unit,
    onQuickPayment: () -> Unit
) {
    val kesemColors = LocalKesemColors.current
    val debt = item.debt
    val isLent = debt.type == DebtType.LENT
    val accent = if (isLent) kesemColors.income else kesemColors.expense
    val badgeBg = if (isLent) kesemColors.incomeContainer else kesemColors.expenseContainer
    val progress = if (debt.totalAmountKurus > 0L) {
        (item.paidAmountKurus.toDouble() / debt.totalAmountKurus.toDouble()).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    KesemCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isLent) "Alacak" else "Borç",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accent
                    )
                }
                Text(
                    text = debt.personName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (debt.dueDateMillis != null && !debt.isSettled) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Vade: ${MoneyFormatter.formatShortDate(debt.dueDateMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (debt.dueDateMillis < System.currentTimeMillis()) {
                        kesemColors.expense
                    } else {
                        kesemColors.subtleText
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (debt.isSettled) "Kapandı" else "Kalan",
                    style = MaterialTheme.typography.labelSmall,
                    color = kesemColors.subtleText
                )
                AmountText(
                    amountKurus = item.remainingAmountKurus,
                    hidden = hideBalances,
                    color = if (debt.isSettled) kesemColors.subtleText else accent,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Toplam: ${MoneyFormatter.formatKurus(debt.totalAmountKurus, hidden = hideBalances)}",
                style = MaterialTheme.typography.bodySmall,
                color = kesemColors.subtleText
            )
        }

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

        if (!debt.isSettled) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = debt.note.ifBlank { "Detay ve ödeme geçmişi için dokun" },
                    style = MaterialTheme.typography.bodySmall,
                    color = kesemColors.subtleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onQuickPayment,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(if (isLent) "Tahsil Et" else "Ödeme Ekle")
                }
            }
        }
    }
}
