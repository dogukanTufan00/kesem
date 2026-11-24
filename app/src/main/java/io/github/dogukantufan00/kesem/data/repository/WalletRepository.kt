package io.github.dogukantufan00.kesem.data.repository

import androidx.room.withTransaction
import io.github.dogukantufan00.kesem.data.local.KesemDatabase
import io.github.dogukantufan00.kesem.data.local.SeedData
import io.github.dogukantufan00.kesem.data.local.entity.AccountEntity
import io.github.dogukantufan00.kesem.data.local.entity.BudgetEntity
import io.github.dogukantufan00.kesem.data.local.entity.CategoryEntity
import io.github.dogukantufan00.kesem.data.local.entity.DebtEntity
import io.github.dogukantufan00.kesem.data.local.entity.DebtPaymentEntity
import io.github.dogukantufan00.kesem.data.local.entity.InvestmentEntity
import io.github.dogukantufan00.kesem.data.local.entity.RecurringRuleEntity
import io.github.dogukantufan00.kesem.data.local.entity.TransactionEntity
import io.github.dogukantufan00.kesem.data.remote.MarketDataService
import io.github.dogukantufan00.kesem.data.storage.ReceiptStorage
import io.github.dogukantufan00.kesem.model.AccountPeriodTotals
import io.github.dogukantufan00.kesem.model.AccountWithBalance
import io.github.dogukantufan00.kesem.model.CashFlowPointRaw
import io.github.dogukantufan00.kesem.model.CategorySpendSummary
import io.github.dogukantufan00.kesem.model.CategoryType
import io.github.dogukantufan00.kesem.model.DebtType
import io.github.dogukantufan00.kesem.model.DebtWithSummary
import io.github.dogukantufan00.kesem.model.InvestmentAssetType
import io.github.dogukantufan00.kesem.model.MarketRefreshResult
import io.github.dogukantufan00.kesem.model.PeriodTotals
import io.github.dogukantufan00.kesem.model.RecurringRuleWithDetails
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.model.TransactionWithDetails
import io.github.dogukantufan00.kesem.util.CategoryIconRegistry
import io.github.dogukantufan00.kesem.util.InstallmentCalculator
import io.github.dogukantufan00.kesem.util.InvestmentCalculator
import io.github.dogukantufan00.kesem.util.MoneyFormatter
import io.github.dogukantufan00.kesem.util.RecurrenceCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class WalletRepository(
    private val database: KesemDatabase,
    private val receiptStorage: ReceiptStorage,
    private val marketDataService: MarketDataService
) {
    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val debtDao = database.debtDao()
    private val recurringRuleDao = database.recurringRuleDao()
    private val investmentDao = database.investmentDao()
    private val budgetDao = database.budgetDao()

    private fun endOfTodayMillis(): Long {
        return LocalDate.now()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() - 1L
    }

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        database.withTransaction {
            if (categoryDao.countCategories() == 0) {
                categoryDao.insertAll(SeedData.defaultCategories())
            }
            if (accountDao.countAccounts() == 0) {
                accountDao.insert(SeedData.defaultCashAccount())
            }
        }
    }

    suspend fun harmonizeLegacyColors() = withContext(Dispatchers.IO) {
        database.withTransaction {
            CategoryIconRegistry.legacyToWarmColorMap.forEach { (oldHex, newHex) ->
                accountDao.replaceColorHex(oldHex, newHex)
                categoryDao.replaceColorHex(oldHex, newHex)
            }
        }
    }

    // Accounts
    fun observeActiveAccounts(): Flow<List<AccountWithBalance>> =
        accountDao.observeAccountsWithBalance(archived = false)

    fun observeArchivedAccounts(): Flow<List<AccountWithBalance>> =
        accountDao.observeAccountsWithBalance(archived = true)

    fun observeAccount(accountId: Long): Flow<AccountWithBalance?> =
        accountDao.observeAccountWithBalance(accountId)

    suspend fun getAccountById(accountId: Long): AccountEntity? =
        withContext(Dispatchers.IO) { accountDao.getById(accountId) }

    suspend fun insertAccount(account: AccountEntity): Long =
        withContext(Dispatchers.IO) {
            require(account.name.isNotBlank()) { "Hesap adı boş olamaz" }
            accountDao.insert(account)
        }

    suspend fun updateAccount(account: AccountEntity) =
        withContext(Dispatchers.IO) {
            require(account.name.isNotBlank()) { "Hesap adı boş olamaz" }
            accountDao.update(account)
        }

    suspend fun adjustAccountBalance(accountId: Long, targetBalanceKurus: Long) =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val existing = accountDao.getById(accountId) ?: return@withTransaction
                val txCount = accountDao.countTransactionsForAccount(accountId)
                if (txCount == 0) {
                    if (existing.initialBalanceKurus != targetBalanceKurus) {
                        accountDao.update(existing.copy(initialBalanceKurus = targetBalanceKurus))
                    }
                    return@withTransaction
                }

                val txDelta = accountDao.getTransactionDeltaForAccount(accountId)
                val currentBalance = existing.initialBalanceKurus + txDelta
                val diff = targetBalanceKurus - currentBalance
                if (diff == 0L) return@withTransaction

                val now = System.currentTimeMillis()
                transactionDao.insert(
                    TransactionEntity(
                        type = if (diff > 0L) {
                            TransactionType.ADJUSTMENT_IN
                        } else {
                            TransactionType.ADJUSTMENT_OUT
                        },
                        amountKurus = kotlin.math.abs(diff),
                        accountId = accountId,
                        toAccountId = null,
                        categoryId = null,
                        dateMillis = now,
                        note = "Bakiye düzeltme",
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }

    suspend fun setAccountArchived(accountId: Long, archived: Boolean) =
        withContext(Dispatchers.IO) { accountDao.setArchived(accountId, archived) }

    suspend fun deleteAccount(accountId: Long) =
        withContext(Dispatchers.IO) {
            val receiptPaths = transactionDao.getReceiptPathsForAccount(accountId)
            accountDao.deleteById(accountId)
            receiptPaths.forEach { receiptStorage.deleteReceipt(it) }
        }

    // Categories
    fun observeCategories(type: CategoryType): Flow<List<CategoryEntity>> =
        categoryDao.observeByType(type)

    fun observeAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.observeAll()

    suspend fun getCategoryById(id: Long): CategoryEntity? =
        withContext(Dispatchers.IO) { categoryDao.getById(id) }

    suspend fun insertCategory(category: CategoryEntity): Long =
        withContext(Dispatchers.IO) {
            require(category.name.isNotBlank()) { "Kategori adı boş olamaz" }
            categoryDao.insert(category)
        }

    suspend fun updateCategory(category: CategoryEntity) =
        withContext(Dispatchers.IO) {
            require(category.name.isNotBlank()) { "Kategori adı boş olamaz" }
            categoryDao.update(category)
        }

    suspend fun deleteCategory(id: Long) =
        withContext(Dispatchers.IO) { categoryDao.deleteById(id) }

    // Transactions
    fun observePeriodTransactions(
        startMillis: Long,
        endMillisExclusive: Long
    ): Flow<List<TransactionWithDetails>> =
        transactionDao.observeTransactionsWithDetails(startMillis, endMillisExclusive)

    fun observeRecentTransactions(limit: Int = 15): Flow<List<TransactionWithDetails>> =
        transactionDao.observeRecentTransactions(limit = limit, upToMillis = endOfTodayMillis())

    fun observeAllTransactions(): Flow<List<TransactionWithDetails>> =
        transactionDao.observeAllTransactionsWithDetails()

    fun observeAccountTransactions(accountId: Long): Flow<List<TransactionWithDetails>> =
        transactionDao.observeTransactionsForAccount(accountId)

    fun observePastTransactionsForAccount(accountId: Long): Flow<List<TransactionWithDetails>> =
        transactionDao.observePastTransactionsForAccount(accountId, endOfTodayMillis())

    fun observeUpcomingInstallmentsForAccount(accountId: Long): Flow<List<TransactionWithDetails>> =
        transactionDao.observeUpcomingInstallmentsForAccount(accountId, endOfTodayMillis())

    fun observePeriodTotals(startMillis: Long, endMillisExclusive: Long): Flow<PeriodTotals> =
        transactionDao.observePeriodTotals(startMillis, endMillisExclusive)

    fun observeAccountPeriodTotals(
        accountId: Long,
        startMillis: Long,
        endMillisExclusive: Long
    ): Flow<AccountPeriodTotals> =
        transactionDao.observeAccountPeriodTotals(accountId, startMillis, endMillisExclusive)

    fun observeCategoryBreakdown(
        type: TransactionType,
        startMillis: Long,
        endMillisExclusive: Long
    ): Flow<List<CategorySpendSummary>> =
        transactionDao.observeCategoryBreakdown(type, startMillis, endMillisExclusive)

    fun observeDistinctTagsCsv(): Flow<List<String>> =
        transactionDao.observeDistinctTagsCsv()

    fun observeIncomeExpenseInRange(
        startMillis: Long,
        endMillisExclusive: Long
    ): Flow<List<CashFlowPointRaw>> =
        transactionDao.observeIncomeExpenseInRange(startMillis, endMillisExclusive)

    suspend fun getTransactionById(id: Long): TransactionEntity? =
        withContext(Dispatchers.IO) { transactionDao.getById(id) }

    suspend fun saveTransaction(transaction: TransactionEntity): Long =
        withContext(Dispatchers.IO) {
            require(transaction.amountKurus > 0L) { "Tutar sıfırdan büyük olmalı" }
            if (transaction.type == TransactionType.TRANSFER) {
                require(transaction.toAccountId != null) { "Hedef hesap seçilmeli" }
                require(transaction.accountId != transaction.toAccountId) {
                    "Kaynak ve hedef hesap aynı olamaz"
                }
            }

            val now = System.currentTimeMillis()
            if (transaction.id == 0L) {
                transactionDao.insert(
                    transaction.copy(
                        categoryId = if (transaction.type == TransactionType.TRANSFER) null else transaction.categoryId,
                        toAccountId = if (transaction.type == TransactionType.TRANSFER) transaction.toAccountId else null,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            } else {
                val previous = transactionDao.getById(transaction.id)
                val updated = transaction.copy(
                    categoryId = if (transaction.type == TransactionType.TRANSFER) null else transaction.categoryId,
                    toAccountId = if (transaction.type == TransactionType.TRANSFER) transaction.toAccountId else null,
                    createdAt = previous?.createdAt ?: transaction.createdAt,
                    updatedAt = now,
                    installmentGroupId = previous?.installmentGroupId ?: transaction.installmentGroupId,
                    installmentIndex = previous?.installmentIndex ?: transaction.installmentIndex,
                    installmentCount = previous?.installmentCount ?: transaction.installmentCount
                )
                transactionDao.update(updated)
                if (previous?.receiptPath != null && previous.receiptPath != updated.receiptPath) {
                    receiptStorage.deleteReceipt(previous.receiptPath)
                }
                updated.id
            }
        }

    suspend fun saveInstallmentExpense(
        totalAmountKurus: Long,
        installmentCount: Int,
        accountId: Long,
        categoryId: Long?,
        startDateMillis: Long,
        note: String,
        tags: List<String>,
        receiptPath: String?
    ) = withContext(Dispatchers.IO) {
        if (installmentCount <= 1) {
            saveTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amountKurus = totalAmountKurus,
                    accountId = accountId,
                    categoryId = categoryId,
                    dateMillis = startDateMillis,
                    note = note.trim(),
                    tagsCsv = MoneyFormatter.encodeTags(tags),
                    receiptPath = receiptPath
                )
            )
            return@withContext
        }

        val groupId = UUID.randomUUID().toString()
        val slices = InstallmentCalculator.splitInstallments(
            totalKurus = totalAmountKurus,
            count = installmentCount,
            startDateMillis = startDateMillis
        )
        val now = System.currentTimeMillis()
        val encodedTags = MoneyFormatter.encodeTags(tags)
        val cleanNote = note.trim()

        val entities = slices.map { slice ->
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amountKurus = slice.amountKurus,
                accountId = accountId,
                toAccountId = null,
                categoryId = categoryId,
                dateMillis = slice.dateMillis,
                note = cleanNote,
                tagsCsv = encodedTags,
                receiptPath = if (slice.index == 1) receiptPath else null,
                createdAt = now,
                updatedAt = now,
                installmentGroupId = groupId,
                installmentIndex = slice.index,
                installmentCount = slice.count
            )
        }
        database.withTransaction {
            transactionDao.insertAll(entities)
        }
    }

    suspend fun deleteTransaction(id: Long) =
        withContext(Dispatchers.IO) {
            val existing = transactionDao.getById(id)
            transactionDao.deleteById(id)
            if (existing?.receiptPath != null) {
                receiptStorage.deleteReceipt(existing.receiptPath)
            }
        }

    suspend fun deleteInstallmentGroup(groupId: String) =
        withContext(Dispatchers.IO) {
            val paths = transactionDao.getReceiptPathsByInstallmentGroup(groupId)
            transactionDao.deleteByInstallmentGroup(groupId)
            paths.forEach { receiptStorage.deleteReceipt(it) }
        }

    // Debts
    fun observeDebts(): Flow<List<DebtWithSummary>> =
        debtDao.observeDebtsWithSummary()

    fun observeDebtPayments(debtId: Long): Flow<List<DebtPaymentEntity>> =
        debtDao.observePaymentsForDebt(debtId)

    suspend fun saveDebt(debt: DebtEntity, reflectAccountId: Long? = null): Long =
        withContext(Dispatchers.IO) {
            require(debt.personName.isNotBlank()) { "Kişi adı boş olamaz" }
            require(debt.totalAmountKurus > 0L) { "Tutar sıfırdan büyük olmalı" }

            database.withTransaction {
                if (debt.id == 0L) {
                    val newDebtId = debtDao.insertDebt(debt)
                    if (reflectAccountId != null) {
                        val now = System.currentTimeMillis()
                        val txType = if (debt.type == DebtType.LENT) {
                            TransactionType.EXPENSE
                        } else {
                            TransactionType.INCOME
                        }
                        val txNote = if (debt.type == DebtType.LENT) {
                            "Borç verildi: ${debt.personName}"
                        } else {
                            "Borç alındı: ${debt.personName}"
                        }
                        transactionDao.insert(
                            TransactionEntity(
                                type = txType,
                                amountKurus = debt.totalAmountKurus,
                                accountId = reflectAccountId,
                                dateMillis = now,
                                note = txNote,
                                createdAt = now,
                                updatedAt = now
                            )
                        )
                    }
                    newDebtId
                } else {
                    val totalPaid = debtDao.getTotalPaidForDebt(debt.id)
                    debtDao.updateDebt(debt.copy(isSettled = totalPaid >= debt.totalAmountKurus))
                    debt.id
                }
            }
        }

    suspend fun recordDebtPayment(
        debtId: Long,
        amountKurus: Long,
        dateMillis: Long,
        accountId: Long?,
        note: String
    ) = withContext(Dispatchers.IO) {
        require(amountKurus > 0L) { "Ödeme tutarı sıfırdan büyük olmalı" }
        database.withTransaction {
            val debt = debtDao.getDebtById(debtId) ?: return@withTransaction
            val cleanNote = note.trim()
            val linkedTxId = if (accountId != null) {
                val now = System.currentTimeMillis()
                val txType = if (debt.type == DebtType.LENT) {
                    TransactionType.INCOME
                } else {
                    TransactionType.EXPENSE
                }
                val prefix = if (debt.type == DebtType.LENT) {
                    "Alacak tahsilatı: ${debt.personName}"
                } else {
                    "Borç ödemesi: ${debt.personName}"
                }
                val fullNote = if (cleanNote.isNotEmpty()) "$prefix ($cleanNote)" else prefix
                transactionDao.insert(
                    TransactionEntity(
                        type = txType,
                        amountKurus = amountKurus,
                        accountId = accountId,
                        dateMillis = dateMillis,
                        note = fullNote,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            } else {
                null
            }

            debtDao.insertPayment(
                DebtPaymentEntity(
                    debtId = debtId,
                    amountKurus = amountKurus,
                    dateMillis = dateMillis,
                    accountId = accountId,
                    linkedTransactionId = linkedTxId,
                    note = cleanNote
                )
            )
            val totalPaid = debtDao.getTotalPaidForDebt(debtId)
            if (totalPaid >= debt.totalAmountKurus && !debt.isSettled) {
                debtDao.updateDebt(debt.copy(isSettled = true))
            }
        }
    }

    suspend fun deleteDebtPayment(paymentId: Long) =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val payment = debtDao.getPaymentById(paymentId) ?: return@withTransaction
                val linkedTxId = payment.linkedTransactionId
                debtDao.deletePaymentById(paymentId)
                if (linkedTxId != null) {
                    transactionDao.deleteById(linkedTxId)
                }
                val debt = debtDao.getDebtById(payment.debtId) ?: return@withTransaction
                val totalPaid = debtDao.getTotalPaidForDebt(debt.id)
                debtDao.updateDebt(debt.copy(isSettled = totalPaid >= debt.totalAmountKurus))
            }
        }

    suspend fun setDebtSettled(debtId: Long, settled: Boolean) =
        withContext(Dispatchers.IO) {
            val debt = debtDao.getDebtById(debtId) ?: return@withContext
            debtDao.updateDebt(debt.copy(isSettled = settled))
        }

    suspend fun deleteDebt(debtId: Long) =
        withContext(Dispatchers.IO) {
            debtDao.deleteDebtById(debtId)
        }

    // Recurring Rules
    fun observeAllRecurringRules(): Flow<List<RecurringRuleWithDetails>> =
        recurringRuleDao.observeAllWithDetails()

    fun observeDueRecurringRules(upToEpochDay: Long = LocalDate.now().toEpochDay()): Flow<List<RecurringRuleWithDetails>> =
        recurringRuleDao.observeDueRules(upToEpochDay)

    suspend fun saveRecurringRule(rule: RecurringRuleEntity): Long =
        withContext(Dispatchers.IO) {
            require(rule.title.isNotBlank()) { "Başlık boş olamaz" }
            require(rule.amountKurus > 0L) { "Tutar sıfırdan büyük olmalı" }
            if (rule.id == 0L) {
                recurringRuleDao.insert(rule)
            } else {
                recurringRuleDao.update(rule)
                rule.id
            }
        }

    suspend fun setRecurringRuleActive(ruleId: Long, active: Boolean) =
        withContext(Dispatchers.IO) {
            recurringRuleDao.setActive(ruleId, active)
        }

    suspend fun deleteRecurringRule(ruleId: Long) =
        withContext(Dispatchers.IO) {
            recurringRuleDao.deleteById(ruleId)
        }

    suspend fun executeRecurringRule(ruleId: Long) =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val rule = recurringRuleDao.getById(ruleId) ?: return@withTransaction
                val dueDate = LocalDate.ofEpochDay(rule.nextDueDateEpochDay)
                val dateMillis = dueDate.atTime(12, 0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                val txNote = if (rule.note.isNotBlank()) {
                    "${rule.title} · ${rule.note}"
                } else {
                    rule.title
                }
                val now = System.currentTimeMillis()
                transactionDao.insert(
                    TransactionEntity(
                        type = rule.type,
                        amountKurus = rule.amountKurus,
                        accountId = rule.accountId,
                        categoryId = rule.categoryId,
                        dateMillis = dateMillis,
                        note = txNote,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                val nextDate = RecurrenceCalculator.nextDueDate(
                    currentDue = dueDate,
                    frequency = rule.frequency,
                    preferredDayOfMonth = rule.dayOfMonth
                )
                recurringRuleDao.updateNextDueDate(rule.id, nextDate.toEpochDay())
            }
        }

    suspend fun skipRecurringRuleOccurrence(ruleId: Long) =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val rule = recurringRuleDao.getById(ruleId) ?: return@withTransaction
                val currentDue = LocalDate.ofEpochDay(rule.nextDueDateEpochDay)
                val nextDate = RecurrenceCalculator.nextDueDate(
                    currentDue = currentDue,
                    frequency = rule.frequency,
                    preferredDayOfMonth = rule.dayOfMonth
                )
                recurringRuleDao.updateNextDueDate(rule.id, nextDate.toEpochDay())
            }
        }

    // Investments
    fun observeInvestments(): Flow<List<InvestmentEntity>> =
        investmentDao.observeAll()

    suspend fun saveInvestment(entity: InvestmentEntity): Long =
        withContext(Dispatchers.IO) {
            require(entity.name.isNotBlank()) { "Varlık adı boş olamaz" }
            require(entity.symbol.isNotBlank()) { "Sembol boş olamaz" }
            if (entity.id == 0L) {
                investmentDao.insert(entity)
            } else {
                investmentDao.update(entity)
                entity.id
            }
        }

    suspend fun addBuyLot(
        investmentId: Long,
        buyQty: Double,
        buyUnitPriceTry: Double
    ) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val existing = investmentDao.getById(investmentId) ?: return@withTransaction
            val (newQty, newAvgCost) = InvestmentCalculator.applyBuyLot(
                currentQty = existing.quantity,
                currentAvgCostTry = existing.averageCostTry,
                buyQty = buyQty,
                buyUnitPriceTry = buyUnitPriceTry
            )
            investmentDao.update(
                existing.copy(
                    quantity = newQty,
                    averageCostTry = newAvgCost
                )
            )
        }
    }

    suspend fun applySellLot(
        investmentId: Long,
        sellQty: Double
    ) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val existing = investmentDao.getById(investmentId) ?: return@withTransaction
            val newQty = InvestmentCalculator.applySellLot(existing.quantity, sellQty)
            investmentDao.update(existing.copy(quantity = newQty))
        }
    }

    suspend fun updateManualPrice(
        investmentId: Long,
        newPriceTry: Double
    ) = withContext(Dispatchers.IO) {
        require(newPriceTry >= 0.0) { "Fiyat negatif olamaz" }
        val existing = investmentDao.getById(investmentId) ?: return@withContext
        investmentDao.update(
            existing.copy(
                lastPriceTry = newPriceTry,
                priceSource = "Manuel",
                isManualPrice = true,
                lastUpdatedMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteInvestment(id: Long) =
        withContext(Dispatchers.IO) {
            investmentDao.deleteById(id)
        }

    suspend fun refreshInvestmentPrices(force: Boolean = false): MarketRefreshResult =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val all = investmentDao.getAllOnce()
            val refreshable = all.filter { it.assetType != InvestmentAssetType.TERM_DEPOSIT }
            if (refreshable.isEmpty()) {
                return@withContext MarketRefreshResult(0, emptyList(), now)
            }

            val staleWindowMillis = 15L * 60L * 1000L
            val targets = if (force) {
                refreshable
            } else {
                refreshable.filter {
                    !it.isManualPrice &&
                        (it.lastUpdatedMillis == null || (now - it.lastUpdatedMillis) >= staleWindowMillis)
                }
            }

            if (targets.isEmpty()) {
                return@withContext MarketRefreshResult(0, emptyList(), now)
            }

            val (quotesById, failedSymbols) = marketDataService.fetchQuotesFor(targets)
            val updatedAt = System.currentTimeMillis()
            database.withTransaction {
                quotesById.forEach { (id, quote) ->
                    investmentDao.updateMarketPrice(
                        id = id,
                        priceTry = quote.priceTry,
                        dailyChangePercent = quote.dailyChangePercent,
                        source = quote.source,
                        updatedAt = updatedAt
                    )
                }
            }

            MarketRefreshResult(
                updatedCount = quotesById.size,
                failedSymbols = failedSymbols,
                timestampMillis = updatedAt
            )
        }

    // Budgets
    fun observeBudgets(): Flow<List<BudgetEntity>> =
        budgetDao.observeAll()

    suspend fun saveBudget(budget: BudgetEntity): Long =
        withContext(Dispatchers.IO) {
            require(budget.monthlyLimitKurus > 0L) { "Aylık bütçe limiti sıfırdan büyük olmalı" }
            val cleanThreshold = budget.alertThresholdPercent.coerceIn(50, 100)
            database.withTransaction {
                if (budget.id == 0L) {
                    val existing = if (budget.categoryId == null) {
                        budgetDao.getOverallBudget()
                    } else {
                        budgetDao.getByCategoryId(budget.categoryId)
                    }
                    if (existing != null) {
                        budgetDao.update(
                            existing.copy(
                                monthlyLimitKurus = budget.monthlyLimitKurus,
                                alertThresholdPercent = cleanThreshold,
                                isActive = budget.isActive
                            )
                        )
                        existing.id
                    } else {
                        budgetDao.insert(budget.copy(alertThresholdPercent = cleanThreshold))
                    }
                } else {
                    budgetDao.update(budget.copy(alertThresholdPercent = cleanThreshold))
                    budget.id
                }
            }
        }

    suspend fun setBudgetActive(id: Long, active: Boolean) =
        withContext(Dispatchers.IO) {
            budgetDao.setActive(id, active)
        }

    suspend fun deleteBudget(id: Long) =
        withContext(Dispatchers.IO) {
            budgetDao.deleteById(id)
        }
}
