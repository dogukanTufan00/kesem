package io.github.dogukantufan00.kesem.ui.summary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.model.AccountType
import io.github.dogukantufan00.kesem.model.AccountWithBalance
import io.github.dogukantufan00.kesem.model.BudgetProgress
import io.github.dogukantufan00.kesem.model.BudgetStatus
import io.github.dogukantufan00.kesem.model.RecurringRuleWithDetails
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.model.TransactionWithDetails
import io.github.dogukantufan00.kesem.ui.components.AmountText
import io.github.dogukantufan00.kesem.ui.components.DateGroupHeader
import io.github.dogukantufan00.kesem.ui.components.EmptyStateCard
import io.github.dogukantufan00.kesem.ui.components.KesemCard
import io.github.dogukantufan00.kesem.ui.components.PeriodHeader
import io.github.dogukantufan00.kesem.ui.components.TransactionRow
import io.github.dogukantufan00.kesem.ui.components.groupTransactionsByDate
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.ui.theme.adaptiveAccent
import io.github.dogukantufan00.kesem.ui.theme.adaptiveBadgeBackground
import io.github.dogukantufan00.kesem.util.CategoryIconRegistry
import io.github.dogukantufan00.kesem.util.MoneyFormatter
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun SummaryScreen(
    uiState: SummaryUiState,
    onToggleHideBalances: () -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onAccountClick: (Long) -> Unit,
    onAddBankAccount: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onExecuteRecurringRule: (Long) -> Unit,
    onSkipRecurringRule: (Long) -> Unit,
    onTransactionClick: (TransactionWithDetails) -> Unit,
    onSeeAllTransactions: () -> Unit,
    onQuickAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kesemColors = LocalKesemColors.current
    val groupedRecent = groupTransactionsByDate(uiState.recentTransactions)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Kesem",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Surface(
                    onClick = onToggleHideBalances,
                    shape = RoundedCornerShape(50),
                    color = if (uiState.hideBalances) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = if (uiState.hideBalances) {
                        null
                    } else {
                        BorderStroke(1.dp, kesemColors.cardBorder)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.hideBalances) {
                                Icons.Rounded.VisibilityOff
                            } else {
                                Icons.Rounded.Visibility
                            },
                            contentDescription = if (uiState.hideBalances) {
                                "Bakiyeleri göster"
                            } else {
                                "Bakiyeleri gizle"
                            },
                            tint = if (uiState.hideBalances) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = if (uiState.hideBalances) "Gizli" else "Gizle",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.hideBalances) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }

        // Net Worth Hero Card ("Deri Cüzdan" hissi)
        item {
            NetWorthHeroCard(
                uiState = uiState
            )
        }

        // Budget Alerts Card (only when approaching/exceeding limits)
        if (uiState.budgetAlerts.isNotEmpty()) {
            item {
                BudgetAlertsCard(
                    alerts = uiState.budgetAlerts,
                    hideBalances = uiState.hideBalances,
                    onSeeBudgets = onNavigateToBudgets
                )
            }
        }

        // Due Recurring Rules Card (only when due items exist)
        if (uiState.dueRecurringRules.isNotEmpty()) {
            item {
                DueRecurringRulesCard(
                    rules = uiState.dueRecurringRules,
                    hideBalances = uiState.hideBalances,
                    onExecute = onExecuteRecurringRule,
                    onSkip = onSkipRecurringRule,
                    onSeeAll = onNavigateToRecurring
                )
            }
        }

        // Period Summary Card
        item {
            KesemCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                PeriodHeader(
                    label = uiState.periodRange.label,
                    onPrevious = onPreviousPeriod,
                    onNext = onNextPeriod
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PeriodStatBox(
                        label = "Gelir",
                        amountKurus = uiState.periodTotals.incomeKurus,
                        hidden = uiState.hideBalances,
                        color = kesemColors.income,
                        modifier = Modifier.weight(1f)
                    )
                    PeriodStatBox(
                        label = "Gider",
                        amountKurus = uiState.periodTotals.expenseKurus,
                        hidden = uiState.hideBalances,
                        color = kesemColors.expense,
                        modifier = Modifier.weight(1f)
                    )
                    val netFlow = uiState.periodTotals.netFlowKurus
                    val netColor = when {
                        netFlow > 0L -> kesemColors.income
                        netFlow < 0L -> kesemColors.expense
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    PeriodStatBox(
                        label = "Net Akış",
                        amountKurus = netFlow,
                        hidden = uiState.hideBalances,
                        color = netColor,
                        showPlusSign = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Accounts Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Hesaplarım",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = onNavigateToAccounts,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Tümü")
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.accounts, key = { it.account.id }) { item ->
                        AccountStripCard(
                            item = item,
                            hideBalances = uiState.hideBalances,
                            onClick = { onAccountClick(item.account.id) }
                        )
                    }
                    item {
                        AddBankShortcutCard(onClick = onAddBankAccount)
                    }
                }
            }
        }

        // Recent Transactions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Son İşlemler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (uiState.recentTransactions.isNotEmpty()) {
                    TextButton(
                        onClick = onSeeAllTransactions,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Tümünü Gör")
                    }
                }
            }
        }

        if (groupedRecent.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = "Henüz işlem yok",
                    subtitle = "İlk gelir, gider veya transfer kaydını sağ alttaki + tuşundan hızlıca ekleyebilirsin.",
                    actionLabel = "İlk İşlemi Ekle",
                    onAction = onQuickAddTransaction
                )
            }
        } else {
            groupedRecent.forEach { (dateLabel, txs) ->
                item(key = "header_$dateLabel") {
                    DateGroupHeader(title = dateLabel)
                }
                item(key = "group_$dateLabel") {
                    KesemCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        txs.forEachIndexed { index, tx ->
                            TransactionRow(
                                item = tx,
                                hideBalances = uiState.hideBalances,
                                onClick = { onTransactionClick(tx) }
                            )
                            if (index < txs.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = kesemColors.cardBorder
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetAlertsCard(
    alerts: List<BudgetProgress>,
    hideBalances: Boolean,
    onSeeBudgets: () -> Unit
) {
    val kesemColors = LocalKesemColors.current
    val hasExceeded = alerts.any { it.status == BudgetStatus.EXCEEDED }
    val headerIcon = if (hasExceeded) Icons.Rounded.ErrorOutline else Icons.Rounded.WarningAmber
    val headerTint = if (hasExceeded) kesemColors.expense else kesemColors.warning

    KesemCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
        onClick = onSeeBudgets
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = headerIcon,
                    contentDescription = null,
                    tint = headerTint,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Bütçe Uyarıları",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            TextButton(
                onClick = onSeeBudgets,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("Bütçeler")
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        alerts.take(3).forEachIndexed { index, alert ->
            val statusColor = if (alert.status == BudgetStatus.EXCEEDED) {
                kesemColors.expense
            } else {
                kesemColors.warning
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hideBalances) {
                            if (alert.status == BudgetStatus.EXCEEDED) "Aşıldı" else "Eşikte"
                        } else {
                            "%${alert.usagePercent} · ${MoneyFormatter.formatKurus(alert.spentKurus)} / ${MoneyFormatter.formatKurus(alert.limitKurus)}"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(alert.usageRatio.coerceIn(0.03f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                }
            }
            if (index < alerts.take(3).lastIndex) {
                HorizontalDivider(color = kesemColors.cardBorder)
            }
        }
    }
}

@Composable
private fun DueRecurringRulesCard(
    rules: List<RecurringRuleWithDetails>,
    hideBalances: Boolean,
    onExecute: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    onSeeAll: () -> Unit
) {
    val kesemColors = LocalKesemColors.current
    val today = LocalDate.now()

    KesemCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Autorenew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Vadesi Gelen İşlemler",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            TextButton(
                onClick = onSeeAll,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("Tümü")
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        rules.forEachIndexed { index, item ->
            val rule = item.rule
            val dueDate = LocalDate.ofEpochDay(rule.nextDueDateEpochDay)
            val diffDays = ChronoUnit.DAYS.between(dueDate, today)
            val dateText = when {
                diffDays == 0L -> "Bugün"
                diffDays > 0L -> "$diffDays gün gecikti"
                else -> "${-diffDays} gün kaldı"
            }
            val amountColor = if (rule.type == TransactionType.INCOME) {
                kesemColors.income
            } else {
                kesemColors.expense
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rule.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.accountName} · $dateText",
                            style = MaterialTheme.typography.bodySmall,
                            color = kesemColors.subtleText
                        )
                    }
                    AmountText(
                        amountKurus = rule.amountKurus,
                        hidden = hideBalances,
                        showPlusSign = rule.type == TransactionType.INCOME,
                        color = amountColor,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onSkip(rule.id) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Atla")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    FilledTonalButton(
                        onClick = { onExecute(rule.id) },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("Kaydet")
                    }
                }
            }

            if (index < rules.lastIndex) {
                HorizontalDivider(color = kesemColors.cardBorder)
            }
        }
    }
}

