package io.github.dogukantufan00.kesem.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dogukantufan00.kesem.data.local.entity.AccountEntity
import io.github.dogukantufan00.kesem.data.preferences.SettingsRepository
import io.github.dogukantufan00.kesem.data.repository.WalletRepository
import io.github.dogukantufan00.kesem.model.AccountPeriodTotals
import io.github.dogukantufan00.kesem.model.AccountType
import io.github.dogukantufan00.kesem.model.AccountWithBalance
import io.github.dogukantufan00.kesem.model.CreditCardMetrics
import io.github.dogukantufan00.kesem.model.DebtType
import io.github.dogukantufan00.kesem.model.PeriodRange
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.model.TransactionWithDetails
import io.github.dogukantufan00.kesem.util.InvestmentCalculator
import io.github.dogukantufan00.kesem.util.PeriodCalculator
import io.github.dogukantufan00.kesem.util.RecurrenceCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AccountsUiState(
    val bankAccounts: List<AccountWithBalance> = emptyList(),
    val cashAccounts: List<AccountWithBalance> = emptyList(),
    val creditCardAccounts: List<AccountWithBalance> = emptyList(),
    val totalLiquidIncludedKurus: Long = 0L,
    val totalCreditCardDebtKurus: Long = 0L,
    val bankTotalKurus: Long = 0L,
    val cashTotalKurus: Long = 0L,
    val hasExcludedAccounts: Boolean = false,
    val totalReceivablesKurus: Long = 0L,
    val totalPersonalDebtKurus: Long = 0L,
    val openDebtCount: Int = 0,
    val monthlyFixedExpenseKurus: Long = 0L,
    val monthlyFixedIncomeKurus: Long = 0L,
    val activeRecurringCount: Int = 0,
    val investmentTotalKurus: Long = 0L,
    val investmentProfitLossKurus: Long = 0L,
    val investmentCount: Int = 0,
    val hideBalances: Boolean = false
)

