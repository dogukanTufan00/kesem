package io.github.dogukantufan00.kesem.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.dogukantufan00.kesem.model.InvestmentAssetType

@Entity(
    tableName = "investments",
    indices = [Index(value = ["assetType"])]
)
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val assetType: InvestmentAssetType,
    val symbol: String,
    val name: String,
    val quantity: Double,
    val averageCostTry: Double,
    val lastPriceTry: Double,
    val dailyChangePercent: Double? = null,
    val lastUpdatedMillis: Long? = null,
    val priceSource: String = "Manuel",
    val isManualPrice: Boolean = false,
    val includeInTotal: Boolean = true,
    val annualInterestRate: Double? = null,
    val withholdingTaxRate: Double? = null,
    val startEpochDay: Long? = null,
    val maturityEpochDay: Long? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
