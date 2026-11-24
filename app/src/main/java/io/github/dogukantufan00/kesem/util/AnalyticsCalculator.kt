package io.github.dogukantufan00.kesem.util

import io.github.dogukantufan00.kesem.model.CashFlowPointRaw
import io.github.dogukantufan00.kesem.model.DailyExpensePoint
import io.github.dogukantufan00.kesem.model.MonthlyTrendPoint
import io.github.dogukantufan00.kesem.model.PeriodRange
import io.github.dogukantufan00.kesem.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

object AnalyticsCalculator {

    private val TR_LOCALE: Locale = Locale.forLanguageTag("tr-TR")
    private val SHORT_MONTH_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM", TR_LOCALE)
    private val DAY_MONTH_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM", TR_LOCALE)

    fun buildPeriodTrend(
        periods: List<PeriodRange>,
        rows: List<CashFlowPointRaw>
    ): List<MonthlyTrendPoint> {
        return periods.map { period ->
            var income = 0L
            var expense = 0L
            for (row in rows) {
                if (row.dateMillis >= period.startMillis && row.dateMillis < period.endMillisExclusive) {
                    when (row.type) {
                        TransactionType.INCOME -> income += row.amountKurus
                        TransactionType.EXPENSE -> expense += row.amountKurus
                        else -> Unit
                    }
                }
            }
            val rawShort = period.startDate.format(SHORT_MONTH_FORMAT).trimEnd('.')
            val shortLabel = rawShort.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(TR_LOCALE) else ch.toString()
            }
            MonthlyTrendPoint(
                period = period,
                shortLabel = shortLabel,
                incomeKurus = income,
                expenseKurus = expense
            )
        }
    }

    fun buildDailyExpenses(
        period: PeriodRange,
        rows: List<CashFlowPointRaw>,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<DailyExpensePoint> {
        val byDate = HashMap<LocalDate, Long>()
        for (row in rows) {
            if (row.type == TransactionType.EXPENSE &&
                row.dateMillis >= period.startMillis &&
                row.dateMillis < period.endMillisExclusive
            ) {
                val date = Instant.ofEpochMilli(row.dateMillis).atZone(zone).toLocalDate()
                byDate[date] = (byDate[date] ?: 0L) + row.amountKurus
            }
        }

        val result = mutableListOf<DailyExpensePoint>()
        var cursor = period.startDate
        while (!cursor.isAfter(period.endDateInclusive)) {
            result.add(
                DailyExpensePoint(
                    epochDay = cursor.toEpochDay(),
                    dayOfMonth = cursor.dayOfMonth,
                    label = cursor.format(DAY_MONTH_FORMAT),
                    expenseKurus = byDate[cursor] ?: 0L
                )
            )
            cursor = cursor.plusDays(1)
        }
        return result
    }

    fun averageDailyExpenseKurus(
        period: PeriodRange,
        totalExpenseKurus: Long,
        today: LocalDate = LocalDate.now()
    ): Long {
        if (totalExpenseKurus <= 0L) return 0L
        val elapsedDays = when {
            today.isBefore(period.startDate) -> 1L
            today.isAfter(period.endDateInclusive) ->
                ChronoUnit.DAYS.between(period.startDate, period.endDateInclusive) + 1L
            else -> ChronoUnit.DAYS.between(period.startDate, today) + 1L
        }.coerceAtLeast(1L)
        return totalExpenseKurus / elapsedDays
    }

    fun periodExpenseChangePercent(
        currentExpenseKurus: Long,
        previousExpenseKurus: Long
    ): Int? {
        if (previousExpenseKurus <= 0L) return null
        return (((currentExpenseKurus - previousExpenseKurus).toDouble() / previousExpenseKurus.toDouble()) * 100.0)
            .roundToInt()
    }
}
