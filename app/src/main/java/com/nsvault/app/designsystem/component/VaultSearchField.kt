package com.nsvault.app.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors
import com.nsvault.app.designsystem.theme.VaultGradients

/**
 * Glass search pill with instant-as-you-type semantics and a clear
 * affordance once text is present.
 */
@Composable
fun VaultSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.buttonHeight)
            .clip(CircleShape)
            .background(VaultColors.GlassFill)
            .border(Dimens.hairline, VaultGradients.GlassBorder, CircleShape)
            .padding(horizontal = Dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = VaultColors.TextTertiary,
            modifier = Modifier.size(Dimens.iconSize),
        )
        Spacer(modifier = Modifier.width(Dimens.spaceSm))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = VaultColors.TextTertiary,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = VaultColors.TextPrimary),
                cursorBrush = SolidColor(VaultColors.AuroraViolet),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear search",
                    tint = VaultColors.TextSecondary,
                    modifier = Modifier.size(Dimens.iconSize),
                )
            }
        }
    }
}
