package com.purplevarun.trader.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.then
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.purplevarun.trader.BestRun
import com.purplevarun.trader.engine.CityId
import com.purplevarun.trader.engine.EventHeadline
import com.purplevarun.trader.engine.GameEvent
import com.purplevarun.trader.engine.GoodId
import com.purplevarun.trader.engine.PriceShift
import com.purplevarun.trader.engine.RankId
import com.purplevarun.trader.engine.RunState
import com.purplevarun.trader.engine.TransportId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Every screen must hold up on every phone size and every text-zoom setting.
 * Each case lays one real screen out at one size and one font scale and checks what a player
 * would notice: no text cut off, nothing off the edge, every control big enough to tap, and the
 * screen's main action always reachable.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1400dp-h1400dp-mdpi")
class ScreenLayoutTest(private val case: Case) {

    enum class Screen(val mainActionTag: String, val showsMap: Boolean) {
        HOME(TestTags.MARKET_DRAWER, showsMap = true),
        MARKET(TestTags.MARKET_DONE, showsMap = false),
        TRAVEL(TestTags.BOARD_TRAIN, showsMap = true),
        END_RUN(TestTags.CONFIRM_END_RUN, showsMap = true),
        SAHUKAR(TestTags.borrow(1_000), showsMap = true),
        BANK(TestTags.deposit(1_000), showsMap = true),
        LUGGAGE(TestTags.buyTransport(TransportId.TRUNK), showsMap = true),
        GODOWN(TestTags.GODOWN_CLOSE, showsMap = true),
        EVENT(TestTags.GOT_IT_EVENT, showsMap = true),
        COLLECTOR(TestTags.PAY_COLLECTOR, showsMap = true),
        BROKE(TestTags.BORROW_AGAIN, showsMap = true),
        NEW_RUN(TestTags.START_RUN, showsMap = false),
        ENDED(TestTags.NEW_RUN, showsMap = false),
        ;

        fun phase(run: RunState): Phase = when (this) {
            HOME -> Phase.Playing(run, Overlay.None)
            MARKET -> Phase.Playing(run, Overlay.Market)
            TRAVEL -> Phase.Playing(run, Overlay.Travel(CityId.SRINAGAR))
            END_RUN -> Phase.Playing(run, Overlay.EndRun)
            SAHUKAR -> Phase.Playing(run, Overlay.Sahukar)
            BANK -> Phase.Playing(run, Overlay.Bank)
            LUGGAGE -> Phase.Playing(run, Overlay.Luggage)
            GODOWN -> Phase.Playing(run, Overlay.Godown)
            EVENT -> Phase.Playing(
                run,
                Overlay.None,
                pendingEvents = listOf(
                    GameEvent.Checkpost(bribeRupees = 0, seizedGood = GoodId.SILK_SAREES, seizedUnits = 4),
                    GameEvent.PriceSwing(CityId.BANGALORE, GoodId.ONIONS, 4_000, EventHeadline.MONSOON),
                ),
            )
            COLLECTOR -> Phase.Playing(run.copy(day = run.debtDueDay), Overlay.None)
            BROKE -> Phase.Playing(
                run.copy(
                    cashRupees = 0,
                    bankRupees = 0,
                    holdings = emptyMap(),
                    godowns = emptyMap(),
                ),
                Overlay.None,
            )
            NEW_RUN -> Phase.NewRun
            ENDED -> Phase.Ended(run, Ending.RETIRED)
        }
    }

    data class Case(val screen: Screen, val widthDp: Int, val heightDp: Int, val fontScale: Float) {
        override fun toString() = "$screen ${widthDp}x${heightDp}dp text x$fontScale"
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

        /**
         * Long amounts, full luggage and the longest trip (three days, two changes), to give
         * every screen the most to fit.
         */
        private val CROWDED_RUN = RunState(
            seed = 7,
            day = 29,
            city = CityId.BANGALORE,
            cashRupees = 987_654_321,
            debtRupees = 12_345_678,
            debtDueDay = 40,
            bankRupees = 8_765_432,
            transport = TransportId.JHOLA,
            rngRolls = 40,
            priceShifts = listOf(PriceShift(CityId.BANGALORE, GoodId.ONIONS, 2_500)),
            godowns = mapOf(
                CityId.BANGALORE to mapOf(GoodId.SILK_SAREES to 400, GoodId.ONIONS to 95),
                CityId.DELHI to mapOf(GoodId.KESAR to 5),
            ),
            holdings = mapOf(GoodId.SILK_SAREES to 40, GoodId.GOLD to 30, GoodId.ONIONS to 29),
        )

        /** The fullest the best-runs list can be: five entries, big amounts, long runs. */
        private val CROWDED_BEST_RUNS = listOf(
            BestRun(987_654_321, 90, RankId.CROREPATI),
            BestRun(123_456_789, 60, RankId.CROREPATI),
            BestRun(98_765_432, 432, RankId.SETH),
            BestRun(1_234_567, 30, RankId.SETH),
            BestRun(5_432, 60, RankId.THELAWALA),
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = Screen.entries.flatMap { screen ->
            SCREEN_SIZES_DP.flatMap { (width, height) ->
                FONT_SCALES.map { scale -> arrayOf<Any>(Case(screen, width, height, scale)) }
            }
        }
    }

    @get:Rule
    val compose = createComposeRule()

