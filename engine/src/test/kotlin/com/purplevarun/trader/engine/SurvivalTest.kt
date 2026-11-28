package com.purplevarun.trader.engine

import com.purplevarun.trader.engine.CityId.DELHI
import com.purplevarun.trader.engine.CityId.MUMBAI
import com.purplevarun.trader.engine.GoodId.COTTON
import com.purplevarun.trader.engine.GoodId.ONIONS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The survival loop: living costs, the single loan, and the collector's deadline. */
class SurvivalTest {

    private val start = RunState.newRun(seed = 42)

    private fun done(outcome: Outcome): RunState = (outcome as Outcome.Done).state

    private fun rejected(reason: Rejection, outcome: Outcome) {
        assertEquals(Outcome.Rejected(reason), outcome)
    }

    @Test
    fun `every move in a city costs a day of living there`() {
        val after = done(Game.applyQuietly(start, Action.Buy(ONIONS, 1)))
        val spent = 10_000 - after.cashRupees
        val price = after.priceOf(ONIONS)
        assertEquals(price + start.livingCostPerDay, spent)
    }

    @Test
    fun `a day on the rails is lived at the destination's rate`() {
        val after = done(Game.applyQuietly(start, Action.Travel(DELHI)))
        // One rail day at Delhi's rate, and the day's interest besides.
        assertEquals(start.cashRupees - after.livingCostPerDay, after.cashRupees)
        assertEquals(5_400, after.debtRupees)
    }

    @Test
    fun `the sahukar lends once, while you owe there is no more`() {
        rejected(Rejection.ALREADY_OWES, Game.applyQuietly(start, Action.Borrow(1_000)))
        val repaid = done(Game.applyQuietly(start, Action.Repay(5_000)))
        val borrowed = done(Game.applyQuietly(repaid, Action.Borrow(2_000)))
        assertEquals(2_000, borrowed.debtRupees)
        rejected(Rejection.ALREADY_OWES, Game.applyQuietly(borrowed, Action.Borrow(1_000)))
    }

    @Test
    fun `borrowing starts the collector's clock and paying it all off ends it`() {
        val repaid = done(Game.applyQuietly(start, Action.Repay(5_000)))
        assertEquals("a cleared debt clears the deadline", 0, repaid.debtDueDay)

        val borrowed = done(Game.applyQuietly(repaid, Action.Borrow(2_000)))
        assertEquals(repaid.day + GameConstants.COLLECTOR_AFTER_DAYS, borrowed.debtDueDay)
    }

    @Test
    fun `the run begins owing, and the collector is already counting`() {
        assertEquals(1 + GameConstants.COLLECTOR_AFTER_DAYS, start.debtDueDay)
        assertFalse(start.collectorDue)
    }

    @Test
    fun `ten days on, the collector comes for the whole debt`() {
        var state = start.copy(day = start.debtDueDay - 1, cashRupees = 100_000)
        assertFalse(state.collectorDue)
        state = done(Game.applyQuietly(state, Action.Travel(DELHI)))
        assertTrue(state.collectorDue)
    }

    @Test
    fun `the collector takes cash first, then the bank`() {
        val state = start.copy(
            day = start.debtDueDay,
            cashRupees = 4_000,
            bankRupees = 3_000,
        )
        val after = done(Game.applyQuietly(state, Action.PayCollector))
        assertEquals(0, after.cashRupees)
        assertEquals(2_000, after.bankRupees)
        // Paying took a day too, and living it with an empty pocket lands back on the debt.
        assertEquals(after.livingCostPerDay, after.debtRupees)
        assertEquals(0, after.debtDueDay)
        assertFalse(after.collectorDue)
    }

    @Test
    fun `if you cannot pay the collector you do not pay`() {
        val state = start.copy(day = start.debtDueDay, cashRupees = 4_000, bankRupees = 500)
        rejected(Rejection.NOT_ENOUGH_CASH, Game.applyQuietly(state, Action.PayCollector))
    }

    @Test
    fun `paying nothing is refused even at the collector's door`() {
        rejected(Rejection.NOTHING_OWED, Game.applyQuietly(
            start.copy(debtRupees = 0, debtDueDay = 0), Action.PayCollector))
    }

    @Test
    fun `broke means nothing left at all`() {
        assertFalse(start.isBroke)
        assertTrue(start.copy(cashRupees = 0, bankRupees = 0).isBroke)
        // Goods on your back or in a godown still count for something.
        assertFalse(start.copy(cashRupees = 0, bankRupees = 0, holdings = mapOf(ONIONS to 1)).isBroke)
        assertFalse(
            start.copy(
                cashRupees = 0,
                bankRupees = 0,
                godowns = mapOf(DELHI to mapOf(COTTON to 1)),
            ).isBroke,
        )
    }

    @Test
    fun `a bandh day is lived where you are stuck`() {
        val held = Game.applyEvent(start, GameEvent.Bandh(extraDays = 1))
        // The extra day: a day's living at the city you're in, a day's interest.
        assertEquals(start.cashRupees - start.livingCostPerDay, held.cashRupees)
        assertEquals(5_400, held.debtRupees)
    }

    @Test
    fun `living you cannot pay lands on your debt`() {
        val threadbare = start.copy(cashRupees = 10, debtRupees = 0)
        // The deposit leaves 9 in the pocket; the day's living takes it and bills the rest.
        val after = done(Game.applyQuietly(threadbare, Action.Deposit(1)))
        assertEquals(0, after.cashRupees)
        assertEquals(threadbare.livingCostPerDay - 9, after.debtRupees)
    }
}
