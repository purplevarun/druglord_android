package com.purplevarun.trader.engine

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A bot that plays the way a decent casual player would: sells what it carried when it
 * arrives, buys toward a city where the good runs dear, keeps enough cash to live on,
 * answers the collector, and uses the bank and the sahukar in moderation. It knows only
 * what an experienced player would — the usual price of a good in each city, not today's
 * exact board there.
 *
 * The report it prints is the balance pass's instrument: how often the opening collector
 * is survived, how the money grows, and how each run ends. The assertions under it pin
 * the tuned feel loosely, so a constants change cannot quietly make the game trivial or
 * impossible.
 */
class BalanceTest {

    @Test
    fun `the balance report`() {
        val runs = (1..RUNS).map { playRun(it.toLong() * 97_531L) }

        val survivedFirstCollector = runs.count { it.death == null || it.dayReached > FIRST_COLLECTOR_DAY }
        val brokeDeaths = runs.count { it.death == "broke" }
        val collectorDeaths = runs.count { it.death == "collector" }
        val stillGoing = runs.count { it.death == null }

        println("=== Balance report ($RUNS runs, horizon day $HORIZON_DAY) ===")
        println(
            "survived the day-$FIRST_COLLECTOR_DAY collector: " +
                "$survivedFirstCollector/${RUNS} (${percent(survivedFirstCollector)}%)",
        )
        // An "alive" run whose debt already outgrows its money is only waiting for the day.
        val thriving = runs.count { it.death == null && it.finalWorth >= 25_000 }
        val solvent = runs.count { it.death == null && it.finalWorth in 0..24_999 }
        val walkingDead = runs.count { it.death == null && it.finalWorth < 0 }
        println(
            "endings by day $HORIZON_DAY: thriving $thriving (${percent(thriving)}%), " +
                "solvent $solvent (${percent(solvent)}%), " +
                "in debt's grip $walkingDead (${percent(walkingDead)}%), " +
                "broke $brokeDeaths (${percent(brokeDeaths)}%), " +
                "collector $collectorDeaths (${percent(collectorDeaths)}%)",
        )
        for (checkpoint in CHECKPOINTS) {
            val worths = runs.mapNotNull { it.worthAtDay[checkpoint] }.sorted()
            if (worths.isEmpty()) continue
            println(
                "day $checkpoint net worth: p10 ${money(worths.p(10))}, " +
                    "median ${money(worths.p(50))}, p90 ${money(worths.p(90))} " +
                    "(${worths.size} runs reached it)",
            )
        }
        val finalWorths = runs.map { it.finalWorth }.sorted()
        println(
            "final net worth: p10 ${money(finalWorths.p(10))}, median ${money(finalWorths.p(50))}, " +
                "p90 ${money(finalWorths.p(90))}",
        )
        val ranks = runs.groupingBy { it.finalRank }.eachCount()
        println(
            "final ranks: " + RankId.entries.joinToString(", ") {
                "${it.name.lowercase()} ${ranks[it] ?: 0}"
            },
        )
        // Where the money went, summed over every run.
        println(
            "flows per run: trade p&l ${money(runs.sumOf { it.tradePnl } / RUNS)}, " +
                "events ${money(runs.sumOf { it.eventPnl } / RUNS)}, " +
                "living+rent billed ${money(runs.sumOf { it.livingPaid } / RUNS)}, " +
                "debt grown ${money(runs.sumOf { it.interestAccrued } / RUNS)}",
        )
        val days = runs.map { it.dayReached.toLong() }.sorted()
        println("days lasted: p10 ${days.p(10)}, median ${days.p(50)}, p90 ${days.p(90)}")
        if (TRACE_ONE) traceRun(1L * 97_531L)

        // The tuned feel, pinned loosely: decent play usually clears the first collector,
        // rarely starves outright, grows at a running pace — and the top rank stays a
        // horizon, not a routine day-60 landing.
        assertTrue(
            "decent play should mostly survive the first collector (${survivedFirstCollector}/$RUNS did)",
            survivedFirstCollector >= RUNS * 80 / 100,
        )
        assertTrue(
            "broke should be a real but uncommon ending ($brokeDeaths/$RUNS)",
            brokeDeaths <= RUNS * 15 / 100,
        )
        assertTrue(
            "most decent play should end ahead ($thriving/$RUNS thriving)",
            thriving >= RUNS * 60 / 100,
        )
        val medianFinal = finalWorths.p(50)
        assertTrue(
            "a median day-60 run should sit between dukandaar and seth (${money(medianFinal)})",
            medianFinal in 100_000..2_000_000,
        )
        val crorepatis = ranks[RankId.CROREPATI] ?: 0
        assertTrue(
            "crorepati should stay a long-haul rank at day 60 ($crorepatis/$RUNS)",
            crorepatis <= RUNS * 10 / 100,
        )
    }

