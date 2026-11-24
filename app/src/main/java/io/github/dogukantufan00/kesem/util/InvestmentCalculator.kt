package io.github.dogukantufan00.kesem.util

import io.github.dogukantufan00.kesem.data.local.entity.InvestmentEntity
import io.github.dogukantufan00.kesem.model.InvestmentAssetType
import io.github.dogukantufan00.kesem.model.InvestmentValuation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

object InvestmentCalculator {

    private val TR_SYMBOLS = DecimalFormatSymbols(MoneyFormatter.TR_LOCALE).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    fun evaluate(
        entity: InvestmentEntity,
        today: LocalDate = LocalDate.now()
    ): InvestmentValuation {
        if (entity.assetType == InvestmentAssetType.TERM_DEPOSIT) {
            val principalTry = entity.averageCostTry * entity.quantity.coerceAtLeast(1.0)
            val costBasisKurus = (principalTry * 100.0).roundToLong()
            val rate = (entity.annualInterestRate ?: 0.0).coerceAtLeast(0.0)
            val tax = (entity.withholdingTaxRate ?: 15.0).coerceIn(0.0, 100.0)
            val start = entity.startEpochDay?.let { LocalDate.ofEpochDay(it) } ?: today
            val maturity = entity.maturityEpochDay?.let { LocalDate.ofEpochDay(it) } ?: start.plusDays(32)
            val totalDays = ChronoUnit.DAYS.between(start, maturity).coerceAtLeast(1L)
            val elapsedDays = ChronoUnit.DAYS.between(start, today).coerceIn(0L, totalDays)
            val daysRemaining = ChronoUnit.DAYS.between(today, maturity).coerceAtLeast(0L)

            val netMultiplier = (rate / 100.0) * (1.0 - tax / 100.0)
            val accruedNetKurus = (principalTry * netMultiplier * (elapsedDays.toDouble() / 365.0) * 100.0).roundToLong()
            val maturityNetKurus = (principalTry * netMultiplier * (totalDays.toDouble() / 365.0) * 100.0).roundToLong()

            val currentValueKurus = costBasisKurus + accruedNetKurus
            val profitLossPercent = if (costBasisKurus > 0L) {
                (accruedNetKurus.toDouble() / costBasisKurus.toDouble()) * 100.0
            } else {
                null
            }

            return InvestmentValuation(
                currentValueKurus = currentValueKurus,
                costBasisKurus = costBasisKurus,
                profitLossKurus = accruedNetKurus,
                profitLossPercent = profitLossPercent,
                maturityProjectedNetInterestKurus = maturityNetKurus,
                daysRemaining = daysRemaining
            )
        }

        val currentValueKurus = (entity.quantity * entity.lastPriceTry * 100.0).roundToLong()
        val costBasisKurus = (entity.quantity * entity.averageCostTry * 100.0).roundToLong()
        val profitLossKurus = currentValueKurus - costBasisKurus
        val profitLossPercent = if (costBasisKurus > 0L) {
            (profitLossKurus.toDouble() / costBasisKurus.toDouble()) * 100.0
        } else {
            null
        }

        return InvestmentValuation(
            currentValueKurus = currentValueKurus,
            costBasisKurus = costBasisKurus,
            profitLossKurus = profitLossKurus,
            profitLossPercent = profitLossPercent
        )
    }

    fun applyBuyLot(
        currentQty: Double,
        currentAvgCostTry: Double,
        buyQty: Double,
        buyUnitPriceTry: Double
    ): Pair<Double, Double> {
        require(buyQty > 0.0) { "Alış adedi sıfırdan büyük olmalı" }
        require(buyUnitPriceTry >= 0.0) { "Alış birim fiyatı negatif olamaz" }
        val safeCurrentQty = currentQty.coerceAtLeast(0.0)
        val totalQty = safeCurrentQty + buyQty
        val weightedAvg = if (totalQty > 0.0) {
            ((safeCurrentQty * currentAvgCostTry) + (buyQty * buyUnitPriceTry)) / totalQty
        } else {
            buyUnitPriceTry
        }
        return totalQty to weightedAvg
    }

    fun applySellLot(currentQty: Double, sellQty: Double): Double {
        require(sellQty > 0.0) { "Satış adedi sıfırdan büyük olmalı" }
        return (currentQty - sellQty).coerceAtLeast(0.0)
    }

    fun formatQuantity(qty: Double): String {
        val pattern = if (qty % 1.0 == 0.0) "#,##0" else "#,##0.######"
        return DecimalFormat(pattern, TR_SYMBOLS).format(qty)
    }

    fun formatUnitPriceTry(priceTry: Double): String {
        val pattern = if (priceTry > 0.0 && priceTry < 10.0) "#,##0.####" else "#,##0.##"
        return "${DecimalFormat(pattern, TR_SYMBOLS).format(priceTry)} ₺"
    }

    fun formatDecimalInput(value: Double): String {
        val pattern = if (value % 1.0 == 0.0) "0" else "0.######"
        return DecimalFormat(pattern, TR_SYMBOLS).apply { isGroupingUsed = false }.format(value)
    }

    fun parseFlexibleDecimal(raw: String): Double? {
        val cleaned = raw
            .replace("₺", "")
            .replace("TL", "", ignoreCase = true)
            .replace("%", "")
            .replace("\\s+".toRegex(), "")
        if (cleaned.isEmpty()) return null
        if (!cleaned.all { it.isDigit() || it == '.' || it == ',' }) return null

        val lastComma = cleaned.lastIndexOf(',')
        val lastDot = cleaned.lastIndexOf('.')

        val normalized = when {
            lastComma >= 0 && lastDot >= 0 -> {
                if (lastComma > lastDot) {
                    cleaned.replace(".", "").replace(",", ".")
                } else {
                    cleaned.replace(",", "")
                }
            }
            lastComma >= 0 -> {
                if (cleaned.count { it == ',' } > 1) return null
                cleaned.replace(",", ".")
            }
            lastDot >= 0 -> {
                if (cleaned.count { it == '.' } > 1) {
                    val parts = cleaned.split('.')
                    if (parts.drop(1).any { it.length != 3 }) return null
                    parts.joinToString("")
                } else {
                    cleaned
                }
            }
            else -> cleaned
        }

        val value = normalized.toDoubleOrNull() ?: return null
        return if (value.isFinite() && value >= 0.0) value else null
    }
}
