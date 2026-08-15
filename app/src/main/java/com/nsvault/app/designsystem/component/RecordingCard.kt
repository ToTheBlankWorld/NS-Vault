package com.nsvault.app.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors

/**
 * A single memory in the vault: title, date, duration, and favorite.
 *
 * Two trailing modes: pass [onToggleFavorite] for an inline star
 * toggle (home screen), or [onMore] for an overflow menu with the
 * favorite shown as a quiet inline glyph (library rows, where actions
 * live in a sheet).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordingCard(
    title: String,
    dateText: String,
    durationText: String,
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentPadding = Dimens.spaceMd,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(Dimens.iconContainer)
                    .background(VaultColors.SurfaceHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = VaultColors.AuroraCyan,
                    modifier = Modifier.size(Dimens.iconSize),
                )
            }
            Spacer(modifier = Modifier.width(Dimens.spaceMd))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isFavorite && onMore != null) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Favorite",
                            tint = VaultColors.Gold,
                            modifier = Modifier.size(Dimens.spaceMd),
                        )
                        Spacer(modifier = Modifier.width(Dimens.spaceXxs))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = VaultColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "$dateText · $durationText",
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultColors.TextTertiary,
                )
            }
            when {
                onMore != null -> IconButton(onClick = onMore) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options",
                        tint = VaultColors.TextSecondary,
                    )
                }

                onToggleFavorite != null -> IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = if (isFavorite) {
                            "Remove from favorites"
                        } else {
                            "Add to favorites"
                        },
                        tint = if (isFavorite) VaultColors.Gold else VaultColors.TextTertiary,
                    )
                }
            }
        }
    }
}