    private fun percent(count: Int): Int = count * 100 / RUNS

    private fun List<Long>.p(percentile: Int): Long =
        this[((size - 1) * percentile / 100).coerceIn(0, size - 1)]

    private fun money(rupees: Long): String = "₹" + "%,d".format(rupees)

    // --- One simulated run ---

    private data class RunResult(
        val dayReached: Int,
        val death: String?,
        val worthAtDay: Map<Int, Long>,
        val finalWorth: Long,
        val finalRank: RankId,
        val tradePnl: Long,
        val eventPnl: Long,
        val livingPaid: Long,
        val interestAccrued: Long,
    )

    private fun playRun(seed: Long): RunResult {
        val bot = Bot(RunState.newRun(seed))
        val worthAtDay = mutableMapOf<Int, Long>()
        var steps = 0
        while (bot.death == null && bot.state.day <= HORIZON_DAY && steps++ < MAX_STEPS) {
            val nextCheckpoint = CHECKPOINTS.firstOrNull { it !in worthAtDay && bot.state.day >= it }
            if (nextCheckpoint != null) worthAtDay[nextCheckpoint] = bot.state.netWorthRupees
            bot.step()
        }
        for (checkpoint in CHECKPOINTS) {
            if (checkpoint !in worthAtDay && bot.state.day >= checkpoint) {
                worthAtDay[checkpoint] = bot.state.netWorthRupees
            }
        }
        return RunResult(
            dayReached = bot.state.day,
            death = bot.death,
            worthAtDay = worthAtDay,
            finalWorth = bot.state.netWorthRupees,
            finalRank = bot.state.rank,
            tradePnl = bot.tradePnl,
            eventPnl = bot.eventPnl,
            livingPaid = bot.livingPaid,
            interestAccrued = bot.interestAccrued,
        )
    }

    /** One run narrated move by move — for seeing how the bot actually dies. */
    private fun traceRun(seed: Long) {
        val bot = Bot(RunState.newRun(seed))
        var steps = 0
        println("--- trace of seed $seed ---")
        while (bot.death == null && bot.state.day <= HORIZON_DAY && steps++ < 300) {
            val before = bot.state
            bot.step()
            val s = bot.state
            if (steps <= 80) {
                println(
                    "day ${s.day} ${s.city} cash=${s.cashRupees} debt=${s.debtRupees}@${s.debtDueDay} " +
                        "bank=${s.bankRupees} held=${s.holdings} worth=${s.netWorthRupees} " +
                        "<- ${bot.lastAction}${if (bot.death != null) " DEATH=${bot.death}" else ""}",
                )
            }
        }
        println("end: day ${bot.state.day} death=${bot.death} worth=${bot.state.netWorthRupees}")
    }

    /**
     * The player: one action per [step]. Carries a plan — a good bought to sell in a named
     * city — so the luggage never gets sold short of its destination.
     */
    private class Bot(var state: RunState) {
        var death: String? = null
        private var plan: Pair<GoodId, CityId>? = null
        private var costBasis = 0L
        private var soldCostBasis = 0L
        var tradePnl = 0L
        var eventPnl = 0L
        var livingPaid = 0L
        var interestAccrued = 0L
        var lastAction: Action? = null

        fun step() {
            if (death != null) return
            val before = state
            val action = nextAction()
            lastAction = action
            if (death != null) return // the run was declared over while choosing the move
            val outcome = Game.apply(state, action)
            if (outcome is Outcome.Done) {
                state = outcome.state
                track(before, action, outcome)
            } else {
                // A planned move was refused: fall back to a day of doing nothing bankable.
                val fallback = if (state.cashRupees > 1) Action.Deposit(1) else null
                if (fallback != null && Game.apply(state, fallback) is Outcome.Done) {
                    state = (Game.apply(state, fallback) as Outcome.Done).state
                } else {
                    death = "stuck($action)"
                }
            }
        }

