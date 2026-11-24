package io.github.dogukantufan00.kesem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.dogukantufan00.kesem.data.local.entity.TransactionEntity
import io.github.dogukantufan00.kesem.model.AccountPeriodTotals
import io.github.dogukantufan00.kesem.model.CashFlowPointRaw
import io.github.dogukantufan00.kesem.model.CategorySpendSummary
import io.github.dogukantufan00.kesem.model.PeriodTotals
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.model.TransactionWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query(
        """
        SELECT
          t.*,
          src.name AS accountName,
          src.colorHex AS accountColorHex,
          dst.name AS toAccountName,
          cat.name AS categoryName,
          cat.iconKey AS categoryIconKey,
          cat.colorHex AS categoryColorHex
        FROM transactions t
        INNER JOIN accounts src ON t.accountId = src.id
        LEFT JOIN accounts dst ON t.toAccountId = dst.id
        LEFT JOIN categories cat ON t.categoryId = cat.id
        WHERE t.dateMillis >= :startMillis AND t.dateMillis < :endMillisExclusive
        ORDER BY t.dateMillis DESC, t.id DESC
        """
    )
    fun observeTransactionsWithDetails(
        startMillis: Long,
        endMillisExclusive: Long
    ): Flow<List<TransactionWithDetails>>

    @Query(
        """
        SELECT
          t.*,
          src.name AS accountName,
          src.colorHex AS accountColorHex,
          dst.name AS toAccountName,
          cat.name AS categoryName,
          cat.iconKey AS categoryIconKey,
          cat.colorHex AS categoryColorHex
        FROM transactions t
        INNER JOIN accounts src ON t.accountId = src.id
        LEFT JOIN accounts dst ON t.toAccountId = dst.id
        LEFT JOIN categories cat ON t.categoryId = cat.id
        WHERE t.dateMillis <= :upToMillis
        ORDER BY t.dateMillis DESC, t.id DESC
        LIMIT :limit
        """
    )
    fun observeRecentTransactions(limit: Int, upToMillis: Long): Flow<List<TransactionWithDetails>>

    @Query(
        """
        SELECT
          t.*,
          src.name AS accountName,
          src.colorHex AS accountColorHex,
          dst.name AS toAccountName,
          cat.name AS categoryName,
          cat.iconKey AS categoryIconKey,
          cat.colorHex AS categoryColorHex
        FROM transactions t
        INNER JOIN accounts src ON t.accountId = src.id
        LEFT JOIN accounts dst ON t.toAccountId = dst.id
        LEFT JOIN categories cat ON t.categoryId = cat.id
        ORDER BY t.dateMillis DESC, t.id DESC
        """
    )
    fun observeAllTransactionsWithDetails(): Flow<List<TransactionWithDetails>>

    @Query(
        """
        SELECT
          t.*,
          src.name AS accountName,
          src.colorHex AS accountColorHex,
          dst.name AS toAccountName,
          cat.name AS categoryName,
          cat.iconKey AS categoryIconKey,
          cat.colorHex AS categoryColorHex
        FROM transactions t
        INNER JOIN accounts src ON t.accountId = src.id
        LEFT JOIN accounts dst ON t.toAccountId = dst.id
        LEFT JOIN categories cat ON t.categoryId = cat.id
        WHERE t.accountId = :accountId OR t.toAccountId = :accountId
        ORDER BY t.dateMillis DESC, t.id DESC
        """
    )
    fun observeTransactionsForAccount(accountId: Long): Flow<List<TransactionWithDetails>>

    @Query(
        """
        SELECT
          t.*,
          src.name AS accountName,
          src.colorHex AS accountColorHex,
          dst.name AS toAccountName,
          cat.name AS categoryName,
          cat.iconKey AS categoryIconKey,
          cat.colorHex AS categoryColorHex
        FROM transactions t
        INNER JOIN accounts src ON t.accountId = src.id
        LEFT JOIN accounts dst ON t.toAccountId = dst.id
        LEFT JOIN categories cat ON t.categoryId = cat.id
        WHERE (t.accountId = :accountId OR t.toAccountId = :accountId)
          AND t.dateMillis <= :upToMillis
        ORDER BY t.dateMillis DESC, t.id DESC
        """
    )
    fun observePastTransactionsForAccount(
        accountId: Long,
        upToMillis: Long
    ): Flow<List<TransactionWithDetails>>

    @Query(
        """
        SELECT
          t.*,
          src.name AS accountName,
          src.colorHex AS accountColorHex,
          dst.name AS toAccountName,
          cat.name AS categoryName,
          cat.iconKey AS categoryIconKey,
          cat.colorHex AS categoryColorHex
        FROM transactions t
        INNER JOIN accounts src ON t.accountId = src.id
        LEFT JOIN accounts dst ON t.toAccountId = dst.id
        LEFT JOIN categories cat ON t.categoryId = cat.id
        WHERE t.accountId = :accountId
          AND t.installmentGroupId IS NOT NULL
          AND t.dateMillis > :afterMillis
        ORDER BY t.dateMillis ASC, t.installmentIndex ASC
        """
    )
    fun observeUpcomingInstallmentsForAccount(
        accountId: Long,
        afterMillis: Long
    ): Flow<List<TransactionWithDetails>>

    @Query(
        """
        SELECT
          COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amountKurus ELSE 0 END), 0) AS incomeKurus,
          COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amountKurus ELSE 0 END), 0) AS expenseKurus
        FROM transactions
        WHERE dateMillis >= :startMillis AND dateMillis < :endMillisExclusive
        """
    )
    fun observePeriodTotals(startMillis: Long, endMillisExclusive: Long): Flow<PeriodTotals>

    @Query(
        """
        SELECT
          COALESCE(SUM(
            CASE
              WHEN type = 'INCOME' AND accountId = :accountId THEN amountKurus
              WHEN type = 'TRANSFER' AND toAccountId = :accountId THEN amountKurus
              ELSE 0
            END
          ), 0) AS inflowKurus,
          COALESCE(SUM(
            CASE
              WHEN type = 'EXPENSE' AND accountId = :accountId THEN amountKurus
              WHEN type = 'TRANSFER' AND accountId = :accountId THEN amountKurus
              ELSE 0
            END
          ), 0) AS outflowKurus
        FROM transactions
        WHERE (accountId = :accountId OR toAccountId = :accountId)
          AND dateMillis >= :startMillis AND dateMillis < :endMillisExclusive
        """
    )
    fun observeAccountPeriodTotals(
        accountId: Long,
        startMillis: Long,
        endMillisExclusive: Long
    ): Flow<AccountPeriodTotals>

    @Query(
        """
        SELECT
          t.categoryId AS categoryId,
          COALESCE(cat.name, 'Kategorisiz') AS categoryName,
          COALESCE(cat.iconKey, 'more_horiz') AS iconKey,
          COALESCE(cat.colorHex, 4286410854) AS colorHex,
          COALESCE(SUM(t.amountKurus), 0) AS totalKurus,
          COUNT(t.id) AS transactionCount
        FROM transactions t
        LEFT JOIN categories cat ON t.categoryId = cat.id
        WHERE t.type = :type
          AND t.dateMillis >= :startMillis
          AND t.dateMillis < :endMillisExclusive
        GROUP BY t.categoryId, cat.name, cat.iconKey, cat.colorHex
        ORDER BY totalKurus DESC
        """
    )
    fun observeCategoryBreakdown(
        type: TransactionType,
        startMillis: Long,
        endMillisExclusive: Long
    ): Flow<List<CategorySpendSummary>>

    @Query("SELECT DISTINCT tagsCsv FROM transactions WHERE tagsCsv != '' ORDER BY updatedAt DESC LIMIT 50")
    fun observeDistinctTagsCsv(): Flow<List<String>>

    @Query(
        """
        SELECT id, type, amountKurus, dateMillis
        FROM transactions
        WHERE type IN ('INCOME', 'EXPENSE')
          AND dateMillis >= :startMillis
          AND dateMillis < :endMillisExclusive
        ORDER BY dateMillis ASC, id ASC
        """
    )
    fun observeIncomeExpenseInRange(
        startMillis: Long,
        endMillisExclusive: Long
    ): Flow<List<CashFlowPointRaw>>

    @Query("SELECT * FROM transactions ORDER BY id ASC")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Query(
        """
        SELECT
          t.*,
          src.name AS accountName,
          src.colorHex AS accountColorHex,
          dst.name AS toAccountName,
          cat.name AS categoryName,
          cat.iconKey AS categoryIconKey,
          cat.colorHex AS categoryColorHex
        FROM transactions t
        INNER JOIN accounts src ON t.accountId = src.id
        LEFT JOIN accounts dst ON t.toAccountId = dst.id
        LEFT JOIN categories cat ON t.categoryId = cat.id
        ORDER BY t.dateMillis DESC, t.id DESC
        """
    )
    suspend fun getAllWithDetailsOnce(): List<TransactionWithDetails>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT receiptPath FROM transactions WHERE accountId = :accountId AND receiptPath IS NOT NULL")
    suspend fun getReceiptPathsForAccount(accountId: Long): List<String>

    @Query("SELECT receiptPath FROM transactions WHERE installmentGroupId = :groupId AND receiptPath IS NOT NULL")
    suspend fun getReceiptPathsByInstallmentGroup(groupId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE installmentGroupId = :groupId")
    suspend fun deleteByInstallmentGroup(groupId: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
