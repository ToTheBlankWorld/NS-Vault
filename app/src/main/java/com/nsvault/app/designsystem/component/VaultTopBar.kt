package com.nsvault.app.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors

@Composable
fun VaultTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Dimens.spaceXs, vertical = Dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(Dimens.minTouchTarget)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = VaultColors.TextPrimary,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(Dimens.spaceSm))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = VaultColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}
