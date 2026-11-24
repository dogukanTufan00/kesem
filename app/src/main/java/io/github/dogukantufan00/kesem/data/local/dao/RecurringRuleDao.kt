package io.github.dogukantufan00.kesem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.dogukantufan00.kesem.data.local.entity.RecurringRuleEntity
import io.github.dogukantufan00.kesem.model.RecurringRuleWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringRuleDao {

    @Query(
        """
        SELECT
          r.*,
          a.name AS accountName,
          a.colorHex AS accountColorHex,
          c.name AS categoryName,
          c.iconKey AS categoryIconKey,
          c.colorHex AS categoryColorHex
        FROM recurring_rules r
        INNER JOIN accounts a ON r.accountId = a.id
        LEFT JOIN categories c ON r.categoryId = c.id
        ORDER BY r.isActive DESC, r.nextDueDateEpochDay ASC, r.id ASC
        """
    )
    fun observeAllWithDetails(): Flow<List<RecurringRuleWithDetails>>

    @Query(
        """
        SELECT
          r.*,
          a.name AS accountName,
          a.colorHex AS accountColorHex,
          c.name AS categoryName,
          c.iconKey AS categoryIconKey,
          c.colorHex AS categoryColorHex
        FROM recurring_rules r
        INNER JOIN accounts a ON r.accountId = a.id
        LEFT JOIN categories c ON r.categoryId = c.id
        WHERE r.isActive = 1 AND r.nextDueDateEpochDay <= :upToEpochDay
        ORDER BY r.nextDueDateEpochDay ASC, r.id ASC
        """
    )
    fun observeDueRules(upToEpochDay: Long): Flow<List<RecurringRuleWithDetails>>

    @Query("SELECT * FROM recurring_rules ORDER BY id ASC")
    suspend fun getAllOnce(): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RecurringRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RecurringRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<RecurringRuleEntity>)

    @Update
    suspend fun update(rule: RecurringRuleEntity)

    @Query("UPDATE recurring_rules SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("UPDATE recurring_rules SET nextDueDateEpochDay = :nextEpochDay WHERE id = :id")
    suspend fun updateNextDueDate(id: Long, nextEpochDay: Long)

    @Query("DELETE FROM recurring_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recurring_rules")
    suspend fun deleteAll()
}
