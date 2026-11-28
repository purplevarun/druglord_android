package com.purplevarun.trader.engine

import com.purplevarun.trader.engine.GoodId.COTTON
import com.purplevarun.trader.engine.GoodId.GOLD
import com.purplevarun.trader.engine.GoodId.ONIONS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameTest {

    private val start = RunState.newRun(seed = 42)

    @Test
    fun `a new run starts in Mumbai on day 1 of 60 with 10,000 cash, 5,000 debt and empty luggage`() {
        assertEquals(CityId.MUMBAI, start.city)
        assertEquals(1, start.day)
        assertEquals(60, start.runLengthDays)
        assertEquals(10_000L, start.cashRupees)
        assertEquals(5_000L, start.debtRupees)
        assertEquals(0, start.luggageUsed)
        assertEquals(100, start.luggageCapacity)
    }

    @Test
    fun `buying takes the price from cash and puts the goods in the luggage`() {
        val after = done(Game.applyQuietly(start, Action.Buy(ONIONS, 3)))
        // The order nudged the price up a touch; the goods were charged at the moved price.
        assertEquals(10_000L - 3 * after.priceOf(ONIONS), after.cashRupees)
        assertEquals(3, after.held(ONIONS))
        assertEquals(3, after.luggageUsed)
    }

    @Test
    fun `selling back what was just bought loses a little to the market`() {
        val bought = done(Game.applyQuietly(start, Action.Buy(COTTON, 2)))
        val sold = done(Game.applyQuietly(bought, Action.Sell(COTTON, 2)))
        // The buy pushed the price up and the sell pulled it back: the spread is the market's cut.
        assertTrue(sold.cashRupees < 10_000)
        assertTrue(sold.cashRupees >= 10_000 - bought.priceOf(COTTON))
        assertEquals(0, sold.held(COTTON))
        assertFalse("nothing left of a good means it is not listed at all", COTTON in sold.holdings)
    }

    @Test
    fun `buying does not change net worth on the same day`() {
        val loaded = done(Game.applyQuietly(start, Action.Buy(COTTON, 4)))
        assertEquals(5_000L, start.netWorthRupees)
        assertEquals(5_000L, loaded.netWorthRupees)
    }

    @Test
    fun `you cannot spend more cash than you have`() {
        rejected(Rejection.NOT_ENOUGH_CASH, Game.applyQuietly(start, Action.Buy(GOLD, 1)))
    }

    @Test
    fun `you cannot carry more than the luggage holds`() {
        val rich = start.copy(cashRupees = 100_000_000)
        val full = done(Game.applyQuietly(rich, Action.Buy(ONIONS, 100)))
        rejected(Rejection.NOT_ENOUGH_LUGGAGE_SPACE, Game.applyQuietly(full, Action.Buy(ONIONS, 1)))
        rejected(Rejection.NOT_ENOUGH_LUGGAGE_SPACE, Game.applyQuietly(rich, Action.Buy(COTTON, 101)))
    }

    @Test
    fun `you cannot sell more than you carry`() {
        rejected(Rejection.NOT_ENOUGH_HELD, Game.applyQuietly(start, Action.Sell(ONIONS, 1)))
        val two = done(Game.applyQuietly(start, Action.Buy(ONIONS, 2)))
        rejected(Rejection.NOT_ENOUGH_HELD, Game.applyQuietly(two, Action.Sell(ONIONS, 3)))
    }

    @Test
    fun `quantities must be at least one`() {
        rejected(Rejection.NOT_A_POSITIVE_QUANTITY, Game.applyQuietly(start, Action.Buy(ONIONS, 0)))
        rejected(Rejection.NOT_A_POSITIVE_QUANTITY, Game.applyQuietly(start, Action.Sell(ONIONS, -1)))
    }

    @Test
    fun `the most you can buy is set by cash or by luggage space, whichever runs out first`() {
        val price = start.priceOf(ONIONS)
        assertEquals(7, start.copy(cashRupees = price * 8 - 1).maxBuyable(ONIONS))
        val nearlyFull = start.copy(cashRupees = 100_000_000, holdings = mapOf(COTTON to 95))
        assertEquals(5, nearlyFull.maxBuyable(ONIONS))
        assertEquals(0, start.maxBuyable(GOLD))
    }

    @Test
    fun `travelling moves you and spends the days the trip takes, keeping cash and goods`() {
        val loaded = done(Game.applyQuietly(start, Action.Buy(ONIONS, 5)))
        val arrived = done(Game.applyQuietly(loaded, Action.Travel(CityId.SRINAGAR)))
        assertEquals(CityId.SRINAGAR, arrived.city)
        assertEquals(3, arrived.day)
        assertEquals(loaded.cashRupees, arrived.cashRupees)
        assertEquals(5, arrived.held(ONIONS))
    }

    @Test
    fun `you cannot take a trip that ends after the last day`() {
        val lastButOneDay = start.copy(day = 59)
        assertEquals(60, done(Game.applyQuietly(lastButOneDay, Action.Travel(CityId.DELHI))).day)
        rejected(Rejection.NOT_ENOUGH_DAYS, Game.applyQuietly(lastButOneDay, Action.Travel(CityId.SRINAGAR)))
    }

    @Test
    fun `travelling to where you already are is not a trip`() {
        rejected(Rejection.ALREADY_THERE, Game.applyQuietly(start, Action.Travel(CityId.MUMBAI)))
    }

    @Test
    fun `an endless run has no last day`() {
        val endless = RunState.newRun(seed = 42, runLengthDays = GameConstants.ENDLESS_RUN_LENGTH_DAYS)
        assertTrue(endless.isEndless)
        assertFalse(start.isEndless)
        val farAlong = endless.copy(day = 1_000)
        assertEquals(1_001, done(Game.applyQuietly(farAlong, Action.Travel(CityId.DELHI))).day)
    }

    @Test
    fun `the last day is the day the run ends on, which an endless run never has`() {
        assertFalse(start.isOnLastDay)
        assertTrue(start.copy(day = 60).isOnLastDay)
        val endless = RunState.newRun(seed = 42, runLengthDays = GameConstants.ENDLESS_RUN_LENGTH_DAYS)
        assertFalse(endless.copy(day = 1_000).isOnLastDay)
    }

    private fun done(outcome: Outcome): RunState {
        check(outcome is Outcome.Done) { "expected the action to go through, but it was $outcome" }
        return outcome.state
    }

    private fun rejected(expected: Rejection, outcome: Outcome) {
        assertEquals(Outcome.Rejected(expected), outcome)
    }
}
