package com.nsvault.app.feature.auth.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.nsvault.app.designsystem.component.KeypadAction
import com.nsvault.app.designsystem.component.PinDots
import com.nsvault.app.designsystem.component.PinKeypad
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors

/**
 * The one PIN-entry composition used by setup, lock, and change-PIN,
 * so the interaction is pixel-identical everywhere.
 */
@Composable
fun PinEntrySection(
    title: String,
    length: Int,
    filled: Int,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleIsError: Boolean = false,
    errorPulse: Int = 0,
    success: Boolean = false,
    enabled: Boolean = true,
    leftAction: KeypadAction? = null,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = VaultColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Dimens.spaceXs))
        Text(
            text = subtitle.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (subtitleIsError) VaultColors.Error else VaultColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(modifier = Modifier.height(Dimens.spaceXl))
        PinDots(
            length = length,
            filled = filled,
            errorPulse = errorPulse,
            success = success,
        )
        Spacer(modifier = Modifier.height(Dimens.spaceXxl))
        PinKeypad(
            onDigit = onDigit,
            onBackspace = onBackspace,
            enabled = enabled,
            leftAction = leftAction,
        )
        footer()
    }
}
