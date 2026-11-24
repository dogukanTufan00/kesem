package io.github.dogukantufan00.kesem.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class IconOption(
    val key: String,
    val label: String,
    val icon: ImageVector
)

object CategoryIconRegistry {

    val options: List<IconOption> = listOf(
        IconOption("shopping_cart", "Market", Icons.Rounded.ShoppingCart),
        IconOption("restaurant", "Yemek", Icons.Rounded.Restaurant),
        IconOption("coffee", "Kahve", Icons.Rounded.Coffee),
        IconOption("directions_bus", "Ulaşım", Icons.Rounded.DirectionsBus),
        IconOption("receipt_long", "Fatura", Icons.AutoMirrored.Rounded.ReceiptLong),
        IconOption("home", "Ev", Icons.Rounded.Home),
        IconOption("medical_services", "Sağlık", Icons.Rounded.MedicalServices),
        IconOption("checkroom", "Giyim", Icons.Rounded.Checkroom),
        IconOption("movie", "Eğlence", Icons.Rounded.Movie),
        IconOption("school", "Eğitim", Icons.Rounded.School),
        IconOption("subscriptions", "Abonelik", Icons.Rounded.Subscriptions),
        IconOption("shopping_bag", "Alışveriş", Icons.Rounded.ShoppingBag),
        IconOption("payments", "Maaş", Icons.Rounded.Payments),
        IconOption("work", "İş", Icons.Rounded.Work),
        IconOption("trending_up", "Yatırım", Icons.AutoMirrored.Rounded.TrendingUp),
        IconOption("card_giftcard", "Hediye", Icons.Rounded.CardGiftcard),
        IconOption("account_balance", "Banka", Icons.Rounded.AccountBalance),
        IconOption("wallet", "Cüzdan", Icons.Rounded.AccountBalanceWallet),
        IconOption("credit_card", "Kart", Icons.Rounded.CreditCard),
        IconOption("handshake", "Borç", Icons.Rounded.Handshake),
        IconOption("autorenew", "Düzenli", Icons.Rounded.Autorenew),
        IconOption("pets", "Evcil", Icons.Rounded.Pets),
        IconOption("fitness_center", "Spor", Icons.Rounded.FitnessCenter),
        IconOption("flight", "Seyahat", Icons.Rounded.Flight),
        IconOption("build", "Bakım", Icons.Rounded.Build),
        IconOption("savings", "Birikim", Icons.Rounded.Savings),
        IconOption("more_horiz", "Diğer", Icons.Rounded.MoreHoriz)
    )

    private val iconMap: Map<String, ImageVector> = options.associate { it.key to it.icon }

    val colorPalette: List<Long> = listOf(
        0xFFB34E2E, // Pişmiş Toprak
        0xFF4A7C59, // Adaçayı Yeşili
        0xFF4A6FA5, // Kot Mavisi
        0xFF3E7C8C, // Ege Petrolü
        0xFF845A8E, // Mürdüm
        0xFFB8515D, // Gül Kurusu
        0xFFC97B36, // Tarçın / Bal
        0xFF7D7066  // Sıcak Taş
    )

    val legacyToWarmColorMap: Map<Long, Long> = mapOf(
        0xFF0D4A3E to 0xFFB34E2E,
        0xFF16A34A to 0xFF4A7C59,
        0xFFEA580C to 0xFFB34E2E,
        0xFF2563EB to 0xFF4A6FA5,
        0xFFDC2626 to 0xFFC14638,
        0xFF7C3AED to 0xFF845A8E,
        0xFFE11D48 to 0xFFB8515D,
        0xFFDB2777 to 0xFFB8515D,
        0xFF9333EA to 0xFF845A8E,
        0xFF0284C7 to 0xFF3E7C8C,
        0xFF0891B2 to 0xFF3E7C8C,
        0xFFD97706 to 0xFFC97B36,
        0xFF64748B to 0xFF7D7066,
        0xFF059669 to 0xFF3D7A56,
        0xFF0D9488 to 0xFF4A7C59
    )

    fun iconFor(key: String?): ImageVector = iconMap[key] ?: Icons.Rounded.MoreHoriz
}