@Composable
private fun NetWorthHeroCard(
    uiState: SummaryUiState
) {
    val kesemColors = LocalKesemColors.current
    val shape = RoundedCornerShape(22.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, kesemColors.heroBorder),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(kesemColors.heroStart, kesemColors.heroEnd)
                    )
                )
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val glowCenter = Offset(size.width * 0.92f, size.height * 0.14f)
                val glowRadius = size.width * 0.56f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE28562).copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = glowCenter,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = glowCenter
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(kesemColors.heroDivider.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = kesemColors.heroText,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "Net Varlık",
                        style = MaterialTheme.typography.labelLarge,
                        color = kesemColors.heroSubtleText
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                AmountText(
                    amountKurus = uiState.netWorth.netWorthKurus,
                    hidden = uiState.hideBalances,
                    style = MaterialTheme.typography.displaySmall,
                    color = kesemColors.heroText
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = kesemColors.heroDivider)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NetWorthMetricColumn(
                        label = "Likit",
                        amountKurus = uiState.netWorth.liquidKurus,
                        hidden = uiState.hideBalances,
                        labelColor = kesemColors.heroSubtleText,
                        valueColor = kesemColors.heroText,
                        modifier = Modifier.weight(1f)
                    )
                    NetWorthMetricColumn(
                        label = "Yatırım",
                        amountKurus = uiState.netWorth.investmentKurus,
                        hidden = uiState.hideBalances,
                        labelColor = kesemColors.heroSubtleText,
                        valueColor = if (uiState.netWorth.investmentKurus > 0L) {
                            kesemColors.heroText
                        } else {
                            kesemColors.heroSubtleText
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NetWorthMetricColumn(
                        label = "Alacak",
                        amountKurus = uiState.netWorth.receivablesKurus,
                        hidden = uiState.hideBalances,
                        labelColor = kesemColors.heroSubtleText,
                        valueColor = if (uiState.netWorth.receivablesKurus > 0L) {
                            kesemColors.heroPositive
                        } else {
                            kesemColors.heroSubtleText
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NetWorthMetricColumn(
                        label = "Borç",
                        amountKurus = uiState.netWorth.debtKurus,
                        hidden = uiState.hideBalances,
                        labelColor = kesemColors.heroSubtleText,
                        valueColor = if (uiState.netWorth.debtKurus > 0L) {
                            kesemColors.heroNegative
                        } else {
                            kesemColors.heroSubtleText
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NetWorthMetricColumn(
    label: String,
    amountKurus: Long,
    hidden: Boolean,
    valueColor: Color,
    modifier: Modifier = Modifier,
    labelColor: Color = LocalKesemColors.current.subtleText
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor
        )
        AmountText(
            amountKurus = amountKurus,
            hidden = hidden,
            color = valueColor,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PeriodStatBox(
    label: String,
    amountKurus: Long,
    hidden: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    showPlusSign: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalKesemColors.current.subtleText
        )
        AmountText(
            amountKurus = amountKurus,
            hidden = hidden,
            showPlusSign = showPlusSign,
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AccountStripCard(
    item: AccountWithBalance,
    hideBalances: Boolean,
    onClick: () -> Unit
) {
    val account = item.account
    val rawAccent = Color(account.colorHex)
    val kesemColors = LocalKesemColors.current
    val accentColor = rawAccent.adaptiveAccent(kesemColors.isDark)
    val isCard = account.type == AccountType.CREDIT_CARD
    val displayKurus = if (isCard) {
        (-item.currentBalanceKurus).coerceAtLeast(0L)
    } else {
        item.currentBalanceKurus
    }

    KesemCard(
        modifier = Modifier.width(168.dp),
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(rawAccent.adaptiveBadgeBackground(kesemColors.isDark)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconRegistry.iconFor(account.iconKey),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = when (account.type) {
                    AccountType.CASH -> "Nakit"
                    AccountType.CREDIT_CARD -> "Kredi Kartı"
                    else -> account.bankName ?: "Banka"
                },
                style = MaterialTheme.typography.labelSmall,
                color = kesemColors.subtleText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = account.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (isCard) {
            Text(
                text = "Borç: ${MoneyFormatter.formatKurus(displayKurus, hidden = hideBalances)}",
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = if (displayKurus > 0L) kesemColors.expense else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            AmountText(
                amountKurus = displayKurus,
                hidden = hideBalances,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AddBankShortcutCard(onClick: () -> Unit) {
    KesemCard(
        modifier = Modifier.width(136.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Banka Ekle",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalance,
                contentDescription = null,
                tint = LocalKesemColors.current.subtleText,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "Vadesiz",
                style = MaterialTheme.typography.labelSmall,
                color = LocalKesemColors.current.subtleText
            )
        }
    }
}
