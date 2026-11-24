package io.github.dogukantufan00.kesem

import io.github.dogukantufan00.kesem.data.backup.KesemCsvExporter
import io.github.dogukantufan00.kesem.data.local.entity.TransactionEntity
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.model.TransactionWithDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class KesemCsvExporterTest {

    @Test
    fun `exportTransactionsCsv uses UTF8 BOM semicolon delimiter signed decimal comma and proper escaping`() {
        val zone = ZoneId.of("Europe/Istanbul")
        val millis1 = LocalDateTime.of(2026, 11, 20, 14, 30).atZone(zone).toInstant().toEpochMilli()
        val millis2 = LocalDateTime.of(2026, 11, 19, 9, 15).atZone(zone).toInstant().toEpochMilli()

        val items = listOf(
            TransactionWithDetails(
                transaction = TransactionEntity(
                    id = 1L,
                    type = TransactionType.EXPENSE,
                    amountKurus = 125_050L,
                    accountId = 1L,
                    categoryId = 2L,
                    dateMillis = millis1,
                    note = "Market; \"özel\" indirim",
                    tagsCsv = "ev,haftalik",
                    installmentGroupId = "grp",
                    installmentIndex = 2,
                    installmentCount = 6
                ),
                accountName = "Kredi Kartı",
                accountColorHex = 0xFF0D4A3EL,
                toAccountName = null,
                categoryName = "Market",
                categoryIconKey = "shopping_cart",
                categoryColorHex = 0xFF16A34AL
            ),
            TransactionWithDetails(
                transaction = TransactionEntity(
                    id = 2L,
                    type = TransactionType.TRANSFER,
                    amountKurus = 500_000L,
                    accountId = 1L,
                    toAccountId = 2L,
                    categoryId = null,
                    dateMillis = millis2,
                    note = "Kira birikim"
                ),
                accountName = "Maaş",
                accountColorHex = 0xFF0D4A3EL,
                toAccountName = "Vadeli",
                categoryName = null,
                categoryIconKey = null,
                categoryColorHex = null
            )
        )

        val csv = KesemCsvExporter.exportTransactionsCsv(items, zone)
        assertTrue(csv.startsWith("\uFEFF"))

        val lines = csv.removePrefix("\uFEFF").trimEnd('\r', '\n').split("\r\n")
        assertEquals(3, lines.size)
        assertEquals("Tarih;Saat;Tür;Kategori;Hesap;Hedef Hesap;Tutar (TL);Taksit;Etiketler;Not", lines[0])
        assertEquals(
            "20.11.2026;14:30;Gider;Market;Kredi Kartı;;-1250,50;2/6;#ev, #haftalik;\"Market; \"\"özel\"\" indirim\"",
            lines[1]
        )
        assertEquals(
            "19.11.2026;09:15;Transfer;;Maaş;Vadeli;5000,00;;;Kira birikim",
            lines[2]
        )
    }
}
