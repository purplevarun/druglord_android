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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.purplevarun.trader.R
import com.purplevarun.trader.engine.Action
import com.purplevarun.trader.engine.CityId
import com.purplevarun.trader.engine.Game
import com.purplevarun.trader.engine.Outcome
import com.purplevarun.trader.engine.RailNetwork
import com.purplevarun.trader.engine.RunState

/** The sheet that slides up when you tap another city: how long the trip takes, and a button to go. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TravelSheet(state: RunState, destination: CityId, onBoard: () -> Unit, onDismiss: () -> Unit) {
    val route = RailNetwork.route(state.city, destination)
    val days = RailNetwork.travelDays(state.city, destination)
    val canBoard = Game.apply(state, Action.Travel(destination)) is Outcome.Done

    val daysText = pluralStringResource(R.plurals.trip_days, days, days)
    val changes = route.drop(1).dropLast(1)
    val summary = if (changes.isEmpty()) {
        stringResource(R.string.trip_direct, daysText)
    } else {
        val separator = stringResource(R.string.list_separator)
        stringResource(R.string.trip_via, daysText, changes.map { stringResource(cityNameRes(it)) }.joinToString(separator))
    }
    val daysLeftAfter = state.daysLeft - days
    val detail = when {
        !canBoard -> stringResource(R.string.too_far, dateText(state.runLengthDays))
        state.isEndless -> stringResource(R.string.arrival_endless, dateText(state.day + days))
        else -> stringResource(
            R.string.arrival,
            dateText(state.day + days),
            pluralStringResource(R.plurals.days_left_after_trip, daysLeftAfter, daysLeftAfter),
        )
    }

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
                .testTag(TestTags.TRAVEL_SHEET),
        ) {
            // Only the words scroll, so the button is always on screen.
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
                        stringResource(cityNameRes(destination)),
                        style = UiType.SheetTitle,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    BasicText(summary, style = UiType.Body, modifier = Modifier.align(Alignment.CenterVertically))
                }
                Spacer(Modifier.height(UiDimens.SheetLineGap))
                BasicText(detail, style = if (canBoard) UiType.Body else UiType.Body.copy(color = UiColors.Down))
            }
            Spacer(Modifier.height(UiDimens.SheetActionsGap))
            PrimaryButton(
                text = stringResource(R.string.board_train),
                enabled = canBoard,
                onClick = onBoard,
                modifier = Modifier.testTag(TestTags.BOARD_TRAIN),
            )
            TextButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
