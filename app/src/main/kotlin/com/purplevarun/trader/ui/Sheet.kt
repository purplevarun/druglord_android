package com.purplevarun.trader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.purplevarun.trader.R

/**
 * The chrome every bottom sheet shares: a dimmed map behind, a panel with a grab handle,
 * words that scroll, and actions that never leave the screen. Tapping the dimmed area or
 * the sheet's own close control dismisses it.
 */
@Composable
fun Sheet(
    onDismiss: () -> Unit,
    testTag: String,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(UiColors.Scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = stringResource(R.string.close),
                    onClick = onDismiss,
                ),
        )
        val shape = RoundedCornerShape(topStart = UiDimens.DrawerCorner, topEnd = UiDimens.DrawerCorner)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = maxHeight * UiDimens.SHEET_MAX_HEIGHT_FRACTION)
                .clip(shape)
                .background(UiColors.Panel)
                .border(UiDimens.DrawerBorder, UiColors.Line, shape)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(horizontal = UiDimens.ScreenPadding)
                .padding(top = UiDimens.DrawerPaddingTop, bottom = UiDimens.DrawerPaddingBottom)
                .testTag(testTag),
        ) {
            // Only the words scroll, so the actions are always on screen.
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(UiDimens.GrabHandleWidth, UiDimens.GrabHandleHeight)
                        .clip(CircleShape)
                        .background(UiColors.Mute.copy(alpha = UiType.GRAB_HANDLE_ALPHA)),
                )
                Spacer(Modifier.height(UiDimens.GrabHandleGap))
                content()
            }
            Spacer(Modifier.height(UiDimens.SheetActionsGap))
            actions()
        }
    }
}
