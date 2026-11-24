package io.github.dogukantufan00.kesem.data.backup

import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.model.TransactionWithDetails
import io.github.dogukantufan00.kesem.util.MoneyFormatter
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object KesemCsvExporter {

    private const val UTF8_BOM = "\uFEFF"
    private val TR_LOCALE: Locale = Locale.forLanguageTag("tr-TR")
    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", TR_LOCALE)
    private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", TR_LOCALE)

    fun exportTransactionsCsv(
        transactions: List<TransactionWithDetails>,
        zone: ZoneId = ZoneId.systemDefault()
    ): String {
        val sb = StringBuilder()
        sb.append(UTF8_BOM)
        sb.append("Tarih;Saat;Tür;Kategori;Hesap;Hedef Hesap;Tutar (TL);Taksit;Etiketler;Not\r\n")

        for (item in transactions) {
            val tx = item.transaction
            val dateTime = Instant.ofEpochMilli(tx.dateMillis).atZone(zone)
            val dateStr = dateTime.format(DATE_FORMAT)
            val timeStr = dateTime.format(TIME_FORMAT)
            val typeStr = when (tx.type) {
                TransactionType.INCOME -> "Gelir"
                TransactionType.EXPENSE -> "Gider"
                TransactionType.TRANSFER -> "Transfer"
                TransactionType.ADJUSTMENT_IN -> "Bakiye Düzeltme (+)"
                TransactionType.ADJUSTMENT_OUT -> "Bakiye Düzeltme (-)"
            }
            val categoryStr = when (tx.type) {
                TransactionType.TRANSFER,
                TransactionType.ADJUSTMENT_IN,
                TransactionType.ADJUSTMENT_OUT -> ""
                else -> item.categoryName.orEmpty()
            }
            val toAccountStr = item.toAccountName.orEmpty()
            val signedKurus = when (tx.type) {
                TransactionType.EXPENSE,
                TransactionType.ADJUSTMENT_OUT -> -tx.amountKurus
                else -> tx.amountKurus
            }
            val amountStr = BigDecimal.valueOf(signedKurus, 2).toPlainString().replace('.', ',')
            val installmentStr = if (
                tx.installmentCount != null &&
                tx.installmentCount > 1 &&
                tx.installmentIndex != null
            ) {
                "${tx.installmentIndex}/${tx.installmentCount}"
            } else {
                ""
            }
            val tagsStr = MoneyFormatter.parseTags(tx.tagsCsv).joinToString(", ") { "#$it" }

            val cols = listOf(
                dateStr,
                timeStr,
                typeStr,
                categoryStr,
                item.accountName,
                toAccountStr,
                amountStr,
                installmentStr,
                tagsStr,
                tx.note
            )
            sb.append(cols.joinToString(";") { escapeCsvField(it) })
            sb.append("\r\n")
        }
        return sb.toString()
    }

    private fun escapeCsvField(raw: String): String {
        val needsQuote = raw.contains(';') || raw.contains('"') || raw.contains('\n') || raw.contains('\r')
        if (!needsQuote) return raw
        return "\"${raw.replace("\"", "\"\"")}\""
    }
}
