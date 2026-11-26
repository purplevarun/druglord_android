package com.purplevarun.trader.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.then
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.purplevarun.trader.engine.CityId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The home screen must hold up on every phone size and every text-zoom setting.
 * Each case lays the real screen out at one size and one font scale and checks what a
 * player would notice: no text cut off, nothing off the edge, controls big enough to tap.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1400dp-h1400dp-mdpi")
class HomeScreenLayoutTest(private val screen: ScreenCase) {

    data class ScreenCase(val widthDp: Int, val heightDp: Int, val fontScale: Float) {
        override fun toString() = "${widthDp}x${heightDp}dp text x$fontScale"
    }

    companion object {
        /** Width by height in dp, from a small phone at the largest display-size setting up to a tablet. */
        private val SCREEN_SIZES_DP = listOf(
            280 to 520, // small phone, display size set to largest
            320 to 568, // small phone
            360 to 640, // common phone
            412 to 915, // large phone
            600 to 960, // small tablet or unfolded foldable
            640 to 360, // phone in landscape
            915 to 412, // large phone in landscape
            1280 to 800, // tablet in landscape
        )

        /** Android's font-size setting runs from 0.85 to 2.0. */
        private val FONT_SCALES = listOf(0.85f, 1.0f, 1.3f, 1.5f, 2.0f)

        private const val MIN_TOUCH_TARGET_DP = 48
        private const val MIN_MAP_SIDE_DP = 160
        private const val ROUNDING_TOLERANCE_PX = 1f

        /** Long amounts and a late day, to give the layout the most to fit. */
        private val CROWDED_STATE = HomeUiState(
            netWorthRupees = 999_999_999,
            cashRupees = 87_654_321,
            debtRupees = 12_345_678,
            dayOfRun = 89,
            runLengthDays = 90,
            currentCity = CityId.MUMBAI,
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = SCREEN_SIZES_DP.flatMap { (width, height) ->
            FONT_SCALES.map { scale -> arrayOf<Any>(ScreenCase(width, height, scale)) }
        }
    }

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun showHomeScreen() {
        compose.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(screen.widthDp.dp, screen.heightDp.dp)) then
                    DeviceConfigurationOverride.FontScale(screen.fontScale),
            ) {
                HomeScreen(CROWDED_STATE)
            }
        }
    }

    @Test
    fun `no text is cut off or runs off the screen`() {
        val screenNode = compose.onNodeWithTag(TestTags.SCREEN).fetchSemanticsNode()
        val textNodes = compose
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("the screen shows some text", textNodes.isNotEmpty())

        for (node in textNodes) {
            val text = node.config[SemanticsProperties.Text].joinToString()
            val layouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
            val layout = layouts.single()
            // Too many lines, or lines taller than the space given.
            assertFalse(
                "\"$text\" is cut off: ${layout.lineCount} line(s) do not fit in ${layout.size.height} px of height",
                layout.didOverflowHeight,
            )
            // A line wider than the space given. (hasVisualOverflow cannot be used here: it also
            // reports text that is simply shorter than the line it sits on.)
            val widestLinePx = (0 until layout.lineCount).maxOf { layout.getLineRight(it) - layout.getLineLeft(it) }
            assertTrue(
                "\"$text\" is cut off: its widest line is $widestLinePx px but it was given ${layout.size.width} px",
                widestLinePx <= layout.size.width + ROUNDING_TOLERANCE_PX,
            )
            assertTrue("\"$text\" runs off the left edge", node.left >= screenNode.left - ROUNDING_TOLERANCE_PX)
            assertTrue("\"$text\" runs off the right edge", node.right <= screenNode.right + ROUNDING_TOLERANCE_PX)
        }
    }

    @Test
    fun `the market drawer stays on screen and is big enough to tap`() {
        val screenNode = compose.onNodeWithTag(TestTags.SCREEN).fetchSemanticsNode()
        val drawer = compose.onNodeWithTag(TestTags.MARKET_DRAWER).assertIsDisplayed().fetchSemanticsNode()
        val minTouchTargetPx = with(compose.density) { MIN_TOUCH_TARGET_DP.dp.toPx() }

        assertTrue("the drawer ends above the bottom edge", drawer.bottom <= screenNode.bottom + ROUNDING_TOLERANCE_PX)
        assertTrue("the drawer starts below the top edge", drawer.top >= screenNode.top - ROUNDING_TOLERANCE_PX)
        assertTrue("the drawer is tall enough to tap", drawer.size.height >= minTouchTargetPx - ROUNDING_TOLERANCE_PX)
        assertTrue("the drawer is wide enough to tap", drawer.size.width >= minTouchTargetPx - ROUNDING_TOLERANCE_PX)
    }

    @Test
    fun `the map keeps a usable size and shows every city inside it`() {
        val map = compose.onNodeWithTag(TestTags.RAIL_MAP).fetchSemanticsNode()
        val minMapSidePx = with(compose.density) { MIN_MAP_SIDE_DP.dp.toPx() }
        assertTrue("the map is at least $MIN_MAP_SIDE_DP dp wide", map.size.width >= minMapSidePx)
        assertTrue("the map is at least $MIN_MAP_SIDE_DP dp tall", map.size.height >= minMapSidePx)

        val resources = RuntimeEnvironment.getApplication().resources
        for (city in CityId.entries) {
            val name = resources.getString(cityNameRes(city))
            val labels = compose
                .onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(androidx.compose.ui.text.AnnotatedString(name))), useUnmergedTree = true)
                .fetchSemanticsNodes()
            assertEquals("$name appears once on the map", 1, labels.size)
            val label = labels.single()
            assertTrue("$name is inside the map", label.left >= map.left - ROUNDING_TOLERANCE_PX)
            assertTrue("$name is inside the map", label.right <= map.right + ROUNDING_TOLERANCE_PX)
            assertTrue("$name is inside the map", label.top >= map.top - ROUNDING_TOLERANCE_PX)
            assertTrue("$name is inside the map", label.bottom <= map.bottom + ROUNDING_TOLERANCE_PX)
        }
    }

    // Unclipped edges, so text that is merely scrolled out of view is not mistaken for cut-off text.
    private val SemanticsNode.left get() = positionInRoot.x
    private val SemanticsNode.top get() = positionInRoot.y
    private val SemanticsNode.right get() = positionInRoot.x + size.width
    private val SemanticsNode.bottom get() = positionInRoot.y + size.height
}
