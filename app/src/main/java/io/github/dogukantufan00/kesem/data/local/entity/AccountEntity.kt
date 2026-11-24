package io.github.dogukantufan00.kesem.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.dogukantufan00.kesem.model.AccountType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: AccountType,
    val bankName: String? = null,
    val currency: String = "TRY",
    val initialBalanceKurus: Long = 0L,
    val colorHex: Long,
    val iconKey: String,
    val includeInTotal: Boolean = true,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val creditLimitKurus: Long? = null,
    val statementDay: Int? = null,
    val paymentDueDay: Int? = null
)
