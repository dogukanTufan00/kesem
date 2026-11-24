package io.github.dogukantufan00.kesem.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "debt_payments",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedTransactionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["debtId"]),
        Index(value = ["linkedTransactionId"])
    ]
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val debtId: Long,
    val amountKurus: Long,
    val dateMillis: Long,
    val accountId: Long? = null,
    val linkedTransactionId: Long? = null,
    val note: String = ""
)
