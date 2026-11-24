package io.github.dogukantufan00.kesem.util

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object MoneyFormatter {

    val TR_LOCALE: Locale = Locale.forLanguageTag("tr-TR")
    private val TR_SYMBOLS = DecimalFormatSymbols(TR_LOCALE).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val FULL_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", TR_LOCALE)
    private val SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", TR_LOCALE)

    fun formatKurus(
        amountKurus: Long,
        hidden: Boolean = false,
        showPlusSign: Boolean = false
    ): String {
        if (hidden) return "•••••• ₺"

        val isNegative = amountKurus < 0L
        val absBigDecimal = if (amountKurus == Long.MIN_VALUE) {
            BigDecimal(Long.MAX_VALUE).add(BigDecimal.ONE).movePointLeft(2)
        } else {
            BigDecimal.valueOf(kotlin.math.abs(amountKurus), 2)
        }

        val formatter = DecimalFormat("#,##0.00", TR_SYMBOLS)
        val formattedNumber = formatter.format(absBigDecimal)
        val sign = when {
            isNegative -> "-"
            showPlusSign && amountKurus > 0L -> "+"
            else -> ""
        }
        return "$sign$formattedNumber ₺"
    }

    fun formatInputFromKurus(amountKurus: Long): String {
        val absKurus = kotlin.math.abs(amountKurus)
        val liras = absKurus / 100L
        val kurus = absKurus % 100L
        return if (kurus == 0L) {
            liras.toString()
        } else {
            "$liras,${kurus.toString().padStart(2, '0')}"
        }
    }

    fun formatSignedInputFromKurus(amountKurus: Long): String {
        val formatted = formatInputFromKurus(amountKurus)
        return if (amountKurus < 0L) "-$formatted" else formatted
    }

    fun parseSignedAmountToKurus(raw: String): Long? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val isNegative = trimmed.startsWith('-')
        val unsignedRaw = if (isNegative) trimmed.substring(1) else trimmed
        if (unsignedRaw.contains('-')) return null
        val unsignedKurus = parseAmountToKurus(unsignedRaw, allowZero = true) ?: return null
        return if (isNegative) -unsignedKurus else unsignedKurus
    }

    fun parseAmountToKurus(raw: String, allowZero: Boolean = false): Long? {
        val cleaned = raw
            .replace("₺", "")
            .replace("TL", "", ignoreCase = true)
            .replace("\\s+".toRegex(), "")
        if (cleaned.isEmpty()) return null
        if (!cleaned.all { it.isDigit() || it == '.' || it == ',' }) return null

        val lastComma = cleaned.lastIndexOf(',')
        val lastDot = cleaned.lastIndexOf('.')

        val normalized: String = when {
            lastComma >= 0 && lastDot >= 0 -> {
                if (lastComma > lastDot) {
                    val decimalPart = cleaned.substring(lastComma + 1)
                    if (decimalPart.isEmpty() || decimalPart.length > 2 || decimalPart.contains('.')) return null
                    val intPart = cleaned.substring(0, lastComma).replace(".", "")
                    if (intPart.isEmpty() || intPart.contains(',')) return null
                    "$intPart.$decimalPart"
                } else {
                    val decimalPart = cleaned.substring(lastDot + 1)
                    if (decimalPart.isEmpty() || decimalPart.length > 2 || decimalPart.contains(',')) return null
                    val intPart = cleaned.substring(0, lastDot).replace(",", "")
                    if (intPart.isEmpty() || intPart.contains('.')) return null
                    "$intPart.$decimalPart"
                }
            }
            lastComma >= 0 -> {
                val firstComma = cleaned.indexOf(',')
                if (firstComma != lastComma) return null
                val decimalPart = cleaned.substring(lastComma + 1)
                val intPart = cleaned.substring(0, lastComma)
                if (intPart.isEmpty() || decimalPart.isEmpty() || decimalPart.length > 2) return null
                "$intPart.$decimalPart"
            }
            lastDot >= 0 -> {
                val dotCount = cleaned.count { it == '.' }
                if (dotCount == 1) {
                    val intPart = cleaned.substring(0, lastDot)
                    val fracPart = cleaned.substring(lastDot + 1)
                    if (intPart.isEmpty() || fracPart.isEmpty()) return null
                    when (fracPart.length) {
                        1, 2 -> "$intPart.$fracPart"
                        3 -> "$intPart$fracPart"
                        else -> return null
                    }
                } else {
                    val segments = cleaned.split('.')
                    if (segments.first().isEmpty() || segments.drop(1).any { it.length != 3 }) {
                        return null
                    }
                    segments.joinToString("")
                }
            }
            else -> cleaned
        }

        val bd = normalized.toBigDecimalOrNull() ?: return null
        val scaled = bd.movePointRight(2)
        val kurus = runCatching { scaled.longValueExact() }.getOrNull() ?: return null
        return if (allowZero) {
            if (kurus >= 0L) kurus else null
        } else {
            if (kurus > 0L) kurus else null
        }
    }

    fun formatDateHeader(
        dateMillis: Long,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): String {
        val date = Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate()
        return when (date) {
            today -> "Bugün"
            today.minusDays(1) -> "Dün"
            else -> date.format(FULL_DATE_FORMAT)
        }
    }

    fun formatShortDate(
        dateMillis: Long,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): String {
        val date = Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate()
        return when (date) {
            today -> "Bugün"
            today.minusDays(1) -> "Dün"
            else -> date.format(SHORT_DATE_FORMAT)
        }
    }

    fun parseTags(csv: String): List<String> =
        csv.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    fun encodeTags(tags: List<String>): String =
        tags.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(",")
}
