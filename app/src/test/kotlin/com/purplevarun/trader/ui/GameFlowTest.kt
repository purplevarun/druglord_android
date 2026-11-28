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
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import com.purplevarun.trader.RunStore
import com.purplevarun.trader.engine.Action
import com.purplevarun.trader.engine.CityId
import com.purplevarun.trader.engine.Game
import com.purplevarun.trader.engine.GameConstants
import com.purplevarun.trader.engine.GoodId
import com.purplevarun.trader.engine.Outcome
import com.purplevarun.trader.engine.PriceShift
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
                newRun = { RunState.newRun(seed = 42) },
            )
        }
        return store
    }

    private fun openMarket() = compose.onNodeWithTag(TestTags.MARKET_DRAWER).performClick()

    private fun tapCity(city: CityId) = compose
        .onNodeWithTag(TestTags.cityLabel(city), useUnmergedTree = true)
        .performSemanticsAction(SemanticsActions.OnClick)

    /** Taps away whatever the dice told, so the next tap can land where it was aimed. */
    private fun settleEvents() {
        while (compose.onAllNodesWithTag(TestTags.EVENT_SHEET).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag(TestTags.GOT_IT_EVENT).performClick()
        }
    }

    @Test
    fun `buying one of a good takes its price from cash and puts it in the luggage`() {
        val price = newRun.priceOf(GoodId.ONIONS)
        start()
        openMarket()

        compose.onNodeWithContentDescription("Buy one Onions").performClick()
        settleEvents()

        compose.onNodeWithText("quintal · 1 held").assertExists()
        compose.onNodeWithText("Day 1 · ${formatRupees(10_000 - price - newRun.livingCostPerDay)}").assertExists()
        compose.onNodeWithText("Luggage 1/100").assertExists()
    }

    @Test
    fun `selling one gives its price back, give or take the market's cut`() {
        start()
        openMarket()
        compose.onNodeWithContentDescription("Buy one Cotton").performClick()
        settleEvents()

        compose.onNodeWithContentDescription("Sell one Cotton").performClick()
        settleEvents()

        // The buy nudged the price up and the sell sold at base: a rupee of market impact,
        // and two moves cost two days of living.
        compose.onNodeWithText("Day 1 · ${formatRupees(9_999 - 2 * newRun.livingCostPerDay)}").assertExists()
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
        settleEvents()

        compose.onNodeWithText("Day 2").assertExists()
        compose.onNodeWithText("Delhi mandi").assertExists()
    }

    @Test
    fun `a trip with changes says where it changes and when it arrives`() {
        start(newRun.copy(city = CityId.BANGALORE))
        tapCity(CityId.SRINAGAR)

        compose.onNodeWithText("3 days by train · via Mumbai, Delhi").assertExists()
        compose.onNodeWithText("Arrives 4 Jan 2000").assertExists()
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
            GameScreen(store = store, newRun = { RunState.newRun(seed = 42) })
        }
        openMarket()
        compose.onNodeWithContentDescription("Buy one Onions").performClick()
        settleEvents()

        restorer.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag(TestTags.MARKET).assertExists()
        compose.onNodeWithText("quintal · 1 held").assertExists()
    }

    @Test
    fun `with no run saved the new-run screen shows the best runs and a start`() {
        start(run = null)

        compose.onNodeWithTag(TestTags.NEW_RUN_SCREEN).assertExists()
        compose.onNodeWithText("No finished runs yet.").assertExists()
    }

    @Test
    fun `starting a run begins on day one in Mumbai`() {
        start(run = null)

        compose.onNodeWithTag(TestTags.START_RUN).performClick()

        compose.onNodeWithText("Day 1").assertExists()
        compose.onNodeWithText("Mumbai mandi").assertExists()
    }

    @Test
    fun `retiring lands on the end screen with the score and the rank`() {
        start()

        compose.onNodeWithTag(TestTags.END_RUN).performClick()
        compose.onNodeWithText("End the run?").assertExists()
        compose.onNodeWithTag(TestTags.CONFIRM_END_RUN).performClick()

        compose.onNodeWithTag(TestTags.END_SCREEN).assertExists()
        compose.onNodeWithText("₹5,000").assertExists()
        compose.onNodeWithText("Thelawala").assertExists()
        compose.onNodeWithText("You retired.").assertExists()
        compose.onNodeWithText("RUN OVER · 1 JAN 2000").assertExists()
    }

    @Test
    fun `ending a run early asks first and can be refused`() {
        start()

        compose.onNodeWithTag(TestTags.END_RUN).performClick()
        compose.onNodeWithText("End the run?").assertExists()
        compose.onNodeWithText("Net worth ₹5,000 · Thelawala").assertExists()

        compose.onNodeWithText("Keep trading").performClick()

        compose.onNodeWithTag(TestTags.END_RUN_SHEET).assertDoesNotExist()
        compose.onNodeWithText("Day 1").assertExists()
    }

    @Test
    fun `a finished run lands on the best runs list`() {
        // Well ahead of the collector's day so his sheet does not sit over the retire one.
        val store = start(newRun.copy(day = 60, debtDueDay = 70))

        compose.onNodeWithTag(TestTags.END_RUN).performClick()
        compose.onNodeWithTag(TestTags.CONFIRM_END_RUN).performClick()
        compose.onNodeWithTag(TestTags.NEW_RUN).performClick()

        compose.onNodeWithTag(TestTags.NEW_RUN_SCREEN).assertExists()
        compose.onNodeWithText("₹5,000 · Thelawala · 60 days").assertExists()
        assertEquals(1, store.bestRuns().size)
    }

    @Test
    fun `a saved run picks up where it left off`() {
        start(newRun.copy(day = 12, city = CityId.DELHI, cashRupees = 42_000, debtDueDay = 30))

        compose.onNodeWithText("Day 12").assertExists()
        compose.onNodeWithText("Delhi mandi").assertExists()
    }

    @Test
    fun `the collector knocks when the ten days run out`() {
        start(newRun.copy(day = newRun.debtDueDay - 1, cashRupees = 100_000))

        tapCity(CityId.DELHI)
        compose.onNodeWithTag(TestTags.BOARD_TRAIN).performClick()
        settleEvents()

        // The trip put the run past its deadline: he is at the door, and the money is there.
        compose.onNodeWithTag(TestTags.COLLECTOR_SHEET).assertExists()
        compose.onNodeWithTag(TestTags.PAY_COLLECTOR).performClick()
        settleEvents()

        compose.onNodeWithTag(TestTags.COLLECTOR_SHEET).assertDoesNotExist()
        compose.onNodeWithText("Debt ₹0", substring = true).assertExists()
    }

    @Test
    fun `when you cannot pay the collector ends the run`() {
        start(newRun.copy(day = newRun.debtDueDay - 1, cashRupees = 100))

        tapCity(CityId.DELHI)
        compose.onNodeWithTag(TestTags.BOARD_TRAIN).performClick()
        settleEvents()

        compose.onNodeWithTag(TestTags.COLLECTOR_SHEET).assertExists()
        compose.onNodeWithTag(TestTags.PAY_COLLECTOR).assertIsNotEnabled()
        compose.onNodeWithTag(TestTags.MEET_FATE).performClick()

        compose.onNodeWithTag(TestTags.END_SCREEN).assertExists()
        compose.onNodeWithText("The collector got you.").assertExists()
    }

    @Test
    fun `broke asks for the last loan or the end`() {
        start(newRun.copy(cashRupees = 0, debtRupees = 0, debtDueDay = 0))

        compose.onNodeWithTag(TestTags.BROKE_SHEET).assertExists()
        compose.onNodeWithTag(TestTags.BORROW_AGAIN).performClick()
        settleEvents()

        compose.onNodeWithTag(TestTags.BROKE_SHEET).assertDoesNotExist()
        compose.onNodeWithText("Debt ₹5,000", substring = true).assertExists()
    }

    @Test
    fun `broke while owing is just the end`() {
        start(newRun.copy(cashRupees = 0, bankRupees = 0))

        compose.onNodeWithTag(TestTags.BROKE_SHEET).assertExists()
        compose.onNodeWithTag(TestTags.BORROW_AGAIN).assertIsNotEnabled()
        compose.onNodeWithTag(TestTags.MEET_FATE).performClick()

        compose.onNodeWithTag(TestTags.END_SCREEN).assertExists()
        compose.onNodeWithText("You died broke.").assertExists()
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
    fun `the sahukar lends once, and a cleared debt opens him again`() {
        start()
        compose.onNodeWithTag(TestTags.PLACE_SAHUKAR).performClick()

        // Owing already, there is no borrowing.
        compose.onNodeWithTag(TestTags.borrow(1_000)).assertIsNotEnabled()

        compose.onNodeWithTag(TestTags.REPAY_ALL).performClick()
        settleEvents()
        compose.onNodeWithText("You owe nothing.").assertExists()

        compose.onNodeWithTag(TestTags.borrow(1_000)).performClick()
        settleEvents()

        compose.onNodeWithText("You owe ₹1,000").assertExists()
        compose.onNodeWithTag(TestTags.borrow(1_000)).assertIsNotEnabled()
    }

    @Test
    fun `the sahukar only takes back what is owed and what is held`() {
        start()
        compose.onNodeWithTag(TestTags.PLACE_SAHUKAR).performClick()

        // Repaying more than the debt is refused, though the cash is there.
        compose.onNodeWithTag(TestTags.repay(10_000)).assertIsNotEnabled()

        compose.onNodeWithTag(TestTags.REPAY_ALL).performClick()
        settleEvents()

        compose.onNodeWithText("You owe nothing.").assertExists()
        compose.onNodeWithText("Cash ₹4,880").assertExists()
    }

    @Test
    fun `the bank keeps your savings and gives them back`() {
        start()
        compose.onNodeWithTag(TestTags.PLACE_BANK).performClick()

        compose.onNodeWithTag(TestTags.withdraw(1_000)).assertIsNotEnabled()
        compose.onNodeWithTag(TestTags.deposit(1_000)).performClick()
        settleEvents()

        compose.onNodeWithText("In the bank ₹1,000").assertExists()
        compose.onNodeWithText("Cash ₹8,880").assertExists()

        compose.onNodeWithTag(TestTags.WITHDRAW_ALL).performClick()
        settleEvents()

        compose.onNodeWithText("Nothing in the bank.").assertExists()
        compose.onNodeWithText("Cash ₹9,760").assertExists()
    }

    @Test
    fun `the train grows your debt and your savings`() {
        start(newRun.copy(debtRupees = 0, debtDueDay = 0))

        compose.onNodeWithTag(TestTags.PLACE_SAHUKAR).performClick()
        compose.onNodeWithTag(TestTags.borrow(10_000)).performClick()
        settleEvents()
        compose.onNodeWithText("Close").performClick()

        compose.onNodeWithTag(TestTags.PLACE_BANK).performClick()
        compose.onNodeWithTag(TestTags.deposit(10_000)).performClick()
        settleEvents()
        compose.onNodeWithText("Close").performClick()

        tapCity(CityId.DELHI)
        compose.onNodeWithTag(TestTags.BOARD_TRAIN).performClick()
        settleEvents()

        // One day on the rails: the sahukar's 8%, the bank's 1%, and a day's keep in Delhi.
        compose.onNodeWithText("Cash ₹9,660 · Debt ₹10,800").assertExists()
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
        settleEvents()

        compose.onNodeWithText("Trunk · 0 of 250 filled").assertExists()
        compose.onNodeWithText("Cash ₹5,880 · Debt ₹5,000").assertExists()

        compose.onNodeWithText("Close").performClick()
        openMarket()
        compose.onNodeWithText("Luggage 0/250").assertExists()
    }

    @Test
    fun `what the day brings is told in a sheet and tapped away`() {
        // Find a dice count where the next move brings an event — the dice are deterministic.
        val lucky = (0L..500L).first { rolls ->
            val outcome = Game.apply(newRun.copy(rngRolls = rolls), Action.Buy(GoodId.ONIONS, 1))
            outcome is Outcome.Done && outcome.events.isNotEmpty()
        }
        start(newRun.copy(rngRolls = lucky))
        openMarket()

        compose.onNodeWithContentDescription("Buy one Onions").performClick()

        compose.onNodeWithTag(TestTags.EVENT_SHEET).assertExists()
        compose.onNodeWithTag(TestTags.GOT_IT_EVENT).performClick()
        compose.onNodeWithTag(TestTags.EVENT_SHEET).assertDoesNotExist()
        compose.onNodeWithText("quintal · 1 held").assertExists()
    }

    @Test
    fun `the godown takes goods off your back and gives them back`() {
        val loaded = RunState.newRun(seed = 42).copy(holdings = mapOf(GoodId.ONIONS to 5))
        start(loaded)
        compose.onNodeWithTag(TestTags.PLACE_GODOWN).performClick()

        compose.onNodeWithText("Nothing stored in this city.").assertExists()
        compose.onNodeWithTag(TestTags.storeAll(GoodId.ONIONS)).performClick()
        settleEvents()

        compose.onNodeWithText("Onions · 5 stored").assertExists()
        compose.onNodeWithText("Carrying nothing.").assertExists()

        compose.onNodeWithTag(TestTags.take(GoodId.ONIONS, 10)).assertIsNotEnabled()
        compose.onNodeWithTag(TestTags.takeAll(GoodId.ONIONS)).performClick()
        settleEvents()

        compose.onNodeWithText("Nothing stored in this city.").assertExists()
        compose.onNodeWithText("Onions · 5 carried").assertExists()
    }

    @Test
    fun `selling from the godown pays without touching the luggage`() {
        val parked = RunState.newRun(seed = 42)
            .copy(godowns = mapOf(CityId.MUMBAI to mapOf(GoodId.ONIONS to 30)))
        start(parked)
        compose.onNodeWithTag(TestTags.PLACE_GODOWN).performClick()

        compose.onNodeWithTag(TestTags.sellAllStored(GoodId.ONIONS)).performClick()
        settleEvents()

        compose.onNodeWithText("Nothing stored in this city.").assertExists()
        compose.onNodeWithText("Close").performClick()

        // Thirty onions at whatever the dump left the price at — the engine does the math.
        val cashed = Game.apply(parked, Action.SellStored(GoodId.ONIONS, 30)) as Outcome.Done
        compose.onNodeWithText("Cash ${formatRupees(cashed.state.cashRupees)} · Debt ₹5,000").assertExists()
    }

    @Test
    fun `the godown man bills every day on the rails`() {
        val parked = RunState.newRun(seed = 42)
            .copy(godowns = mapOf(CityId.MUMBAI to mapOf(GoodId.ONIONS to 10)))
        start(parked)
        tapCity(CityId.DELHI)
        compose.onNodeWithTag(TestTags.BOARD_TRAIN).performClick()
        settleEvents()

        // One day on the rails at fifty a day for the goods sitting in Mumbai,
        // a day's keep in Delhi, the sahukar's 8% on the day besides.
        compose.onNodeWithText("Cash ₹9,850 · Debt ₹5,400").assertExists()
        compose.onNodeWithTag(TestTags.PLACE_GODOWN).performClick()
        compose.onNodeWithText("Nothing stored in this city.").assertExists()
    }

    @Test
    fun `holding a market button opens the keypad and buys the amount typed`() {
        start()
        openMarket()

        compose.onNodeWithContentDescription("Buy one Onions")
            .performSemanticsAction(SemanticsActions.OnLongClick)

        compose.onNodeWithTag(TestTags.KEYPAD_SHEET).assertExists()
        // Nothing typed yet, so nothing can be confirmed.
        compose.onNodeWithTag(TestTags.AMOUNT_CONFIRM).assertIsNotEnabled()

        // More than the pocket holds: the button stays dark.
        compose.onNodeWithTag(TestTags.key("2")).performClick()
        compose.onNodeWithTag(TestTags.key("0")).performClick()
        compose.onNodeWithTag(TestTags.key("0")).performClick()
        compose.onNodeWithTag(TestTags.AMOUNT_CONFIRM).assertIsNotEnabled()

        compose.onNodeWithTag(TestTags.key("del")).performClick()
        compose.onNodeWithText("Buy 20 Onions").assertExists()
        compose.onNodeWithTag(TestTags.AMOUNT_CONFIRM).performClick()
        settleEvents()

        compose.onNodeWithText("quintal · 20 held").assertExists()

        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag(TestTags.KEYPAD_SHEET).assertDoesNotExist()
    }

    @Test
    fun `the exact chip on a good opens the keypad too`() {
        start()
        openMarket()

        compose.onNodeWithTag(TestTags.exact("market_ONIONS")).performClick()

        compose.onNodeWithTag(TestTags.KEYPAD_SHEET).assertExists()
        compose.onNodeWithText("0 quintal").assertExists()
    }

    @Test
    fun `the keypad lends an exact amount`() {
        start(newRun.copy(debtRupees = 0, debtDueDay = 0))
        compose.onNodeWithTag(TestTags.PLACE_SAHUKAR).performClick()

        compose.onNodeWithTag(TestTags.exact("money_BORROW")).performClick()
        compose.onNodeWithTag(TestTags.key("7")).performClick()
        compose.onNodeWithTag(TestTags.key("5")).performClick()
        compose.onNodeWithTag(TestTags.key("0")).performClick()
        compose.onNodeWithTag(TestTags.key("0")).performClick()

        compose.onNodeWithText("Borrow ₹7,500").assertExists()
        compose.onNodeWithTag(TestTags.AMOUNT_CONFIRM).performClick()
        settleEvents()

        compose.onNodeWithText("You owe ₹7,500").assertExists()
    }

    @Test
    fun `the godown stores exactly what is typed`() {
        val loaded = RunState.newRun(seed = 42).copy(holdings = mapOf(GoodId.ONIONS to 50))
        start(loaded)
        compose.onNodeWithTag(TestTags.PLACE_GODOWN).performClick()

        compose.onNodeWithTag(TestTags.exact("godown_ONIONS_STORE")).performClick()
        compose.onNodeWithTag(TestTags.key("3")).performClick()
        compose.onNodeWithTag(TestTags.key("0")).performClick()

        compose.onNodeWithText("Store 30 Onions").assertExists()
        compose.onNodeWithTag(TestTags.AMOUNT_CONFIRM).performClick()
        settleEvents()

        compose.onNodeWithText("Onions · 30 stored").assertExists()
        compose.onNodeWithText("Onions · 20 carried").assertExists()
    }

    @Test
    fun `news on the price shows in the mandi`() {
        val shifted = newRun.copy(
            priceShifts = listOf(PriceShift(CityId.MUMBAI, GoodId.ONIONS, 5_000)),
        )
        start(shifted)
        openMarket()

        // Onions at +50% of their usual Mumbai price.
        compose.onNodeWithText(formatRupees(shifted.priceOf(GoodId.ONIONS))).assertExists()
    }
}
