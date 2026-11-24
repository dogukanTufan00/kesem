package io.github.dogukantufan00.kesem

import io.github.dogukantufan00.kesem.data.local.entity.InvestmentEntity
import io.github.dogukantufan00.kesem.model.InvestmentAssetType
import io.github.dogukantufan00.kesem.util.InvestmentCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import kotlin.math.roundToLong

class InvestmentCalculatorTest {

    @Test
    fun `evaluates normal asset profit and loss correctly`() {
        val usd = InvestmentEntity(
            assetType = InvestmentAssetType.CURRENCY,
            symbol = "USD",
            name = "Amerikan Doları",
            quantity = 100.0,
            averageCostTry = 32.50,
            lastPriceTry = 34.25
        )
        val valuation = InvestmentCalculator.evaluate(usd)
        assertEquals(342_500L, valuation.currentValueKurus)
        assertEquals(325_000L, valuation.costBasisKurus)
        assertEquals(17_500L, valuation.profitLossKurus)
        assertNotNull(valuation.profitLossPercent)
        assertEquals(5.3846, valuation.profitLossPercent!!, 0.01)
    }

    @Test
    fun `evaluates fractional tefas fund quantity accurately`() {
        val fund = InvestmentEntity(
            assetType = InvestmentAssetType.TEFAS_FUND,
            symbol = "MAC",
            name = "Marmara Capital",
            quantity = 1234.567891,
            averageCostTry = 2.15,
            lastPriceTry = 2.48
        )
        val valuation = InvestmentCalculator.evaluate(fund)
        val expectedValue = (1234.567891 * 2.48 * 100.0).roundToLong()
        val expectedCost = (1234.567891 * 2.15 * 100.0).roundToLong()
        assertEquals(expectedValue, valuation.currentValueKurus)
        assertEquals(expectedCost, valuation.costBasisKurus)
        assertEquals(expectedValue - expectedCost, valuation.profitLossKurus)
    }

    @Test
    fun `applyBuyLot computes weighted average cost and applySellLot clamps at zero`() {
        val (newQty, newAvg) = InvestmentCalculator.applyBuyLot(
            currentQty = 100.0,
            currentAvgCostTry = 30.0,
            buyQty = 100.0,
            buyUnitPriceTry = 34.0
        )
        assertEquals(200.0, newQty, 0.0001)
        assertEquals(32.0, newAvg, 0.0001)

        assertEquals(140.0, InvestmentCalculator.applySellLot(200.0, 60.0), 0.0001)
        assertEquals(0.0, InvestmentCalculator.applySellLot(140.0, 200.0), 0.0001)
    }

    @Test
    fun `evaluates term deposit daily net accrual and maturity projection`() {
        val start = LocalDate.of(2026, 11, 1)
        val maturity = LocalDate.of(2026, 12, 3) // 32 days
        val deposit = InvestmentEntity(
            assetType = InvestmentAssetType.TERM_DEPOSIT,
            symbol = "MEVDUAT",
            name = "32 Günlük Mevduat",
            quantity = 1.0,
            averageCostTry = 100_000.0,
            lastPriceTry = 100_000.0,
            annualInterestRate = 45.0,
            withholdingTaxRate = 15.0,
            startEpochDay = start.toEpochDay(),
            maturityEpochDay = maturity.toEpochDay()
        )

        val midDay = LocalDate.of(2026, 11, 11) // 10 days elapsed, 22 remaining
        val midVal = InvestmentCalculator.evaluate(deposit, today = midDay)
        val expected10dNet = (100_000.0 * 0.45 * 0.85 * (10.0 / 365.0) * 100.0).roundToLong()
        val expected32dNet = (100_000.0 * 0.45 * 0.85 * (32.0 / 365.0) * 100.0).roundToLong()

        assertEquals(10_000_000L, midVal.costBasisKurus)
        assertEquals(expected10dNet, midVal.profitLossKurus)
        assertEquals(10_000_000L + expected10dNet, midVal.currentValueKurus)
        assertEquals(expected32dNet, midVal.maturityProjectedNetInterestKurus)
        assertEquals(22L, midVal.daysRemaining)

        // Past maturity clamps at 32 days and 0 remaining
        val afterVal = InvestmentCalculator.evaluate(deposit, today = LocalDate.of(2026, 12, 10))
        assertEquals(expected32dNet, afterVal.profitLossKurus)
        assertEquals(0L, afterVal.daysRemaining)
    }

    @Test
    fun `parseFlexibleDecimal parses turkish and standard decimals`() {
        assertEquals(1250.75, InvestmentCalculator.parseFlexibleDecimal("1.250,75")!!, 0.0001)
        assertEquals(34.5, InvestmentCalculator.parseFlexibleDecimal("34,50")!!, 0.0001)
        assertEquals(0.123456, InvestmentCalculator.parseFlexibleDecimal("0.123456")!!, 0.0000001)
        assertNull(InvestmentCalculator.parseFlexibleDecimal("abc"))
        assertNull(InvestmentCalculator.parseFlexibleDecimal("-10"))
    }
}
