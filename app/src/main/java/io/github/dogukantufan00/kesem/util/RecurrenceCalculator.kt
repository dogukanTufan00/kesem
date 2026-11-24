package io.github.dogukantufan00.kesem.util

import io.github.dogukantufan00.kesem.model.RecurrenceFrequency
import java.time.LocalDate

object RecurrenceCalculator {

    fun nextDueDate(
        currentDue: LocalDate,
        frequency: RecurrenceFrequency,
        preferredDayOfMonth: Int
    ): LocalDate {
        val clampedDay = preferredDayOfMonth.coerceIn(1, 28)
        return when (frequency) {
            RecurrenceFrequency.WEEKLY -> currentDue.plusWeeks(1)
            RecurrenceFrequency.MONTHLY -> currentDue.plusMonths(1).withDayOfMonth(clampedDay)
            RecurrenceFrequency.YEARLY -> currentDue.plusYears(1).withDayOfMonth(clampedDay)
        }
    }

    fun monthlyEquivalentKurus(
        amountKurus: Long,
        frequency: RecurrenceFrequency
    ): Long {
        return when (frequency) {
            RecurrenceFrequency.MONTHLY -> amountKurus
            RecurrenceFrequency.WEEKLY -> (amountKurus * 52L) / 12L
            RecurrenceFrequency.YEARLY -> amountKurus / 12L
        }
    }
}
