package io.github.dogukantufan00.kesem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.dogukantufan00.kesem.data.local.entity.DebtEntity
import io.github.dogukantufan00.kesem.data.local.entity.DebtPaymentEntity
import io.github.dogukantufan00.kesem.model.DebtWithSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Query(
        """
        SELECT d.*, COALESCE(SUM(p.amountKurus), 0) AS paidAmountKurus
        FROM debts d
        LEFT JOIN debt_payments p ON p.debtId = d.id
        GROUP BY d.id
        ORDER BY d.isSettled ASC, COALESCE(d.dueDateMillis, 9223372036854775807) ASC, d.id DESC
        """
    )
    fun observeDebtsWithSummary(): Flow<List<DebtWithSummary>>

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY dateMillis DESC, id DESC")
    fun observePaymentsForDebt(debtId: Long): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
    suspend fun getDebtById(id: Long): DebtEntity?

    @Query("SELECT COALESCE(SUM(amountKurus), 0) FROM debt_payments WHERE debtId = :debtId")
    suspend fun getTotalPaidForDebt(debtId: Long): Long

    @Query("SELECT * FROM debt_payments WHERE id = :paymentId LIMIT 1")
    suspend fun getPaymentById(paymentId: Long): DebtPaymentEntity?

    @Query("SELECT linkedTransactionId FROM debt_payments WHERE debtId = :debtId AND linkedTransactionId IS NOT NULL")
    suspend fun getLinkedTransactionIdsForDebt(debtId: Long): List<Long>

    @Query("SELECT * FROM debts ORDER BY id ASC")
    suspend fun getAllDebtsOnce(): List<DebtEntity>

    @Query("SELECT * FROM debt_payments ORDER BY id ASC")
    suspend fun getAllPaymentsOnce(): List<DebtPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<DebtEntity>)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebtById(id: Long)

    @Query("DELETE FROM debts")
    suspend fun deleteAllDebts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: DebtPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<DebtPaymentEntity>)

    @Query("DELETE FROM debt_payments WHERE id = :paymentId")
    suspend fun deletePaymentById(paymentId: Long)

    @Query("DELETE FROM debt_payments")
    suspend fun deleteAllPayments()
}
