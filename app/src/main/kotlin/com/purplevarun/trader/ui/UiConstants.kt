package com.purplevarun.trader.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.purplevarun.trader.R

/** Every colour, size and text style in the UI lives in this file. */
object UiColors {
    val Paper = Color(0xFFF3EEE3)
    val Panel = Color(0xFFFFFAF0)
    val Ink = Color(0xFF1D1B16)
    val Mute = Color(0xFF857C69)
    val Accent = Color(0xFFC8452C)
    val Line = Color(0xFFDDD3BF)
    val Land = Color(0xFFE9E0CC)
    val Up = Color(0xFF2F8F5B)
    val Down = Color(0xFFC8452C)
    val Scrim = Color(0x4D1D1B16)
    val OnAccent = Color(0xFFFFFFFF)
}

/** Sizes are in dp so they track the phone's display-size setting. */
object UiDimens {
    val ScreenPadding = 18.dp
    val MinTouchTarget = 48.dp

    /** At or above this width, or whenever the screen is wider than tall, the map sits beside the header. */
    val SideBySideMinWidth = 600.dp

    val HeaderPaddingTop = 16.dp
    val HeaderPaddingBottom = 6.dp
    val HeaderProgressGapTop = 8.dp
    val HeaderProgressGapBottom = 6.dp
    val HeaderLineGap = 2.dp
    val ProgressHeight = 4.dp

    val MapPadding = 12.dp
    val LandStroke = 1.5.dp
    val RailStroke = 1.dp
    val RailTieLength = 5.dp
    val RailTieThickness = 1.2.dp
    val RailTieSpacing = 5.dp
    val CityDotRadius = 6.dp
    val CurrentCityDotRadius = 9.dp
    val CityDotStroke = 1.5.dp
    val CityLabelGap = 5.dp

    val DrawerCorner = 20.dp
    val DrawerPaddingTop = 8.dp
    val DrawerPaddingBottom = 16.dp
    val DrawerBorder = 1.dp
    val GrabHandleWidth = 36.dp
    val GrabHandleHeight = 4.dp
    val GrabHandleGap = 10.dp

    /** A tap on the map picks the nearest city, if one is this close. */
    val CityTapRadius = 56.dp
    val RouteStroke = 2.dp
    val RouteTieLength = 6.dp
    val DestinationRingStroke = 3.dp

    val DividerThickness = 1.dp
    val BarPaddingTop = 16.dp
    val BarPaddingBottom = 10.dp
    val BarLineGap = 2.dp
    val FooterPaddingVertical = 4.dp
    val RowPaddingVertical = 6.dp
    val RowMinHeight = 56.dp
    val RowGap = 8.dp
    val RowStackGap = 2.dp
    val PriceToButtonsGap = 4.dp
    val RoundButtonSize = 36.dp
    val RoundButtonBorder = 1.5.dp

    val ButtonCorner = 12.dp
    val ButtonPaddingHorizontal = 16.dp
    val ButtonPaddingVertical = 12.dp
    val TextButtonPaddingHorizontal = 12.dp
    val SheetLineGap = 4.dp
    val SheetActionsGap = 12.dp

    val OptionDotSize = 14.dp
    val OptionDotInnerSize = 6.dp
    val OptionRowGap = 8.dp
    val OptionDotToTextGap = 12.dp
    val SectionGap = 14.dp
    val FinishBarPaddingBottom = 8.dp
    val RankGap = 2.dp
    val EndScreenGap = 8.dp

    /** The travel sheet never covers more than this share of the screen; its text scrolls inside. */
    const val SHEET_MAX_HEIGHT_FRACTION = 0.85f
}

/**
 * Text sizes are in sp so they track the phone's font-size setting.
 * Line heights are about 1.3 to 1.45 times the font size; letter spacing is in em so it
 * scales with the text.
 */
object UiType {
    /**
     * Gelasio, bundled so headings and amounts look the same on every phone. It is one
     * variable font file, so each weight must be selected from it explicitly: declaring a
     * weight alone leaves the file at its default (regular). Licence: licenses/Gelasio-OFL.txt.
     */
    private val Display = FontFamily(
        gelasio(FontWeight.Normal),
        gelasio(FontWeight.Bold),
    )

    @OptIn(ExperimentalTextApi::class)
    private fun gelasio(weight: FontWeight) = Font(
        resId = R.font.gelasio,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )
    private val Interface = FontFamily.SansSerif

    /** Fixed-width digits, so amounts line up and do not jitter as they change. */
    private const val TABULAR_FIGURES = "tnum"

    /** Map labels stop growing here so city names cannot collide; the full-size name appears wherever a city is opened. */
    const val MAP_LABEL_MAX_FONT_SCALE = 1.3f

    const val GRAB_HANDLE_ALPHA = 0.5f
    const val DISABLED_ALPHA = 0.3f

    /** The net worth shrinks within this range to stay on one line. */
    val NetWorthMaxSize = 36.sp
    val NetWorthMinSize = 18.sp
    val NetWorthSizeStep = 1.sp

    val NetWorth = TextStyle(
        color = UiColors.Ink,
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = NetWorthMaxSize,
        lineHeight = 44.sp,
        letterSpacing = (-0.01).em,
        fontFeatureSettings = TABULAR_FIGURES,
    )
    val Overline = TextStyle(
        color = UiColors.Mute,
        fontFamily = Interface,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.12.em,
    )
    val Body = TextStyle(
        color = UiColors.Mute,
        fontFamily = Interface,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    )
    val Title = TextStyle(
        color = UiColors.Ink,
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    )
    val MapLabel = TextStyle(
        color = UiColors.Ink,
        fontFamily = Display,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 17.sp,
    )
    val MapLabelCurrent = MapLabel.copy(fontWeight = FontWeight.Bold)

    val SheetTitle = TextStyle(
        color = UiColors.Ink,
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    )
    val Rank = TextStyle(
        color = UiColors.Accent,
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    )
    val GoodName = TextStyle(
        color = UiColors.Ink,
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    )
    val Small = TextStyle(
        color = UiColors.Mute,
        fontFamily = Interface,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    )
    val Price = TextStyle(
        color = UiColors.Ink,
        fontFamily = Interface,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    )
    val Change = Small.copy(textAlign = TextAlign.End)
    val RoundButtonSymbol = TextStyle(
        fontFamily = Interface,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        textAlign = TextAlign.Center,
    )
    val Button = TextStyle(
        color = UiColors.OnAccent,
        fontFamily = Interface,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center,
    )
    val Link = Button.copy(color = UiColors.Accent)
}

object TestTags {
    const val SCREEN = "screen"
    const val NET_WORTH = "netWorth"
    const val RAIL_MAP = "railMap"
    const val MARKET_DRAWER = "marketDrawer"
    const val MARKET = "market"
    const val MARKET_DONE = "marketDone"
    const val TRAVEL_SHEET = "travelSheet"
    const val BOARD_TRAIN = "boardTrain"
    const val END_RUN = "endRun"
    const val FINISH_RUN = "finishRun"
    const val END_RUN_SHEET = "endRunSheet"
    const val CONFIRM_END_RUN = "confirmEndRun"
    const val NEW_RUN_SCREEN = "newRunScreen"
    const val START_RUN = "startRun"
    const val RUN_LENGTH_ENDLESS = "runLengthEndless"
    const val END_SCREEN = "endScreen"
    const val NEW_RUN = "newRun"

    fun cityLabel(city: com.purplevarun.trader.engine.CityId) = "city_${city.name}"
    fun runLength(days: Int) = "runLength_$days"
}
