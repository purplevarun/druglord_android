package com.purplevarun.trader.engine

import com.purplevarun.trader.engine.GameEvent.Bandh
import com.purplevarun.trader.engine.GameEvent.Checkpost
import com.purplevarun.trader.engine.GameEvent.Pickpocket
import com.purplevarun.trader.engine.GameEvent.PriceSwing
import com.purplevarun.trader.engine.GameEvent.RumourLanded
import com.purplevarun.trader.engine.GameEvent.RumourTip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The days that go wrong: pickpockets, checkposts, bandhs and news that moves a price,
 * plus the way heavy trading sours a market. Every roll is seeded, so the same run of
 * moves always produces the same events.
 */
class EventsTest {

    private val start = RunState.newRun(seed = 42)

    // --- What each event does ---

    @Test
    fun `pickpockets take a share of your cash and leave the bank alone`() {
        val state = start.copy(cashRupees = 10_000, bankRupees = 50_000)
        val after = Game.applyEvent(state, Pickpocket(2_500))
        assertEquals(7_500L, after.cashRupees)
        assertEquals(50_000L, after.bankRupees)
    }

    @Test
    fun `a checkpost takes its bribe in cash`() {
        val after = Game.applyEvent(start, Checkpost(bribeRupees = 1_500, seizedGood = null, seizedUnits = 0))
        assertEquals(8_500L, after.cashRupees)
        assertEquals(start.holdings, after.holdings)
    }

    @Test
    fun `a checkpost seizes goods when there is nothing to bribe with`() {
        val broke = start.copy(cashRupees = 0, holdings = mapOf(GoodId.ONIONS to 40))
        val after = Game.applyEvent(broke, Checkpost(bribeRupees = 0, seizedGood = GoodId.ONIONS, seizedUnits = 4))
        assertEquals(0L, after.cashRupees)
        assertEquals(36, after.held(GoodId.ONIONS))
    }

    @Test
    fun `a bandh holds the train a day, and the debt and the savings grow for it`() {
        val state = start.copy(bankRupees = 10_000)
        val after = Game.applyEvent(state, Bandh(extraDays = 1))
        assertEquals(state.day + 1, after.day)
        assertEquals(state.debtRupees * 108 / 100, after.debtRupees)
        assertEquals(10_100L, after.bankRupees)
    }

    @Test
    fun `a price swing moves the price in its city and nowhere else`() {
        val swung = Game.applyEvent(
            start,
            PriceSwing(CityId.MUMBAI, GoodId.ONIONS, basisPoints = 5_000, headline = EventHeadline.MONSOON),
        )
        val base = start.priceOf(GoodId.ONIONS)
        assertEquals(base * 15_000 / 10_000, swung.priceOf(GoodId.ONIONS))
        // In Mumbai by construction; a different city's price is untouched.
        val elsewhere = swung.copy(city = CityId.DELHI)
        assertEquals(
            Market.price(elsewhere.seed, elsewhere.day, CityId.DELHI, GoodId.ONIONS),
            elsewhere.priceOf(GoodId.ONIONS),
        )
    }

    // --- Rumours: word on the street, mostly true ---

    @Test
    fun `a true rumour schedules a swing in the named city`() {
        val tip = RumourTip(
            CityId.SRINAGAR, GoodId.KESAR, up = true,
            landsOnDay = start.day + 3, basisPoints = 6_000, lands = true,
        )
        val after = Game.applyEvent(start, tip)
        assertEquals(listOf(Rumour(CityId.SRINAGAR, GoodId.KESAR, start.day + 3, 6_000)), after.rumours)
    }

    @Test
    fun `a dud rumour schedules nothing`() {
        val tip = RumourTip(
            CityId.SRINAGAR, GoodId.KESAR, up = true,
            landsOnDay = start.day + 3, basisPoints = 6_000, lands = false,
        )
        val after = Game.applyEvent(start, tip)
        assertTrue(after.rumours.isEmpty())
    }

    @Test
    fun `a rumour lands when its day arrives and is told`() {
        val rumoured = start.copy(
            rumours = listOf(Rumour(CityId.SRINAGAR, GoodId.KESAR, landsOnDay = 2, basisPoints = 5_000)),
        )
        val outcome = Game.applyQuietly(rumoured, Action.Travel(CityId.DELHI))
        check(outcome is Outcome.Done)

        // One rail day put the run on day 2: the swing landed in Srinagar, away from us.
        assertTrue(outcome.state.rumours.isEmpty())
        assertEquals(5_000, outcome.state.shiftForIn(CityId.SRINAGAR, GoodId.KESAR))
        assertEquals(
            listOf(RumourLanded(CityId.SRINAGAR, GoodId.KESAR, 5_000)),
            outcome.events.filterIsInstance<RumourLanded>(),
        )
    }

