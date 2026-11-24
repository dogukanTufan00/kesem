package io.github.dogukantufan00.kesem.data.remote

import org.json.JSONObject
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

data class MarketQuote(
    val priceTry: Double,
    val dailyChangePercent: Double? = null,
    val source: String
)

data class RawPrice(
    val price: Double,
    val changePercent: Double? = null
)

object MarketDataParser {

    private val TRUNCGIL_ALIASES: Map<String, String> = mapOf(
        "usd" to "USD",
        "abd dolari" to "USD",
        "dolar" to "USD",
        "eur" to "EUR",
        "euro" to "EUR",
        "gbp" to "GBP",
        "ingiliz sterlini" to "GBP",
        "chf" to "CHF",
        "isvicre frangi" to "CHF",
        "cad" to "CAD",
        "kanada dolari" to "CAD",
        "jpy" to "JPY",
        "japon yeni" to "JPY",
        "gram-altin" to "GRAM_ALTIN",
        "gram altin" to "GRAM_ALTIN",
        "gra" to "GRAM_ALTIN",
        "ceyrek-altin" to "CEYREK_ALTIN",
        "ceyrek altin" to "CEYREK_ALTIN",
        "yarim-altin" to "YARIM_ALTIN",
        "yarim altin" to "YARIM_ALTIN",
        "tam-altin" to "TAM_ALTIN",
        "tam altin" to "TAM_ALTIN",
        "cumhuriyet-altini" to "CUMHURIYET_ALTIN",
        "cumhuriyet altini" to "CUMHURIYET_ALTIN",
        "ata-altin" to "ATA_ALTIN",
        "ata altin" to "ATA_ALTIN",
        "22-ayar-bilezik" to "BILEZIK_22",
        "22 ayar bilezik" to "BILEZIK_22",
        "gumus" to "GUMUS",
        "gram-gumus" to "GUMUS",
        "gram gumus" to "GUMUS"
    )

    fun parseTruncgilJson(json: String): Map<String, MarketQuote> {
        val result = mutableMapOf<String, MarketQuote>()
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return result

        val keys = root.keys()
        while (keys.hasNext()) {
            val rawKey = keys.next()
            val entry = root.optJSONObject(rawKey) ?: continue
            val canonical = canonicalTruncgilSymbol(rawKey) ?: continue

            val selling = parseAnyNumber(entry, "Selling", "Satış", "Satis", "Buying", "Alış", "Alis")
                ?: continue
            if (selling <= 0.0) continue

            val change = parseAnyPercent(entry, "Change", "Değişim", "Degisim")
            result[canonical] = MarketQuote(
                priceTry = selling,
                dailyChangePercent = change,
                source = "Truncgil"
            )
        }
        return result
    }

