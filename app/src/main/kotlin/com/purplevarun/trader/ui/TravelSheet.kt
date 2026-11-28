package com.purplevarun.trader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val canBoard = Game.check(state, Action.Travel(destination)) == null

    val daysText = pluralStringResource(R.plurals.trip_days, days, days)
    val changes = route.drop(1).dropLast(1)
    val summary = if (changes.isEmpty()) {
        stringResource(R.string.trip_direct, daysText)
    } else {
        val separator = stringResource(R.string.list_separator)
        stringResource(R.string.trip_via, daysText, changes.map { stringResource(cityNameRes(it)) }.joinToString(separator))
    }
    val detail = stringResource(R.string.arrival, dateText(state.day + days))

    Sheet(
        onDismiss = onDismiss,
        testTag = TestTags.TRAVEL_SHEET,
        content = {
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
        },
        actions = {
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
        },
    )
}
