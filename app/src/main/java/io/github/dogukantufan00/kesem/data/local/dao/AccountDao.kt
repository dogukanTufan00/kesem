package io.github.dogukantufan00.kesem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.dogukantufan00.kesem.data.local.entity.AccountEntity
import io.github.dogukantufan00.kesem.model.AccountWithBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query(
        """
        SELECT a.*, (
          a.initialBalanceKurus + COALESCE((
            SELECT SUM(
              CASE
                WHEN t.type = 'INCOME' AND t.accountId = a.id THEN t.amountKurus
                WHEN t.type = 'EXPENSE' AND t.accountId = a.id THEN -t.amountKurus
                WHEN t.type = 'TRANSFER' AND t.accountId = a.id THEN -t.amountKurus
                WHEN t.type = 'TRANSFER' AND t.toAccountId = a.id THEN t.amountKurus
                WHEN t.type = 'ADJUSTMENT_IN' AND t.accountId = a.id THEN t.amountKurus
                WHEN t.type = 'ADJUSTMENT_OUT' AND t.accountId = a.id THEN -t.amountKurus
                ELSE 0
              END
            )
            FROM transactions t
            WHERE t.accountId = a.id OR t.toAccountId = a.id
          ), 0)
        ) AS currentBalanceKurus
        FROM accounts a
        WHERE a.isArchived = :archived
        ORDER BY a.type ASC, a.sortOrder ASC, a.id ASC
        """
    )
    fun observeAccountsWithBalance(archived: Boolean = false): Flow<List<AccountWithBalance>>

    @Query(
        """
        SELECT a.*, (
          a.initialBalanceKurus + COALESCE((
            SELECT SUM(
              CASE
                WHEN t.type = 'INCOME' AND t.accountId = a.id THEN t.amountKurus
                WHEN t.type = 'EXPENSE' AND t.accountId = a.id THEN -t.amountKurus
                WHEN t.type = 'TRANSFER' AND t.accountId = a.id THEN -t.amountKurus
                WHEN t.type = 'TRANSFER' AND t.toAccountId = a.id THEN t.amountKurus
                WHEN t.type = 'ADJUSTMENT_IN' AND t.accountId = a.id THEN t.amountKurus
                WHEN t.type = 'ADJUSTMENT_OUT' AND t.accountId = a.id THEN -t.amountKurus
                ELSE 0
              END
            )
            FROM transactions t
            WHERE t.accountId = a.id OR t.toAccountId = a.id
          ), 0)
        ) AS currentBalanceKurus
        FROM accounts a
        WHERE a.id = :accountId
        LIMIT 1
        """
    )
    fun observeAccountWithBalance(accountId: Long): Flow<AccountWithBalance?>

    @Query("SELECT * FROM accounts WHERE id = :accountId LIMIT 1")
    suspend fun getById(accountId: Long): AccountEntity?

    @Query(
        """
        SELECT COALESCE(SUM(
          CASE
            WHEN t.type = 'INCOME' AND t.accountId = :accountId THEN t.amountKurus
            WHEN t.type = 'EXPENSE' AND t.accountId = :accountId THEN -t.amountKurus
            WHEN t.type = 'TRANSFER' AND t.accountId = :accountId THEN -t.amountKurus
            WHEN t.type = 'TRANSFER' AND t.toAccountId = :accountId THEN t.amountKurus
            WHEN t.type = 'ADJUSTMENT_IN' AND t.accountId = :accountId THEN t.amountKurus
            WHEN t.type = 'ADJUSTMENT_OUT' AND t.accountId = :accountId THEN -t.amountKurus
            ELSE 0
          END
        ), 0)
        FROM transactions t
        WHERE t.accountId = :accountId OR t.toAccountId = :accountId
        """
    )
    suspend fun getTransactionDeltaForAccount(accountId: Long): Long

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId")
    suspend fun countTransactionsForAccount(accountId: Long): Int

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    suspend fun getAllOnce(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE accounts SET isArchived = :archived WHERE id = :accountId")
    suspend fun setArchived(accountId: Long, archived: Boolean)

    @Query("UPDATE accounts SET colorHex = :newHex WHERE colorHex = :oldHex")
    suspend fun replaceColorHex(oldHex: Long, newHex: Long)

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteById(accountId: Long)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun countAccounts(): Int
}