    fun parseTcmbXml(xml: String): Map<String, MarketQuote> {
        val result = mutableMapOf<String, MarketQuote>()
        val doc = runCatching {
            val factory = DocumentBuilderFactory.newInstance().apply {
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            }
            factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        }.getOrNull() ?: return result

        val nodes = doc.getElementsByTagName("Currency")
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as? Element ?: continue
            val code = el.getAttribute("CurrencyCode").trim().uppercase()
            if (code.isEmpty()) continue

            val unit = el.firstChildText("Unit")?.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
            val sellingText = el.firstChildText("ForexSelling")
                ?: el.firstChildText("BanknoteSelling")
                ?: el.firstChildText("ForexBuying")
                ?: continue
            val selling = parseFlexibleNumber(sellingText) ?: continue
            val unitPrice = selling / unit
            if (unitPrice > 0.0) {
                result[code] = MarketQuote(
                    priceTry = unitPrice,
                    dailyChangePercent = null,
                    source = "TCMB"
                )
            }
        }
        return result
    }

    fun parseYahooChartJson(json: String): RawPrice? {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val meta = root.optJSONObject("chart")
            ?.optJSONArray("result")
            ?.optJSONObject(0)
            ?.optJSONObject("meta")
            ?: return null

        val price = meta.optDouble("regularMarketPrice", Double.NaN)
        if (price.isNaN() || price <= 0.0) return null

        val prevClose = meta.optDouble("chartPreviousClose", Double.NaN).let {
            if (it.isNaN() || it <= 0.0) meta.optDouble("previousClose", Double.NaN) else it
        }
        val change = if (!prevClose.isNaN() && prevClose > 0.0) {
            ((price - prevClose) / prevClose) * 100.0
        } else {
            null
        }
        return RawPrice(price = price, changePercent = change)
    }

    fun parseBinance24hrJson(json: String): RawPrice? {
        val obj = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val price = obj.optString("lastPrice", "").toDoubleOrNull()
            ?: obj.optDouble("lastPrice", Double.NaN).takeIf { !it.isNaN() }
            ?: return null
        if (price <= 0.0) return null

        val change = obj.optString("priceChangePercent", "").toDoubleOrNull()
            ?: obj.optDouble("priceChangePercent", Double.NaN).takeIf { !it.isNaN() }
        return RawPrice(price = price, changePercent = change)
    }

    fun parseTefasJson(json: String, fundCode: String): Double? {
        val arr = runCatching { JSONObject(json) }.getOrNull()
            ?.optJSONArray("data")
            ?: return null
        val target = fundCode.trim().uppercase()

        var bestDateKey = Long.MIN_VALUE
        var bestPrice: Double? = null

        for (i in 0 until arr.length()) {
            val row = arr.optJSONObject(i) ?: continue
            val code = row.optString("FONKODU", "").trim().uppercase()
            if (code.isNotEmpty() && code != target) continue

            val price = parseAnyNumber(row, "FIYAT", "fiyat") ?: continue
            if (price <= 0.0) continue

            val dateKey = row.optString("TARIH", "").filter { it.isDigit() }.toLongOrNull()
                ?: i.toLong()
            if (dateKey >= bestDateKey) {
                bestDateKey = dateKey
                bestPrice = price
            }
        }
        return bestPrice
    }

    private fun canonicalTruncgilSymbol(rawKey: String): String? {
        val normalized = normalizeTurkishKey(rawKey)
        return TRUNCGIL_ALIASES[normalized]
    }

    private fun normalizeTurkishKey(raw: String): String =
        raw.trim()
            .lowercase()
            .replace('ı', 'i')
            .replace('İ', 'i')
            .replace('ğ', 'g')
            .replace('ü', 'u')
            .replace('ş', 's')
            .replace('ö', 'o')
            .replace('ç', 'c')

    private fun parseAnyNumber(obj: JSONObject, vararg keys: String): Double? {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val raw = obj.opt(key)
            when (raw) {
                is Number -> return raw.toDouble()
                is String -> {
                    val parsed = parseFlexibleNumber(raw)
                    if (parsed != null) return parsed
                }
            }
        }
        return null
    }

    private fun parseAnyPercent(obj: JSONObject, vararg keys: String): Double? {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val raw = obj.opt(key)
            when (raw) {
                is Number -> return raw.toDouble()
                is String -> {
                    val clean = raw.replace("%", "").replace("+", "").trim()
                    val parsed = parseFlexibleNumber(clean, allowNegative = true)
                    if (parsed != null) return parsed
                }
            }
        }
        return null
    }

    private fun parseFlexibleNumber(raw: String, allowNegative: Boolean = false): Double? {
        val trimmed = raw.replace("₺", "").replace("$", "").replace("\\s+".toRegex(), "")
        if (trimmed.isEmpty()) return null
        val sign = if (allowNegative && trimmed.startsWith('-')) -1.0 else 1.0
        val unsigned = trimmed.removePrefix("-").removePrefix("+")
        val lastComma = unsigned.lastIndexOf(',')
        val lastDot = unsigned.lastIndexOf('.')

        val normalized = when {
            lastComma >= 0 && lastDot >= 0 -> {
                if (lastComma > lastDot) {
                    unsigned.replace(".", "").replace(",", ".")
                } else {
                    unsigned.replace(",", "")
                }
            }
            lastComma >= 0 -> unsigned.replace(",", ".")
            else -> unsigned
        }
        val num = normalized.toDoubleOrNull() ?: return null
        return if (num.isFinite()) sign * num else null
    }

    private fun Element.firstChildText(tag: String): String? {
        val list = getElementsByTagName(tag)
        if (list.length == 0) return null
        return list.item(0)?.textContent?.trim()
    }
}
