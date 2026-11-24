package io.github.dogukantufan00.kesem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.dogukantufan00.kesem.data.local.entity.CategoryEntity
import io.github.dogukantufan00.kesem.model.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder ASC, id ASC")
    fun observeByType(type: CategoryType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY type ASC, sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getAllOnce(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("UPDATE categories SET colorHex = :newHex WHERE colorHex = :oldHex")
    suspend fun replaceColorHex(oldHex: Long, newHex: Long)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int
}
