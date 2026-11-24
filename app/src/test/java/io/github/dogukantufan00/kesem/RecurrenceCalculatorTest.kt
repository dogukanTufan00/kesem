package io.github.dogukantufan00.kesem

import io.github.dogukantufan00.kesem.model.RecurrenceFrequency
import io.github.dogukantufan00.kesem.util.RecurrenceCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecurrenceCalculatorTest {

    @Test
    fun `weekly advances by 7 days`() {
        val next = RecurrenceCalculator.nextDueDate(
            currentDue = LocalDate.of(2026, 11, 20),
            frequency = RecurrenceFrequency.WEEKLY,
            preferredDayOfMonth = 20
        )
        assertEquals(LocalDate.of(2026, 11, 27), next)
    }

    @Test
    fun `monthly advances by 1 month respecting preferred day clamped to 28`() {
        val next = RecurrenceCalculator.nextDueDate(
            currentDue = LocalDate.of(2026, 1, 31),
            frequency = RecurrenceFrequency.MONTHLY,
            preferredDayOfMonth = 31
        )
        assertEquals(LocalDate.of(2026, 2, 28), next)

        val nextMar = RecurrenceCalculator.nextDueDate(
            currentDue = next,
            frequency = RecurrenceFrequency.MONTHLY,
            preferredDayOfMonth = 15
        )
        assertEquals(LocalDate.of(2026, 3, 15), nextMar)
    }

    @Test
    fun `yearly advances by 1 year`() {
        val next = RecurrenceCalculator.nextDueDate(
            currentDue = LocalDate.of(2026, 11, 20),
            frequency = RecurrenceFrequency.YEARLY,
            preferredDayOfMonth = 20
        )
        assertEquals(LocalDate.of(2027, 11, 20), next)
    }

    @Test
    fun `monthlyEquivalentKurus computes weekly monthly and yearly equivalents`() {
        assertEquals(500_00L, RecurrenceCalculator.monthlyEquivalentKurus(500_00L, RecurrenceFrequency.MONTHLY))
        assertEquals((120_00L * 52L) / 12L, RecurrenceCalculator.monthlyEquivalentKurus(120_00L, RecurrenceFrequency.WEEKLY))
        assertEquals(100_00L, RecurrenceCalculator.monthlyEquivalentKurus(1200_00L, RecurrenceFrequency.YEARLY))
    }
}
