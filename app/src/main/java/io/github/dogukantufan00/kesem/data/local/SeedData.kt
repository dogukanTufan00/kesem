package io.github.dogukantufan00.kesem.data.local

import io.github.dogukantufan00.kesem.data.local.entity.AccountEntity
import io.github.dogukantufan00.kesem.data.local.entity.CategoryEntity
import io.github.dogukantufan00.kesem.model.AccountType
import io.github.dogukantufan00.kesem.model.CategoryType
import io.github.dogukantufan00.kesem.model.InvestmentAssetType

data class AssetPreset(
    val symbol: String,
    val name: String,
    val assetType: InvestmentAssetType
)

object SeedData {

    val POPULAR_TR_BANKS: List<String> = listOf(
        "Garanti BBVA",
        "İş Bankası",
        "Ziraat Bankası",
        "Yapı Kredi",
        "Akbank",
        "QNB",
        "Enpara",
        "VakıfBank",
        "Halkbank",
        "DenizBank",
        "TEB",
        "Papara",
        "Diğer"
    )

    val POPULAR_SUBSCRIPTIONS: List<String> = listOf(
        "Netflix",
        "Spotify",
        "YouTube",
        "Amazon Prime",
        "iCloud",
        "Kira",
        "İnternet",
        "Telefon",
        "Elektrik",
        "Doğalgaz",
        "Su",
        "Maaş"
    )

    val POPULAR_INVESTMENT_PRESETS: List<AssetPreset> = listOf(
        // Döviz
        AssetPreset("USD", "Amerikan Doları", InvestmentAssetType.CURRENCY),
        AssetPreset("EUR", "Euro", InvestmentAssetType.CURRENCY),
        AssetPreset("GBP", "İngiliz Sterlini", InvestmentAssetType.CURRENCY),
        AssetPreset("CHF", "İsviçre Frangı", InvestmentAssetType.CURRENCY),
        AssetPreset("CAD", "Kanada Doları", InvestmentAssetType.CURRENCY),
        AssetPreset("JPY", "Japon Yeni", InvestmentAssetType.CURRENCY),

        // Altın & Değerli Maden
        AssetPreset("GRAM_ALTIN", "Gram Altın", InvestmentAssetType.GOLD),
        AssetPreset("CEYREK_ALTIN", "Çeyrek Altın", InvestmentAssetType.GOLD),
        AssetPreset("YARIM_ALTIN", "Yarım Altın", InvestmentAssetType.GOLD),
        AssetPreset("TAM_ALTIN", "Tam Altın", InvestmentAssetType.GOLD),
        AssetPreset("CUMHURIYET_ALTIN", "Cumhuriyet Altını", InvestmentAssetType.GOLD),
        AssetPreset("ATA_ALTIN", "Ata Altın", InvestmentAssetType.GOLD),
        AssetPreset("BILEZIK_22", "22 Ayar Bilezik", InvestmentAssetType.GOLD),
        AssetPreset("GUMUS", "Gram Gümüş", InvestmentAssetType.GOLD),

        // BIST
        AssetPreset("THYAO", "Türk Hava Yolları", InvestmentAssetType.BIST_STOCK),
        AssetPreset("ASELS", "Aselsan", InvestmentAssetType.BIST_STOCK),
        AssetPreset("TUPRS", "Tüpraş", InvestmentAssetType.BIST_STOCK),
        AssetPreset("GARAN", "Garanti BBVA", InvestmentAssetType.BIST_STOCK),
        AssetPreset("SISE", "Şişecam", InvestmentAssetType.BIST_STOCK),
        AssetPreset("BIMAS", "BİM Mağazalar", InvestmentAssetType.BIST_STOCK),
        AssetPreset("KCHOL", "Koç Holding", InvestmentAssetType.BIST_STOCK),
        AssetPreset("AKBNK", "Akbank", InvestmentAssetType.BIST_STOCK),
        AssetPreset("EREGL", "Erdemir", InvestmentAssetType.BIST_STOCK),
        AssetPreset("SAHOL", "Sabancı Holding", InvestmentAssetType.BIST_STOCK),

        // ABD Hisseleri
        AssetPreset("AAPL", "Apple", InvestmentAssetType.US_STOCK),
        AssetPreset("NVDA", "NVIDIA", InvestmentAssetType.US_STOCK),
        AssetPreset("MSFT", "Microsoft", InvestmentAssetType.US_STOCK),
        AssetPreset("GOOGL", "Alphabet", InvestmentAssetType.US_STOCK),
        AssetPreset("AMZN", "Amazon", InvestmentAssetType.US_STOCK),
        AssetPreset("TSLA", "Tesla", InvestmentAssetType.US_STOCK),
        AssetPreset("VOO", "Vanguard S&P 500 ETF", InvestmentAssetType.US_STOCK),
        AssetPreset("QQQ", "Invesco QQQ ETF", InvestmentAssetType.US_STOCK),

        // TEFAS Fonları
        AssetPreset("MAC", "Marmara Capital Hisse Fonu", InvestmentAssetType.TEFAS_FUND),
        AssetPreset("IIH", "İstanbul Portföy 3. Hisse", InvestmentAssetType.TEFAS_FUND),
        AssetPreset("AFT", "Ak Portföy Yeni Teknolojiler", InvestmentAssetType.TEFAS_FUND),
        AssetPreset("YAY", "Yapı Kredi Yabancı Teknoloji", InvestmentAssetType.TEFAS_FUND),
        AssetPreset("TCD", "Tacirler Değişken Fon", InvestmentAssetType.TEFAS_FUND),
        AssetPreset("NNF", "Hedef Portföy 1. Hisse", InvestmentAssetType.TEFAS_FUND),
        AssetPreset("TI2", "İş Portföy BIST 30", InvestmentAssetType.TEFAS_FUND),
        AssetPreset("PPN", "Nurol Para Piyasası Fonu", InvestmentAssetType.TEFAS_FUND),

        // Kripto
        AssetPreset("BTC", "Bitcoin", InvestmentAssetType.CRYPTO),
        AssetPreset("ETH", "Ethereum", InvestmentAssetType.CRYPTO),
        AssetPreset("SOL", "Solana", InvestmentAssetType.CRYPTO),
        AssetPreset("BNB", "BNB", InvestmentAssetType.CRYPTO),
        AssetPreset("XRP", "Ripple XRP", InvestmentAssetType.CRYPTO),
        AssetPreset("AVAX", "Avalanche", InvestmentAssetType.CRYPTO),
        AssetPreset("DOGE", "Dogecoin", InvestmentAssetType.CRYPTO),
        AssetPreset("USDT", "Tether USD", InvestmentAssetType.CRYPTO)
    )

