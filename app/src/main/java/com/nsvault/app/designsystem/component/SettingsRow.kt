package com.nsvault.app.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors

/**
 * Standard settings entry: leading icon chip, title/subtitle, and a
 * trailing slot (chevron by default). Use [SettingsSwitchRow] for toggles.
 */
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = VaultColors.TextTertiary,
        )
    },
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .heightIn(min = Dimens.buttonHeight)
            .padding(horizontal = Dimens.spaceXs, vertical = Dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.iconContainer)
                .clip(MaterialTheme.shapes.small)
                .background(VaultColors.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VaultColors.AuroraIndigo,
                modifier = Modifier.size(Dimens.iconSize),
            )
        }
        Spacer(modifier = Modifier.width(Dimens.spaceMd))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = VaultColors.TextPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultColors.TextTertiary,
                )
            }
        }
        Spacer(modifier = Modifier.width(Dimens.spaceSm))
        trailing()
    }
}

/**
 * Settings entry whose trailing element is a themed switch. The whole
 * row is the touch target.
 */
@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        onClick = if (enabled) {
            { onCheckedChange(!checked) }
        } else {
            null
        },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = VaultColors.AuroraViolet,
                    checkedThumbColor = VaultColors.TextPrimary,
                    uncheckedTrackColor = VaultColors.SurfaceHigh,
                    uncheckedThumbColor = VaultColors.TextTertiary,
                    uncheckedBorderColor = VaultColors.Hairline,
                ),
            )
        },
    )
}
