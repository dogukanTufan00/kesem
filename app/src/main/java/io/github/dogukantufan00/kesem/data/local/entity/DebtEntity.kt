package io.github.dogukantufan00.kesem.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.dogukantufan00.kesem.model.DebtType

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val personName: String,
    val type: DebtType,
    val totalAmountKurus: Long,
    val dueDateMillis: Long? = null,
    val note: String = "",
    val isSettled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
