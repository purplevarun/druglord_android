package com.purplevarun.trader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import com.purplevarun.trader.R
import com.purplevarun.trader.engine.Action
import com.purplevarun.trader.engine.Game
import com.purplevarun.trader.engine.GameConstants
import com.purplevarun.trader.engine.Good
import com.purplevarun.trader.engine.GoodId
import com.purplevarun.trader.engine.Outcome
import com.purplevarun.trader.engine.RunState
import kotlin.math.max

/** The market of the city you are in: one row per good, with − to sell one and + to buy one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarketScreen(
    state: RunState,
    onBuy: (GoodId) -> Unit,
    onSell: (GoodId) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(UiColors.Paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(TestTags.MARKET),
    ) {
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = UiDimens.ScreenPadding)
                .padding(top = UiDimens.BarPaddingTop, bottom = UiDimens.BarPaddingBottom),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(UiDimens.BarLineGap),
        ) {
            BasicText(
                text = stringResource(R.string.market_title, stringResource(cityNameRes(state.city))),
                style = UiType.Title,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            BasicText(
                text = stringResource(R.string.market_status, state.day, formatRupees(state.cashRupees)),
                style = UiType.Body,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        Divider()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            for (good in GameConstants.GOODS) {
                MarketRow(state, good, onBuy, onSell)
                Divider()
            }
        }
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(start = UiDimens.ScreenPadding)
                .padding(vertical = UiDimens.FooterPaddingVertical),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicText(
                text = stringResource(R.string.luggage, state.luggageUsed, state.luggageCapacity),
                style = UiType.Body,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            TextButton(
                text = stringResource(R.string.done),
                onClick = onClose,
                modifier = Modifier.testTag(TestTags.MARKET_DONE),
            )
        }
    }
}

@Composable
private fun MarketRow(state: RunState, good: Good, onBuy: (GoodId) -> Unit, onSell: (GoodId) -> Unit) {
    val name = stringResource(goodNameRes(good.id))
    val unit = stringResource(unitNameRes(good.unit))
    val held = state.held(good.id)
    val canBuy = Game.check(state, Action.Buy(good.id, 1)) == null
    val canSell = Game.check(state, Action.Sell(good.id, 1)) == null

    InfoBesideOrAboveTrade(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = UiDimens.RowMinHeight)
            .padding(horizontal = UiDimens.ScreenPadding, vertical = UiDimens.RowPaddingVertical),
        info = {
            Column {
                BasicText(name, style = UiType.GoodName)
                BasicText(
                    text = if (held > 0) {
                        stringResource(R.string.held_some, unit, held)
                    } else {
                        stringResource(R.string.held_none, unit)
                    },
                    style = UiType.Small,
                )
            }
        },
        trade = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    BasicText(formatRupees(state.priceOf(good.id)), style = UiType.Price)
                    ChangeText(state.changePercentOf(good.id))
                }
                Spacer(Modifier.width(UiDimens.PriceToButtonsGap))
                RoundButton(
                    symbol = "−",
                    label = stringResource(R.string.sell_one, name),
                    filled = false,
                    enabled = canSell,
                    onClick = { onSell(good.id) },
                )
                RoundButton(
                    symbol = "+",
                    label = stringResource(R.string.buy_one, name),
                    filled = true,
                    enabled = canBuy,
                    onClick = { onBuy(good.id) },
                )
            }
        },
    )
}

@Composable
private fun ChangeText(percent: Int) {
    val (text, color) = when {
        percent > 0 -> stringResource(R.string.change_up, percent) to UiColors.Up
        percent < 0 -> stringResource(R.string.change_down, -percent) to UiColors.Down
        else -> stringResource(R.string.change_flat) to UiColors.Mute
    }
    BasicText(text, style = UiType.Change.copy(color = color))
}

/**
 * Puts the good's name beside its price and buttons when there is room for its longest word,
 * and above them, right-aligned, when there is not (a narrow phone or very large text).
 * Nothing is ever squeezed narrower than a word.
 */
@Composable
private fun InfoBesideOrAboveTrade(
    info: @Composable () -> Unit,
    trade: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(contents = listOf(info, trade), modifier = modifier) { (infoMeasurables, tradeMeasurables), constraints ->
        val width = constraints.maxWidth
        val gap = UiDimens.RowGap.roundToPx()
        val tradePlaceable = tradeMeasurables.single().measure(Constraints(maxWidth = width))
        val infoMeasurable = infoMeasurables.single()
        val besideWidth = width - tradePlaceable.width - gap
        val fitsBeside = besideWidth > 0 && infoMeasurable.minIntrinsicWidth(Constraints.Infinity) <= besideWidth

        if (fitsBeside) {
            val infoPlaceable = infoMeasurable.measure(Constraints(maxWidth = besideWidth))
            val height = max(max(infoPlaceable.height, tradePlaceable.height), constraints.minHeight)
            layout(width, height) {
                infoPlaceable.place(0, (height - infoPlaceable.height) / 2)
                tradePlaceable.place(width - tradePlaceable.width, (height - tradePlaceable.height) / 2)
            }
        } else {
            val infoPlaceable = infoMeasurable.measure(Constraints(maxWidth = width))
            val stackGap = UiDimens.RowStackGap.roundToPx()
            val height = max(infoPlaceable.height + stackGap + tradePlaceable.height, constraints.minHeight)
            layout(width, height) {
                infoPlaceable.place(0, 0)
                tradePlaceable.place(width - tradePlaceable.width, infoPlaceable.height + stackGap)
            }
        }
    }
}
