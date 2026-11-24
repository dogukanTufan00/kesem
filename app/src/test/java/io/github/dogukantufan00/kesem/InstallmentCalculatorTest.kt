package io.github.dogukantufan00.kesem

import io.github.dogukantufan00.kesem.util.InstallmentCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class InstallmentCalculatorTest {

    private val zone = ZoneId.of("Europe/Istanbul")

    @Test
    fun `splits evenly divisible amount across months`() {
        val startMillis = LocalDate.of(2026, 3, 15).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val slices = InstallmentCalculator.splitInstallments(
            totalKurus = 300_00L,
            count = 3,
            startDateMillis = startMillis,
            zone = zone
        )

        assertEquals(3, slices.size)
        assertEquals(listOf(100_00L, 100_00L, 100_00L), slices.map { it.amountKurus })
        assertEquals(listOf(1, 2, 3), slices.map { it.index })
        assertEquals(
            listOf(
                LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 5, 15)
            ),
            slices.map { Instant.ofEpochMilli(it.dateMillis).atZone(zone).toLocalDate() }
        )
    }

    @Test
    fun `puts indivisible kurus remainder onto first installment`() {
        val startMillis = LocalDate.of(2026, 5, 10).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val slices = InstallmentCalculator.splitInstallments(
            totalKurus = 100_00L,
            count = 3,
            startDateMillis = startMillis,
            zone = zone
        )

        assertEquals(3, slices.size)
        assertEquals(33_34L, slices[0].amountKurus)
        assertEquals(33_33L, slices[1].amountKurus)
        assertEquals(33_33L, slices[2].amountKurus)
        assertEquals(100_00L, slices.sumOf { it.amountKurus })
    }

    @Test
    fun `month end jan 31 clamps cleanly to feb 28 or 29`() {
        val nonLeapStart = LocalDate.of(2026, 1, 31).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val slices = InstallmentCalculator.splitInstallments(
            totalKurus = 200_00L,
            count = 3,
            startDateMillis = nonLeapStart,
            zone = zone
        )

        val dates = slices.map { Instant.ofEpochMilli(it.dateMillis).atZone(zone).toLocalDate() }
        assertEquals(LocalDate.of(2026, 1, 31), dates[0])
        assertEquals(LocalDate.of(2026, 2, 28), dates[1])
        assertEquals(LocalDate.of(2026, 3, 31), dates[2])
    }

    @Test
    fun `single installment returns exact input`() {
        val startMillis = LocalDate.of(2026, 11, 20).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val slices = InstallmentCalculator.splitInstallments(
            totalKurus = 450_50L,
            count = 1,
            startDateMillis = startMillis,
            zone = zone
        )
        assertEquals(1, slices.size)
        assertEquals(450_50L, slices[0].amountKurus)
        assertEquals(startMillis, slices[0].dateMillis)
    }
}
