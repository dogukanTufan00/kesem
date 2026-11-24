package io.github.dogukantufan00.kesem

import io.github.dogukantufan00.kesem.data.backup.BackupSettings
import io.github.dogukantufan00.kesem.data.backup.KesemBackupCodec
import io.github.dogukantufan00.kesem.data.backup.KesemBackupSnapshot
import io.github.dogukantufan00.kesem.data.local.entity.AccountEntity
import io.github.dogukantufan00.kesem.data.local.entity.BudgetEntity
import io.github.dogukantufan00.kesem.data.local.entity.CategoryEntity
import io.github.dogukantufan00.kesem.data.local.entity.DebtEntity
import io.github.dogukantufan00.kesem.data.local.entity.DebtPaymentEntity
import io.github.dogukantufan00.kesem.data.local.entity.InvestmentEntity
import io.github.dogukantufan00.kesem.data.local.entity.RecurringRuleEntity
import io.github.dogukantufan00.kesem.data.local.entity.TransactionEntity
import io.github.dogukantufan00.kesem.model.AccountType
import io.github.dogukantufan00.kesem.model.CategoryType
import io.github.dogukantufan00.kesem.model.DebtType
import io.github.dogukantufan00.kesem.model.InvestmentAssetType
import io.github.dogukantufan00.kesem.model.RecurrenceFrequency
import io.github.dogukantufan00.kesem.model.ThemeMode
import io.github.dogukantufan00.kesem.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KesemBackupCodecTest {

    @Test
    fun `encode and decode round-trips all 8 tables settings and nullable fields`() {
        val snapshot = KesemBackupSnapshot(
            schemaVersion = 4,
            exportedAtMillis = 1_763_650_000_000L,
            appVersion = "1.0.0",
            settings = BackupSettings(monthStartDay = 15, hideBalances = true, themeMode = ThemeMode.DARK),
            accounts = listOf(
                AccountEntity(
                    id = 1L,
                    name = "Maaş Bankası",
                    type = AccountType.BANK,
                    bankName = "Garanti BBVA",
                    initialBalanceKurus = 2_500_000L,
                    colorHex = 0xFF0D4A3EL,
                    iconKey = "account_balance",
                    includeInTotal = true,
                    creditLimitKurus = null,
                    statementDay = null,
                    paymentDueDay = null
                )
            ),
            categories = listOf(
                CategoryEntity(
                    id = 2L,
                    name = "Market",
                    type = CategoryType.EXPENSE,
                    iconKey = "shopping_cart",
                    colorHex = 0xFF16A34AL,
                    isDefault = true,
                    sortOrder = 1
                )
            ),
            transactions = listOf(
                TransactionEntity(
                    id = 10L,
                    type = TransactionType.EXPENSE,
                    amountKurus = 45_050L,
                    accountId = 1L,
                    toAccountId = null,
                    categoryId = 2L,
                    dateMillis = 1_763_600_000_000L,
                    note = "Haftalık alışveriş",
                    tagsCsv = "ev,mutfak",
                    receiptPath = null,
                    installmentGroupId = null,
                    installmentIndex = null,
                    installmentCount = null
                )
            ),
            debts = listOf(
                DebtEntity(
                    id = 5L,
                    personName = "Ahmet",
                    type = DebtType.LENT,
                    totalAmountKurus = 100_000L,
                    dueDateMillis = null,
                    note = "Kahve",
                    isSettled = false
                )
            ),
            debtPayments = listOf(
                DebtPaymentEntity(
                    id = 6L,
                    debtId = 5L,
                    amountKurus = 50_000L,
                    dateMillis = 1_763_610_000_000L,
                    accountId = 1L,
                    linkedTransactionId = null,
                    note = "Yarısı"
                )
            ),
            recurringRules = listOf(
                RecurringRuleEntity(
                    id = 7L,
                    title = "Kira",
                    type = TransactionType.EXPENSE,
                    amountKurus = 1_800_000L,
                    accountId = 1L,
                    categoryId = 2L,
                    frequency = RecurrenceFrequency.MONTHLY,
                    dayOfMonth = 1,
                    nextDueDateEpochDay = 20_400L,
                    isActive = true,
                    note = ""
                )
            ),
            investments = listOf(
                InvestmentEntity(
                    id = 8L,
                    assetType = InvestmentAssetType.GOLD,
                    symbol = "GRAM_ALTIN",
                    name = "Gram Altın",
                    quantity = 5.5,
                    averageCostTry = 2800.0,
                    lastPriceTry = 3150.25,
                    dailyChangePercent = 1.25,
                    lastUpdatedMillis = 1_763_640_000_000L,
                    priceSource = "Truncgil"
                )
            ),
            budgets = listOf(
                BudgetEntity(
                    id = 9L,
                    categoryId = null,
                    monthlyLimitKurus = 2_000_000L,
                    alertThresholdPercent = 85,
                    isActive = true
                ),
                BudgetEntity(
                    id = 10L,
                    categoryId = 2L,
                    monthlyLimitKurus = 500_000L,
                    alertThresholdPercent = 80,
                    isActive = true
                )
            )
        )

        val encoded = KesemBackupCodec.encode(snapshot)
        val decoded = KesemBackupCodec.decode(encoded)

        assertEquals(snapshot.settings, decoded.settings)
        assertEquals(1, decoded.accounts.size)
        assertNull(decoded.accounts[0].creditLimitKurus)
        assertEquals("Garanti BBVA", decoded.accounts[0].bankName)

        assertEquals(1, decoded.transactions.size)
        assertNull(decoded.transactions[0].toAccountId)
        assertNull(decoded.transactions[0].receiptPath)
        assertEquals("ev,mutfak", decoded.transactions[0].tagsCsv)

        assertEquals(1, decoded.debts.size)
        assertNull(decoded.debts[0].dueDateMillis)
        assertEquals(1, decoded.debtPayments.size)
        assertNull(decoded.debtPayments[0].linkedTransactionId)

        assertEquals(2, decoded.budgets.size)
        assertNull(decoded.budgets[0].categoryId)
        assertEquals(2L, decoded.budgets[1].categoryId)

        val summary = KesemBackupCodec.summarize(decoded)
        assertEquals(1, summary.accountsCount)
        assertEquals(1, summary.transactionsCount)
        assertEquals(2, summary.budgetsCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decode rejects non-kesem json payload`() {
        KesemBackupCodec.decode("""{"app":"OtherWallet","schemaVersion":1}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decode rejects malformed json string`() {
        KesemBackupCodec.decode("{not-valid-json")
    }
}
