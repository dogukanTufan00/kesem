package io.github.dogukantufan00.kesem.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.dogukantufan00.kesem.model.RecurrenceFrequency
import io.github.dogukantufan00.kesem.model.TransactionType

@Entity(
    tableName = "recurring_rules",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["nextDueDateEpochDay"])
    ]
)
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val type: TransactionType,
    val amountKurus: Long,
    val accountId: Long,
    val categoryId: Long? = null,
    val frequency: RecurrenceFrequency,
    val dayOfMonth: Int,
    val nextDueDateEpochDay: Long,
    val isActive: Boolean = true,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
