package com.nsvault.app.designsystem.layout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive width caps. Screens fill small phones edge-to-edge and
 * stay centered, readable columns on foldables and tablets. Place the
 * capped child in a container with `CenterHorizontally` alignment.
 */
object VaultLayout {
    /** General content column (home, library, settings). */
    val ContentMaxWidth = 600.dp

    /** Focused single-task columns (PIN entry, dialog-like screens). */
    val FocusMaxWidth = 420.dp
}

/** Cap width at [max], then fill the available space up to it. */
fun Modifier.vaultContentWidth(max: Dp = VaultLayout.ContentMaxWidth): Modifier =
    widthIn(max = max).fillMaxWidth()
