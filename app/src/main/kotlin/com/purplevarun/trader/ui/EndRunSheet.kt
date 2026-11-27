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

    Sheet(
        onDismiss = onDismiss,
        testTag = TestTags.END_RUN_SHEET,
        content = {
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
        },
        actions = {
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
        },
    )
}
