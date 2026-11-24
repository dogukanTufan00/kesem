package io.github.dogukantufan00.kesem.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

// Warm Stone & Terracotta — Light Palette ("Fildişi Kağıt & Pişmiş Toprak")
val LightBackground = Color(0xFFF6F2EB)
val LightSurface = Color(0xFFFDFBF7)
val LightSurfaceVariant = Color(0xFFEDE6DA)
val LightPrimary = Color(0xFFB34E2E)
val LightOnPrimary = Color(0xFFFFFBF7)
val LightPrimaryContainer = Color(0xFFF5DFD3)
val LightOnPrimaryContainer = Color(0xFF5A2110)
val LightSecondary = Color(0xFF3E352E)
val LightOnSecondary = Color(0xFFFFFBF7)
val LightSecondaryContainer = Color(0xFFE9E0D4)
val LightOnSecondaryContainer = Color(0xFF2B241F)
val LightTextPrimary = Color(0xFF211B17)
val LightTextSecondary = Color(0xFF72665C)
val LightBorder = Color(0xFFE3D9CA)
val LightError = Color(0xFFC13F32)
val LightErrorContainer = Color(0xFFF9E0DC)

// Warm Espresso & Copper — Dark Palette ("Sıcak Kömür & Bakır")
val DarkBackground = Color(0xFF13110F)
val DarkSurface = Color(0xFF1C1815)
val DarkSurfaceVariant = Color(0xFF26211D)
val DarkPrimary = Color(0xFFE28562)
val DarkOnPrimary = Color(0xFF2B1108)
val DarkPrimaryContainer = Color(0xFF47251A)
val DarkOnPrimaryContainer = Color(0xFFF8DDD0)
val DarkSecondary = Color(0xFFD6CBC0)
val DarkOnSecondary = Color(0xFF211B17)
val DarkSecondaryContainer = Color(0xFF2E2722)
val DarkOnSecondaryContainer = Color(0xFFEBE2D7)
val DarkTextPrimary = Color(0xFFF3ECE2)
val DarkTextSecondary = Color(0xFFA69A8E)
val DarkBorder = Color(0xFF332C26)
val DarkError = Color(0xFFEB786B)
val DarkErrorContainer = Color(0xFF4B1E1A)

val KesemLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFA6998C),
    outlineVariant = LightBorder,
    error = LightError,
    onError = Color(0xFFFFFBF7),
    errorContainer = LightErrorContainer,
    onErrorContainer = Color(0xFF6B1A13),
    surfaceContainerLowest = Color(0xFFFFFDF9),
    surfaceContainerLow = LightSurface,
    surfaceContainer = Color(0xFFF8F3EB),
    surfaceContainerHigh = LightSurfaceVariant,
    surfaceContainerHighest = Color(0xFFE4DACB)
)

val KesemDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0xFF70655B),
    outlineVariant = DarkBorder,
    error = DarkError,
    onError = Color(0xFF3A100C),
    errorContainer = DarkErrorContainer,
    onErrorContainer = Color(0xFFFADCD8),
    surfaceContainerLowest = Color(0xFF0F0D0B),
    surfaceContainerLow = DarkSurface,
    surfaceContainer = Color(0xFF1F1B17),
    surfaceContainerHigh = DarkSurfaceVariant,
    surfaceContainerHighest = Color(0xFF312A24)
)

@Immutable
data class KesemExtendedColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val transfer: Color,
    val transferContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val cardBorder: Color,
    val subtleText: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val heroBorder: Color,
    val heroText: Color,
    val heroSubtleText: Color,
    val heroDivider: Color,
    val heroPositive: Color,
    val heroNegative: Color,
    val isDark: Boolean
)

val LightExtendedColors = KesemExtendedColors(
    income = Color(0xFF3D7A56),
    incomeContainer = Color(0xFFE1EFE5),
    expense = Color(0xFFC14638),
    expenseContainer = Color(0xFFF8E1DD),
    transfer = Color(0xFF4A6FA5),
    transferContainer = Color(0xFFE1E9F4),
    warning = Color(0xFFC5821D),
    warningContainer = Color(0xFFFAEDD4),
    cardBorder = LightBorder,
    subtleText = LightTextSecondary,
    heroStart = Color(0xFF2B221D),
    heroEnd = Color(0xFF412F25),
    heroBorder = Color(0xFF523C30),
    heroText = Color(0xFFFAF5EE),
    heroSubtleText = Color(0xFFC9B9AB),
    heroDivider = Color(0xFF4D3C33),
    heroPositive = Color(0xFF8CD1A6),
    heroNegative = Color(0xFFF09285),
    isDark = false
)

val DarkExtendedColors = KesemExtendedColors(
    income = Color(0xFF7DC498),
    incomeContainer = Color(0xFF1E3629),
    expense = Color(0xFFEB7D70),
    expenseContainer = Color(0xFF47211C),
    transfer = Color(0xFF83A8DC),
    transferContainer = Color(0xFF1D2C42),
    warning = Color(0xFFE8B054),
    warningContainer = Color(0xFF3E2B12),
    cardBorder = DarkBorder,
    subtleText = DarkTextSecondary,
    heroStart = Color(0xFF271F1A),
    heroEnd = Color(0xFF3B281F),
    heroBorder = Color(0xFF533A2D),
    heroText = Color(0xFFF7F0E6),
    heroSubtleText = Color(0xFFBBA99B),
    heroDivider = Color(0xFF47362D),
    heroPositive = Color(0xFF86CA9F),
    heroNegative = Color(0xFFEF8C7F),
    isDark = true
)

private val WarmIvoryHighlight = Color(0xFFF3ECE2)

/**
 * Koyu arka planlar üzerinde düşük parlaklıktaki vurgu renklerini soğuk beyaza değil
 * sıcak fildişine doğru yumuşakça açar.
 */
fun Color.adaptiveAccent(isDark: Boolean): Color {
    if (!isDark) return this
    return if (luminance() < 0.24f) {
        lerp(this, WarmIvoryHighlight, 0.36f)
    } else {
        this
    }
}

fun Color.adaptiveBadgeBackground(isDark: Boolean): Color {
    val adapted = adaptiveAccent(isDark)
    return adapted.copy(alpha = if (isDark) 0.20f else 0.13f)
}
