package com.purplevarun.trader.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The sahukar, the bank and the luggage shop: borrowing and repaying, deposits and
 * withdrawals, transport upgrades, and the interest each travel day adds.
 */
class FacilitiesTest {

    private val start = RunState.newRun(seed = 42)

    @Test
    fun `borrowing puts money in your pocket and the same on your debt`() {
        val after = done(Game.apply(start, Action.Borrow(2_000)))
        assertEquals(12_000L, after.cashRupees)
        assertEquals(7_000L, after.debtRupees)
        assertEquals(start.netWorthRupees, after.netWorthRupees)
    }

    @Test
    fun `repaying takes cash off you and debt off the books`() {
        val after = done(Game.apply(start, Action.Repay(3_000)))
        assertEquals(7_000L, after.cashRupees)
        assertEquals(2_000L, after.debtRupees)
    }

    @Test
    fun `you cannot repay more than you owe or more than you have`() {
        rejected(Rejection.REPAYS_MORE_THAN_OWED, Game.apply(start, Action.Repay(5_001)))
        val poor = start.copy(cashRupees = 1_000)
        rejected(Rejection.NOT_ENOUGH_CASH, Game.apply(poor, Action.Repay(2_000)))
    }

    @Test
    fun `a deposit moves cash into the bank and a withdrawal brings it back`() {
        val saved = done(Game.apply(start, Action.Deposit(4_000)))
        assertEquals(6_000L, saved.cashRupees)
        assertEquals(4_000L, saved.bankRupees)

        val back = done(Game.apply(saved, Action.Withdraw(1_500)))
        assertEquals(7_500L, back.cashRupees)
        assertEquals(2_500L, back.bankRupees)
    }

    @Test
    fun `you cannot deposit what you do not have or withdraw what you did not save`() {
        rejected(Rejection.NOT_ENOUGH_CASH, Game.apply(start, Action.Deposit(10_001)))
        rejected(Rejection.NOT_ENOUGH_SAVED, Game.apply(start, Action.Withdraw(1)))
    }

    @Test
    fun `amounts must be at least one rupee`() {
        for (action in listOf(
            Action.Borrow(0), Action.Repay(0), Action.Deposit(0), Action.Withdraw(-5),
        )) {
            rejected(Rejection.NOT_A_POSITIVE_QUANTITY, Game.apply(start, action))
        }
    }

    @Test
    fun `the sahukar charges 8 percent on every day the train takes`() {
        val indebted = done(Game.apply(start, Action.Borrow(10_000)))
        val day2 = done(Game.apply(indebted, Action.Travel(CityId.DELHI)))
        assertEquals(15_000L * 108 / 100, day2.debtRupees)

        val day4 = done(Game.apply(day2.copy(city = CityId.MUMBAI), Action.Travel(CityId.SRINAGAR)))
        assertEquals(18_896L, day4.debtRupees)
    }

    @Test
    fun `the bank pays 1 percent on deposits for every travel day`() {
        val saved = done(Game.apply(start, Action.Deposit(10_000)))
        val day2 = done(Game.apply(saved, Action.Travel(CityId.DELHI)))
        assertEquals(10_100L, day2.bankRupees)

        val day4 = done(Game.apply(day2.copy(city = CityId.MUMBAI), Action.Travel(CityId.SRINAGAR)))
        assertEquals(10_303L, day4.bankRupees)
    }

    @Test
    fun `saved money counts toward net worth`() {
        val saved = done(Game.apply(start, Action.Deposit(10_000)))
        assertEquals(start.netWorthRupees, saved.netWorthRupees)
    }

    @Test
    fun `a better transport costs its price and raises the luggage`() {
        val after = done(Game.apply(start, Action.BuyTransport(TransportId.TRUNK)))
        assertEquals(6_000L, after.cashRupees)
        assertEquals(TransportId.TRUNK, after.transport)
        assertEquals(250, after.luggageCapacity)
    }

    @Test
    fun `you cannot buy a transport that holds no more than yours`() {
        rejected(Rejection.NOT_AN_UPGRADE, Game.apply(start, Action.BuyTransport(TransportId.JHOLA)))
        val trunked = done(Game.apply(start, Action.BuyTransport(TransportId.TRUNK)))
        rejected(Rejection.NOT_AN_UPGRADE, Game.apply(trunked, Action.BuyTransport(TransportId.TRUNK)))
    }

    @Test
    fun `you cannot buy a transport you cannot afford`() {
        rejected(Rejection.NOT_ENOUGH_CASH, Game.apply(start, Action.BuyTransport(TransportId.TRUCK)))
    }

    private fun done(outcome: Outcome): RunState {
        check(outcome is Outcome.Done) { "expected the action to go through, but it was $outcome" }
        return outcome.state
    }

    private fun rejected(expected: Rejection, outcome: Outcome) {
        assertEquals(Outcome.Rejected(expected), outcome)
    }
}
