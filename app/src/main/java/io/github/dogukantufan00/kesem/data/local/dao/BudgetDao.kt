package io.github.dogukantufan00.kesem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.dogukantufan00.kesem.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets ORDER BY CASE WHEN categoryId IS NULL THEN 0 ELSE 1 END, id ASC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY CASE WHEN categoryId IS NULL THEN 0 ELSE 1 END, id ASC")
    suspend fun getAllOnce(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE categoryId IS NULL LIMIT 1")
    suspend fun getOverallBudget(): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId LIMIT 1")
    suspend fun getByCategoryId(categoryId: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("UPDATE budgets SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}
