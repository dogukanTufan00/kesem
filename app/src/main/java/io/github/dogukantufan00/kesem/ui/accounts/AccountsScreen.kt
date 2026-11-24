package io.github.dogukantufan00.kesem.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.model.AccountType
import io.github.dogukantufan00.kesem.model.AccountWithBalance
import io.github.dogukantufan00.kesem.ui.components.AmountText
import io.github.dogukantufan00.kesem.ui.components.EmptyStateCard
import io.github.dogukantufan00.kesem.ui.components.KesemCard
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.ui.theme.adaptiveAccent
import io.github.dogukantufan00.kesem.ui.theme.adaptiveBadgeBackground
import io.github.dogukantufan00.kesem.util.CategoryIconRegistry
import io.github.dogukantufan00.kesem.util.MoneyFormatter

@Composable
fun AccountsScreen(
    uiState: AccountsUiState,
    onAddAccount: (AccountType) -> Unit,
    onAccountClick: (Long) -> Unit,
    onNavigateToInvestments: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kesemColors = LocalKesemColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Hesaplar",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(
                    onClick = { onAddAccount(AccountType.BANK) },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hesap Ekle")
                }
            }
        }

        // Liquid Total + Card Debt Summary Card
        item {
            KesemCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Toplam Likit Para",
                    style = MaterialTheme.typography.labelLarge,
                    color = kesemColors.subtleText
                )
                Spacer(modifier = Modifier.height(6.dp))
                AmountText(
                    amountKurus = uiState.totalLiquidIncludedKurus,
                    hidden = uiState.hideBalances,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (uiState.creditCardAccounts.isNotEmpty() || uiState.totalCreditCardDebtKurus > 0L) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = kesemColors.cardBorder)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Toplam Kredi Kartı Borcu",
                            style = MaterialTheme.typography.bodySmall,
                            color = kesemColors.subtleText
                        )
                        AmountText(
                            amountKurus = uiState.totalCreditCardDebtKurus,
                            hidden = uiState.hideBalances,
                            color = if (uiState.totalCreditCardDebtKurus > 0L) {
                                kesemColors.expense
                            } else {
                                kesemColors.subtleText
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (uiState.hasExcludedAccounts) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Toplama dahil edilmeyen hesaplar hariç tutulur",
                        style = MaterialTheme.typography.bodySmall,
                        color = kesemColors.subtleText
                    )
                }
            }
        }

        // Bank Accounts Section
        item {
            SectionTitleRow(
                title = "Banka Hesapları",
                totalKurus = uiState.bankTotalKurus,
                hideBalances = uiState.hideBalances
            )
        }

        if (uiState.bankAccounts.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.AccountBalance,
                    title = "Henüz banka hesabı yok",
                    subtitle = "Vadesiz banka hesaplarını ayrı ayrı ekleyip bakiyelerini takip edebilirsin.",
                    actionLabel = "Banka Hesabı Ekle",
                    onAction = { onAddAccount(AccountType.BANK) }
                )
            }
        } else {
            item {
                KesemCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    uiState.bankAccounts.forEachIndexed { index, account ->
                        AccountListRow(
                            item = account,
                            hideBalances = uiState.hideBalances,
                            onClick = { onAccountClick(account.account.id) }
                        )
                        if (index < uiState.bankAccounts.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = kesemColors.cardBorder
                            )
                        }
                    }
                }
            }
        }

        // Cash Accounts Section
        item {
            SectionTitleRow(
                title = "Nakit",
                totalKurus = uiState.cashTotalKurus,
                hideBalances = uiState.hideBalances
            )
        }

        if (uiState.cashAccounts.isNotEmpty()) {
            item {
                KesemCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    uiState.cashAccounts.forEachIndexed { index, account ->
                        AccountListRow(
                            item = account,
                            hideBalances = uiState.hideBalances,
                            onClick = { onAccountClick(account.account.id) }
                        )
                        if (index < uiState.cashAccounts.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = kesemColors.cardBorder
                            )
                        }
                    }
                }
            }
        } else {
            item {
                FilledTonalButton(
                    onClick = { onAddAccount(AccountType.CASH) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Nakit Hesabı Ekle")
                }
            }
        }

        // Credit Cards Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Kredi Kartları",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (uiState.creditCardAccounts.isNotEmpty()) {
                    AmountText(
                        amountKurus = uiState.totalCreditCardDebtKurus,
                        hidden = uiState.hideBalances,
                        color = if (uiState.totalCreditCardDebtKurus > 0L) {
                            kesemColors.expense
                        } else {
                            kesemColors.subtleText
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (uiState.creditCardAccounts.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.CreditCard,
                    title = "Kredi kartı eklenmedi",
                    subtitle = "Limit, dönem içi harcama ve taksit takibi için kartını ekleyebilirsin.",
                    actionLabel = "Kredi Kartı Ekle",
                    onAction = { onAddAccount(AccountType.CREDIT_CARD) }
                )
            }
        } else {
            item {
                KesemCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    uiState.creditCardAccounts.forEachIndexed { index, card ->
                        CreditCardListRow(
                            item = card,
                            hideBalances = uiState.hideBalances,
                            onClick = { onAccountClick(card.account.id) }
                        )
                        if (index < uiState.creditCardAccounts.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = kesemColors.cardBorder
                            )
                        }
                    }
                }
            }
        }

        // Debt / Receivables & Recurring Shortcut Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Takip ve Planlama",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                val investmentSubtitle = if (uiState.investmentCount == 0) {
                    "Döviz, altın, hisse, fon, kripto ve mevduat"
                } else {
                    val totalStr = MoneyFormatter.formatKurus(
                        uiState.investmentTotalKurus,
                        hidden = uiState.hideBalances
                    )
                    val plStr = MoneyFormatter.formatKurus(
                        uiState.investmentProfitLossKurus,
                        hidden = uiState.hideBalances,
                        showPlusSign = true
                    )
                    "Toplam: $totalStr · K/Z: $plStr (${uiState.investmentCount} varlık)"
                }
                ModuleShortcutCard(
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    title = "Yatırımlar & Portföy",
                    subtitle = investmentSubtitle,
                    onClick = onNavigateToInvestments
                )
                val debtSubtitle = if (uiState.openDebtCount == 0) {
                    "Açık borç veya alacak kaydı yok"
                } else {
                    val rec = MoneyFormatter.formatKurus(
                        uiState.totalReceivablesKurus,
                        hidden = uiState.hideBalances
                    )
                    val deb = MoneyFormatter.formatKurus(
                        uiState.totalPersonalDebtKurus,
                        hidden = uiState.hideBalances
                    )
                    "Alacak: $rec · Borç: $deb (${uiState.openDebtCount} açık)"
                }
                ModuleShortcutCard(
                    icon = Icons.Rounded.Handshake,
                    title = "Borç & Alacak Defteri",
                    subtitle = debtSubtitle,
                    onClick = onNavigateToDebts
                )

                val recurringSubtitle = if (uiState.activeRecurringCount == 0) {
                    "Maaş, kira veya abonelik kuralı ekle"
                } else {
                    val exp = MoneyFormatter.formatKurus(
                        uiState.monthlyFixedExpenseKurus,
                        hidden = uiState.hideBalances
                    )
                    val inc = MoneyFormatter.formatKurus(
                        uiState.monthlyFixedIncomeKurus,
                        hidden = uiState.hideBalances
                    )
                    "Aylık sabit gider: $exp · gelir: $inc"
                }
                ModuleShortcutCard(
                    icon = Icons.Rounded.Autorenew,
                    title = "Düzenli İşlemler & Abonelikler",
                    subtitle = recurringSubtitle,
                    onClick = onNavigateToRecurring
                )
            }
        }
    }
}

