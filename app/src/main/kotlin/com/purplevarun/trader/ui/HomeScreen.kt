package com.purplevarun.trader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.purplevarun.trader.R
import com.purplevarun.trader.engine.CityId
import com.purplevarun.trader.engine.GameCalendar
import com.purplevarun.trader.engine.GameConstants
import java.util.Locale

data class HomeUiState(
    val netWorthRupees: Long,
    val cashRupees: Long,
    val debtRupees: Long,
    val dayOfRun: Int,
    val runLengthDays: Int,
    val currentCity: CityId,
)

/**
 * The home screen: net worth, the rail map, and the market drawer.
 *
 * Nothing here has a fixed pixel size. On a tall screen the header and map stack and scroll if
 * they must; on a wide or landscape screen the map sits beside the header. The market drawer is
 * always pinned to the bottom so it can never be scrolled out of reach.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onOpenMarket: () -> Unit = {},
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(UiColors.Paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(TestTags.SCREEN),
    ) {
        val sideBySide = maxWidth >= UiDimens.SideBySideMinWidth || maxWidth > maxHeight
        if (sideBySide) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Header(state)
                    }
                    MarketDrawer(state.currentCity, onOpenMarket)
                }
                RailMap(state.currentCity, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Header(state)
                    RailMap(state.currentCity, Modifier.fillMaxWidth().aspectRatio(MapGeometry.ASPECT_RATIO))
                }
                MarketDrawer(state.currentCity, onOpenMarket)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(state: HomeUiState) {
    val date = GameCalendar.dateOn(state.dayOfRun)
    val monthName = stringArrayResource(R.array.month_abbreviations)[date.month - 1]
    val dateText = stringResource(R.string.date_format, date.day, monthName, date.year)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = UiDimens.ScreenPadding)
            .padding(top = UiDimens.HeaderPaddingTop, bottom = UiDimens.HeaderPaddingBottom),
    ) {
        BasicText(
            text = stringResource(R.string.net_worth_label, dateText).uppercase(Locale.ROOT),
            style = UiType.Overline,
        )
        BasicText(
            text = formatRupees(state.netWorthRupees),
            modifier = Modifier.fillMaxWidth().testTag(TestTags.NET_WORTH),
            style = UiType.NetWorth,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = UiType.NetWorthMinSize,
                maxFontSize = UiType.NetWorthMaxSize,
                stepSize = UiType.NetWorthSizeStep,
            ),
        )
        Spacer(Modifier.height(UiDimens.HeaderProgressGapTop))
        RunProgress(fraction = state.dayOfRun.toFloat() / state.runLengthDays)
        Spacer(Modifier.height(UiDimens.HeaderProgressGapBottom))
        // Sits on one line when there is room and wraps onto two when there is not.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(UiDimens.HeaderLineGap),
        ) {
            BasicText(
                text = stringResource(R.string.day_of_run, state.dayOfRun, state.runLengthDays),
                style = UiType.Body,
            )
            BasicText(
                text = stringResource(
                    R.string.cash_and_debt,
                    formatRupees(state.cashRupees),
                    formatRupees(state.debtRupees),
                ),
                style = UiType.Body,
            )
        }
    }
}

@Composable
private fun RunProgress(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(UiDimens.ProgressHeight)
            .clip(CircleShape)
            .background(UiColors.Line),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(UiColors.Accent),
        )
    }
}

@Composable
private fun MarketDrawer(city: CityId, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(topStart = UiDimens.DrawerCorner, topEnd = UiDimens.DrawerCorner)
    Column(
        Modifier
            .fillMaxWidth()
            .testTag(TestTags.MARKET_DRAWER)
            .clip(shape)
            .background(UiColors.Panel)
            .border(UiDimens.DrawerBorder, UiColors.Line, shape)
            .clickable(
                onClickLabel = stringResource(R.string.open_market),
                role = Role.Button,
                onClick = onOpen,
            )
            .heightIn(min = UiDimens.MinTouchTarget)
            .padding(horizontal = UiDimens.ScreenPadding)
            .padding(top = UiDimens.DrawerPaddingTop, bottom = UiDimens.DrawerPaddingBottom),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(UiDimens.GrabHandleWidth, UiDimens.GrabHandleHeight)
                .clip(CircleShape)
                .background(UiColors.Mute.copy(alpha = UiType.GRAB_HANDLE_ALPHA)),
        )
        Spacer(Modifier.height(UiDimens.GrabHandleGap))
        BasicText(
            text = stringResource(R.string.market_title, stringResource(cityNameRes(city))),
            style = UiType.Title,
        )
        BasicText(
            text = stringResource(R.string.market_hint, GameConstants.GOODS.size),
            style = UiType.Body,
        )
    }
}
