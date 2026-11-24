package io.github.dogukantufan00.kesem.data.local

import androidx.room.TypeConverter
import io.github.dogukantufan00.kesem.model.AccountType
import io.github.dogukantufan00.kesem.model.CategoryType
import io.github.dogukantufan00.kesem.model.DebtType
import io.github.dogukantufan00.kesem.model.InvestmentAssetType
import io.github.dogukantufan00.kesem.model.RecurrenceFrequency
import io.github.dogukantufan00.kesem.model.TransactionType

class Converters {
    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter
    fun fromCategoryType(value: CategoryType): String = value.name

    @TypeConverter
    fun toCategoryType(value: String): CategoryType = CategoryType.valueOf(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromDebtType(value: DebtType): String = value.name

    @TypeConverter
    fun toDebtType(value: String): DebtType = DebtType.valueOf(value)

    @TypeConverter
    fun fromRecurrenceFrequency(value: RecurrenceFrequency): String = value.name

    @TypeConverter
    fun toRecurrenceFrequency(value: String): RecurrenceFrequency = RecurrenceFrequency.valueOf(value)

    @TypeConverter
    fun fromInvestmentAssetType(value: InvestmentAssetType): String = value.name

    @TypeConverter
    fun toInvestmentAssetType(value: String): InvestmentAssetType = InvestmentAssetType.valueOf(value)
}
