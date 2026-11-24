package io.github.dogukantufan00.kesem.model

import androidx.room.Embedded
import io.github.dogukantufan00.kesem.data.local.entity.AccountEntity
import io.github.dogukantufan00.kesem.data.local.entity.BudgetEntity
import io.github.dogukantufan00.kesem.data.local.entity.DebtEntity
import io.github.dogukantufan00.kesem.data.local.entity.InvestmentEntity
import io.github.dogukantufan00.kesem.data.local.entity.RecurringRuleEntity
import io.github.dogukantufan00.kesem.data.local.entity.TransactionEntity
import java.time.LocalDate

data class AccountWithBalance(
    @Embedded val account: AccountEntity,
    val currentBalanceKurus: Long
)

data class TransactionWithDetails(
    @Embedded val transaction: TransactionEntity,
    val accountName: String,
    val accountColorHex: Long,
    val toAccountName: String?,
    val categoryName: String?,
    val categoryIconKey: String?,
    val categoryColorHex: Long?
)

data class PeriodTotals(
    val incomeKurus: Long = 0L,
    val expenseKurus: Long = 0L
) {
    val netFlowKurus: Long get() = incomeKurus - expenseKurus
}

data class AccountPeriodTotals(
    val inflowKurus: Long = 0L,
    val outflowKurus: Long = 0L
)

data class CategorySpendSummary(
    val categoryId: Long?,
    val categoryName: String,
    val iconKey: String,
    val colorHex: Long,
    val totalKurus: Long,
    val transactionCount: Int
)

data class PeriodRange(
    val startDate: LocalDate,
    val endDateInclusive: LocalDate,
    val startMillis: Long,
    val endMillisExclusive: Long,
    val label: String
)

data class NetWorthSummary(
    val liquidKurus: Long = 0L,
    val investmentKurus: Long = 0L,
    val debtKurus: Long = 0L,
    val receivablesKurus: Long = 0L
) {
    val netWorthKurus: Long
        get() = liquidKurus + investmentKurus + receivablesKurus - debtKurus
}

data class DebtWithSummary(
    @Embedded val debt: DebtEntity,
    val paidAmountKurus: Long
) {
    val remainingAmountKurus: Long
        get() = (debt.totalAmountKurus - paidAmountKurus).coerceAtLeast(0L)
}

data class RecurringRuleWithDetails(
    @Embedded val rule: RecurringRuleEntity,
    val accountName: String,
    val accountColorHex: Long,
    val categoryName: String?,
    val categoryIconKey: String?,
    val categoryColorHex: Long?
)

data class CreditCardMetrics(
    val totalDebtKurus: Long,
    val availableLimitKurus: Long,
    val limitKurus: Long,
    val statementPeriodSpendKurus: Long,
    val futureInstallmentsKurus: Long,
    val futureInstallmentCount: Int
)

data class InvestmentValuation(
    val currentValueKurus: Long,
    val costBasisKurus: Long,
    val profitLossKurus: Long,
    val profitLossPercent: Double?,
    val maturityProjectedNetInterestKurus: Long? = null,
    val daysRemaining: Long? = null
)

data class InvestmentWithValuation(
    val investment: InvestmentEntity,
    val valuation: InvestmentValuation
)

data class MarketRefreshResult(
    val updatedCount: Int,
    val failedSymbols: List<String>,
    val timestampMillis: Long
)

data class CashFlowPointRaw(
    val id: Long,
    val type: TransactionType,
    val amountKurus: Long,
    val dateMillis: Long
)

data class MonthlyTrendPoint(
    val period: PeriodRange,
    val shortLabel: String,
    val incomeKurus: Long,
    val expenseKurus: Long
) {
    val netFlowKurus: Long
        get() = incomeKurus - expenseKurus
}

data class DailyExpensePoint(
    val epochDay: Long,
    val dayOfMonth: Int,
    val label: String,
    val expenseKurus: Long
)

data class BudgetProgress(
    val budget: BudgetEntity,
    val title: String,
    val iconKey: String,
    val colorHex: Long,
    val spentKurus: Long,
    val limitKurus: Long,
    val remainingKurus: Long,
    val usageRatio: Float,
    val usagePercent: Int,
    val status: BudgetStatus
)

data class AppLockConfig(
    val isPinEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val pinHash: String = "",
    val pinSalt: String = ""
)

data class BackupSummary(
    val exportedAtMillis: Long,
    val schemaVersion: Int,
    val accountsCount: Int,
    val categoriesCount: Int,
    val transactionsCount: Int,
    val debtsCount: Int,
    val debtPaymentsCount: Int,
    val recurringRulesCount: Int,
    val investmentsCount: Int,
    val budgetsCount: Int
)