    private val screenNode: SemanticsNode
        get() = compose.onNodeWithTag(TestTags.SCREEN).fetchSemanticsNode()

    private val minTouchTargetPx: Float
        get() = with(compose.density) { MIN_TOUCH_TARGET_DP.dp.toPx() }

    @Before
    fun showScreen() {
        compose.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(case.widthDp.dp, case.heightDp.dp)) then
                    DeviceConfigurationOverride.FontScale(case.fontScale),
            ) {
                GameScreenContent(
                    phase = case.screen.phase(CROWDED_RUN),
                    bestRuns = CROWDED_BEST_RUNS,
                    onAction = {},
                    onOverlayChange = {},
                    onEventDismissed = {},
                    onStartRun = {},
                    onEndRun = {},
                    onNewRun = {},
                )
            }
        }
    }

    @Test
    fun `no text is cut off or runs off the screen`() {
        val screen = screenNode
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
            // A word split across two lines ("Bas" / "mati") is not cut off, but it is unreadable.
            for (line in 0 until layout.lineCount - 1) {
                val breakAt = layout.getLineEnd(line)
                val before = text.getOrNull(breakAt - 1)
                val after = text.getOrNull(breakAt)
                assertFalse(
                    "\"$text\" splits a word across lines at \"${text.take(breakAt)}|${text.drop(breakAt)}\"",
                    before != null && after != null && before.isLetterOrDigit() && after.isLetterOrDigit(),
                )
            }
            assertTrue("\"$text\" runs off the left edge", node.left >= screen.left - ROUNDING_TOLERANCE_PX)
            assertTrue("\"$text\" runs off the right edge", node.right <= screen.right + ROUNDING_TOLERANCE_PX)
        }
    }

    @Test
    fun `every control is big enough to tap and fits across the screen`() {
        val screen = screenNode
        // City names on the map are not the tap targets: a tap anywhere near a city picks it.
        val controls = compose
            .onAllNodes(hasClickAction() and !hasAnyAncestor(hasTestTag(TestTags.RAIL_MAP)))
            .fetchSemanticsNodes()
        assertTrue("the screen has controls", controls.isNotEmpty())

        for (control in controls) {
            val name = control.describe()
            assertTrue("$name is too narrow to tap", control.size.width >= minTouchTargetPx - ROUNDING_TOLERANCE_PX)
            assertTrue("$name is too short to tap", control.size.height >= minTouchTargetPx - ROUNDING_TOLERANCE_PX)
            assertTrue("$name runs off the left edge", control.left >= screen.left - ROUNDING_TOLERANCE_PX)
            assertTrue("$name runs off the right edge", control.right <= screen.right + ROUNDING_TOLERANCE_PX)
        }
    }

    @Test
    fun `the screen's main action is always fully on screen`() {
        val screen = screenNode
        val main = compose.onNodeWithTag(case.screen.mainActionTag).fetchSemanticsNode()

        assertTrue("the main action starts below the top edge", main.top >= screen.top - ROUNDING_TOLERANCE_PX)
        assertTrue("the main action ends above the bottom edge", main.bottom <= screen.bottom + ROUNDING_TOLERANCE_PX)
        assertTrue("the main action starts inside the left edge", main.left >= screen.left - ROUNDING_TOLERANCE_PX)
        assertTrue("the main action ends inside the right edge", main.right <= screen.right + ROUNDING_TOLERANCE_PX)
    }

    @Test
    fun `the map keeps a usable size and shows every city inside it`() {
        assumeTrue("this screen does not show the map", case.screen.showsMap)
        val map = compose.onNodeWithTag(TestTags.RAIL_MAP).fetchSemanticsNode()
        val minMapSidePx = with(compose.density) { MIN_MAP_SIDE_DP.dp.toPx() }
        assertTrue("the map is at least $MIN_MAP_SIDE_DP dp wide", map.size.width >= minMapSidePx)
        assertTrue("the map is at least $MIN_MAP_SIDE_DP dp tall", map.size.height >= minMapSidePx)

        for (city in CityId.entries) {
            val label = compose.onNodeWithTag(TestTags.cityLabel(city), useUnmergedTree = true).fetchSemanticsNode()
            assertTrue("$city's name is inside the map", label.left >= map.left - ROUNDING_TOLERANCE_PX)
            assertTrue("$city's name is inside the map", label.right <= map.right + ROUNDING_TOLERANCE_PX)
            assertTrue("$city's name is inside the map", label.top >= map.top - ROUNDING_TOLERANCE_PX)
            assertTrue("$city's name is inside the map", label.bottom <= map.bottom + ROUNDING_TOLERANCE_PX)
        }
    }

    private fun SemanticsNode.describe(): String {
        val tag = config.getOrNull(SemanticsProperties.TestTag)
        val description = config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
        val text = config.getOrNull(SemanticsProperties.Text)?.joinToString()
        return "control [${tag ?: description ?: text ?: "unnamed"}]"
    }

    // Unclipped edges, so something merely scrolled out of view is not mistaken for cut off.
    private val SemanticsNode.left get() = positionInRoot.x
    private val SemanticsNode.top get() = positionInRoot.y
    private val SemanticsNode.right get() = positionInRoot.x + size.width
    private val SemanticsNode.bottom get() = positionInRoot.y + size.height
}
