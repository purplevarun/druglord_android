package com.purplevarun.trader.engine

import com.purplevarun.trader.engine.CityId.DELHI
import com.purplevarun.trader.engine.CityId.MUMBAI
import com.purplevarun.trader.engine.GoodId.COTTON
import com.purplevarun.trader.engine.GoodId.GOLD
import com.purplevarun.trader.engine.GoodId.ONIONS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The godowns: goods parked in a city, paying rent, safe from the road's hazards. */
class GodownsTest {

    private val start = RunState.newRun(seed = 42)

    private fun done(outcome: Outcome): RunState =
        (outcome as Outcome.Done).state

    private fun rejected(reason: Rejection, outcome: Outcome) {
        assertEquals(Outcome.Rejected(reason), outcome)
    }

    @Test
    fun `stashing takes goods off your back and puts them in the godown`() {
        val loaded = done(Game.applyQuietly(start, Action.Buy(ONIONS, 5)))
        val stored = done(Game.applyQuietly(loaded, Action.Store(ONIONS, 3)))

        assertEquals(2, stored.held(ONIONS))
        assertEquals(3, stored.stored(MUMBAI, ONIONS))
        assertEquals(2, stored.luggageUsed)
    }

    @Test
    fun `you can only stash what you carry`() {
        rejected(Rejection.NOT_ENOUGH_HELD, Game.applyQuietly(start, Action.Store(ONIONS, 1)))
    }

    @Test
    fun `a godown holds five hundred and no more`() {
        val nearlyFull = start.copy(
            cashRupees = 1,
            godowns = mapOf(MUMBAI to mapOf(ONIONS to 490)),
            holdings = mapOf(COTTON to 20),
        )
        rejected(Rejection.GODOWN_FULL, Game.applyQuietly(nearlyFull, Action.Store(COTTON, 11)))
        val topped = done(Game.applyQuietly(nearlyFull, Action.Store(COTTON, 10)))
        assertEquals(500, topped.godownUsed(MUMBAI))
    }

    @Test
    fun `retrieving needs the goods there and room on your back`() {
        val stored = start.copy(godowns = mapOf(MUMBAI to mapOf(ONIONS to 95)))
        rejected(Rejection.NOT_ENOUGH_STORED, Game.applyQuietly(stored, Action.Retrieve(ONIONS, 96)))

        val loaded = stored.copy(holdings = mapOf(COTTON to 50))
        rejected(Rejection.NOT_ENOUGH_LUGGAGE_SPACE, Game.applyQuietly(loaded, Action.Retrieve(ONIONS, 60)))
        assertEquals(100, done(Game.applyQuietly(loaded, Action.Retrieve(ONIONS, 50))).luggageUsed)
    }

    @Test
    fun `a godown only lists what it really holds`() {
        val stored = start.copy(godowns = mapOf(MUMBAI to mapOf(ONIONS to 2)))
        val emptied = done(Game.applyQuietly(stored, Action.Retrieve(ONIONS, 2)))
        assertFalse("an empty godown is not listed at all", MUMBAI in emptied.godowns)
    }

    @Test
    fun `selling from the store pays its price and moves the market`() {
        val stored = start.copy(godowns = mapOf(MUMBAI to mapOf(ONIONS to 95)))
        val base = stored.priceOf(ONIONS)

        val sold = done(Game.applyQuietly(stored, Action.SellStored(ONIONS, 95)))

        assertTrue(sold.cashRupees > start.cashRupees)
        assertFalse(MUMBAI in sold.godowns)
        assertTrue(sold.priceOf(ONIONS) < base)
        rejected(Rejection.NOT_ENOUGH_STORED, Game.applyQuietly(stored, Action.SellStored(ONIONS, 96)))
    }

    @Test
    fun `rent is charged for each day on the rails, for each city your goods sit in`() {
        val parked = start.copy(
            cashRupees = 10_000,
            godowns = mapOf(MUMBAI to mapOf(ONIONS to 10), DELHI to mapOf(GOLD to 1)),
        )
        val after = done(Game.applyQuietly(parked, Action.Travel(DELHI)))
        // Two godowns at fifty a day each, one day on the rails.
        assertEquals(10_000 - 100, after.cashRupees)
    }

    @Test
    fun `rent you cannot pay lands on your debt`() {
        val parked = start.copy(
            cashRupees = 20,
            godowns = mapOf(MUMBAI to mapOf(ONIONS to 10), DELHI to mapOf(GOLD to 1)),
        )
        val after = done(Game.applyQuietly(parked, Action.Travel(DELHI)))
        assertEquals(0, after.cashRupees)
        assertEquals(5_400 + 80, after.debtRupees) // a day's 8% on 5,000, plus the unpaid 80
    }

    @Test
    fun `empty godowns cost nothing`() {
        val after = done(Game.applyQuietly(start, Action.Travel(DELHI)))
        assertEquals(start.cashRupees, after.cashRupees)
    }

    @Test
    fun `what is stashed counts in your net worth at its city's price`() {
        val stashed = start.copy(godowns = mapOf(DELHI to mapOf(ONIONS to 10)))
        val worth = start.cashRupees + 10 * stashed.priceOfIn(DELHI, ONIONS) - start.debtRupees
        assertEquals(worth, stashed.netWorthRupees)
    }

    @Test
    fun `what is stashed survives the road's hazards`() {
        val parked = start.copy(
            godowns = mapOf(MUMBAI to mapOf(ONIONS to 30)),
            holdings = mapOf(COTTON to 10),
        )
        val robbed = Game.applyEvent(
            parked,
            GameEvent.Checkpost(bribeRupees = 0, seizedGood = COTTON, seizedUnits = 5),
        )
        assertEquals(30, robbed.stored(MUMBAI, ONIONS))
        assertEquals(5, robbed.held(COTTON))
    }

    @Test
    fun `a bandh day charges a day's rent too`() {
        val parked = start.copy(
            cashRupees = 10_000,
            godowns = mapOf(MUMBAI to mapOf(ONIONS to 10)),
        )
        val held = Game.applyEvent(parked, GameEvent.Bandh(extraDays = 1))
        assertEquals(10_000 - GameConstants.GODOWN_DAILY_RENT_RUPEES, held.cashRupees)
    }
}