    fun presetsFor(assetType: InvestmentAssetType): List<AssetPreset> =
        POPULAR_INVESTMENT_PRESETS.filter { it.assetType == assetType }

    fun defaultCategories(): List<CategoryEntity> = listOf(
        // 12 Gider kategorisi
        CategoryEntity(
            name = "Market",
            type = CategoryType.EXPENSE,
            iconKey = "shopping_cart",
            colorHex = 0xFF4A7C59,
            isDefault = true,
            sortOrder = 1
        ),
        CategoryEntity(
            name = "Yeme & İçme",
            type = CategoryType.EXPENSE,
            iconKey = "restaurant",
            colorHex = 0xFFB34E2E,
            isDefault = true,
            sortOrder = 2
        ),
        CategoryEntity(
            name = "Ulaşım",
            type = CategoryType.EXPENSE,
            iconKey = "directions_bus",
            colorHex = 0xFF4A6FA5,
            isDefault = true,
            sortOrder = 3
        ),
        CategoryEntity(
            name = "Faturalar",
            type = CategoryType.EXPENSE,
            iconKey = "receipt_long",
            colorHex = 0xFFC14638,
            isDefault = true,
            sortOrder = 4
        ),
        CategoryEntity(
            name = "Kira & Ev",
            type = CategoryType.EXPENSE,
            iconKey = "home",
            colorHex = 0xFF845A8E,
            isDefault = true,
            sortOrder = 5
        ),
        CategoryEntity(
            name = "Sağlık",
            type = CategoryType.EXPENSE,
            iconKey = "medical_services",
            colorHex = 0xFFB8515D,
            isDefault = true,
            sortOrder = 6
        ),
        CategoryEntity(
            name = "Giyim",
            type = CategoryType.EXPENSE,
            iconKey = "checkroom",
            colorHex = 0xFFB8515D,
            isDefault = true,
            sortOrder = 7
        ),
        CategoryEntity(
            name = "Eğlence",
            type = CategoryType.EXPENSE,
            iconKey = "movie",
            colorHex = 0xFF845A8E,
            isDefault = true,
            sortOrder = 8
        ),
        CategoryEntity(
            name = "Eğitim",
            type = CategoryType.EXPENSE,
            iconKey = "school",
            colorHex = 0xFF3E7C8C,
            isDefault = true,
            sortOrder = 9
        ),
        CategoryEntity(
            name = "Abonelikler",
            type = CategoryType.EXPENSE,
            iconKey = "subscriptions",
            colorHex = 0xFF3E7C8C,
            isDefault = true,
            sortOrder = 10
        ),
        CategoryEntity(
            name = "Alışveriş",
            type = CategoryType.EXPENSE,
            iconKey = "shopping_bag",
            colorHex = 0xFFC97B36,
            isDefault = true,
            sortOrder = 11
        ),
        CategoryEntity(
            name = "Diğer",
            type = CategoryType.EXPENSE,
            iconKey = "more_horiz",
            colorHex = 0xFF7D7066,
            isDefault = true,
            sortOrder = 12
        ),

        // 5 Gelir kategorisi
        CategoryEntity(
            name = "Maaş",
            type = CategoryType.INCOME,
            iconKey = "payments",
            colorHex = 0xFF3D7A56,
            isDefault = true,
            sortOrder = 1
        ),
        CategoryEntity(
            name = "Ek Gelir",
            type = CategoryType.INCOME,
            iconKey = "work",
            colorHex = 0xFF4A7C59,
            isDefault = true,
            sortOrder = 2
        ),
        CategoryEntity(
            name = "Yatırım Geliri",
            type = CategoryType.INCOME,
            iconKey = "trending_up",
            colorHex = 0xFF4A6FA5,
            isDefault = true,
            sortOrder = 3
        ),
        CategoryEntity(
            name = "Hediye / İade",
            type = CategoryType.INCOME,
            iconKey = "card_giftcard",
            colorHex = 0xFFC97B36,
            isDefault = true,
            sortOrder = 4
        ),
        CategoryEntity(
            name = "Diğer Gelir",
            type = CategoryType.INCOME,
            iconKey = "more_horiz",
            colorHex = 0xFF7D7066,
            isDefault = true,
            sortOrder = 5
        )
    )

    fun defaultCashAccount(): AccountEntity = AccountEntity(
        name = "Nakit Cüzdan",
        type = AccountType.CASH,
        bankName = null,
        currency = "TRY",
        initialBalanceKurus = 0L,
        colorHex = 0xFFB34E2E,
        iconKey = "wallet",
        includeInTotal = true,
        isArchived = false,
        sortOrder = 0
    )
}
