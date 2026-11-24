package io.github.dogukantufan00.kesem.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dogukantufan00.kesem.data.preferences.SettingsRepository
import io.github.dogukantufan00.kesem.data.repository.WalletRepository
import io.github.dogukantufan00.kesem.model.AccountType
import io.github.dogukantufan00.kesem.model.AccountWithBalance
import io.github.dogukantufan00.kesem.model.BudgetProgress
import io.github.dogukantufan00.kesem.model.BudgetStatus
import io.github.dogukantufan00.kesem.model.CategoryType
import io.github.dogukantufan00.kesem.model.DebtType
import io.github.dogukantufan00.kesem.model.NetWorthSummary
import io.github.dogukantufan00.kesem.model.PeriodRange
import io.github.dogukantufan00.kesem.model.PeriodTotals
import io.github.dogukantufan00.kesem.model.RecurringRuleWithDetails
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.model.TransactionWithDetails
import io.github.dogukantufan00.kesem.util.BudgetCalculator
import io.github.dogukantufan00.kesem.util.InvestmentCalculator
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

data class SummaryUiState(
    val netWorth: NetWorthSummary = NetWorthSummary(),
    val periodRange: PeriodRange = PeriodCalculator.periodFor(LocalDate.now(), 1),
    val periodTotals: PeriodTotals = PeriodTotals(),
    val accounts: List<AccountWithBalance> = emptyList(),
    val recentTransactions: List<TransactionWithDetails> = emptyList(),
    val dueRecurringRules: List<RecurringRuleWithDetails> = emptyList(),
    val budgetAlerts: List<BudgetProgress> = emptyList(),
    val hideBalances: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModel(
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

    private val currentExpenseBreakdownFlow = settingsRepository.monthStartDay.flatMapLatest { monthStartDay ->
        val currentPeriod = PeriodCalculator.periodFor(LocalDate.now(), monthStartDay)
        walletRepository.observeCategoryBreakdown(
            type = TransactionType.EXPENSE,
            startMillis = currentPeriod.startMillis,
            endMillisExclusive = currentPeriod.endMillisExclusive
        )
    }

    private val currentPeriodTotalsFlow = settingsRepository.monthStartDay.flatMapLatest { monthStartDay ->
        val currentPeriod = PeriodCalculator.periodFor(LocalDate.now(), monthStartDay)
        walletRepository.observePeriodTotals(currentPeriod.startMillis, currentPeriod.endMillisExclusive)
    }

    private val budgetAlertsFlow = combine(
        walletRepository.observeBudgets(),
        walletRepository.observeCategories(CategoryType.EXPENSE),
        currentExpenseBreakdownFlow,
        currentPeriodTotalsFlow
    ) { budgets, categories, breakdown, currentTotals ->
        BudgetCalculator.evaluateAll(
            budgets = budgets,
            categories = categories,
            expenseBreakdown = breakdown,
            totalExpenseKurus = currentTotals.expenseKurus,
            includeInactive = false
        ).filter { it.status == BudgetStatus.WARNING || it.status == BudgetStatus.EXCEEDED }
    }

    private val baseDataFlow = combine(
        walletRepository.observeActiveAccounts(),
        walletRepository.observeRecentTransactions(15),
        periodFlow,
        periodTotalsFlow,
        settingsRepository.hideBalances
    ) { accounts, recentTx, periodRange, periodTotals, hideBalances ->
        BaseSummaryData(accounts, recentTx, periodRange, periodTotals, hideBalances)
    }

    val uiState: StateFlow<SummaryUiState> = combine(
        baseDataFlow,
        walletRepository.observeDebts(),
        walletRepository.observeDueRecurringRules(),
        walletRepository.observeInvestments(),
        budgetAlertsFlow
    ) { base, debts, dueRules, investments, budgetAlerts ->
        val liquidKurus = base.accounts
            .filter {
                it.account.includeInTotal &&
                    (it.account.type == AccountType.BANK || it.account.type == AccountType.CASH)
            }
            .sumOf { it.currentBalanceKurus }

        val cardDebtKurus = base.accounts
            .filter { it.account.includeInTotal && it.account.type == AccountType.CREDIT_CARD }
            .sumOf { (-it.currentBalanceKurus).coerceAtLeast(0L) }

        val openPersonalDebtKurus = debts
            .filter { !it.debt.isSettled && it.debt.type == DebtType.BORROWED }
            .sumOf { it.remainingAmountKurus }

        val openReceivablesKurus = debts
            .filter { !it.debt.isSettled && it.debt.type == DebtType.LENT }
            .sumOf { it.remainingAmountKurus }

        val investmentKurus = investments
            .filter { it.includeInTotal }
            .sumOf { InvestmentCalculator.evaluate(it).currentValueKurus }

        SummaryUiState(
            netWorth = NetWorthSummary(
                liquidKurus = liquidKurus,
                investmentKurus = investmentKurus,
                debtKurus = cardDebtKurus + openPersonalDebtKurus,
                receivablesKurus = openReceivablesKurus
            ),
            periodRange = base.periodRange,
            periodTotals = base.periodTotals,
            accounts = base.accounts,
            recentTransactions = base.recentTransactions,
            dueRecurringRules = dueRules,
            budgetAlerts = budgetAlerts,
            hideBalances = base.hideBalances
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SummaryUiState()
    )

    fun toggleHideBalances() {
        viewModelScope.launch {
            settingsRepository.toggleHideBalances()
        }
    }

    fun previousPeriod() {
        periodOffset.value -= 1L
    }

    fun nextPeriod() {
        periodOffset.value += 1L
    }

    fun executeRecurringRule(ruleId: Long) {
        viewModelScope.launch {
            walletRepository.executeRecurringRule(ruleId)
        }
    }

    fun skipRecurringRule(ruleId: Long) {
        viewModelScope.launch {
            walletRepository.skipRecurringRuleOccurrence(ruleId)
        }
    }

    private data class BaseSummaryData(
        val accounts: List<AccountWithBalance>,
        val recentTransactions: List<TransactionWithDetails>,
        val periodRange: PeriodRange,
        val periodTotals: PeriodTotals,
        val hideBalances: Boolean
    )
}
