package com.purplevarun.trader.engine

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Thousands of random moves, checking after every one that no rule of a run has been broken. */
class RunSimulationTest {

    @Test
    fun `random play never breaks the rules of a run`() {
        val random = Random(20_000)
        var buys = 0
        var sells = 0
        var trips = 0

        repeat(RUNS) {
            var state = RunState.newRun(seed = random.nextLong())
            repeat(MOVES_PER_RUN) {
                val action = randomAction(random)
                val outcome = Game.apply(state, action)
                if (outcome is Outcome.Done) {
                    when (action) {
                        is Action.Buy -> buys++
                        is Action.Sell -> sells++
                        is Action.Travel -> trips++
                    }
                    state = outcome.state
                }
                assertTrue("cash went negative after $action", state.cashRupees >= 0)
                assertTrue("luggage overflowed after $action", state.luggageUsed <= state.luggageCapacity)
                assertTrue("day left the run after $action", state.day in 1..state.runLengthDays)
                assertTrue("an empty good was listed after $action", state.holdings.values.all { it > 0 })
            }
        }

        assertTrue("random play should buy something", buys > 0)
        assertTrue("random play should sell something", sells > 0)
        assertTrue("random play should travel", trips > 0)
    }

    private fun randomAction(random: Random): Action = when (random.nextInt(3)) {
        0 -> Action.Buy(GoodId.entries.random(random), random.nextInt(-2, 40))
        1 -> Action.Sell(GoodId.entries.random(random), random.nextInt(-2, 40))
        else -> Action.Travel(CityId.entries.random(random))
    }

    private companion object {
        const val RUNS = 50
        const val MOVES_PER_RUN = 400
    }
}
