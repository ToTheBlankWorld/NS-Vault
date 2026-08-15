package com.nsvault.app.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nsvault.app.designsystem.theme.VaultColors

/**
 * The signature canvas of NS Vault: an obsidian base with two faint
 * aurora glows breathing at opposite corners. Every screen sits on it.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VaultColors.Obsidian)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VaultColors.AuroraViolet.copy(alpha = 0.14f),
                            Color.Transparent,
                        ),
                        center = Offset(x = size.width * 0.12f, y = size.height * 0.02f),
                        radius = size.width * 0.95f,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VaultColors.AuroraCyan.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        center = Offset(x = size.width * 0.95f, y = size.height * 0.92f),
                        radius = size.width * 0.85f,
                    ),
                )
            },
        content = content,
    )
}
