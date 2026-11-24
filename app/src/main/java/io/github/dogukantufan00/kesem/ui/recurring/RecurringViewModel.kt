package io.github.dogukantufan00.kesem.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dogukantufan00.kesem.data.local.entity.CategoryEntity
import io.github.dogukantufan00.kesem.data.local.entity.RecurringRuleEntity
import io.github.dogukantufan00.kesem.data.preferences.SettingsRepository
import io.github.dogukantufan00.kesem.data.repository.WalletRepository
import io.github.dogukantufan00.kesem.model.AccountWithBalance
import io.github.dogukantufan00.kesem.model.CategoryType
import io.github.dogukantufan00.kesem.model.RecurrenceFrequency
import io.github.dogukantufan00.kesem.model.RecurringRuleWithDetails
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.util.RecurrenceCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecurringUiState(
    val rules: List<RecurringRuleWithDetails> = emptyList(),
    val activeAccounts: List<AccountWithBalance> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val monthlyFixedExpenseKurus: Long = 0L,
    val monthlyFixedIncomeKurus: Long = 0L,
    val hideBalances: Boolean = false
)

class RecurringViewModel(
    private val walletRepository: WalletRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<RecurringUiState> = combine(
        walletRepository.observeAllRecurringRules(),
        walletRepository.observeActiveAccounts(),
        walletRepository.observeCategories(CategoryType.EXPENSE),
        walletRepository.observeCategories(CategoryType.INCOME),
        settingsRepository.hideBalances
    ) { rules, accounts, expenseCats, incomeCats, hideBalances ->
        val activeRules = rules.filter { it.rule.isActive }
        val monthlyExpense = activeRules
            .filter { it.rule.type == TransactionType.EXPENSE }
            .sumOf { RecurrenceCalculator.monthlyEquivalentKurus(it.rule.amountKurus, it.rule.frequency) }
        val monthlyIncome = activeRules
            .filter { it.rule.type == TransactionType.INCOME }
            .sumOf { RecurrenceCalculator.monthlyEquivalentKurus(it.rule.amountKurus, it.rule.frequency) }

        RecurringUiState(
            rules = rules,
            activeAccounts = accounts,
            expenseCategories = expenseCats,
            incomeCategories = incomeCats,
            monthlyFixedExpenseKurus = monthlyExpense,
            monthlyFixedIncomeKurus = monthlyIncome,
            hideBalances = hideBalances
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecurringUiState()
    )

    fun saveRule(
        editingRule: RecurringRuleEntity?,
        title: String,
        type: TransactionType,
        amountKurus: Long,
        accountId: Long,
        categoryId: Long?,
        frequency: RecurrenceFrequency,
        dayOfMonth: Int,
        nextDueDateEpochDay: Long,
        note: String
    ) {
        viewModelScope.launch {
            val entity = if (editingRule == null) {
                RecurringRuleEntity(
                    title = title.trim(),
                    type = type,
                    amountKurus = amountKurus,
                    accountId = accountId,
                    categoryId = categoryId,
                    frequency = frequency,
                    dayOfMonth = dayOfMonth.coerceIn(1, 28),
                    nextDueDateEpochDay = nextDueDateEpochDay,
                    isActive = true,
                    note = note.trim()
                )
            } else {
                editingRule.copy(
                    title = title.trim(),
                    type = type,
                    amountKurus = amountKurus,
                    accountId = accountId,
                    categoryId = categoryId,
                    frequency = frequency,
                    dayOfMonth = dayOfMonth.coerceIn(1, 28),
                    nextDueDateEpochDay = nextDueDateEpochDay,
                    note = note.trim()
                )
            }
            walletRepository.saveRecurringRule(entity)
        }
    }

    fun setRuleActive(ruleId: Long, active: Boolean) {
        viewModelScope.launch {
            walletRepository.setRecurringRuleActive(ruleId, active)
        }
    }

    fun executeRule(ruleId: Long) {
        viewModelScope.launch {
            walletRepository.executeRecurringRule(ruleId)
        }
    }

    fun skipRule(ruleId: Long) {
        viewModelScope.launch {
            walletRepository.skipRecurringRuleOccurrence(ruleId)
        }
    }

    fun deleteRule(ruleId: Long) {
        viewModelScope.launch {
            walletRepository.deleteRecurringRule(ruleId)
        }
    }
}
