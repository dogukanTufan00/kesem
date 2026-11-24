package io.github.dogukantufan00.kesem.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PieChartOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.model.BudgetProgress
import io.github.dogukantufan00.kesem.model.BudgetStatus
import io.github.dogukantufan00.kesem.model.CategorySpendSummary
import io.github.dogukantufan00.kesem.model.CategoryType
import io.github.dogukantufan00.kesem.model.DailyExpensePoint
import io.github.dogukantufan00.kesem.model.MonthlyTrendPoint
import io.github.dogukantufan00.kesem.ui.components.AmountText
import io.github.dogukantufan00.kesem.ui.components.EmptyStateCard
import io.github.dogukantufan00.kesem.ui.components.KesemCard
import io.github.dogukantufan00.kesem.ui.components.PeriodHeader
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.ui.theme.adaptiveAccent
import io.github.dogukantufan00.kesem.ui.theme.adaptiveBadgeBackground
import io.github.dogukantufan00.kesem.util.CategoryIconRegistry
import io.github.dogukantufan00.kesem.util.MoneyFormatter
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

@Composable
fun AnalyticsScreen(
    uiState: AnalyticsUiState,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onSelectCategoryType: (CategoryType) -> Unit,
    onNavigateToBudgets: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val kesemColors = LocalKesemColors.current
    var selectedBreakdownIndex by remember(uiState.selectedType, uiState.periodRange.startMillis) {
        mutableStateOf<Int?>(null)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Analiz",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Period + KPI Summary Card
        item {
            KesemCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp)
            ) {
                PeriodHeader(
                    label = uiState.periodRange.label,
                    onPrevious = onPreviousPeriod,
                    onNext = onNextPeriod
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsSummaryTile(
                        label = "Gelir",
                        valueText = MoneyFormatter.formatKurus(
                            uiState.periodTotals.incomeKurus,
                            hidden = uiState.hideBalances
                        ),
                        valueColor = kesemColors.income,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsSummaryTile(
                        label = "Gider",
                        valueText = MoneyFormatter.formatKurus(
                            uiState.periodTotals.expenseKurus,
                            hidden = uiState.hideBalances
                        ),
                        valueColor = kesemColors.expense,
                        modifier = Modifier.weight(1f)
                    )
                    val rate = uiState.savingsRatePercent
                    val rateText = when {
                        uiState.hideBalances -> "••%"
                        rate == null -> "—"
                        else -> "%$rate"
                    }
                    val rateColor = when {
                        rate == null -> kesemColors.subtleText
                        rate >= 0 -> kesemColors.income
                        else -> kesemColors.expense
                    }
                    AnalyticsSummaryTile(
                        label = "Tasarruf",
                        valueText = rateText,
                        valueColor = rateColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                val changePct = uiState.expenseChangePercent
                if (changePct != null && !uiState.hideBalances) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val isReduction = changePct <= 0
                    val pillColor = if (isReduction) kesemColors.income else kesemColors.expense
                    val pillBg = if (isReduction) kesemColors.incomeContainer else kesemColors.expenseContainer
                    val sign = if (changePct > 0) "+" else ""
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = pillBg
                    ) {
                        Text(
                            text = "Giderler geçen döneme göre $sign%$changePct",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = pillColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // 6-Period Income vs Expense Grouped Bar Chart
        if (uiState.monthlyTrend.isNotEmpty()) {
            item {
                MonthlyTrendChartCard(
                    points = uiState.monthlyTrend,
                    hideBalances = uiState.hideBalances
                )
            }
        }

        // Category Type Selector
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    CategoryType.INCOME to "Gelir Dağılımı",
                    CategoryType.EXPENSE to "Gider Dağılımı"
                )
                options.forEachIndexed { index, (type, label) ->
                    SegmentedButton(
                        selected = uiState.selectedType == type,
                        onClick = { onSelectCategoryType(type) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        )
                    ) {
                        Text(label)
                    }
                }
            }
        }

        // Interactive Donut Chart + Category Breakdown
        if (uiState.breakdown.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.PieChartOutline,
                    title = "Bu dönemde veri yok",
                    subtitle = if (uiState.selectedType == CategoryType.EXPENSE) {
                        "Seçili dönemde gider kaydı bulunmuyor."
                    } else {
                        "Seçili dönemde gelir kaydı bulunmuyor."
                    }
                )
            }
        } else {
            item {
                val totalKurus = uiState.selectedTotalKurus.coerceAtLeast(1L)
                val safeSelectedIndex = selectedBreakdownIndex?.takeIf { it in uiState.breakdown.indices }
                KesemCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    CategoryDonutChart(
                        breakdown = uiState.breakdown,
                        totalKurus = totalKurus,
                        selectedType = uiState.selectedType,
                        selectedIndex = safeSelectedIndex,
                        hideBalances = uiState.hideBalances,
                        onSelectIndex = { clickedIdx ->
                            selectedBreakdownIndex = if (clickedIdx == safeSelectedIndex) null else clickedIdx
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = kesemColors.cardBorder
                    )
                    uiState.breakdown.forEachIndexed { index, item ->
                        CategoryBreakdownRow(
                            summary = item,
                            totalKurus = totalKurus,
                            hideBalances = uiState.hideBalances,
                            isSelected = index == safeSelectedIndex,
                            onClick = {
                                selectedBreakdownIndex = if (index == safeSelectedIndex) null else index
                            }
                        )
                        if (index < uiState.breakdown.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = kesemColors.cardBorder
                            )
                        }
                    }
                }
            }
        }

        // Daily Expense Distribution Sparkbar + Averages
        if (uiState.dailyExpenses.isNotEmpty()) {
            item {
                DailyExpenseChartCard(
                    dailyExpenses = uiState.dailyExpenses,
                    averageDailyKurus = uiState.averageDailyExpenseKurus,
                    hideBalances = uiState.hideBalances
                )
            }
        }

        // Active Budgets Snapshot
        item {
            AnalyticsBudgetsCard(
                budgets = uiState.budgetProgress,
                hideBalances = uiState.hideBalances,
                onManageBudgets = onNavigateToBudgets
            )
        }
    }
}

