package com.nsvault.app.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors

/**
 * Themed modal bottom sheet: raised obsidian surface, soft top radius,
 * and a quiet pill drag handle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = VaultColors.SurfaceRaised,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = Dimens.spaceSm)
                    .width(Dimens.spaceXl)
                    .height(Dimens.spaceXxs)
                    .background(VaultColors.TextTertiary, CircleShape),
            )
        },
    ) {
        Box(modifier = Modifier.navigationBarsPadding()) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(
                    start = Dimens.screenPadding,
                    end = Dimens.screenPadding,
                    bottom = Dimens.spaceLg,
                ),
                content = content,
            )
        }
    }
}
