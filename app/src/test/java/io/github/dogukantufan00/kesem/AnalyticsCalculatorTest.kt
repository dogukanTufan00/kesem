package io.github.dogukantufan00.kesem

import io.github.dogukantufan00.kesem.model.CashFlowPointRaw
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.util.AnalyticsCalculator
import io.github.dogukantufan00.kesem.util.PeriodCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AnalyticsCalculatorTest {

    private val zone = ZoneId.of("Europe/Istanbul")

    private fun noonMillis(date: LocalDate): Long =
        date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `buildPeriodTrend buckets custom start day periods accurately`() {
        val current = PeriodCalculator.periodFor(LocalDate.of(2026, 11, 20), 15, zone)
        val periods = listOf(
            PeriodCalculator.stepPeriod(current, -1, 15, zone),
            current
        )
        val rows = listOf(
            CashFlowPointRaw(1L, TransactionType.INCOME, 3_000_00L, noonMillis(LocalDate.of(2026, 10, 20))),
            CashFlowPointRaw(2L, TransactionType.EXPENSE, 1_200_00L, noonMillis(LocalDate.of(2026, 11, 14))), // last day of prev period
            CashFlowPointRaw(3L, TransactionType.EXPENSE, 800_00L, noonMillis(LocalDate.of(2026, 11, 15))) // first day of current
        )

        val trend = AnalyticsCalculator.buildPeriodTrend(periods, rows)
        assertEquals(2, trend.size)
        assertEquals(3_000_00L, trend[0].incomeKurus)
        assertEquals(1_200_00L, trend[0].expenseKurus)
        assertEquals(1_800_00L, trend[0].netFlowKurus)
        assertEquals(0L, trend[1].incomeKurus)
        assertEquals(800_00L, trend[1].expenseKurus)
    }

    @Test
    fun `buildDailyExpenses creates full day series and sums multiple expenses on same day`() {
        val period = PeriodCalculator.periodFor(LocalDate.of(2026, 11, 10), 1, zone) // 30 days
        val rows = listOf(
            CashFlowPointRaw(1L, TransactionType.EXPENSE, 15_000L, noonMillis(LocalDate.of(2026, 11, 5))),
            CashFlowPointRaw(2L, TransactionType.EXPENSE, 25_000L, noonMillis(LocalDate.of(2026, 11, 5))),
            CashFlowPointRaw(3L, TransactionType.INCOME, 99_000L, noonMillis(LocalDate.of(2026, 11, 5)))
        )

        val daily = AnalyticsCalculator.buildDailyExpenses(period, rows, zone)
        assertEquals(30, daily.size)
        val nov5 = daily.first { it.dayOfMonth == 5 }
        assertEquals(40_000L, nov5.expenseKurus)
        assertEquals(0L, daily.first().expenseKurus)
    }

    @Test
    fun `averageDailyExpenseKurus respects elapsed days vs completed period`() {
        val period = PeriodCalculator.periodFor(LocalDate.of(2026, 11, 1), 1, zone) // Nov 1..30 (30 days)
        // Mid-month: Nov 10 -> 10 elapsed days
        assertEquals(
            12_000L,
            AnalyticsCalculator.averageDailyExpenseKurus(period, 120_000L, LocalDate.of(2026, 11, 10))
        )
        // Past period: Dec 5 -> full 30 days
        assertEquals(
            4_000L,
            AnalyticsCalculator.averageDailyExpenseKurus(period, 120_000L, LocalDate.of(2026, 12, 5))
        )
    }

    @Test
    fun `periodExpenseChangePercent handles increases decreases and zero baseline`() {
        assertEquals(25, AnalyticsCalculator.periodExpenseChangePercent(125_000L, 100_000L))
        assertEquals(-20, AnalyticsCalculator.periodExpenseChangePercent(80_000L, 100_000L))
        assertNull(AnalyticsCalculator.periodExpenseChangePercent(50_000L, 0L))
    }
}
