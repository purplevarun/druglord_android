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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.purplevarun.trader.R
import com.purplevarun.trader.engine.CityId
import com.purplevarun.trader.engine.GameConstants
import com.purplevarun.trader.engine.RunState
import java.util.Locale

/**
 * The home screen: net worth, the rail map, and the market drawer.
 *
 * Nothing here has a fixed pixel size. On a tall screen the header and map stack and scroll if
 * they must; on a wide or landscape screen the map sits beside the header. The market drawer is
 * always pinned to the bottom so it can never be scrolled out of reach.
 */
@Composable
fun HomeScreen(
    state: RunState,
    onOpenMarket: () -> Unit,
    onCityTap: (CityId) -> Unit,
    onEndRun: () -> Unit,
    onOpenPlace: (Overlay) -> Unit,
    modifier: Modifier = Modifier,
    route: List<CityId> = emptyList(),
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(UiColors.Paper)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val sideBySide = maxWidth >= UiDimens.SideBySideMinWidth || maxWidth > maxHeight
        val placesMaxHeight = maxHeight * UiDimens.PLACES_HEIGHT_FRACTION
        if (sideBySide) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Header(state, onEndRun)
                    }
                    FinishBar(state, onEndRun)
                    MarketDrawer(state, onOpenMarket, onOpenPlace, placesMaxHeight)
                }
                RailMap(state.city, onCityTap, Modifier.weight(1f).fillMaxHeight(), route)
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Header(state, onEndRun)
                    RailMap(
                        state.city,
                        onCityTap,
                        Modifier.fillMaxWidth().aspectRatio(MapGeometry.ASPECT_RATIO),
                        route,
                    )
                }
                FinishBar(state, onEndRun)
                MarketDrawer(state, onOpenMarket, onOpenPlace, placesMaxHeight)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(state: RunState, onEndRun: () -> Unit) {
    val today = dateText(state.day)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = UiDimens.ScreenPadding)
            .padding(top = UiDimens.HeaderPaddingTop, bottom = UiDimens.HeaderPaddingBottom),
    ) {
        BasicText(
            text = stringResource(R.string.net_worth_label, today).uppercase(Locale.ROOT),
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
        if (!state.isEndless) {
            Spacer(Modifier.height(UiDimens.HeaderProgressGapTop))
            RunProgress(fraction = state.day.toFloat() / state.runLengthDays)
        }
        Spacer(Modifier.height(UiDimens.HeaderProgressGapBottom))
        // Sits on one line when there is room and wraps when there is not.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(UiDimens.HeaderLineGap),
        ) {
            BasicText(
                text = if (state.isEndless) {
                    stringResource(R.string.day_endless, state.day)
                } else {
                    stringResource(R.string.day_of_run, state.day, state.runLengthDays)
                },
                style = UiType.Body,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            BasicText(
                text = stringResource(
                    R.string.cash_and_debt,
                    formatRupees(state.cashRupees),
                    formatRupees(state.debtRupees),
                ),
                style = UiType.Body,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            TextButton(
                text = stringResource(R.string.end_the_run),
                onClick = onEndRun,
                modifier = Modifier.testTag(TestTags.END_RUN),
            )
        }
    }
}

/**
 * On the last day of a finite run, the way out: a wide button pinned above the market drawer
 * so it is always on screen. Endless runs never get one; they end from the header instead.
 */
@Composable
private fun FinishBar(state: RunState, onEndRun: () -> Unit) {
    if (!state.isOnLastDay) return
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = UiDimens.ScreenPadding)
            .padding(bottom = UiDimens.FinishBarPaddingBottom),
    ) {
        PrimaryButton(
            text = stringResource(R.string.finish_the_run),
            enabled = true,
            onClick = onEndRun,
            modifier = Modifier.testTag(TestTags.FINISH_RUN),
        )
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

/**
 * The drawer pinned to the bottom of the home screen: the city's mandi up top, and below
 * it the places every city has — the sahukar, the bank and the luggage shop — each a chip
 * that opens its own sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MarketDrawer(
    state: RunState,
    onOpen: () -> Unit,
    onOpenPlace: (Overlay) -> Unit,
    placesMaxHeight: Dp,
) {
    val shape = RoundedCornerShape(topStart = UiDimens.DrawerCorner, topEnd = UiDimens.DrawerCorner)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(UiColors.Panel)
            .border(UiDimens.DrawerBorder, UiColors.Line, shape)
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
        Column(
            Modifier
                .fillMaxWidth()
                .testTag(TestTags.MARKET_DRAWER)
                .clip(RoundedCornerShape(UiDimens.ButtonCorner))
                .clickable(
                    onClickLabel = stringResource(R.string.open_market),
                    role = Role.Button,
                    onClick = onOpen,
                )
                .heightIn(min = UiDimens.MinTouchTarget),
        ) {
            BasicText(
                text = stringResource(R.string.market_title, stringResource(cityNameRes(state.city))),
                style = UiType.Title,
            )
            BasicText(
                text = stringResource(R.string.market_hint, GameConstants.GOODS.size),
                style = UiType.Body,
            )
        }
        Spacer(Modifier.height(UiDimens.PlacesGapTop))
        // Capped and scrolled inside, so a big text size can never push the drawer off screen.
        FlowRow(
            Modifier
                .fillMaxWidth()
                .heightIn(max = placesMaxHeight)
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(UiDimens.ChipGap),
            verticalArrangement = Arrangement.spacedBy(UiDimens.ChipGap),
        ) {
            PlaceChip(
                name = stringResource(R.string.place_sahukar),
                status = if (state.debtRupees > 0) {
                    stringResource(R.string.owe_amount, formatRupees(state.debtRupees))
                } else {
                    stringResource(R.string.no_debt)
                },
                testTag = TestTags.PLACE_SAHUKAR,
                onClick = { onOpenPlace(Overlay.Sahukar) },
            )
            PlaceChip(
                name = stringResource(R.string.place_bank),
                status = if (state.bankRupees > 0) {
                    stringResource(R.string.saved_amount, formatRupees(state.bankRupees))
                } else {
                    stringResource(R.string.nothing_saved)
                },
                testTag = TestTags.PLACE_BANK,
                onClick = { onOpenPlace(Overlay.Bank) },
            )
            PlaceChip(
                name = stringResource(R.string.place_luggage),
                status = stringResource(
                    R.string.transport_and_capacity,
                    stringResource(transportNameRes(state.transport)),
                    state.luggageCapacity,
                ),
                testTag = TestTags.PLACE_LUGGAGE,
                onClick = { onOpenPlace(Overlay.Luggage) },
            )
            PlaceChip(
                name = stringResource(R.string.place_godown),
                status = if (state.godownUsed(state.city) > 0) {
                    stringResource(
                        R.string.godown_stored,
                        state.godownUsed(state.city),
                        GameConstants.GODOWN_DAILY_RENT_RUPEES,
                    )
                } else {
                    stringResource(R.string.godown_nothing)
                },
                testTag = TestTags.PLACE_GODOWN,
                onClick = { onOpenPlace(Overlay.Godown) },
            )
        }
    }
}

/** One place in the drawer: its name and how you stand with it, small enough to sit in a row. */
@Composable
private fun PlaceChip(name: String, status: String, testTag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(UiDimens.ButtonCorner)
    Column(
        Modifier
            .heightIn(min = UiDimens.MinTouchTarget)
            .clip(shape)
            .background(UiColors.Paper)
            .border(UiDimens.ChipBorder, UiColors.Line, shape)
            .clickable(role = Role.Button, onClickLabel = name, onClick = onClick)
            .padding(
                horizontal = UiDimens.ChipPaddingHorizontal,
                vertical = UiDimens.ChipPaddingVertical,
            )
            .testTag(testTag),
    ) {
        BasicText(name, style = UiType.PlaceName)
        Spacer(Modifier.height(UiDimens.PlaceChipLineGap))
        BasicText(status, style = UiType.Small)
    }
}
