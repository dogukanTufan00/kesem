package io.github.dogukantufan00.kesem.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dogukantufan00.kesem.data.local.entity.BudgetEntity
import io.github.dogukantufan00.kesem.data.local.entity.CategoryEntity
import io.github.dogukantufan00.kesem.data.preferences.SettingsRepository
import io.github.dogukantufan00.kesem.data.repository.WalletRepository
import io.github.dogukantufan00.kesem.model.BudgetProgress
import io.github.dogukantufan00.kesem.model.CashFlowPointRaw
import io.github.dogukantufan00.kesem.model.CategorySpendSummary
import io.github.dogukantufan00.kesem.model.CategoryType
import io.github.dogukantufan00.kesem.model.DailyExpensePoint
import io.github.dogukantufan00.kesem.model.MonthlyTrendPoint
import io.github.dogukantufan00.kesem.model.PeriodRange
import io.github.dogukantufan00.kesem.model.PeriodTotals
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.util.AnalyticsCalculator
import io.github.dogukantufan00.kesem.util.BudgetCalculator
import io.github.dogukantufan00.kesem.util.PeriodCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class AnalyticsUiState(
    val periodRange: PeriodRange = PeriodCalculator.periodFor(LocalDate.now(), 1),
    val periodTotals: PeriodTotals = PeriodTotals(),
    val selectedType: CategoryType = CategoryType.EXPENSE,
    val breakdown: List<CategorySpendSummary> = emptyList(),
    val monthlyTrend: List<MonthlyTrendPoint> = emptyList(),
    val dailyExpenses: List<DailyExpensePoint> = emptyList(),
    val averageDailyExpenseKurus: Long = 0L,
    val expenseChangePercent: Int? = null,
    val budgetProgress: List<BudgetProgress> = emptyList(),
    val hideBalances: Boolean = false
) {
    val selectedTotalKurus: Long
        get() = breakdown.sumOf { it.totalKurus }

    val savingsRatePercent: Int?
        get() {
            val income = periodTotals.incomeKurus
            if (income <= 0L) return null
            val saved = income - periodTotals.expenseKurus
            return ((saved.toDouble() / income.toDouble()) * 100.0).toInt()
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val walletRepository: WalletRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val periodOffset = MutableStateFlow(0L)
    private val selectedType = MutableStateFlow(CategoryType.EXPENSE)

    private val periodWindowFlow = combine(
        settingsRepository.monthStartDay,
        periodOffset
    ) { monthStartDay, offset ->
        val base = PeriodCalculator.periodFor(LocalDate.now(), monthStartDay)
        val selected = if (offset == 0L) base else PeriodCalculator.stepPeriod(base, offset, monthStartDay)
        (-5L..0L).map { delta ->
            if (delta == 0L) selected else PeriodCalculator.stepPeriod(selected, delta, monthStartDay)
        }
    }

    private val windowRowsFlow = periodWindowFlow.flatMapLatest { periods ->
        walletRepository.observeIncomeExpenseInRange(
            startMillis = periods.first().startMillis,
            endMillisExclusive = periods.last().endMillisExclusive
        )
    }

    private val selectedBreakdownFlow = combine(periodWindowFlow, selectedType) { periods, catType ->
        periods.last() to catType
    }.flatMapLatest { (range, catType) ->
        val txType = if (catType == CategoryType.EXPENSE) {
            TransactionType.EXPENSE
        } else {
            TransactionType.INCOME
        }
        walletRepository.observeCategoryBreakdown(
            type = txType,
            startMillis = range.startMillis,
            endMillisExclusive = range.endMillisExclusive
        )
    }

    private val expenseBreakdownFlow = periodWindowFlow.flatMapLatest { periods ->
        val current = periods.last()
        walletRepository.observeCategoryBreakdown(
            type = TransactionType.EXPENSE,
            startMillis = current.startMillis,
            endMillisExclusive = current.endMillisExclusive
        )
    }

    private val chartCoreFlow = combine(
        periodWindowFlow,
        windowRowsFlow,
        selectedType,
        selectedBreakdownFlow
    ) { periods, rows, catType, breakdown ->
        ChartCoreData(periods, rows, catType, breakdown)
    }

    private val budgetMetaFlow = combine(
        walletRepository.observeBudgets(),
        walletRepository.observeCategories(CategoryType.EXPENSE),
        expenseBreakdownFlow
    ) { budgets, categories, expenseBreakdown ->
        BudgetMetaData(budgets, categories, expenseBreakdown)
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        chartCoreFlow,
        budgetMetaFlow,
        settingsRepository.hideBalances
    ) { core, budgetMeta, hideBalances ->
        val currentPeriod = core.periods.last()
        val trend = AnalyticsCalculator.buildPeriodTrend(core.periods, core.rows)
        val currentTrend = trend.last()
        val previousTrend = trend.getOrNull(trend.size - 2)
        val totals = PeriodTotals(
            incomeKurus = currentTrend.incomeKurus,
            expenseKurus = currentTrend.expenseKurus
        )
        val dailyExpenses = AnalyticsCalculator.buildDailyExpenses(currentPeriod, core.rows)
        val avgDaily = AnalyticsCalculator.averageDailyExpenseKurus(
            period = currentPeriod,
            totalExpenseKurus = totals.expenseKurus
        )
        val changePct = previousTrend?.let {
            AnalyticsCalculator.periodExpenseChangePercent(totals.expenseKurus, it.expenseKurus)
        }
        val budgetProgress = BudgetCalculator.evaluateAll(
            budgets = budgetMeta.budgets,
            categories = budgetMeta.categories,
            expenseBreakdown = budgetMeta.expenseBreakdown,
            totalExpenseKurus = totals.expenseKurus,
            includeInactive = false
        )

        AnalyticsUiState(
            periodRange = currentPeriod,
            periodTotals = totals,
            selectedType = core.selectedType,
            breakdown = core.breakdown,
            monthlyTrend = trend,
            dailyExpenses = dailyExpenses,
            averageDailyExpenseKurus = avgDaily,
            expenseChangePercent = changePct,
            budgetProgress = budgetProgress,
            hideBalances = hideBalances
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState()
    )

    fun selectCategoryType(type: CategoryType) {
        selectedType.value = type
    }

    fun previousPeriod() {
        periodOffset.value -= 1L
    }

    fun nextPeriod() {
        periodOffset.value += 1L
    }

    private data class ChartCoreData(
        val periods: List<PeriodRange>,
        val rows: List<CashFlowPointRaw>,
        val selectedType: CategoryType,
        val breakdown: List<CategorySpendSummary>
    )

    private data class BudgetMetaData(
        val budgets: List<BudgetEntity>,
        val categories: List<CategoryEntity>,
        val expenseBreakdown: List<CategorySpendSummary>
    )
}
