package io.github.dogukantufan00.kesem.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.dogukantufan00.kesem.model.CategoryType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: CategoryType,
    val iconKey: String,
    val colorHex: Long,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)
