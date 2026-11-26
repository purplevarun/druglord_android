package com.purplevarun.trader.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Every colour, size and text style in the UI lives in this file. */
object UiColors {
    val Paper = Color(0xFFF3EEE3)
    val Panel = Color(0xFFFFFAF0)
    val Ink = Color(0xFF1D1B16)
    val Mute = Color(0xFF857C69)
    val Accent = Color(0xFFC8452C)
    val Line = Color(0xFFDDD3BF)
    val Land = Color(0xFFE9E0CC)
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
}

/**
 * Text sizes are in sp so they track the phone's font-size setting.
 * Line heights are about 1.3 to 1.45 times the font size; letter spacing is in em so it
 * scales with the text.
 */
object UiType {
    private val Display = FontFamily.Serif
    private val Interface = FontFamily.SansSerif

    /** Fixed-width digits, so amounts line up and do not jitter as they change. */
    private const val TABULAR_FIGURES = "tnum"

    /** Map labels stop growing here so city names cannot collide; the full-size name appears wherever a city is opened. */
    const val MAP_LABEL_MAX_FONT_SCALE = 1.3f

    const val GRAB_HANDLE_ALPHA = 0.5f

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
}

object TestTags {
    const val SCREEN = "screen"
    const val NET_WORTH = "netWorth"
    const val RAIL_MAP = "railMap"
    const val MARKET_DRAWER = "marketDrawer"
}
