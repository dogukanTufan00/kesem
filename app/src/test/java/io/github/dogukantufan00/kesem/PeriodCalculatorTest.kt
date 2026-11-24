package io.github.dogukantufan00.kesem

import io.github.dogukantufan00.kesem.util.PeriodCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PeriodCalculatorTest {

    private val zone = ZoneId.of("Europe/Istanbul")

    @Test
    fun `day 1 gives full calendar month`() {
        val range = PeriodCalculator.periodFor(LocalDate.of(2026, 11, 20), 1, zone)
        assertEquals(LocalDate.of(2026, 11, 1), range.startDate)
        assertEquals(LocalDate.of(2026, 11, 30), range.endDateInclusive)
        assertEquals("Kasım 2026", range.label)
    }

    @Test
    fun `day 15 before start day resolves to previous month cycle`() {
        val range = PeriodCalculator.periodFor(LocalDate.of(2026, 11, 10), 15, zone)
        assertEquals(LocalDate.of(2026, 10, 15), range.startDate)
        assertEquals(LocalDate.of(2026, 11, 14), range.endDateInclusive)
    }

    @Test
    fun `day 15 on or after start day resolves to current month cycle`() {
        val range = PeriodCalculator.periodFor(LocalDate.of(2026, 11, 15), 15, zone)
        assertEquals(LocalDate.of(2026, 11, 15), range.startDate)
        assertEquals(LocalDate.of(2026, 12, 14), range.endDateInclusive)
    }

    @Test
    fun `year boundary includes years in label when custom start day`() {
        val range = PeriodCalculator.periodFor(LocalDate.of(2026, 12, 20), 15, zone)
        assertEquals(LocalDate.of(2026, 12, 15), range.startDate)
        assertEquals(LocalDate.of(2027, 1, 14), range.endDateInclusive)
        assertEquals("15 Ara 2026 – 14 Oca 2027", range.label)
    }

    @Test
    fun `day 28 handles leap year february correctly`() {
        val leapRange = PeriodCalculator.periodFor(LocalDate.of(2024, 2, 29), 28, zone)
        assertEquals(LocalDate.of(2024, 2, 28), leapRange.startDate)
        assertEquals(LocalDate.of(2024, 3, 27), leapRange.endDateInclusive)

        val nonLeapBefore = PeriodCalculator.periodFor(LocalDate.of(2025, 2, 27), 28, zone)
        assertEquals(LocalDate.of(2025, 1, 28), nonLeapBefore.startDate)
        assertEquals(LocalDate.of(2025, 2, 27), nonLeapBefore.endDateInclusive)
    }

    @Test
    fun `stepPeriod moves forward and backward cleanly`() {
        val current = PeriodCalculator.periodFor(LocalDate.of(2026, 11, 20), 15, zone)
        val prev = PeriodCalculator.stepPeriod(current, -1, 15, zone)
        val next = PeriodCalculator.stepPeriod(current, 1, 15, zone)

        assertEquals(LocalDate.of(2026, 10, 15), prev.startDate)
        assertEquals(LocalDate.of(2026, 11, 14), prev.endDateInclusive)

        assertEquals(LocalDate.of(2026, 12, 15), next.startDate)
        assertEquals(LocalDate.of(2027, 1, 14), next.endDateInclusive)
    }

    @Test
    fun `start and end millis align to start of day boundaries`() {
        val range = PeriodCalculator.periodFor(LocalDate.of(2026, 11, 20), 1, zone)
        val expectedStart = LocalDate.of(2026, 11, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val expectedEnd = LocalDate.of(2026, 12, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(expectedStart, range.startMillis)
        assertEquals(expectedEnd, range.endMillisExclusive)
    }
}
