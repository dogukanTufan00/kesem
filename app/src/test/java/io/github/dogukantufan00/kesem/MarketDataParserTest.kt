package io.github.dogukantufan00.kesem

import io.github.dogukantufan00.kesem.data.remote.MarketDataParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MarketDataParserTest {

    @Test
    fun `parseTruncgilJson parses v4 format for currency and gold`() {
        val v4Json = """
            {
              "Update_Date": "2026-11-20 18:30:00",
              "USD": { "Name": "Amerikan Doları", "Type": "Currency", "Buying": 34.50, "Selling": 34.62, "Change": 0.25 },
              "EUR": { "Name": "Euro", "Type": "Currency", "Buying": 37.10, "Selling": 37.25, "Change": -0.12 },
              "gram-altin": { "Name": "Gram Altın", "Type": "Gold", "Buying": 3020.5, "Selling": 3045.8, "Change": 0.85 },
              "ceyrek-altin": { "Name": "Çeyrek Altın", "Type": "Gold", "Buying": 4950.0, "Selling": 5020.0, "Change": 0.50 },
              "gumus": { "Name": "Gümüş", "Type": "Gold", "Buying": 35.10, "Selling": 35.90, "Change": 1.1 }
            }
        """.trimIndent()

        val quotes = MarketDataParser.parseTruncgilJson(v4Json)
        assertEquals(34.62, quotes["USD"]!!.priceTry, 0.001)
        assertEquals(0.25, quotes["USD"]!!.dailyChangePercent!!, 0.001)
        assertEquals("Truncgil", quotes["USD"]!!.source)
        assertEquals(37.25, quotes["EUR"]!!.priceTry, 0.001)
        assertEquals(-0.12, quotes["EUR"]!!.dailyChangePercent!!, 0.001)
        assertEquals(3045.8, quotes["GRAM_ALTIN"]!!.priceTry, 0.001)
        assertEquals(5020.0, quotes["CEYREK_ALTIN"]!!.priceTry, 0.001)
        assertEquals(35.90, quotes["GUMUS"]!!.priceTry, 0.001)
    }

    @Test
    fun `parseTruncgilJson parses legacy Turkish today json format`() {
        val legacyJson = """
            {
              "Update_Date": "2026-11-20 18:30:00",
              "ABD DOLARI": { "Alış": "34,50", "Satış": "34,65", "Değişim": "%+0.15" },
              "Gram Altın": { "Alış": "2.990,00", "Satış": "3.015,50", "Değişim": "%-0.30" }
            }
        """.trimIndent()

        val quotes = MarketDataParser.parseTruncgilJson(legacyJson)
        assertEquals(34.65, quotes["USD"]!!.priceTry, 0.001)
        assertEquals(0.15, quotes["USD"]!!.dailyChangePercent!!, 0.001)
        assertEquals(3015.50, quotes["GRAM_ALTIN"]!!.priceTry, 0.001)
        assertEquals(-0.30, quotes["GRAM_ALTIN"]!!.dailyChangePercent!!, 0.001)
    }

    @Test
    fun `parseTcmbXml parses currency rates and divides by unit`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Tarih_Date>
              <Currency CrossOrder="0" Kod="USD" CurrencyCode="USD">
                <Unit>1</Unit>
                <ForexBuying>34.4500</ForexBuying>
                <ForexSelling>34.5200</ForexSelling>
              </Currency>
              <Currency CrossOrder="1" Kod="JPY" CurrencyCode="JPY">
                <Unit>100</Unit>
                <ForexBuying>22.0100</ForexBuying>
                <ForexSelling>22.1500</ForexSelling>
              </Currency>
            </Tarih_Date>
        """.trimIndent()

        val quotes = MarketDataParser.parseTcmbXml(xml)
        assertEquals(34.52, quotes["USD"]!!.priceTry, 0.0001)
        assertEquals("TCMB", quotes["USD"]!!.source)
        assertEquals(0.2215, quotes["JPY"]!!.priceTry, 0.0001)
    }

    @Test
    fun `parseYahooChartJson extracts regularMarketPrice and daily change`() {
        val yahooJson = """
            {
              "chart": {
                "result": [{
                  "meta": {
                    "currency": "TRY",
                    "symbol": "THYAO.IS",
                    "regularMarketPrice": 305.50,
                    "chartPreviousClose": 300.00
                  }
                }],
                "error": null
              }
            }
        """.trimIndent()

        val parsed = MarketDataParser.parseYahooChartJson(yahooJson)
        assertNotNull(parsed)
        assertEquals(305.50, parsed!!.price, 0.001)
        assertEquals(1.8333, parsed.changePercent!!, 0.01)
    }

    @Test
    fun `parseBinance24hrJson extracts lastPrice and priceChangePercent`() {
        val binanceJson = """
            {
              "symbol": "BTCTRY",
              "priceChangePercent": "2.450",
              "lastPrice": "3150420.50"
            }
        """.trimIndent()

        val parsed = MarketDataParser.parseBinance24hrJson(binanceJson)
        assertNotNull(parsed)
        assertEquals(3150420.50, parsed!!.price, 0.01)
        assertEquals(2.45, parsed.changePercent!!, 0.001)
    }

    @Test
    fun `parseTefasJson picks latest date price for matching fund code`() {
        val tefasJson = """
            {
              "data": [
                { "TARIH": "1763510400000", "FONKODU": "MAC", "FIYAT": 2.4123 },
                { "TARIH": "1763596800000", "FONKODU": "MAC", "FIYAT": 2.4875 }
              ]
            }
        """.trimIndent()

        val price = MarketDataParser.parseTefasJson(tefasJson, "mac")
        assertNotNull(price)
        assertEquals(2.4875, price!!, 0.00001)
    }
}
