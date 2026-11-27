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
import androidx.test.core.app.ApplicationProvider
import com.purplevarun.trader.RunStore
import com.purplevarun.trader.engine.CityId
import com.purplevarun.trader.engine.GameConstants
import com.purplevarun.trader.engine.GoodId
import com.purplevarun.trader.engine.RunState
import org.junit.Assert.assertEquals
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

    /**
     * Opens the game the way the player would find it. With [run] saved in the store the game
     * picks it straight back up; with null it lands on the new-run screen.
     */
    private fun start(run: RunState? = newRun): RunStore {
        val store = RunStore(ApplicationProvider.getApplicationContext())
        run?.let(store::saveRun)
        compose.setContent {
            GameScreen(
                store = store,
                newRun = { days -> RunState.newRun(seed = 42, runLengthDays = days) },
            )
        }
        return store
    }

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
    fun `an endless run never speaks of days left`() {
        start(RunState.newRun(seed = 42, runLengthDays = GameConstants.ENDLESS_RUN_LENGTH_DAYS))
        tapCity(CityId.DELHI)

        compose.onNodeWithText("Arrives 2 Jan 2000").assertExists()
        compose.onNodeWithTag(TestTags.BOARD_TRAIN).performClick()

        compose.onNodeWithText("Day 2 · endless").assertExists()
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
        val store = RunStore(ApplicationProvider.getApplicationContext())
        store.saveRun(newRun)
        val restorer = StateRestorationTester(compose)
        restorer.setContent {
            GameScreen(store = store, newRun = { days -> RunState.newRun(seed = 42, runLengthDays = days) })
        }
        openMarket()
        compose.onNodeWithContentDescription("Buy one Onions").performClick()

        restorer.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag(TestTags.MARKET).assertExists()
        compose.onNodeWithText("quintal · 1 held").assertExists()
    }

    @Test
    fun `with no run saved the new-run screen offers the lengths and the best runs`() {
        start(run = null)

        compose.onNodeWithTag(TestTags.NEW_RUN_SCREEN).assertExists()
        compose.onNodeWithText("90 days").assertExists()
        compose.onNodeWithText("Endless").assertExists()
        compose.onNodeWithText("No finished runs yet.").assertExists()
    }

    @Test
    fun `picking a run length and starting begins on day one of it`() {
        start(run = null)

        compose.onNodeWithTag(TestTags.runLength(90)).performClick()
        compose.onNodeWithTag(TestTags.START_RUN).performClick()

        compose.onNodeWithText("Day 1 of 90").assertExists()
        compose.onNodeWithText("Mumbai mandi").assertExists()
    }

    @Test
    fun `on the last day a finish button ends the run on the end screen`() {
        start(newRun.copy(day = 60))

        compose.onNodeWithTag(TestTags.FINISH_RUN).performClick()
        compose.onNodeWithText("Finish the run?").assertExists()
        compose.onNodeWithTag(TestTags.CONFIRM_END_RUN).performClick()

        compose.onNodeWithTag(TestTags.END_SCREEN).assertExists()
        compose.onNodeWithText("₹5,000").assertExists()
        compose.onNodeWithText("Thelawala").assertExists()
        compose.onNodeWithText("RUN OVER · 29 FEB 2000").assertExists()
    }

    @Test
    fun `ending a run early asks first and can be refused`() {
        start()

        compose.onNodeWithTag(TestTags.END_RUN).performClick()
        compose.onNodeWithText("End the run?").assertExists()
        compose.onNodeWithText("Net worth ₹5,000 · Thelawala").assertExists()

        compose.onNodeWithText("Keep trading").performClick()

        compose.onNodeWithTag(TestTags.END_RUN_SHEET).assertDoesNotExist()
        compose.onNodeWithText("Day 1 of 60").assertExists()
    }

    @Test
    fun `a finished run lands on the best runs list`() {
        val store = start(newRun.copy(day = 60))

        compose.onNodeWithTag(TestTags.FINISH_RUN).performClick()
        compose.onNodeWithTag(TestTags.CONFIRM_END_RUN).performClick()
        compose.onNodeWithTag(TestTags.NEW_RUN).performClick()

        compose.onNodeWithTag(TestTags.NEW_RUN_SCREEN).assertExists()
        compose.onNodeWithText("₹5,000 · Thelawala · 60 days").assertExists()
        assertEquals(1, store.bestRuns().size)
    }

    @Test
    fun `a saved run picks up where it left off`() {
        start(newRun.copy(day = 12, city = CityId.DELHI, cashRupees = 42_000))

        compose.onNodeWithText("Day 12 of 60").assertExists()
        compose.onNodeWithText("Delhi mandi").assertExists()
    }

    @Test
    fun `an endless run has no finish button and still retires`() {
        start(RunState.newRun(seed = 42, runLengthDays = GameConstants.ENDLESS_RUN_LENGTH_DAYS))

        compose.onNodeWithText("Day 1 · endless").assertExists()
        compose.onNodeWithTag(TestTags.FINISH_RUN).assertDoesNotExist()

        compose.onNodeWithTag(TestTags.END_RUN).performClick()
        compose.onNodeWithText("End the run?").assertExists()
        compose.onNodeWithTag(TestTags.CONFIRM_END_RUN).performClick()

        compose.onNodeWithTag(TestTags.END_SCREEN).assertExists()
        compose.onNodeWithText("1 days on the rails").assertExists()
    }

    @Test
    fun `the places panel opens each sheet and the sheets close again`() {
        start()

        compose.onNodeWithTag(TestTags.PLACE_SAHUKAR).performClick()
        compose.onNodeWithTag(TestTags.SAHUKAR_SHEET).assertExists()
        compose.onNodeWithText("Close").performClick()

        compose.onNodeWithTag(TestTags.PLACE_BANK).performClick()
        compose.onNodeWithTag(TestTags.BANK_SHEET).assertExists()
        compose.onNodeWithText("Close").performClick()

        compose.onNodeWithTag(TestTags.PLACE_LUGGAGE).performClick()
        compose.onNodeWithTag(TestTags.LUGGAGE_SHEET).assertExists()
        compose.onNodeWithText("Close").performClick()

        compose.onNodeWithTag(TestTags.MARKET_DRAWER).assertExists()
    }

    @Test
    fun `borrowing fills your pocket and your debt`() {
        start()
        compose.onNodeWithTag(TestTags.PLACE_SAHUKAR).performClick()

        compose.onNodeWithTag(TestTags.borrow(1_000)).performClick()

        compose.onNodeWithText("You owe ₹6,000").assertExists()
        compose.onNodeWithText("Cash ₹11,000 · Debt ₹6,000").assertExists()
    }

    @Test
    fun `the sahukar only takes back what is owed and what is held`() {
        start()
        compose.onNodeWithTag(TestTags.PLACE_SAHUKAR).performClick()

        // Repaying more than the debt is refused, though the cash is there.
        compose.onNodeWithTag(TestTags.repay(10_000)).assertIsNotEnabled()

        compose.onNodeWithTag(TestTags.REPAY_ALL).performClick()

        compose.onNodeWithText("You owe nothing.").assertExists()
        compose.onNodeWithText("Cash ₹5,000").assertExists()
    }

    @Test
    fun `the bank keeps your savings and gives them back`() {
        start()
        compose.onNodeWithTag(TestTags.PLACE_BANK).performClick()

        compose.onNodeWithTag(TestTags.withdraw(1_000)).assertIsNotEnabled()
        compose.onNodeWithTag(TestTags.deposit(1_000)).performClick()

        compose.onNodeWithText("In the bank ₹1,000").assertExists()
        compose.onNodeWithText("Cash ₹9,000").assertExists()

        compose.onNodeWithTag(TestTags.WITHDRAW_ALL).performClick()

        compose.onNodeWithText("Nothing in the bank.").assertExists()
        compose.onNodeWithText("Cash ₹10,000").assertExists()
    }

    @Test
    fun `the train grows your debt and your savings`() {
        start()

        compose.onNodeWithTag(TestTags.PLACE_SAHUKAR).performClick()
        compose.onNodeWithTag(TestTags.borrow(10_000)).performClick()
        compose.onNodeWithText("Close").performClick()

        compose.onNodeWithTag(TestTags.PLACE_BANK).performClick()
        compose.onNodeWithTag(TestTags.deposit(10_000)).performClick()
        compose.onNodeWithText("Close").performClick()

        tapCity(CityId.DELHI)
        compose.onNodeWithTag(TestTags.BOARD_TRAIN).performClick()

        // One day on the rails: the sahukar's 8% and the bank's 1%.
        compose.onNodeWithText("Cash ₹10,000 · Debt ₹16,200").assertExists()
        compose.onNodeWithTag(TestTags.PLACE_BANK).performClick()
        compose.onNodeWithText("In the bank ₹10,100").assertExists()
    }

    @Test
    fun `a bigger transport costs its price and holds more goods`() {
        start()
        compose.onNodeWithTag(TestTags.PLACE_LUGGAGE).performClick()

        compose.onNodeWithText("Jhola · 0 of 100 filled").assertExists()
        compose.onNodeWithTag(TestTags.buyTransport(com.purplevarun.trader.engine.TransportId.TRUCK))
            .assertIsNotEnabled()

        compose.onNodeWithTag(TestTags.buyTransport(com.purplevarun.trader.engine.TransportId.TRUNK))
            .performClick()

        compose.onNodeWithText("Trunk · 0 of 250 filled").assertExists()
        compose.onNodeWithText("Cash ₹6,000 · Debt ₹5,000").assertExists()

        compose.onNodeWithText("Close").performClick()
        openMarket()
        compose.onNodeWithText("Luggage 0/250").assertExists()
    }
}
