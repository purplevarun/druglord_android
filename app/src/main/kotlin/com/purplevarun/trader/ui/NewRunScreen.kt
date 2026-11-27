package com.purplevarun.trader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.purplevarun.trader.BestRun
import com.purplevarun.trader.R
import com.purplevarun.trader.engine.GameConstants

/**
 * Where a run begins: pick how many days on the rails, see the best runs so far, and start.
 * The options and the list scroll if they must; the start button is pinned to the bottom.
 */
@Composable
fun NewRunScreen(bestRuns: List<BestRun>, onStart: (Int) -> Unit, modifier: Modifier = Modifier) {
    var selected by rememberSaveable { mutableIntStateOf(GameConstants.DEFAULT_RUN_LENGTH_DAYS) }

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
            Spacer(Modifier.height(UiDimens.SectionGap))
            BasicText(stringResource(R.string.new_run_subtitle), style = UiType.Title)
            Spacer(Modifier.height(UiDimens.OptionRowGap))
            for (days in GameConstants.RUN_LENGTH_OPTIONS_DAYS) {
                RunLengthRow(
                    selected = selected == days,
                    text = stringResource(R.string.run_length_days, days),
                    hint = null,
                    testTag = TestTags.runLength(days),
                    onSelect = { selected = days },
                )
                Spacer(Modifier.height(UiDimens.OptionRowGap))
            }
            RunLengthRow(
                selected = selected == GameConstants.ENDLESS_RUN_LENGTH_DAYS,
                text = stringResource(R.string.run_length_endless),
                hint = stringResource(R.string.run_length_endless_hint),
                testTag = TestTags.RUN_LENGTH_ENDLESS,
                onSelect = { selected = GameConstants.ENDLESS_RUN_LENGTH_DAYS },
            )
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
                onClick = { onStart(selected) },
                modifier = Modifier.testTag(TestTags.START_RUN),
            )
        }
    }
}

/** One row of the run-length choice: a paper card with a dot that inks in when picked. */
@Composable
private fun RunLengthRow(
    selected: Boolean,
    text: String,
    hint: String?,
    testTag: String,
    onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(UiDimens.ButtonCorner)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = UiDimens.MinTouchTarget)
            .clip(shape)
            .border(UiDimens.DrawerBorder, if (selected) UiColors.Ink else UiColors.Line, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = UiDimens.ButtonPaddingHorizontal)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(UiDimens.OptionDotSize).clip(CircleShape).border(UiDimens.RoundButtonBorder, UiColors.Ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(Modifier.size(UiDimens.OptionDotInnerSize).clip(CircleShape).background(UiColors.Ink))
            }
        }
        Spacer(Modifier.width(UiDimens.OptionDotToTextGap))
        Column(Modifier.weight(1f).padding(vertical = UiDimens.RowPaddingVertical)) {
            BasicText(text, style = UiType.GoodName)
            if (hint != null) {
                BasicText(hint, style = UiType.Small)
            }
        }
    }
}

@Composable
private fun BestRunRow(run: BestRun) {
    val length = if (run.runLengthDays == GameConstants.ENDLESS_RUN_LENGTH_DAYS) {
        stringResource(R.string.best_run_endless_day, run.daysPlayed)
    } else {
        pluralStringResource(R.plurals.best_run_days, run.runLengthDays, run.runLengthDays)
    }
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
                length,
            ),
            style = UiType.Body,
        )
    }
}