@Composable
private fun MonthlyTrendChartCard(
    points: List<MonthlyTrendPoint>,
    hideBalances: Boolean
) {
    val kesemColors = LocalKesemColors.current
    var selectedIdx by remember(points) {
        mutableIntStateOf(points.lastIndex.coerceAtLeast(0))
    }
    val safeSelectedIdx = selectedIdx.coerceIn(0, (points.size - 1).coerceAtLeast(0))
    val selectedPoint = points.getOrNull(safeSelectedIdx)
    val maxVal = points.maxOfOrNull { maxOf(it.incomeKurus, it.expenseKurus) } ?: 0L
    val hasAnyData = maxVal > 0L

    KesemCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "6 Aylık Gelir / Gider",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = kesemColors.income, label = "Gelir")
                LegendDot(color = kesemColors.expense, label = "Gider")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (!hasAnyData) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Son 6 dönem için gelir veya gider kaydı bulunmuyor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = kesemColors.subtleText
                )
            }
        } else {
            val gridColor = kesemColors.cardBorder
            val highlightBg = MaterialTheme.colorScheme.surfaceVariant
            val incomeColor = kesemColors.income
            val expenseColor = kesemColors.expense

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .pointerInput(points.size) {
                        detectTapGestures { tapOffset ->
                            val count = points.size.coerceAtLeast(1)
                            val colWidth = size.width.toFloat() / count.toFloat()
                            val idx = (tapOffset.x / colWidth).toInt().coerceIn(0, count - 1)
                            selectedIdx = idx
                        }
                    }
            ) {
                val count = points.size
                val colWidth = size.width / count.toFloat()
                val topPad = 8.dp.toPx()
                val bottomPad = 4.dp.toPx()
                val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(10f)
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                listOf(0f, 0.5f, 1f).forEach { frac ->
                    val y = topPad + chartHeight * frac
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (frac < 1f) dash else null
                    )
                }

                val barWidth = (colWidth * 0.24f).coerceIn(6.dp.toPx(), 16.dp.toPx())
                val gap = 3.dp.toPx()
                val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                points.forEachIndexed { idx, pt ->
                    val centerX = colWidth * idx + colWidth / 2f
                    if (idx == safeSelectedIdx) {
                        drawRoundRect(
                            color = highlightBg.copy(alpha = 0.65f),
                            topLeft = Offset(colWidth * idx + 4.dp.toPx(), 0f),
                            size = Size(colWidth - 8.dp.toPx(), size.height),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }

                    val incRatio = (pt.incomeKurus.toDouble() / maxVal.toDouble()).toFloat().coerceIn(0f, 1f)
                    val expRatio = (pt.expenseKurus.toDouble() / maxVal.toDouble()).toFloat().coerceIn(0f, 1f)
                    val incH = if (pt.incomeKurus > 0L) (chartHeight * incRatio).coerceAtLeast(4.dp.toPx()) else 0f
                    val expH = if (pt.expenseKurus > 0L) (chartHeight * expRatio).coerceAtLeast(4.dp.toPx()) else 0f
                    val baseLineY = topPad + chartHeight

                    if (incH > 0f) {
                        drawRoundRect(
                            color = incomeColor,
                            topLeft = Offset(centerX - gap / 2f - barWidth, baseLineY - incH),
                            size = Size(barWidth, incH),
                            cornerRadius = corner
                        )
                    }
                    if (expH > 0f) {
                        drawRoundRect(
                            color = expenseColor,
                            topLeft = Offset(centerX + gap / 2f, baseLineY - expH),
                            size = Size(barWidth, expH),
                            cornerRadius = corner
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                points.forEachIndexed { idx, pt ->
                    val isSelected = idx == safeSelectedIdx
                    Text(
                        text = pt.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else kesemColors.subtleText,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedIdx = idx }
                            .padding(vertical = 2.dp)
                    )
                }
            }

            if (selectedPoint != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = kesemColors.cardBorder)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedPoint.period.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "+${MoneyFormatter.formatKurus(selectedPoint.incomeKurus, hidden = hideBalances)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.SemiBold,
                            color = kesemColors.income
                        )
                        Text(
                            text = "-${MoneyFormatter.formatKurus(selectedPoint.expenseKurus, hidden = hideBalances)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.SemiBold,
                            color = kesemColors.expense
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDonutChart(
    breakdown: List<CategorySpendSummary>,
    totalKurus: Long,
    selectedType: CategoryType,
    selectedIndex: Int?,
    hideBalances: Boolean,
    onSelectIndex: (Int?) -> Unit
) {
    val kesemColors = LocalKesemColors.current
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val sliceColors = remember(breakdown, kesemColors.isDark) {
        breakdown.map { Color(it.colorHex).adaptiveAccent(kesemColors.isDark) }
    }
    val sweeps = remember(breakdown, totalKurus) {
        breakdown.map { ((it.totalKurus.toDouble() / totalKurus.toDouble()) * 360.0).toFloat() }
    }
    val selectedItem = selectedIndex?.let { breakdown.getOrNull(it) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(196.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(sweeps) {
                        detectTapGestures { tapOffset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = tapOffset.x - cx
                            val dy = tapOffset.y - cy
                            val radius = hypot(dx, dy)
                            val outerR = min(cx, cy)
                            val innerR = outerR - 44.dp.toPx()
                            if (radius < innerR || radius > outerR) {
                                onSelectIndex(null)
                                return@detectTapGestures
                            }
                            val angleFromTop =
                                ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 90.0 + 360.0) % 360.0).toFloat()
                            var cumulative = 0f
                            var hitIndex: Int? = null
                            for (i in sweeps.indices) {
                                val next = cumulative + sweeps[i]
                                if (angleFromTop >= cumulative && angleFromTop <= next) {
                                    hitIndex = i
                                    break
                                }
                                cumulative = next
                            }
                            onSelectIndex(hitIndex)
                        }
                    }
            ) {
                val defaultStrokePx = 22.dp.toPx()
                val activeStrokePx = 28.dp.toPx()
                val inset = activeStrokePx / 2f + 2.dp.toPx()
                val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                val arcTopLeft = Offset(inset, inset)

                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = defaultStrokePx, cap = StrokeCap.Butt)
                )

                var startAngle = -90f
                val gapDeg = if (breakdown.size > 1) 1.8f else 0f
                breakdown.forEachIndexed { idx, _ ->
                    val sweep = sweeps[idx]
                    val drawSweep = (sweep - gapDeg).coerceAtLeast(1f)
                    val isSelected = selectedIndex == idx
                    val alpha = if (selectedIndex == null || isSelected) 1f else 0.30f
                    val strokeW = if (isSelected) activeStrokePx else defaultStrokePx
                    drawArc(
                        color = sliceColors[idx].copy(alpha = alpha),
                        startAngle = startAngle,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 34.dp)
            ) {
                if (selectedItem != null) {
                    val sharePct = (selectedItem.totalKurus.toDouble() / totalKurus.toDouble()) * 100.0
                    Text(
                        text = selectedItem.categoryName,
                        style = MaterialTheme.typography.labelMedium,
                        color = kesemColors.subtleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = MoneyFormatter.formatKurus(selectedItem.totalKurus, hidden = hideBalances),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (hideBalances) "••%" else String.format(Locale.forLanguageTag("tr-TR"), "%%%.1f", sharePct),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(selectedItem.colorHex).adaptiveAccent(kesemColors.isDark)
                    )
                } else {
                    Text(
                        text = if (selectedType == CategoryType.EXPENSE) "Toplam Gider" else "Toplam Gelir",
                        style = MaterialTheme.typography.labelMedium,
                        color = kesemColors.subtleText
                    )
                    Text(
                        text = MoneyFormatter.formatKurus(totalKurus, hidden = hideBalances),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${breakdown.size} kategori",
                        style = MaterialTheme.typography.labelSmall,
                        color = kesemColors.subtleText
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyExpenseChartCard(
    dailyExpenses: List<DailyExpensePoint>,
    averageDailyKurus: Long,
    hideBalances: Boolean
) {
    val kesemColors = LocalKesemColors.current
    val maxDay = dailyExpenses.maxByOrNull { it.expenseKurus }
    val maxExpense = maxDay?.expenseKurus ?: 0L
    val barColor = MaterialTheme.colorScheme.primary
    val peakColor = kesemColors.expense
    val gridColor = kesemColors.cardBorder

    KesemCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = "Günlük Harcama Dağılımı",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (maxExpense <= 0L) {
            Text(
                text = "Bu dönemde günlük harcama kaydı yok.",
                style = MaterialTheme.typography.bodySmall,
                color = kesemColors.subtleText
            )
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
            ) {
                val count = dailyExpenses.size.coerceAtLeast(1)
                val slotW = size.width / count.toFloat()
                val barW = (slotW * 0.62f).coerceAtLeast(2.dp.toPx())
                val chartH = size.height - 6.dp.toPx()

                drawLine(
                    color = gridColor,
                    start = Offset(0f, size.height - 1.dp.toPx()),
                    end = Offset(size.width, size.height - 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )

                dailyExpenses.forEachIndexed { idx, day ->
                    if (day.expenseKurus > 0L) {
                        val ratio = (day.expenseKurus.toDouble() / maxExpense.toDouble()).toFloat().coerceIn(0f, 1f)
                        val barH = (chartH * ratio).coerceAtLeast(4.dp.toPx())
                        val left = slotW * idx + (slotW - barW) / 2f
                        val isPeak = day.epochDay == maxDay?.epochDay
                        drawRoundRect(
                            color = if (isPeak) peakColor else barColor,
                            topLeft = Offset(left, size.height - 1.dp.toPx() - barH),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dailyExpenses.first().label,
                    style = MaterialTheme.typography.labelSmall,
                    color = kesemColors.subtleText
                )
                Text(
                    text = dailyExpenses[dailyExpenses.size / 2].label,
                    style = MaterialTheme.typography.labelSmall,
                    color = kesemColors.subtleText
                )
                Text(
                    text = dailyExpenses.last().label,
                    style = MaterialTheme.typography.labelSmall,
                    color = kesemColors.subtleText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = kesemColors.cardBorder)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsSummaryTile(
                    label = "Günlük Ortalama",
                    valueText = MoneyFormatter.formatKurus(averageDailyKurus, hidden = hideBalances),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsSummaryTile(
                    label = if (maxDay != null) "En Yüksek (${maxDay.label})" else "En Yüksek Gün",
                    valueText = MoneyFormatter.formatKurus(maxExpense, hidden = hideBalances),
                    valueColor = kesemColors.expense,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AnalyticsBudgetsCard(
    budgets: List<BudgetProgress>,
    hideBalances: Boolean,
    onManageBudgets: () -> Unit
) {
    val kesemColors = LocalKesemColors.current

    KesemCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        onClick = onManageBudgets
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bütçe Limitleri",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(
                onClick = onManageBudgets,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(if (budgets.isEmpty()) "Oluştur" else "Yönet")
            }
        }

        if (budgets.isEmpty()) {
            Text(
                text = "Genel veya kategori bazlı aylık limit ekleyerek durumunu buradan izleyebilirsin.",
                style = MaterialTheme.typography.bodySmall,
                color = kesemColors.subtleText
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            budgets.take(3).forEachIndexed { idx, progress ->
                val barColor = when (progress.status) {
                    BudgetStatus.OK -> kesemColors.income
                    BudgetStatus.WARNING -> kesemColors.warning
                    BudgetStatus.EXCEEDED -> kesemColors.expense
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = progress.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hideBalances) "•• / ••" else "%${progress.usagePercent} (${MoneyFormatter.formatKurus(progress.spentKurus)} / ${MoneyFormatter.formatKurus(progress.limitKurus)})",
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = barColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
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
                                    .background(barColor)
                            )
                        }
                    }
                }
                if (idx < budgets.take(3).lastIndex) {
                    HorizontalDivider(color = kesemColors.cardBorder)
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalKesemColors.current.subtleText
        )
    }
}

@Composable
private fun AnalyticsSummaryTile(
    label: String,
    valueText: String,
    valueColor: Color,
    modifier: Modifier = Modifier
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
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1
        )
    }
}

@Composable
private fun CategoryBreakdownRow(
    summary: CategorySpendSummary,
    totalKurus: Long,
    hideBalances: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val kesemColors = LocalKesemColors.current
    val rawColor = Color(summary.colorHex)
    val catColor = rawColor.adaptiveAccent(kesemColors.isDark)
    val catBg = rawColor.adaptiveBadgeBackground(kesemColors.isDark)
    val fraction = (summary.totalKurus.toDouble() / totalKurus.toDouble()).toFloat().coerceIn(0f, 1f)
    val percentText = String.format(Locale.forLanguageTag("tr-TR"), "%%%.1f", fraction * 100f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(catBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconRegistry.iconFor(summary.iconKey),
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${summary.transactionCount} işlem · $percentText",
                    style = MaterialTheme.typography.bodySmall,
                    color = kesemColors.subtleText
                )
            }
            AmountText(
                amountKurus = summary.totalKurus,
                hidden = hideBalances,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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
                    .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(catColor)
            )
        }
    }
}