        /**
         * Cash-flow bookkeeping, so the report can say where the money went: what trading
         * itself earned (sells against the buy that stocked them), what the events took,
         * and what a day's keep — plus rent — billed, whether cash or debt covered it.
         */
        private fun track(before: RunState, action: Action, outcome: Outcome.Done) {
            when (action) {
                is Action.Buy -> costBasis = before.cashRupees - state.cashRupees
                is Action.Sell -> tradePnl += (state.cashRupees - before.cashRupees) - soldCostBasis
                is Action.SellStored -> tradePnl += state.cashRupees - before.cashRupees
                else -> Unit
            }
            for (event in outcome.events) {
                when (event) {
                    is GameEvent.Pickpocket -> eventPnl -= event.lostRupees
                    is GameEvent.Checkpost -> eventPnl -= event.bribeRupees
                    else -> Unit
                }
            }
            val daysPassed = state.day - before.day
            val billed = if (action is Action.Travel) {
                daysPassed * GameConstants.LIVING_COST_RUPEES.getValue(action.to) +
                    before.godownRentPerDay * daysPassed
            } else {
                before.livingCostPerDay +
                    outcome.events.filterIsInstance<GameEvent.Bandh>().sumOf { it.extraDays } *
                    before.livingCostPerDay
            }
            livingPaid += billed
            // Debt growth beyond principal borrowed is interest and unpaid keep.
            val debtDelta = state.debtRupees - before.debtRupees
            val principal = when (action) {
                is Action.Borrow -> action.amountRupees
                is Action.Repay -> -action.amountRupees
                Action.PayCollector -> -before.debtRupees
                else -> 0L
            }
            interestAccrued += debtDelta - principal
        }

        private fun nextAction(): Action {
            if (state.isBroke) {
                if (state.debtRupees <= 0) return Action.Borrow(GameConstants.SURVIVAL_LOAN_RUPEES)
                death = "broke"
                return Action.Borrow(1) // refused; the loop above ends on death anyway
            }
            if (state.collectorDue) {
                if (Game.check(state, Action.PayCollector) == null) return Action.PayCollector
                death = "collector"
                return Action.PayCollector // refused; run ends
            }

            // Debt costs nothing until the days pass, and a good hop earns more than the
            // sahukar's rate: carry it while trading. Clear it when flush — a clean name
            // reopens his book — and scramble to pay it down as the day closes in.
            val money = state.cashRupees + state.bankRupees
            if (state.debtRupees > 0) {
                if (money >= state.debtRupees + keepBuffer() &&
                    Game.check(state, Action.PayCollector) == null
                ) {
                    return Action.PayCollector
                }
                val pressing = state.debtDueDay - state.day <= 3
                if (pressing && state.cashRupees > state.livingCostPerDay) {
                    return Action.Repay(
                        minOf(state.cashRupees - state.livingCostPerDay, state.debtRupees),
                    )
                }
            }

            // Carrying goods: sell them on arrival, else keep travelling.
            if (state.holdings.isNotEmpty()) {
                val destination = plan?.second
                if (destination != null && destination != state.city) return Action.Travel(destination)
                val (good, quantity) = state.holdings.entries.first()
                plan = null
                soldCostBasis = costBasis
                return Action.Sell(good, quantity)
            }

            // Luggage first — but only once buying it still leaves real trading money.
            val nextTransport = GameConstants.TRANSPORTS.firstOrNull { it.capacity > state.luggageCapacity }
            if (nextTransport != null && state.debtRupees <= 0 &&
                state.cashRupees - nextTransport.priceRupees >= keepBuffer() + START_TRADE_CASH
            ) {
                return Action.BuyTransport(nextTransport.id)
            }

            // The pocket stays stocked: every action is a day of keep. A dry pocket
            // under a fat bank balance is a slow way to starve.
            if (state.cashRupees < keepBuffer() && state.bankRupees > 0) {
                return Action.Withdraw(
                    minOf(state.bankRupees, keepBuffer() * 2 + state.livingCostPerDay - state.cashRupees),
                )
            }
            // The trade itself: the best (good, city) the money and the luggage allow.
            val trade = bestTrade()
            if (trade != null) {
                val (good, destination, quantity) = trade
                // The bank trip is itself a day: draw enough to buy and still eat.
                val cost = buyCost(good, quantity)
                if (cost + state.livingCostPerDay > state.cashRupees && state.bankRupees > 0) {
                    return Action.Withdraw(
                        minOf(state.bankRupees, cost + state.livingCostPerDay - state.cashRupees),
                    )
                }
                plan = good to destination
                return Action.Buy(good, quantity)
            }

            // Only when there is nothing worth trading does surplus bank over pocket.
            if (state.cashRupees > keepBuffer() * 4 && state.debtRupees <= 0) {
                return Action.Deposit(state.cashRupees - keepBuffer() * 2)
            }

            // Nothing worth trading: move toward the cheapest keep, or just on.
            val cheapest = CityId.entries
                .filter { it != state.city }
                .minByOrNull { GameConstants.LIVING_COST_RUPEES.getValue(it) }
            return Action.Travel(cheapest ?: CityId.entries.first { it != state.city })
        }

