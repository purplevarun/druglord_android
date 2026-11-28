package com.purplevarun.trader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.purplevarun.trader.BestRun
import com.purplevarun.trader.R

/**
 * Where a run begins: the best runs so far and a start button. Every run is endless —
 * you retire when you choose, if the sahukar lets you live that long.
 */
@Composable
fun NewRunScreen(bestRuns: List<BestRun>, onStart: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(UiColors.Paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(TestTags.NEW_RUN_SCREEN),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UiDimens.ScreenPadding)
                .padding(top = UiDimens.HeaderPaddingTop),
        ) {
            BasicText(
                text = stringResource(R.string.app_name),
                style = UiType.NetWorth,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = UiType.NetWorthMinSize,
                    maxFontSize = UiType.NetWorthMaxSize,
                    stepSize = UiType.NetWorthSizeStep,
                ),
            )
            Spacer(Modifier.height(UiDimens.OptionRowGap))
            BasicText(stringResource(R.string.new_run_subtitle), style = UiType.Body)
            Spacer(Modifier.height(UiDimens.SectionGap))
            BasicText(stringResource(R.string.best_runs_title), style = UiType.Title)
            Spacer(Modifier.height(UiDimens.OptionRowGap))
            if (bestRuns.isEmpty()) {
                BasicText(stringResource(R.string.no_best_runs), style = UiType.Body)
            }
            for (run in bestRuns) {
                BestRunRow(run)
            }
        }
        Column(Modifier.padding(horizontal = UiDimens.ScreenPadding, vertical = UiDimens.FooterPaddingVertical)) {
            PrimaryButton(
                text = stringResource(R.string.start_the_run),
                enabled = true,
                onClick = onStart,
                modifier = Modifier.testTag(TestTags.START_RUN),
            )
        }
    }
}

@Composable
private fun BestRunRow(run: BestRun) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = UiDimens.MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = stringResource(
                R.string.best_run_line,
                formatRupees(run.netWorthRupees),
                stringResource(rankNameRes(run.rank)),
                pluralStringResource(R.plurals.best_run_days, run.daysPlayed, run.daysPlayed),
            ),
            style = UiType.Body,
        )
    }
}