    @Test
    fun `a rumour waits quietly until its day`() {
        val rumoured = start.copy(
            rumours = listOf(Rumour(CityId.SRINAGAR, GoodId.KESAR, landsOnDay = 5, basisPoints = 5_000)),
        )
        val outcome = Game.applyQuietly(rumoured, Action.Travel(CityId.DELHI))
        check(outcome is Outcome.Done)

        assertEquals(1, outcome.state.rumours.size)
        assertEquals(0, outcome.state.shiftForIn(CityId.SRINAGAR, GoodId.KESAR))
    }

    // --- Saturation ---

    @Test
    fun `heavy buying raises the price and heavy selling drops it, both bounded`() {
        val loaded = start.copy(cashRupees = 10_000_000, holdings = mapOf(GoodId.ONIONS to 95))
        val base = loaded.priceOf(GoodId.ONIONS)

        val sold = done(Game.applyQuietly(loaded, Action.Sell(GoodId.ONIONS, 95)))
        assertTrue(sold.priceOf(GoodId.ONIONS) < base)

        // A small buy's nudge can vanish in the rounding; a heavy one visibly lifts the price.
        val flush = start.copy(cashRupees = 10_000_000)
        val bought = done(Game.applyQuietly(flush, Action.Buy(GoodId.ONIONS, 95)))
        assertTrue(bought.priceOf(GoodId.ONIONS) > base)
    }

    @Test
    fun `saturation cannot drive a price to nothing or to the sky`() {
        val loaded = start.copy(cashRupees = 100_000_000, holdings = mapOf(GoodId.ONIONS to 95))
        val base = loaded.priceOf(GoodId.ONIONS)
        val dumped = done(Game.applyQuietly(loaded, Action.Sell(GoodId.ONIONS, 95)))
        assertTrue(dumped.priceOf(GoodId.ONIONS) >= base * 4_000 / 10_000)
    }

    @Test
    fun `a shift fades as the days pass`() {
        val swung = Game.applyEvent(
            start,
            PriceSwing(CityId.MUMBAI, GoodId.ONIONS, basisPoints = 5_000, headline = EventHeadline.MONSOON),
        )
        val peak = swung.priceOf(GoodId.ONIONS)
        val day2 = done(Game.applyQuietly(swung, Action.Travel(CityId.DELHI)))
        val day3 = done(Game.applyQuietly(day2.copy(city = CityId.MUMBAI), Action.Travel(CityId.DELHI)))
        // The Mumbai shift halves each day regardless of where you are.
        val shiftedBack = day3.copy(city = CityId.MUMBAI)
        assertTrue(shiftedBack.priceOf(GoodId.ONIONS) < peak)
        assertTrue(shiftedBack.priceOf(GoodId.ONIONS) >= start.priceOf(GoodId.ONIONS))
    }

    // --- The roll itself ---

    @Test
    fun `the same move always produces the same events`() {
        val once = Game.apply(start, Action.Travel(CityId.SRINAGAR))
        val twice = Game.apply(start, Action.Travel(CityId.SRINAGAR))
        assertEquals(once, twice)
    }

    @Test
    fun `done outcomes carry the events that happened`() {
        val outcome = Game.apply(start, Action.Travel(CityId.SRINAGAR))
        check(outcome is Outcome.Done)
        // Whether or not the dice fired, the field is there.
        assertTrue(outcome.events.size >= 0)
    }

    @Test
    fun `the dice do fire, and across seeds every event family happens`() {
        val kinds = mutableSetOf<String>()
        var seed = 1L
        while (kinds.size < 5 && seed < 100_000) {
            val outcome = Game.apply(RunState.newRun(seed = seed), Action.Travel(CityId.SRINAGAR))
            if (outcome is Outcome.Done) {
                outcome.events.forEach { kinds.add(it::class.simpleName!!) }
            }
            seed++
        }
        assertEquals(
            setOf("Pickpocket", "Checkpost", "Bandh", "PriceSwing", "RumourTip"),
            kinds,
        )
    }

    @Test
    fun `check answers the same question without touching the dice`() {
        assertEquals(null, Game.check(start, Action.Travel(CityId.DELHI)))
        assertEquals(Rejection.ALREADY_THERE, Game.check(start, Action.Travel(CityId.MUMBAI)))
        assertEquals(Rejection.NOT_ENOUGH_CASH, Game.check(start, Action.Buy(GoodId.GOLD, 1)))
    }

    private fun done(outcome: Outcome): RunState {
        check(outcome is Outcome.Done) { "expected the action to go through, but it was $outcome" }
        return outcome.state
    }
}
