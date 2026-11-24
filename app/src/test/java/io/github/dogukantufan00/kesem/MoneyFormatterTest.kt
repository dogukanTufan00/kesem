package io.github.dogukantufan00.kesem

import io.github.dogukantufan00.kesem.util.MoneyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyFormatterTest {

    @Test
    fun `formatKurus formats positive negative and zero in Turkish format`() {
        assertEquals("0,00 ₺", MoneyFormatter.formatKurus(0L))
        assertEquals("125,50 ₺", MoneyFormatter.formatKurus(12550L))
        assertEquals("12.345,67 ₺", MoneyFormatter.formatKurus(1234567L))
        assertEquals("-450,00 ₺", MoneyFormatter.formatKurus(-45000L))
        assertEquals("+450,00 ₺", MoneyFormatter.formatKurus(45000L, showPlusSign = true))
    }

    @Test
    fun `formatKurus hides balance when hidden flag is true`() {
        assertEquals("•••••• ₺", MoneyFormatter.formatKurus(1234567L, hidden = true))
        assertEquals("•••••• ₺", MoneyFormatter.formatKurus(-100L, hidden = true, showPlusSign = true))
    }

    @Test
    fun `parseAmountToKurus handles various valid formats`() {
        assertEquals(12500L, MoneyFormatter.parseAmountToKurus("125"))
        assertEquals(12550L, MoneyFormatter.parseAmountToKurus("125,5"))
        assertEquals(12550L, MoneyFormatter.parseAmountToKurus("125.50"))
        assertEquals(125075L, MoneyFormatter.parseAmountToKurus("1.250,75"))
        assertEquals(125075L, MoneyFormatter.parseAmountToKurus("1,250.75"))
        assertEquals(125000L, MoneyFormatter.parseAmountToKurus("1.250"))
        assertEquals(9900L, MoneyFormatter.parseAmountToKurus(" 99,00 ₺ "))
    }

    @Test
    fun `parseAmountToKurus rejects zero negative and malformed strings`() {
        assertNull(MoneyFormatter.parseAmountToKurus(""))
        assertNull(MoneyFormatter.parseAmountToKurus("0"))
        assertNull(MoneyFormatter.parseAmountToKurus("0,00"))
        assertNull(MoneyFormatter.parseAmountToKurus("-100"))
        assertNull(MoneyFormatter.parseAmountToKurus("abc"))
        assertNull(MoneyFormatter.parseAmountToKurus("12,345"))
        assertNull(MoneyFormatter.parseAmountToKurus("12..34"))
        assertEquals(0L, MoneyFormatter.parseAmountToKurus("0", allowZero = true))
    }

    @Test
    fun `signed input formatting and parsing supports negative and zero`() {
        assertEquals("-5555", MoneyFormatter.formatSignedInputFromKurus(-555500L))
        assertEquals("0", MoneyFormatter.formatSignedInputFromKurus(0L))
        assertEquals("125,50", MoneyFormatter.formatSignedInputFromKurus(12550L))

        assertEquals(-555500L, MoneyFormatter.parseSignedAmountToKurus("-5555"))
        assertEquals(-125050L, MoneyFormatter.parseSignedAmountToKurus("-1.250,50"))
        assertEquals(0L, MoneyFormatter.parseSignedAmountToKurus("0"))
        assertEquals(555500L, MoneyFormatter.parseSignedAmountToKurus("5555"))
        assertNull(MoneyFormatter.parseSignedAmountToKurus("--5"))
    }

    @Test
    fun `tag parsing and encoding trims and deduplicates`() {
        val tags = MoneyFormatter.parseTags("tatil, market , ,tatil, iş")
        assertEquals(listOf("tatil", "market", "iş"), tags)
        assertEquals("tatil,market,iş", MoneyFormatter.encodeTags(tags))
    }
}
