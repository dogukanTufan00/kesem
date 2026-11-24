package io.github.dogukantufan00.kesem.ui.investments

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import io.github.dogukantufan00.kesem.model.InvestmentAssetType
import io.github.dogukantufan00.kesem.model.InvestmentWithValuation
import io.github.dogukantufan00.kesem.ui.components.AmountText
import io.github.dogukantufan00.kesem.ui.components.EmptyStateCard
import io.github.dogukantufan00.kesem.ui.components.KesemCard
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.util.InvestmentCalculator
import io.github.dogukantufan00.kesem.util.MoneyFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun InvestmentAssetType.turkishLabel(): String = when (this) {
    InvestmentAssetType.CURRENCY -> "Döviz"
    InvestmentAssetType.GOLD -> "Altın"
    InvestmentAssetType.BIST_STOCK -> "BIST"
    InvestmentAssetType.US_STOCK -> "ABD"
    InvestmentAssetType.TEFAS_FUND -> "TEFAS"
    InvestmentAssetType.CRYPTO -> "Kripto"
    InvestmentAssetType.TERM_DEPOSIT -> "Mevduat"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    uiState: InvestmentsUiState,
    onBack: () -> Unit,
    onRefresh: (force: Boolean) -> Unit,
    onDismissStatus: () -> Unit,
    onAddInvestment: () -> Unit,
    onInvestmentClick: (InvestmentWithValuation) -> Unit
) {
    val kesemColors = LocalKesemColors.current
    var selectedFilter by remember { mutableStateOf<InvestmentAssetType?>(null) }

    LaunchedEffect(Unit) {
        onRefresh(false)
    }

    val filtered = remember(uiState.items, selectedFilter) {
        if (selectedFilter == null) {
            uiState.items
        } else {
            uiState.items.filter { it.investment.assetType == selectedFilter }
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
                        text = "Yatırımlar",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    if (uiState.isRefreshing) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        IconButton(onClick = { onRefresh(true) }) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Fiyatları Yenile"
                            )
                        }
                    }
                    IconButton(onClick = onAddInvestment) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Varlık Ekle"
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
            // Portfolio Summary Card
            item {
                KesemCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Toplam Portföy",
                            style = MaterialTheme.typography.labelLarge,
                            color = kesemColors.subtleText
                        )
                        val updatedLabel = if (uiState.lastUpdatedMillis != null) {
                            val timeStr = Instant.ofEpochMilli(uiState.lastUpdatedMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalTime()
                                .format(DateTimeFormatter.ofPattern("HH:mm"))
                            "Son güncelleme: $timeStr"
                        } else {
                            "Önbellek / Mevduat"
                        }
                        Text(
                            text = updatedLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = kesemColors.subtleText
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    AmountText(
                        amountKurus = uiState.totalValueKurus,
                        hidden = uiState.hideBalances,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = kesemColors.cardBorder)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Toplam Maliyet",
                                style = MaterialTheme.typography.labelSmall,
                                color = kesemColors.subtleText
                            )
                            AmountText(
                                amountKurus = uiState.totalCostKurus,
                                hidden = uiState.hideBalances,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Toplam Kâr / Zarar",
                                style = MaterialTheme.typography.labelSmall,
                                color = kesemColors.subtleText
                            )
                            val plColor = when {
                                uiState.totalProfitLossKurus > 0L -> kesemColors.income
                                uiState.totalProfitLossKurus < 0L -> kesemColors.expense
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            val pctText = uiState.totalProfitLossPercent?.let {
                                val sign = if (it > 0) "+" else ""
                                " ($sign${String.format(Locale.forLanguageTag("tr-TR"), "%.1f", it)}%)"
                            } ?: ""
                            Text(
                                text = if (uiState.hideBalances) {
                                    "••••••"
                                } else {
                                    "${
                                        MoneyFormatter.formatKurus(
                                            uiState.totalProfitLossKurus,
                                            showPlusSign = true
                                        )
                                    }$pctText"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.SemiBold,
                                color = plColor
                            )
                        }
                    }
                }
            }

            // Status Banner (e.g. TEFAS manual hint or update count)
            if (uiState.statusMessage != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissStatus,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Kapat",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Asset Type Filters
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            label = { Text("Tümü") }
                        )
                    }
                    items(InvestmentAssetType.entries) { type ->
                        FilterChip(
                            selected = selectedFilter == type,
                            onClick = {
                                selectedFilter = if (selectedFilter == type) null else type
                            },
                            label = { Text(type.turkishLabel()) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        title = "Henüz varlık yok",
                        subtitle = "Döviz, altın, BIST/ABD hisse, TEFAS fon, kripto veya vadeli mevduat ekleyerek portföyünü takip edebilirsin.",
                        actionLabel = "Varlık Ekle",
                        onAction = onAddInvestment
                    )
                }
            } else {
                items(filtered, key = { it.investment.id }) { item ->
                    InvestmentCard(
                        item = item,
                        hideBalances = uiState.hideBalances,
                        onClick = { onInvestmentClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InvestmentCard(
    item: InvestmentWithValuation,
    hideBalances: Boolean,
    onClick: () -> Unit
) {
    val kesemColors = LocalKesemColors.current
    val inv = item.investment
    val valuation = item.valuation
    val isDeposit = inv.assetType == InvestmentAssetType.TERM_DEPOSIT

    val plColor = when {
        valuation.profitLossKurus > 0L -> kesemColors.income
        valuation.profitLossKurus < 0L -> kesemColors.expense
        else -> kesemColors.subtleText
    }
    val pctText = valuation.profitLossPercent?.let {
        val sign = if (it > 0) "+" else ""
        " ($sign${String.format(Locale.forLanguageTag("tr-TR"), "%.1f", it)}%)"
    } ?: ""

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
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = inv.assetType.turkishLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = inv.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = if (isDeposit) {
                        val rateText = String.format(
                            Locale.forLanguageTag("tr-TR"),
                            "%%%.1f faiz",
                            inv.annualInterestRate ?: 0.0
                        )
                        val daysText = valuation.daysRemaining?.let { "$it gün kaldı" } ?: ""
                        listOfNotNull(rateText, daysText.takeIf { it.isNotEmpty() }).joinToString(" · ")
                    } else {
                        val qtyStr = if (hideBalances) "••" else InvestmentCalculator.formatQuantity(inv.quantity)
                        "${inv.symbol} · $qtyStr adet"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = kesemColors.subtleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                AmountText(
                    amountKurus = valuation.currentValueKurus,
                    hidden = hideBalances,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (hideBalances) {
                        "••••"
                    } else {
                        "${MoneyFormatter.formatKurus(valuation.profitLossKurus, showPlusSign = true)}$pctText"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Medium,
                    color = plColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = kesemColors.cardBorder)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDeposit) {
                Text(
                    text = "Anapara: ${MoneyFormatter.formatKurus(valuation.costBasisKurus, hidden = hideBalances)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = kesemColors.subtleText
                )
                val maturityInterest = valuation.maturityProjectedNetInterestKurus ?: 0L
                Text(
                    text = "Vade sonu net: +${MoneyFormatter.formatKurus(maturityInterest, hidden = hideBalances)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = kesemColors.income,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "Birim: ${InvestmentCalculator.formatUnitPriceTry(inv.lastPriceTry)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = kesemColors.subtleText
                )
                val dailyText = inv.dailyChangePercent?.let {
                    val sign = if (it > 0) "+" else ""
                    " · $sign${String.format(Locale.forLanguageTag("tr-TR"), "%.2f", it)}%"
                } ?: ""
                Text(
                    text = "${inv.priceSource}$dailyText",
                    style = MaterialTheme.typography.labelSmall,
                    color = kesemColors.subtleText
                )
            }
        }
    }
}
