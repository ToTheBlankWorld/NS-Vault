package com.nsvault.app.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.nsvault.app.designsystem.theme.VaultColors

/**
 * Dashboard statistic: a large value over a quiet label.
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle? = null,
) {
    GlassCard(modifier = modifier) {
        Text(
            text = value,
            style = valueStyle ?: MaterialTheme.typography.headlineLarge,
            color = VaultColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = VaultColors.TextTertiary,
        )
    }
}
