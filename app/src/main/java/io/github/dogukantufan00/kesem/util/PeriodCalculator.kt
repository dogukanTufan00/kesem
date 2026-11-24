package io.github.dogukantufan00.kesem.util

import io.github.dogukantufan00.kesem.model.PeriodRange
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object PeriodCalculator {

    private val TR_LOCALE: Locale = Locale.forLanguageTag("tr-TR")
    private val MONTH_YEAR_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("LLLL yyyy", TR_LOCALE)
    private val DAY_MONTH_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM", TR_LOCALE)
    private val DAY_MONTH_YEAR_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy", TR_LOCALE)

    fun periodFor(
        referenceDate: LocalDate,
        monthStartDay: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): PeriodRange {
        val clampedDay = monthStartDay.coerceIn(1, 28)
        val startDate = if (referenceDate.dayOfMonth >= clampedDay) {
            referenceDate.withDayOfMonth(clampedDay)
        } else {
            referenceDate.minusMonths(1).withDayOfMonth(clampedDay)
        }
        val nextStartDate = startDate.plusMonths(1)
        val endDateInclusive = nextStartDate.minusDays(1)

        val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillisExclusive = nextStartDate.atStartOfDay(zone).toInstant().toEpochMilli()

        val label = if (clampedDay == 1) {
            startDate.format(MONTH_YEAR_FORMAT).replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(TR_LOCALE) else ch.toString()
            }
        } else {
            if (startDate.year == endDateInclusive.year) {
                "${startDate.format(DAY_MONTH_FORMAT)} – ${endDateInclusive.format(DAY_MONTH_FORMAT)}"
            } else {
                "${startDate.format(DAY_MONTH_YEAR_FORMAT)} – ${endDateInclusive.format(DAY_MONTH_YEAR_FORMAT)}"
            }
        }

        return PeriodRange(
            startDate = startDate,
            endDateInclusive = endDateInclusive,
            startMillis = startMillis,
            endMillisExclusive = endMillisExclusive,
            label = label
        )
    }

    fun stepPeriod(
        current: PeriodRange,
        monthOffset: Long,
        monthStartDay: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): PeriodRange {
        val shiftedStart = current.startDate.plusMonths(monthOffset)
        return periodFor(shiftedStart, monthStartDay, zone)
    }
}
