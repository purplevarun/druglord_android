package com.purplevarun.trader.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.purplevarun.trader.R
import com.purplevarun.trader.engine.Action
import com.purplevarun.trader.engine.Game
import com.purplevarun.trader.engine.GameConstants
import com.purplevarun.trader.engine.GameEvent
import com.purplevarun.trader.engine.RunState
import kotlin.math.abs

/**
 * Something the day brought: one event at a time, told in a sheet, dismissed with a tap.
 * The state already reflects what happened; this only narrates it. When more events are
 * queued behind, the sheet says how many.
 */
@Composable
fun EventSheet(event: GameEvent, moreAfter: Int, onDismissed: () -> Unit) {
    Sheet(
        onDismiss = onDismissed,
        testTag = TestTags.EVENT_SHEET,
        content = {
            TitleLine(
                title = stringResource(eventTitleRes(event)),
                status = if (moreAfter > 0) {
                    stringResource(R.string.event_more_to_tell, moreAfter)
                } else {
                    ""
                },
            )
            Spacer(Modifier.height(UiDimens.SheetLineGap))
            BasicText(eventBody(event), style = UiType.Body)
        },
        actions = {
            PrimaryButton(
                text = stringResource(R.string.event_got_it),
                enabled = true,
                onClick = onDismissed,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.GOT_IT_EVENT),
            )
        },
    )
}

@StringRes
private fun eventTitleRes(event: GameEvent): Int = when (event) {
    is GameEvent.Pickpocket -> R.string.event_pickpocket_title
    is GameEvent.Checkpost -> R.string.event_checkpost_title
    is GameEvent.Bandh -> R.string.event_bandh_title
    is GameEvent.PriceSwing -> eventHeadlineRes(event.headline)
}

@Composable
private fun eventBody(event: GameEvent): String = when (event) {
    is GameEvent.Pickpocket ->
        stringResource(R.string.event_pickpocket_body, formatRupees(event.lostRupees))
    is GameEvent.Checkpost -> {
        val seized = event.seizedGood
        if (seized == null) {
            stringResource(R.string.event_checkpost_bribe_body, formatRupees(event.bribeRupees))
        } else {
            stringResource(
                R.string.event_checkpost_seized_body,
                event.seizedUnits,
                stringResource(goodNameRes(seized)),
            )
        }
    }
    is GameEvent.Bandh -> stringResource(R.string.event_bandh_body)
    is GameEvent.PriceSwing -> {
        val good = stringResource(goodNameRes(event.good))
        val city = stringResource(cityNameRes(event.city))
        val percent = abs(event.basisPoints) / 100
        if (event.basisPoints > 0) {
            stringResource(R.string.event_price_up_body, good, city, percent)
        } else {
            stringResource(R.string.event_price_down_body, good, city, percent)
        }
    }
}

/**
 * The collector has found you. Pay the whole debt — cash first, then the bank — or the run
 * ends here. The sheet cannot be dismissed away: he waits for nothing.
 */
@Composable
fun CollectorSheet(state: RunState, onAction: (Action) -> Unit, onDeath: () -> Unit) {
    val canPay = Game.check(state, Action.PayCollector) == null
    Sheet(
        onDismiss = {},
        testTag = TestTags.COLLECTOR_SHEET,
        content = {
            TitleLine(title = stringResource(R.string.collector_title), status = "")
            Spacer(Modifier.height(UiDimens.SheetLineGap))
            BasicText(
                stringResource(R.string.collector_body, formatRupees(state.debtRupees)),
                style = UiType.Body,
            )
        },
        actions = {
            PrimaryButton(
                text = stringResource(R.string.collector_pay, formatRupees(state.debtRupees)),
                enabled = canPay,
                onClick = { onAction(Action.PayCollector) },
                modifier = Modifier.testTag(TestTags.PAY_COLLECTOR),
            )
            TextButton(
                text = stringResource(R.string.meet_fate),
                onClick = onDeath,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.MEET_FATE),
            )
        },
    )
}

/**
 * Nothing left at all — not a rupee, not a good. The sahukar offers one last loan if you
 * owe nothing; otherwise this is simply the end. The sheet cannot be dismissed away.
 */
@Composable
fun BrokeSheet(state: RunState, onAction: (Action) -> Unit, onDeath: () -> Unit) {
    val canBorrow = state.debtRupees == 0L
    Sheet(
        onDismiss = {},
        testTag = TestTags.BROKE_SHEET,
        content = {
            TitleLine(title = stringResource(R.string.broke_title), status = "")
            Spacer(Modifier.height(UiDimens.SheetLineGap))
            BasicText(
                text = if (canBorrow) {
                    stringResource(R.string.broke_body)
                } else {
                    stringResource(R.string.broke_body) + " " + stringResource(R.string.broke_deadbeat)
                },
                style = UiType.Body,
            )
        },
        actions = {
            PrimaryButton(
                text = stringResource(R.string.broke_borrow, formatRupees(GameConstants.SURVIVAL_LOAN_RUPEES)),
                enabled = canBorrow,
                onClick = { onAction(Action.Borrow(GameConstants.SURVIVAL_LOAN_RUPEES)) },
                modifier = Modifier.testTag(TestTags.BORROW_AGAIN),
            )
            TextButton(
                text = stringResource(R.string.meet_fate),
                onClick = onDeath,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.MEET_FATE),
            )
        },
    )
}
