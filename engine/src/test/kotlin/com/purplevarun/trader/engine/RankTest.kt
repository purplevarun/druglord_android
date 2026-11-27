package com.purplevarun.trader.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/** The rank ladder: which title a finished run earns from its final net worth. */
class RankTest {

    /**
     * A run carrying nothing whose net worth is [netWorth]: net worth is cash less debt,
     * and the run's fixed debt is 5,000.
     */
    private fun stateWorth(netWorth: Long): RunState =
        RunState.newRun(seed = 1).copy(cashRupees = netWorth + GameConstants.STARTING_DEBT_RUPEES)

    @Test
    fun `ending owing more than everything you have makes you a divala`() {
        assertEquals(RankId.DIVALA, stateWorth(-1).rank)
        assertEquals(RankId.DIVALA, stateWorth(-5_000).rank)
    }

    @Test
    fun `ending about where you started is a thelawala`() {
        assertEquals(RankId.THELAWALA, stateWorth(0).rank)
        assertEquals(RankId.THELAWALA, stateWorth(5_000).rank)
        assertEquals(RankId.THELAWALA, stateWorth(24_999).rank)
    }

    @Test
    fun `the ladder climbs with net worth`() {
        assertEquals(RankId.DUKANDAAR, stateWorth(25_000).rank)
        assertEquals(RankId.SAUDAGAR, stateWorth(100_000).rank)
        assertEquals(RankId.SETH, stateWorth(1_000_000).rank)
        assertEquals(RankId.CROREPATI, stateWorth(10_000_000).rank)
        assertEquals(RankId.CROREPATI, stateWorth(123_456_789).rank)
    }

    @Test
    fun `a rupee short of a threshold keeps the lower rank`() {
        assertEquals(RankId.DUKANDAAR, stateWorth(99_999).rank)
        assertEquals(RankId.SAUDAGAR, stateWorth(999_999).rank)
        assertEquals(RankId.SETH, stateWorth(9_999_999).rank)
    }
}
