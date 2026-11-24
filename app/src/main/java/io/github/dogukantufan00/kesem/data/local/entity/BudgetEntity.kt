package io.github.dogukantufan00.kesem.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val categoryId: Long? = null,
    val monthlyLimitKurus: Long,
    val alertThresholdPercent: Int = 80,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
