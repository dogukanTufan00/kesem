package io.github.dogukantufan00.kesem.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.model.TransactionType
import io.github.dogukantufan00.kesem.model.TransactionWithDetails
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.ui.theme.adaptiveAccent
import io.github.dogukantufan00.kesem.ui.theme.adaptiveBadgeBackground
import io.github.dogukantufan00.kesem.util.CategoryIconRegistry
import io.github.dogukantufan00.kesem.util.MoneyFormatter

@Composable
fun TransactionRow(
    item: TransactionWithDetails,
    hideBalances: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kesemColors = LocalKesemColors.current
    val tx = item.transaction

    val badgeColor: Color
    val badgeBg: Color
    val titleText: String
    val accountSubtitle: String
    val amountColor: Color
    val signedKurus: Long
    val showPlus: Boolean

    when (tx.type) {
        TransactionType.EXPENSE -> {
            val rawColor = item.categoryColorHex?.let { Color(it) } ?: kesemColors.expense
            badgeColor = rawColor.adaptiveAccent(kesemColors.isDark)
            badgeBg = rawColor.adaptiveBadgeBackground(kesemColors.isDark)
            titleText = item.categoryName ?: "Kategorisiz"
            accountSubtitle = item.accountName
            amountColor = kesemColors.expense
            signedKurus = -tx.amountKurus
            showPlus = false
        }
        TransactionType.INCOME -> {
            val rawColor = item.categoryColorHex?.let { Color(it) } ?: kesemColors.income
            badgeColor = rawColor.adaptiveAccent(kesemColors.isDark)
            badgeBg = rawColor.adaptiveBadgeBackground(kesemColors.isDark)
            titleText = item.categoryName ?: "Gelir"
            accountSubtitle = item.accountName
            amountColor = kesemColors.income
            signedKurus = tx.amountKurus
            showPlus = true
        }
        TransactionType.TRANSFER -> {
            badgeColor = kesemColors.transfer
            badgeBg = kesemColors.transferContainer
            titleText = "Transfer"
            accountSubtitle = "${item.accountName} → ${item.toAccountName ?: "Hesap"}"
            amountColor = kesemColors.transfer
            signedKurus = tx.amountKurus
            showPlus = false
        }
        TransactionType.ADJUSTMENT_IN -> {
            badgeColor = kesemColors.income
            badgeBg = kesemColors.incomeContainer
            titleText = "Bakiye Düzeltme"
            accountSubtitle = item.accountName
            amountColor = kesemColors.income
            signedKurus = tx.amountKurus
            showPlus = true
        }
        TransactionType.ADJUSTMENT_OUT -> {
            badgeColor = kesemColors.expense
            badgeBg = kesemColors.expenseContainer
            titleText = "Bakiye Düzeltme"
            accountSubtitle = item.accountName
            amountColor = kesemColors.expense
            signedKurus = -tx.amountKurus
            showPlus = false
        }
    }

    val tags = MoneyFormatter.parseTags(tx.tagsCsv)
    val installmentLabel = if (tx.installmentCount != null && tx.installmentCount > 1 && tx.installmentIndex != null) {
        "Taksit ${tx.installmentIndex}/${tx.installmentCount}"
    } else {
        null
    }
    val detailParts = buildList {
        add(accountSubtitle)
        if (installmentLabel != null) add(installmentLabel)
        if (tx.note.isNotBlank()) add(tx.note)
        if (tags.isNotEmpty()) add(tags.joinToString(" ") { "#$it" })
    }.joinToString(" · ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(badgeBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (tx.type) {
                    TransactionType.TRANSFER -> Icons.Rounded.SwapHoriz
                    TransactionType.ADJUSTMENT_IN,
                    TransactionType.ADJUSTMENT_OUT -> Icons.Rounded.Tune
                    else -> CategoryIconRegistry.iconFor(item.categoryIconKey)
                },
                contentDescription = titleText,
                tint = badgeColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!tx.receiptPath.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.Rounded.AttachFile,
                        contentDescription = "Fiş ekli",
                        tint = kesemColors.subtleText,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = detailParts,
                style = MaterialTheme.typography.bodySmall,
                color = kesemColors.subtleText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        AmountText(
            amountKurus = signedKurus,
            hidden = hideBalances,
            showPlusSign = showPlus,
            color = amountColor,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
