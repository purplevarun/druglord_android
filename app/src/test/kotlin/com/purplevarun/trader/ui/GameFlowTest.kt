package com.purplevarun.trader.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.purplevarun.trader.engine.CityId
import com.purplevarun.trader.engine.GoodId
import com.purplevarun.trader.engine.RunState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Plays the game through the screen, the way a player would, and checks what they would see. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class GameFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val newRun = RunState.newRun(seed = 42)

    private fun start(run: RunState = newRun) = compose.setContent { GameScreen(newRun = { run }) }

    private fun openMarket() = compose.onNodeWithTag(TestTags.MARKET_DRAWER).performClick()

    private fun tapCity(city: CityId) = compose
        .onNodeWithTag(TestTags.cityLabel(city), useUnmergedTree = true)
        .performSemanticsAction(SemanticsActions.OnClick)

    @Test
    fun `buying one of a good takes its price from cash and puts it in the luggage`() {
        val price = newRun.priceOf(GoodId.ONIONS)
        start()
        openMarket()

        compose.onNodeWithContentDescription("Buy one Onions").performClick()

        compose.onNodeWithText("quintal · 1 held").assertExists()
        compose.onNodeWithText("Day 1 · ${formatRupees(10_000 - price)}").assertExists()
        compose.onNodeWithText("Luggage 1/100").assertExists()
    }

    @Test
    fun `selling one gives its price back`() {
        start()
        openMarket()
        compose.onNodeWithContentDescription("Buy one Cotton").performClick()

        compose.onNodeWithContentDescription("Sell one Cotton").performClick()

        compose.onNodeWithText("Day 1 · ₹10,000").assertExists()
        compose.onNodeWithContentDescription("Sell one Cotton").assertIsNotEnabled()
    }

    @Test
    fun `what cash cannot cover cannot be bought, and what is not carried cannot be sold`() {
        start()
        openMarket()

        compose.onNodeWithContentDescription("Buy one Gold").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Sell one Gold").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Buy one Onions").assertIsEnabled()
    }

    @Test
    fun `travelling takes days and moves you to the next market`() {
        start()
        tapCity(CityId.DELHI)

        compose.onNodeWithText("1 day by train · direct").assertExists()
        compose.onNodeWithTag(TestTags.BOARD_TRAIN).performClick()

        compose.onNodeWithText("Day 2 of 60").assertExists()
        compose.onNodeWithText("Delhi mandi").assertExists()
    }

    @Test
    fun `a trip with changes says where it changes and when it arrives`() {
        start(newRun.copy(city = CityId.BANGALORE))
        tapCity(CityId.SRINAGAR)

        compose.onNodeWithText("3 days by train · via Mumbai, Delhi").assertExists()
        compose.onNodeWithText("Arrives 4 Jan 2000 · 56 days left after this trip").assertExists()
    }

    @Test
    fun `a trip longer than the days left cannot be boarded`() {
        start(newRun.copy(day = 59, city = CityId.BANGALORE))
        tapCity(CityId.SRINAGAR)

        compose.onNodeWithText("Not enough days left: the run ends on 29 Feb 2000.").assertExists()
        compose.onNodeWithTag(TestTags.BOARD_TRAIN).assertIsNotEnabled()
    }

    @Test
    fun `tapping the city you are in opens its market`() {
        start()
        tapCity(CityId.MUMBAI)

        compose.onNodeWithTag(TestTags.MARKET).assertExists()
    }

    @Test
    fun `back closes the market`() {
        start()
        openMarket()

        // Let the market finish appearing (and its back handler switch on) before pressing back.
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }

        compose.onNodeWithTag(TestTags.MARKET).assertDoesNotExist()
        compose.onNodeWithTag(TestTags.MARKET_DRAWER).assertExists()
    }

    @Test
    fun `the run and the open screen survive the activity being rebuilt`() {
        val restorer = StateRestorationTester(compose)
        restorer.setContent { GameScreen(newRun = { newRun }) }
        openMarket()
        compose.onNodeWithContentDescription("Buy one Onions").performClick()

        restorer.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag(TestTags.MARKET).assertExists()
        compose.onNodeWithText("quintal · 1 held").assertExists()
    }
}
