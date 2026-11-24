package io.github.dogukantufan00.kesem.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import io.github.dogukantufan00.kesem.data.local.KesemDatabase
import io.github.dogukantufan00.kesem.data.preferences.SettingsRepository
import io.github.dogukantufan00.kesem.model.BackupSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupManager(
    private val context: Context,
    private val database: KesemDatabase,
    private val settingsRepository: SettingsRepository
) {

    suspend fun exportJsonToUri(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val snapshot = database.withTransaction {
            val now = System.currentTimeMillis()
            val settings = settingsRepository.getBackupSettingsOnce()
            KesemBackupSnapshot(
                schemaVersion = KesemBackupCodec.CURRENT_SCHEMA_VERSION,
                exportedAtMillis = now,
                appVersion = "1.0.0",
                settings = settings,
                accounts = database.accountDao().getAllOnce(),
                categories = database.categoryDao().getAllOnce(),
                transactions = database.transactionDao().getAllOnce(),
                debts = database.debtDao().getAllDebtsOnce(),
                debtPayments = database.debtDao().getAllPaymentsOnce(),
                recurringRules = database.recurringRuleDao().getAllOnce(),
                investments = database.investmentDao().getAllOnce(),
                budgets = database.budgetDao().getAllOnce()
            )
        }

        val jsonText = KesemBackupCodec.encode(snapshot)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(jsonText.toByteArray(Charsets.UTF_8))
            out.flush()
        } ?: error("Yedek dosyası yazılamadı")

        KesemBackupCodec.summarize(snapshot)
    }

    suspend fun readAndParseBackupUri(uri: Uri): Pair<KesemBackupSnapshot, BackupSummary> =
        withContext(Dispatchers.IO) {
            val rawJson = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("Yedek dosyası okunamadı")

            val snapshot = KesemBackupCodec.decode(rawJson)
            snapshot to KesemBackupCodec.summarize(snapshot)
        }

    suspend fun restoreSnapshot(snapshot: KesemBackupSnapshot): BackupSummary =
        withContext(Dispatchers.IO) {
            val accountIds = snapshot.accounts.map { it.id }.toSet()
            val categoryIds = snapshot.categories.map { it.id }.toSet()
            val debtIds = snapshot.debts.map { it.id }.toSet()

            val cleanTransactions = snapshot.transactions
                .filter { it.accountId in accountIds }
                .map { tx ->
                    tx.copy(
                        toAccountId = tx.toAccountId?.takeIf { it in accountIds },
                        categoryId = tx.categoryId?.takeIf { it in categoryIds }
                    )
                }
            val cleanTxIds = cleanTransactions.map { it.id }.toSet()

            val cleanDebtPayments = snapshot.debtPayments
                .filter { it.debtId in debtIds }
                .map { payment ->
                    payment.copy(
                        accountId = payment.accountId?.takeIf { it in accountIds },
                        linkedTransactionId = payment.linkedTransactionId?.takeIf { it in cleanTxIds }
                    )
                }

            val cleanRecurringRules = snapshot.recurringRules
                .filter { it.accountId in accountIds }
                .map { rule ->
                    rule.copy(categoryId = rule.categoryId?.takeIf { it in categoryIds })
                }

            val seenBudgetCat = HashSet<Long?>()
            val cleanBudgets = snapshot.budgets
                .filter { it.categoryId == null || it.categoryId in categoryIds }
                .filter { seenBudgetCat.add(it.categoryId) }

            database.withTransaction {
                database.debtDao().deleteAllPayments()
                database.budgetDao().deleteAll()
                database.recurringRuleDao().deleteAll()
                database.transactionDao().deleteAll()
                database.debtDao().deleteAllDebts()
                database.investmentDao().deleteAll()
                database.categoryDao().deleteAll()
                database.accountDao().deleteAll()

                if (snapshot.accounts.isNotEmpty()) {
                    database.accountDao().insertAll(snapshot.accounts)
                }
                if (snapshot.categories.isNotEmpty()) {
                    database.categoryDao().insertAll(snapshot.categories)
                }
                if (cleanTransactions.isNotEmpty()) {
                    database.transactionDao().insertAll(cleanTransactions)
                }
                if (snapshot.debts.isNotEmpty()) {
                    database.debtDao().insertDebts(snapshot.debts)
                }
                if (cleanDebtPayments.isNotEmpty()) {
                    database.debtDao().insertPayments(cleanDebtPayments)
                }
                if (cleanRecurringRules.isNotEmpty()) {
                    database.recurringRuleDao().insertAll(cleanRecurringRules)
                }
                if (snapshot.investments.isNotEmpty()) {
                    database.investmentDao().insertAll(snapshot.investments)
                }
                if (cleanBudgets.isNotEmpty()) {
                    database.budgetDao().insertAll(cleanBudgets)
                }
            }

            settingsRepository.restoreBackupSettings(snapshot.settings)
            KesemBackupCodec.summarize(
                snapshot.copy(
                    transactions = cleanTransactions,
                    debtPayments = cleanDebtPayments,
                    recurringRules = cleanRecurringRules,
                    budgets = cleanBudgets
                )
            )
        }

    suspend fun exportCsvToUri(uri: Uri): Int = withContext(Dispatchers.IO) {
        val transactions = database.transactionDao().getAllWithDetailsOnce()
        val csvText = KesemCsvExporter.exportTransactionsCsv(transactions)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(csvText.toByteArray(Charsets.UTF_8))
            out.flush()
        } ?: error("CSV dosyası yazılamadı")
        transactions.size
    }
}
