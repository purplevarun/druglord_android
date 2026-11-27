package com.purplevarun.trader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.purplevarun.trader.R
import com.purplevarun.trader.engine.RunState

/**
 * The sheet that asks before a run ends: what the goods fetch today, and what the run scores.
 * On the last day of a finite run it reads as a finish; at any other time, and in an endless
 * run, it reads as a retirement.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EndRunSheet(state: RunState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val title = stringResource(if (state.isOnLastDay) R.string.finish_run_title else R.string.end_run_title)
    val confirm = stringResource(if (state.isOnLastDay) R.string.finish_the_run else R.string.end_the_run)

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
                .testTag(TestTags.END_RUN_SHEET),
        ) {
            // Only the words scroll, so the buttons are always on screen.
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(UiDimens.GrabHandleWidth, UiDimens.GrabHandleHeight)
                        .clip(CircleShape)
                        .background(UiColors.Mute.copy(alpha = UiType.GRAB_HANDLE_ALPHA)),
                )
                Spacer(Modifier.height(UiDimens.GrabHandleGap))
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(UiDimens.SheetLineGap),
                ) {
                    BasicText(
                        title,
                        style = UiType.SheetTitle,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    BasicText(
                        text = stringResource(
                            R.string.end_run_net_worth,
                            formatRupees(state.netWorthRupees),
                            stringResource(rankNameRes(state.rank)),
                        ),
                        style = UiType.Body,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
                Spacer(Modifier.height(UiDimens.SheetLineGap))
                BasicText(stringResource(R.string.end_run_explain), style = UiType.Body)
            }
            Spacer(Modifier.height(UiDimens.SheetActionsGap))
            PrimaryButton(
                text = confirm,
                enabled = true,
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.CONFIRM_END_RUN),
            )
            TextButton(
                text = stringResource(R.string.keep_trading),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
