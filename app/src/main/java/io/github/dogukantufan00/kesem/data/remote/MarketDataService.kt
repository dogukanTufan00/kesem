package io.github.dogukantufan00.kesem.data.remote

import io.github.dogukantufan00.kesem.data.local.entity.InvestmentEntity
import io.github.dogukantufan00.kesem.model.InvestmentAssetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MarketDataService {

    suspend fun fetchQuotesFor(
        investments: List<InvestmentEntity>
    ): Pair<Map<Long, MarketQuote>, List<String>> = withContext(Dispatchers.IO) {
        val refreshable = investments.filter { it.assetType != InvestmentAssetType.TERM_DEPOSIT }
        if (refreshable.isEmpty()) return@withContext emptyMap<Long, MarketQuote>() to emptyList()

        val quotesById = mutableMapOf<Long, MarketQuote>()
        val failedSymbols = mutableListOf<String>()

        val needsTruncgil = refreshable.any {
            it.assetType == InvestmentAssetType.CURRENCY || it.assetType == InvestmentAssetType.GOLD
        }
        val needsUsdRate = refreshable.any {
            it.assetType == InvestmentAssetType.US_STOCK || it.assetType == InvestmentAssetType.CRYPTO
        }

        val truncgilQuotes = if (needsTruncgil || needsUsdRate) {
            fetchTruncgilQuotes()
        } else {
            emptyMap()
        }

        val tcmbQuotes by lazy { fetchTcmbQuotes() }

        var usdTry: Double? = truncgilQuotes["USD"]?.priceTry
            ?: tcmbQuotes["USD"]?.priceTry
        if (usdTry == null && (needsUsdRate || needsTruncgil)) {
            usdTry = fetchYahooRaw("USDTRY=X")?.price
        }

        for (inv in refreshable) {
            val sym = inv.symbol.trim().uppercase()
            val quote: MarketQuote? = when (inv.assetType) {
                InvestmentAssetType.CURRENCY -> {
                    truncgilQuotes[sym] ?: tcmbQuotes[sym] ?: fetchYahooRaw("${sym}TRY=X")?.let {
                        MarketQuote(it.price, it.changePercent, "Yahoo")
                    }
                }
                InvestmentAssetType.GOLD -> {
                    truncgilQuotes[sym] ?: run {
                        val rate = usdTry
                        if (rate != null && sym == "GRAM_ALTIN") {
                            fetchYahooRaw("GC=F")?.let { oz ->
                                MarketQuote((oz.price / TROY_OUNCE_GRAMS) * rate, oz.changePercent, "Yahoo")
                            }
                        } else if (rate != null && sym == "GUMUS") {
                            fetchYahooRaw("SI=F")?.let { oz ->
                                MarketQuote((oz.price / TROY_OUNCE_GRAMS) * rate, oz.changePercent, "Yahoo")
                            }
                        } else {
                            null
                        }
                    }
                }
                InvestmentAssetType.BIST_STOCK -> {
                    val ticker = if (sym.endsWith(".IS")) sym else "$sym.IS"
                    fetchYahooRaw(ticker)?.let {
                        MarketQuote(it.price, it.changePercent, "Yahoo")
                    }
                }
                InvestmentAssetType.US_STOCK -> {
                    val rate = usdTry
                    if (rate != null) {
                        fetchYahooRaw(sym)?.let {
                            MarketQuote(it.price * rate, it.changePercent, "Yahoo")
                        }
                    } else {
                        null
                    }
                }
                InvestmentAssetType.CRYPTO -> {
                    if (sym == "USDT") {
                        usdTry?.let { MarketQuote(it, truncgilQuotes["USD"]?.dailyChangePercent, "Kur") }
                    } else {
                        fetchBinanceQuoteTry(sym, usdTry)
                    }
                }
                InvestmentAssetType.TEFAS_FUND -> {
                    fetchTefasFundPrice(sym)?.let {
                        MarketQuote(it, null, "TEFAS")
                    }
                }
                InvestmentAssetType.TERM_DEPOSIT -> null
            }

            if (quote != null && quote.priceTry > 0.0) {
                quotesById[inv.id] = quote
            } else {
                failedSymbols.add(sym)
            }
        }

        quotesById to failedSymbols.distinct()
    }

    private fun fetchTruncgilQuotes(): Map<String, MarketQuote> {
        val v4Body = httpGet("https://finans.truncgil.com/v4/today.json")
        if (v4Body != null) {
            val parsed = MarketDataParser.parseTruncgilJson(v4Body)
            if (parsed.isNotEmpty()) return parsed
        }
        val legacyBody = httpGet("https://finans.truncgil.com/today.json") ?: return emptyMap()
        return MarketDataParser.parseTruncgilJson(legacyBody)
    }

    private fun fetchTcmbQuotes(): Map<String, MarketQuote> {
        val xml = httpGet("https://www.tcmb.gov.tr/kurlar/today.xml") ?: return emptyMap()
        return MarketDataParser.parseTcmbXml(xml)
    }

    private fun fetchYahooRaw(symbol: String): RawPrice? {
        val encoded = URLEncoder.encode(symbol, "UTF-8")
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$encoded?interval=1d&range=1d"
        val body = httpGet(url) ?: return null
        return MarketDataParser.parseYahooChartJson(body)
    }

    private fun fetchBinanceQuoteTry(symbol: String, usdTry: Double?): MarketQuote? {
        val tryBody = httpGet("https://api.binance.com/api/v3/ticker/24hr?symbol=${symbol}TRY")
        if (tryBody != null) {
            val parsed = MarketDataParser.parseBinance24hrJson(tryBody)
            if (parsed != null) {
                return MarketQuote(parsed.price, parsed.changePercent, "Binance")
            }
        }
        val rate = usdTry ?: return null
        val usdtBody = httpGet("https://api.binance.com/api/v3/ticker/24hr?symbol=${symbol}USDT") ?: return null
        val parsedUsdt = MarketDataParser.parseBinance24hrJson(usdtBody) ?: return null
        return MarketQuote(parsedUsdt.price * rate, parsedUsdt.changePercent, "Binance")
    }

    private fun fetchTefasFundPrice(fundCode: String): Double? {
        val today = LocalDate.now()
        val weekAgo = today.minusDays(7)
        val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val form = "fontip=YAT&sfontur=&fonkod=${URLEncoder.encode(fundCode, "UTF-8")}" +
            "&fongrup=&bastarih=${weekAgo.format(fmt)}&bittarih=${today.format(fmt)}" +
            "&fonturkod=&fonunvantip=&kurucukod="

        val body = httpPostForm(
            urlStr = "https://www.tefas.gov.tr/api/DB/BindHistoryInfo",
            formBody = form,
            referer = "https://www.tefas.gov.tr/TarihselVeriler.aspx"
        ) ?: return null
        return MarketDataParser.parseTefasJson(body, fundCode)
    }

    private fun httpGet(urlStr: String): String? = runCatching {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json,text/xml,*/*")
        }
        try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    private fun httpPostForm(urlStr: String, formBody: String, referer: String): String? = runCatching {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 8_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("Referer", referer)
            setRequestProperty("X-Requested-With", "XMLHttpRequest")
        }
        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(formBody)
                writer.flush()
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    companion object {
        private const val TROY_OUNCE_GRAMS = 31.1034768
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/125.0"
    }
}
