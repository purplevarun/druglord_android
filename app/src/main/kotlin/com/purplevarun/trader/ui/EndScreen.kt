package com.purplevarun.trader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.purplevarun.trader.R
import com.purplevarun.trader.engine.RunState
import java.util.Locale

/**
 * How a run ended: the final net worth, the rank it earned, and a way back to the start.
 * The story scrolls if it must; the next-run button is pinned to the bottom.
 */
@Composable
fun EndScreen(run: RunState, onNewRun: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(UiColors.Paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(TestTags.END_SCREEN),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UiDimens.ScreenPadding)
                .padding(top = UiDimens.HeaderPaddingTop),
        ) {
            BasicText(
                text = stringResource(R.string.run_over, dateText(run.day)).uppercase(Locale.ROOT),
                style = UiType.Overline,
            )
            BasicText(
                text = formatRupees(run.netWorthRupees),
                style = UiType.NetWorth,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = UiType.NetWorthMinSize,
                    maxFontSize = UiType.NetWorthMaxSize,
                    stepSize = UiType.NetWorthSizeStep,
                ),
            )
            Spacer(Modifier.height(UiDimens.RankGap))
            BasicText(
                text = stringResource(rankNameRes(run.rank)),
                style = UiType.Rank,
            )
            Spacer(Modifier.height(UiDimens.EndScreenGap))
            BasicText(
                text = if (run.isEndless) {
                    stringResource(R.string.end_days_endless, run.day)
                } else {
                    stringResource(R.string.day_of_run, run.day, run.runLengthDays)
                },
                style = UiType.Body,
            )
            BasicText(
                text = stringResource(
                    R.string.cash_and_debt,
                    formatRupees(run.cashRupees),
                    formatRupees(run.debtRupees),
                ),
                style = UiType.Body,
            )
        }
        Column(Modifier.padding(horizontal = UiDimens.ScreenPadding, vertical = UiDimens.FooterPaddingVertical)) {
            PrimaryButton(
                text = stringResource(R.string.new_run),
                enabled = true,
                onClick = onNewRun,
                modifier = Modifier.testTag(TestTags.NEW_RUN),
            )
        }
    }
}
