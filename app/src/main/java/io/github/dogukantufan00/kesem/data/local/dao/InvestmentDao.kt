package io.github.dogukantufan00.kesem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.dogukantufan00.kesem.data.local.entity.InvestmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {

    @Query("SELECT * FROM investments ORDER BY assetType ASC, id DESC")
    fun observeAll(): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments ORDER BY assetType ASC, id DESC")
    suspend fun getAllOnce(): List<InvestmentEntity>

    @Query("SELECT * FROM investments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): InvestmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: InvestmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InvestmentEntity>)

    @Update
    suspend fun update(entity: InvestmentEntity)

    @Query(
        """
        UPDATE investments
        SET lastPriceTry = :priceTry,
            dailyChangePercent = :dailyChangePercent,
            priceSource = :source,
            lastUpdatedMillis = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateMarketPrice(
        id: Long,
        priceTry: Double,
        dailyChangePercent: Double?,
        source: String,
        updatedAt: Long
    )

    @Query("DELETE FROM investments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM investments")
    suspend fun deleteAll()
}
