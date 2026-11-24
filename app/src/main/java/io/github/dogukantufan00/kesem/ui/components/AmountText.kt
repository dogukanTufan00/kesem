package io.github.dogukantufan00.kesem.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import io.github.dogukantufan00.kesem.util.MoneyFormatter

@Composable
fun AmountText(
    amountKurus: Long,
    modifier: Modifier = Modifier,
    hidden: Boolean = false,
    showPlusSign: Boolean = false,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1
) {
    val formatted = MoneyFormatter.formatKurus(
        amountKurus = amountKurus,
        hidden = hidden,
        showPlusSign = showPlusSign
    )
    Text(
        text = formatted,
        modifier = modifier,
        color = color,
        style = style.copy(fontFeatureSettings = "tnum"),
        fontWeight = fontWeight ?: style.fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}
