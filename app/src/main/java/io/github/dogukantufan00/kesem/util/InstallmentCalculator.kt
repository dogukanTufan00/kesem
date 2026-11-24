package io.github.dogukantufan00.kesem.util

import java.time.Instant
import java.time.ZoneId

data class InstallmentSlice(
    val index: Int,
    val count: Int,
    val amountKurus: Long,
    val dateMillis: Long
)

object InstallmentCalculator {

    fun splitInstallments(
        totalKurus: Long,
        count: Int,
        startDateMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<InstallmentSlice> {
        require(totalKurus > 0L) { "Toplam tutar sıfırdan büyük olmalı" }
        require(count >= 1) { "Taksit sayısı en az 1 olmalı" }

        val base = totalKurus / count
        val remainder = totalKurus % count
        val startZdt = Instant.ofEpochMilli(startDateMillis).atZone(zone)

        return (1..count).map { idx ->
            val sliceAmount = if (idx == 1) base + remainder else base
            val sliceMillis = startZdt
                .plusMonths((idx - 1).toLong())
                .toInstant()
                .toEpochMilli()
            InstallmentSlice(
                index = idx,
                count = count,
                amountKurus = sliceAmount,
                dateMillis = sliceMillis
            )
        }
    }
}
