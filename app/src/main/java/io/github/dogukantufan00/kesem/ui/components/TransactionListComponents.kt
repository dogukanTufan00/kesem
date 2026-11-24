package io.github.dogukantufan00.kesem.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.model.TransactionWithDetails
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.util.MoneyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

fun groupTransactionsByDate(
    items: List<TransactionWithDetails>,
    today: LocalDate = LocalDate.now()
): List<Pair<String, List<TransactionWithDetails>>> {
    if (items.isEmpty()) return emptyList()
    return items
        .groupBy { MoneyFormatter.formatDateHeader(it.transaction.dateMillis, today = today) }
        .entries
        .map { it.key to it.value }
}

@Composable
fun DateGroupHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = LocalKesemColors.current.subtleText,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    KesemCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LocalKesemColors.current.subtleText,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                FilledTonalButton(
                    onClick = onAction,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

@Composable
fun ReceiptThumbnail(
    file: File,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, file.absolutePath) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
            }.getOrNull()
        }
    }

    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, LocalKesemColors.current.cardBorder, shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "Fiş önizleme",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Text(
                text = "Fiş",
                style = MaterialTheme.typography.labelSmall,
                color = LocalKesemColors.current.subtleText
            )
        }
    }
}