data class AccountDetailUiState(
    val accountWithBalance: AccountWithBalance? = null,
    val periodRange: PeriodRange = PeriodCalculator.periodFor(LocalDate.now(), 1),
    val periodTotals: AccountPeriodTotals = AccountPeriodTotals(),
    val transactions: List<TransactionWithDetails> = emptyList(),
    val upcomingInstallments: List<TransactionWithDetails> = emptyList(),
    val creditCardMetrics: CreditCardMetrics? = null,
    val hideBalances: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModel(
    private val walletRepository: WalletRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val detailPeriodOffset = MutableStateFlow(0L)

    val uiState: StateFlow<AccountsUiState> = combine(
        walletRepository.observeActiveAccounts(),
        walletRepository.observeDebts(),
        walletRepository.observeAllRecurringRules(),
        walletRepository.observeInvestments(),
        settingsRepository.hideBalances
    ) { accounts, debts, recurringRules, investments, hideBalances ->
        val banks = accounts.filter { it.account.type == AccountType.BANK }
        val cash = accounts.filter { it.account.type == AccountType.CASH }
        val cards = accounts.filter { it.account.type == AccountType.CREDIT_CARD }

        val liquidTotal = accounts
            .filter {
                it.account.includeInTotal &&
                    (it.account.type == AccountType.BANK || it.account.type == AccountType.CASH)
            }
            .sumOf { it.currentBalanceKurus }

        val cardDebtTotal = cards
            .filter { it.account.includeInTotal }
            .sumOf { (-it.currentBalanceKurus).coerceAtLeast(0L) }

        val openDebts = debts.filter { !it.debt.isSettled }
        val receivables = openDebts
            .filter { it.debt.type == DebtType.LENT }
            .sumOf { it.remainingAmountKurus }
        val personalDebts = openDebts
            .filter { it.debt.type == DebtType.BORROWED }
            .sumOf { it.remainingAmountKurus }

        val activeRules = recurringRules.filter { it.rule.isActive }
        val monthlyExpense = activeRules
            .filter { it.rule.type == TransactionType.EXPENSE }
            .sumOf { RecurrenceCalculator.monthlyEquivalentKurus(it.rule.amountKurus, it.rule.frequency) }
        val monthlyIncome = activeRules
            .filter { it.rule.type == TransactionType.INCOME }
            .sumOf { RecurrenceCalculator.monthlyEquivalentKurus(it.rule.amountKurus, it.rule.frequency) }

        val includedValuations = investments
            .filter { it.includeInTotal }
            .map { InvestmentCalculator.evaluate(it) }
        val investmentTotal = includedValuations.sumOf { it.currentValueKurus }
        val investmentCost = includedValuations.sumOf { it.costBasisKurus }

        AccountsUiState(
            bankAccounts = banks,
            cashAccounts = cash,
            creditCardAccounts = cards,
            totalLiquidIncludedKurus = liquidTotal,
            totalCreditCardDebtKurus = cardDebtTotal,
            bankTotalKurus = banks.filter { it.account.includeInTotal }.sumOf { it.currentBalanceKurus },
            cashTotalKurus = cash.filter { it.account.includeInTotal }.sumOf { it.currentBalanceKurus },
            hasExcludedAccounts = accounts.any { !it.account.includeInTotal },
            totalReceivablesKurus = receivables,
            totalPersonalDebtKurus = personalDebts,
            openDebtCount = openDebts.size,
            monthlyFixedExpenseKurus = monthlyExpense,
            monthlyFixedIncomeKurus = monthlyIncome,
            activeRecurringCount = activeRules.size,
            investmentTotalKurus = investmentTotal,
            investmentProfitLossKurus = investmentTotal - investmentCost,
            investmentCount = investments.size,
            hideBalances = hideBalances
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountsUiState()
    )

    fun observeAccountDetail(accountId: Long): Flow<AccountDetailUiState> {
        val periodFlow = combine(
            settingsRepository.monthStartDay,
            detailPeriodOffset
        ) { monthStartDay, offset ->
            val current = PeriodCalculator.periodFor(LocalDate.now(), monthStartDay)
            if (offset == 0L) current else PeriodCalculator.stepPeriod(current, offset, monthStartDay)
        }

        val periodTotalsFlow = periodFlow.flatMapLatest { range ->
            walletRepository.observeAccountPeriodTotals(
                accountId = accountId,
                startMillis = range.startMillis,
                endMillisExclusive = range.endMillisExclusive
            )
        }

        val baseFlow = combine(
            walletRepository.observeAccount(accountId),
            periodFlow,
            periodTotalsFlow,
            walletRepository.observePastTransactionsForAccount(accountId),
            settingsRepository.hideBalances
        ) { account, periodRange, totals, pastTransactions, hideBalances ->
            DetailBase(account, periodRange, totals, pastTransactions, hideBalances)
        }

        return combine(
            baseFlow,
            walletRepository.observeUpcomingInstallmentsForAccount(accountId)
        ) { base, upcomingInstallments ->
            val accWithBalance = base.accountWithBalance
            val cardMetrics = if (accWithBalance?.account?.type == AccountType.CREDIT_CARD) {
                val acc = accWithBalance.account
                val totalDebt = (-accWithBalance.currentBalanceKurus).coerceAtLeast(0L)
                val limit = acc.creditLimitKurus ?: 0L
                val availableLimit = (limit - totalDebt).coerceAtLeast(0L)
                val statementRange = PeriodCalculator.periodFor(
                    referenceDate = LocalDate.now(),
                    monthStartDay = acc.statementDay ?: 1
                )
                val statementSpend = base.pastTransactions
                    .filter {
                        it.transaction.type == TransactionType.EXPENSE &&
                            it.transaction.dateMillis >= statementRange.startMillis &&
                            it.transaction.dateMillis < statementRange.endMillisExclusive
                    }
                    .sumOf { it.transaction.amountKurus }
                val futureSum = upcomingInstallments.sumOf { it.transaction.amountKurus }

                CreditCardMetrics(
                    totalDebtKurus = totalDebt,
                    availableLimitKurus = availableLimit,
                    limitKurus = limit,
                    statementPeriodSpendKurus = statementSpend,
                    futureInstallmentsKurus = futureSum,
                    futureInstallmentCount = upcomingInstallments.size
                )
            } else {
                null
            }

            AccountDetailUiState(
                accountWithBalance = accWithBalance,
                periodRange = base.periodRange,
                periodTotals = base.periodTotals,
                transactions = base.pastTransactions,
                upcomingInstallments = upcomingInstallments,
                creditCardMetrics = cardMetrics,
                hideBalances = base.hideBalances
            )
        }
    }

    fun previousDetailPeriod() {
        detailPeriodOffset.value -= 1L
    }

    fun nextDetailPeriod() {
        detailPeriodOffset.value += 1L
    }

    fun resetDetailPeriod() {
        detailPeriodOffset.value = 0L
    }

    fun saveAccount(
        editingAccountId: Long?,
        name: String,
        type: AccountType,
        bankName: String?,
        balanceKurus: Long,
        colorHex: Long,
        includeInTotal: Boolean,
        creditLimitKurus: Long? = null,
        statementDay: Int? = null,
        paymentDueDay: Int? = null
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            val cleanBank = if (type == AccountType.BANK || type == AccountType.CREDIT_CARD) {
                bankName?.trim()?.takeIf { it.isNotEmpty() }
            } else {
                null
            }
            val iconKey = when (type) {
                AccountType.BANK -> "account_balance"
                AccountType.CASH -> "wallet"
                AccountType.CREDIT_CARD -> "credit_card"
                AccountType.INVESTMENT -> "trending_up"
            }
            val storedTargetBalance = if (type == AccountType.CREDIT_CARD) {
                -kotlin.math.abs(balanceKurus)
            } else {
                balanceKurus
            }
            val normalizedLimit = if (type == AccountType.CREDIT_CARD) creditLimitKurus else null
            val normalizedStatementDay = if (type == AccountType.CREDIT_CARD) {
                statementDay?.coerceIn(1, 28)
            } else {
                null
            }
            val normalizedDueDay = if (type == AccountType.CREDIT_CARD) {
                paymentDueDay?.coerceIn(1, 28)
            } else {
                null
            }

            if (editingAccountId == null) {
                walletRepository.insertAccount(
                    AccountEntity(
                        name = trimmedName,
                        type = type,
                        bankName = cleanBank,
                        initialBalanceKurus = storedTargetBalance,
                        colorHex = colorHex,
                        iconKey = iconKey,
                        includeInTotal = includeInTotal,
                        creditLimitKurus = normalizedLimit,
                        statementDay = normalizedStatementDay,
                        paymentDueDay = normalizedDueDay
                    )
                )
            } else {
                val existing = walletRepository.getAccountById(editingAccountId) ?: return@launch
                walletRepository.updateAccount(
                    existing.copy(
                        name = trimmedName,
                        type = type,
                        bankName = cleanBank,
                        colorHex = colorHex,
                        iconKey = iconKey,
                        includeInTotal = includeInTotal,
                        creditLimitKurus = normalizedLimit,
                        statementDay = normalizedStatementDay,
                        paymentDueDay = normalizedDueDay
                    )
                )
                walletRepository.adjustAccountBalance(editingAccountId, storedTargetBalance)
            }
        }
    }

    fun adjustBalance(accountId: Long, targetBalanceKurus: Long) {
        viewModelScope.launch {
            walletRepository.adjustAccountBalance(accountId, targetBalanceKurus)
        }
    }

    fun archiveAccount(accountId: Long, archived: Boolean) {
        viewModelScope.launch {
            walletRepository.setAccountArchived(accountId, archived)
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            walletRepository.deleteAccount(accountId)
        }
    }

    private data class DetailBase(
        val accountWithBalance: AccountWithBalance?,
        val periodRange: PeriodRange,
        val periodTotals: AccountPeriodTotals,
        val pastTransactions: List<TransactionWithDetails>,
        val hideBalances: Boolean
    )
}
