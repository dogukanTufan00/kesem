package io.github.dogukantufan00.kesem.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dogukantufan00.kesem.data.local.entity.BudgetEntity
import io.github.dogukantufan00.kesem.data.local.entity.CategoryEntity
import io.github.dogukantufan00.kesem.data.preferences.SettingsRepository
import io.github.dogukantufan00.kesem.data.repository.WalletRepository
import io.github.dogukantufan00.kesem.model.BudgetProgress
import io.github.dogukantufan00.kesem.model.CategorySpendSummary
import io.github.dogukantufan00.kesem.model.CategoryType
import io.github.dogukantufan00.kesem.model.PeriodRange
import io.github.dogukantufan00.kesem.model.PeriodTotals
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.util.BudgetCalculator
import io.github.dogukantufan00.kesem.util.PeriodCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BudgetsUiState(
    val periodRange: PeriodRange = PeriodCalculator.periodFor(LocalDate.now(), 1),
    val periodTotals: PeriodTotals = PeriodTotals(),
    val activeProgress: List<BudgetProgress> = emptyList(),
    val inactiveProgress: List<BudgetProgress> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val existingCategoryIds: Set<Long?> = emptySet(),
    val hideBalances: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModel(
    private val walletRepository: WalletRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val periodOffset = MutableStateFlow(0L)

    private val periodFlow = combine(
        settingsRepository.monthStartDay,
        periodOffset
    ) { monthStartDay, offset ->
        val current = PeriodCalculator.periodFor(LocalDate.now(), monthStartDay)
        if (offset == 0L) current else PeriodCalculator.stepPeriod(current, offset, monthStartDay)
    }

    private val periodTotalsFlow = periodFlow.flatMapLatest { range ->
        walletRepository.observePeriodTotals(range.startMillis, range.endMillisExclusive)
    }

    private val expenseBreakdownFlow = periodFlow.flatMapLatest { range ->
        walletRepository.observeCategoryBreakdown(
            type = TransactionType.EXPENSE,
            startMillis = range.startMillis,
            endMillisExclusive = range.endMillisExclusive
        )
    }

    private val periodDataFlow = combine(
        periodFlow,
        periodTotalsFlow,
        expenseBreakdownFlow
    ) { range, totals, breakdown ->
        PeriodBudgetData(range, totals, breakdown)
    }

    val uiState: StateFlow<BudgetsUiState> = combine(
        periodDataFlow,
        walletRepository.observeBudgets(),
        walletRepository.observeCategories(CategoryType.EXPENSE),
        settingsRepository.hideBalances
    ) { periodData, budgets, categories, hideBalances ->
        val allProgress = BudgetCalculator.evaluateAll(
            budgets = budgets,
            categories = categories,
            expenseBreakdown = periodData.breakdown,
            totalExpenseKurus = periodData.totals.expenseKurus,
            includeInactive = true
        )
        BudgetsUiState(
            periodRange = periodData.range,
            periodTotals = periodData.totals,
            activeProgress = allProgress.filter { it.budget.isActive },
            inactiveProgress = allProgress.filter { !it.budget.isActive },
            expenseCategories = categories,
            existingCategoryIds = budgets.map { it.categoryId }.toSet(),
            hideBalances = hideBalances
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetsUiState()
    )

    fun previousPeriod() {
        periodOffset.value -= 1L
    }

    fun nextPeriod() {
        periodOffset.value += 1L
    }

    fun saveBudget(
        editingBudget: BudgetEntity?,
        categoryId: Long?,
        monthlyLimitKurus: Long,
        alertThresholdPercent: Int,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            val entity = editingBudget?.copy(
                monthlyLimitKurus = monthlyLimitKurus,
                alertThresholdPercent = alertThresholdPercent,
                isActive = isActive
            ) ?: BudgetEntity(
                categoryId = categoryId,
                monthlyLimitKurus = monthlyLimitKurus,
                alertThresholdPercent = alertThresholdPercent,
                isActive = isActive
            )
            walletRepository.saveBudget(entity)
        }
    }

    fun setBudgetActive(id: Long, active: Boolean) {
        viewModelScope.launch {
            walletRepository.setBudgetActive(id, active)
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            walletRepository.deleteBudget(id)
        }
    }

    private data class PeriodBudgetData(
        val range: PeriodRange,
        val totals: PeriodTotals,
        val breakdown: List<CategorySpendSummary>
    )
}
