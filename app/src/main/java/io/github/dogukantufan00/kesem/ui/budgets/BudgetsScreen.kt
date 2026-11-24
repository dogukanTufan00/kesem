package io.github.dogukantufan00.kesem.ui.budgets

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.data.local.entity.BudgetEntity
import io.github.dogukantufan00.kesem.model.BudgetProgress
import io.github.dogukantufan00.kesem.model.BudgetStatus
import io.github.dogukantufan00.kesem.ui.components.EmptyStateCard
import io.github.dogukantufan00.kesem.ui.components.KesemCard
import io.github.dogukantufan00.kesem.ui.components.PeriodHeader
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.ui.theme.adaptiveAccent
import io.github.dogukantufan00.kesem.ui.theme.adaptiveBadgeBackground
import io.github.dogukantufan00.kesem.util.CategoryIconRegistry
import io.github.dogukantufan00.kesem.util.MoneyFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    uiState: BudgetsUiState,
    onBack: () -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onAddBudget: () -> Unit,
    onEditBudget: (BudgetEntity) -> Unit,
    onToggleActive: (Long, Boolean) -> Unit
) {
    val kesemColors = LocalKesemColors.current

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
                        text = "Bütçe Limitleri",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = onAddBudget) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Bütçe Ekle"
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
                    if (uiState.activeProgress.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = kesemColors.cardBorder)
                        Spacer(modifier = Modifier.height(10.dp))
                        val warningCount = uiState.activeProgress.count { it.status == BudgetStatus.WARNING }
                        val exceededCount = uiState.activeProgress.count { it.status == BudgetStatus.EXCEEDED }
                        val overallBudget = uiState.activeProgress.firstOrNull { it.budget.categoryId == null }
                        val targetLimit = overallBudget?.limitKurus
                            ?: uiState.activeProgress.sumOf { it.limitKurus }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = if (overallBudget != null) "Genel Bütçe / Harcanan" else "Toplam Limit / Dönem Gideri",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = kesemColors.subtleText
                                )
                                Text(
                                    text = "${MoneyFormatter.formatKurus(uiState.periodTotals.expenseKurus, hidden = uiState.hideBalances)} / ${MoneyFormatter.formatKurus(targetLimit, hidden = uiState.hideBalances)}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (exceededCount > 0 || warningCount > 0) {
                                val badgeBg = if (exceededCount > 0) kesemColors.expenseContainer else kesemColors.warningContainer
                                val badgeFg = if (exceededCount > 0) kesemColors.expense else kesemColors.warning
                                val label = when {
                                    exceededCount > 0 && warningCount > 0 -> "$exceededCount aşıldı · $warningCount uyarı"
                                    exceededCount > 0 -> "$exceededCount bütçe aşıldı"
                                    else -> "$warningCount bütçe eşikte"
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = badgeBg
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = badgeFg,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.activeProgress.isEmpty() && uiState.inactiveProgress.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        title = "Henüz bütçe limiti yok",
                        subtitle = "Genel aylık veya kategori bazlı limitler belirleyerek harcamalarını takip edebilir, eşiğe yaklaşınca uyarı alabilirsin.",
                        actionLabel = "Bütçe Ekle",
                        onAction = onAddBudget
                    )
                }
            } else {
                items(
                    items = uiState.activeProgress,
                    key = { it.budget.id }
                ) { progress ->
                    BudgetProgressCard(
                        progress = progress,
                        hideBalances = uiState.hideBalances,
                        onClick = { onEditBudget(progress.budget) }
                    )
                }

                if (uiState.inactiveProgress.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pasif Bütçeler",
                            style = MaterialTheme.typography.labelLarge,
                            color = kesemColors.subtleText,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }
                    item {
                        KesemCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            uiState.inactiveProgress.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Limit: ${MoneyFormatter.formatKurus(item.limitKurus, hidden = uiState.hideBalances)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = kesemColors.subtleText
                                        )
                                    }
                                    Switch(
                                        checked = false,
                                        onCheckedChange = { onToggleActive(item.budget.id, true) }
                                    )
                                }
                                if (index < uiState.inactiveProgress.lastIndex) {
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
}

@Composable
fun BudgetProgressCard(
    progress: BudgetProgress,
    hideBalances: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kesemColors = LocalKesemColors.current
    val rawAccent = Color(progress.colorHex)
    val accent = rawAccent.adaptiveAccent(kesemColors.isDark)
    val badgeBg = rawAccent.adaptiveBadgeBackground(kesemColors.isDark)

    val statusColor: Color
    val statusBg: Color
    val statusLabel: String
    val statusIcon = when (progress.status) {
        BudgetStatus.OK -> {
            statusColor = kesemColors.income
            statusBg = kesemColors.incomeContainer
            statusLabel = "Yolunda"
            Icons.Rounded.CheckCircle
        }
        BudgetStatus.WARNING -> {
            statusColor = kesemColors.warning
            statusBg = kesemColors.warningContainer
            statusLabel = "Uyarı"
            Icons.Rounded.WarningAmber
        }
        BudgetStatus.EXCEEDED -> {
            statusColor = kesemColors.expense
            statusBg = kesemColors.expenseContainer
            statusLabel = "Aşıldı"
            Icons.Rounded.ErrorOutline
        }
    }

    KesemCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconRegistry.iconFor(progress.iconKey),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = progress.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (hideBalances) {
                        "Uyarı eşiği: %${progress.budget.alertThresholdPercent}"
                    } else {
                        "%${progress.usagePercent} · Eşik: %${progress.budget.alertThresholdPercent}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = kesemColors.subtleText
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = statusBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val fill = progress.usageRatio.coerceIn(0f, 1f)
            if (fill > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fill.coerceAtLeast(0.02f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Harcanan: ${MoneyFormatter.formatKurus(progress.spentKurus, hidden = hideBalances)}",
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = kesemColors.subtleText
            )
            val remainingText = if (progress.remainingKurus >= 0L) {
                "Kalan: ${MoneyFormatter.formatKurus(progress.remainingKurus, hidden = hideBalances)}"
            } else {
                "Aşım: ${MoneyFormatter.formatKurus(abs(progress.remainingKurus), hidden = hideBalances)}"
            }
            Text(
                text = remainingText,
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = if (progress.remainingKurus >= 0L) MaterialTheme.colorScheme.onSurface else kesemColors.expense
            )
        }
    }
}
