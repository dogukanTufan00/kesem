package io.github.dogukantufan00.kesem.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.dogukantufan00.kesem.model.TransactionType

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.SET_NULL
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
        Index(value = ["toAccountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["dateMillis"]),
        Index(value = ["type"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: TransactionType,
    val amountKurus: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val dateMillis: Long,
    val note: String = "",
    val tagsCsv: String = "",
    val receiptPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val installmentGroupId: String? = null,
    val installmentIndex: Int? = null,
    val installmentCount: Int? = null
)