@Composable
private fun SectionTitleRow(
    title: String,
    totalKurus: Long,
    hideBalances: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        AmountText(
            amountKurus = totalKurus,
            hidden = hideBalances,
            color = LocalKesemColors.current.subtleText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AccountListRow(
    item: AccountWithBalance,
    hideBalances: Boolean,
    onClick: () -> Unit
) {
    val account = item.account
    val rawAccent = Color(account.colorHex)
    val kesemColors = LocalKesemColors.current
    val accent = rawAccent.adaptiveAccent(kesemColors.isDark)

    val subtitleParts = buildList {
        if (!account.bankName.isNullOrBlank() && account.bankName != account.name) {
            add(account.bankName)
        }
        if (!account.includeInTotal) {
            add("Toplama dahil değil")
        }
    }.joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(rawAccent.adaptiveBadgeBackground(kesemColors.isDark)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CategoryIconRegistry.iconFor(account.iconKey),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitleParts.isNotEmpty()) {
                Text(
                    text = subtitleParts,
                    style = MaterialTheme.typography.bodySmall,
                    color = kesemColors.subtleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        AmountText(
            amountKurus = item.currentBalanceKurus,
            hidden = hideBalances,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(4.dp))

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = kesemColors.subtleText,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CreditCardListRow(
    item: AccountWithBalance,
    hideBalances: Boolean,
    onClick: () -> Unit
) {
    val card = item.account
    val rawAccent = Color(card.colorHex)
    val kesemColors = LocalKesemColors.current
    val accent = rawAccent.adaptiveAccent(kesemColors.isDark)
    val debtKurus = (-item.currentBalanceKurus).coerceAtLeast(0L)
    val limitKurus = card.creditLimitKurus ?: 0L
    val availableKurus = (limitKurus - debtKurus).coerceAtLeast(0L)

    val dayMeta = buildList {
        card.statementDay?.let { add("Kesim: $it") }
        card.paymentDueDay?.let { add("Son ödeme: $it") }
    }.joinToString(" · ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(rawAccent.adaptiveBadgeBackground(kesemColors.isDark)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CreditCard,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sub = listOfNotNull(
                    card.bankName?.takeIf { it != card.name },
                    dayMeta.takeIf { it.isNotEmpty() }
                ).joinToString(" · ")
                if (sub.isNotEmpty()) {
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = kesemColors.subtleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                AmountText(
                    amountKurus = debtKurus,
                    hidden = hideBalances,
                    color = if (debtKurus > 0L) kesemColors.expense else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (limitKurus > 0L) {
                    Text(
                        text = "Kalan: ${MoneyFormatter.formatKurus(availableKurus, hidden = hideBalances)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = kesemColors.subtleText
                    )
                }
            }
        }

        if (limitKurus > 0L) {
            val usage = (debtKurus.toDouble() / limitKurus.toDouble()).toFloat().coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (usage > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(usage.coerceAtLeast(0.02f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (usage > 0.85f) kesemColors.expense else accent)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleShortcutCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val kesemColors = LocalKesemColors.current
    KesemCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = kesemColors.subtleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = kesemColors.subtleText
            )
        }
    }
}