        /**
         * Every (good, destination) pair judged on what an experienced trader would expect:
         * the usual price there (base times the city's way with it) against today's local
         * board, less the days and the keep the trip costs. Returns (good, city, quantity).
         */
        private fun bestTrade(): Triple<GoodId, CityId, Int>? {
            val space = state.luggageCapacity - state.luggageUsed
            if (space <= 0) return null
            var best: Triple<GoodId, CityId, Int>? = null
            var bestScore = 0.0
            val budget = state.cashRupees + state.bankRupees - keepBuffer()
            for (good in GoodId.entries) {
                val priceHere = state.priceOf(good)
                // Size for all the money there is, not just the pocket: a fat enough
                // trade is worth a trip to the bank.
                val quantity = buyableWith(good, budget)
                if (quantity <= 0) continue
                for (city in CityId.entries) {
                    if (city == state.city) continue
                    val usualThere = GameConstants.GOODS.first { it.id == good }.basePriceRupees *
                        (GameConstants.CITY_PRICE_FACTORS[city]?.get(good) ?: 1.0)
                    val days = RailNetwork.travelDays(state.city, city)
                    val keep = days * GameConstants.LIVING_COST_RUPEES.getValue(city)
                    val score = (usualThere - priceHere) * quantity - keep
                    if (score > bestScore) {
                        bestScore = score
                        best = Triple(good, city, quantity)
                    }
                }
            }
            // A hop is worth it once it beats a day's keep on top of the trip's own cost.
            if (best == null || bestScore <= state.livingCostPerDay) return null
            return best
        }

        /** Cash to live on: eight moves' keep, plus whatever the collector will soon want. */
        private fun keepBuffer(): Long =
            state.livingCostPerDay * 8 +
                if (state.debtDueDay - state.day in 1..4) state.debtRupees else 0L

        /** What [quantity] units of [good] cost at once, at the price the order itself moves to. */
        private fun buyCost(good: GoodId, quantity: Int): Long {
            val base = Market.price(state.seed, state.day, state.city, good)
            val rise = base * GameConstants.SATURATION_BP_PER_UNIT / 10_000.0
            return (quantity * state.priceOf(good) + quantity * quantity * rise).toLong()
        }

        /** The most of [good] [budget] and the luggage allow, priced the way the buy moves it. */
        private fun buyableWith(good: GoodId, budget: Long): Int {
            val space = (state.luggageCapacity - state.luggageUsed).toLong()
            if (space <= 0 || budget <= 0) return 0
            val price = state.priceOf(good)
            val base = Market.price(state.seed, state.day, state.city, good)
            val rise = base * GameConstants.SATURATION_BP_PER_UNIT / 10_000.0
            val affordable = if (rise <= 0.0) {
                budget / price
            } else {
                ((-price + kotlin.math.sqrt(price.toDouble() * price + 4 * rise * budget)) / (2 * rise))
                    .toLong()
            }
            return minOf(affordable, space).toInt()
        }
    }

    private companion object {
        const val TRACE_ONE = false
        const val RUNS = 300
        const val HORIZON_DAY = 60
        const val FIRST_COLLECTOR_DAY = 11
        const val MAX_STEPS = 20_000
        const val BORROW_WHEN_THIN = 20_000L
        const val START_TRADE_CASH = 8_000L
        val CHECKPOINTS = listOf(10, 20, 30, 45, 60)
    }
}
