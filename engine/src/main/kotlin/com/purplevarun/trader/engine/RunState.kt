package com.purplevarun.trader.engine

/** Everything about a run in progress. Prices are not stored: they follow from the seed. */
data class RunState(
    val seed: Long,
    val day: Int,
    val runLengthDays: Int,
    val city: CityId,
    val cashRupees: Long,
    val debtRupees: Long,
    val bankRupees: Long,
    val transport: TransportId,
    /** Only goods actually carried are listed, each with a quantity of at least one. */
    val holdings: Map<GoodId, Int>,
) {
    /** Days still to come after today. Always a long way off in an endless run. */
    val daysLeft: Int get() = runLengthDays - day

    val isEndless: Boolean get() = runLengthDays == GameConstants.ENDLESS_RUN_LENGTH_DAYS

    /** True on the last day of a finite run: trading still works, and the run can be finished. */
    val isOnLastDay: Boolean get() = !isEndless && daysLeft <= 0

    val luggageCapacity: Int get() = GameConstants.transport(transport).capacity

    val luggageUsed: Int get() = holdings.values.sum()

    fun held(good: GoodId): Int = holdings[good] ?: 0

    fun priceOf(good: GoodId): Long = Market.price(seed, day, city, good)

    fun changePercentOf(good: GoodId): Int = Market.changePercent(seed, day, city, good)

    /** Cash, plus savings and everything carried at today's local price, less debt. */
    val netWorthRupees: Long
        get() = cashRupees + bankRupees + holdings.entries.sumOf { (good, quantity) -> quantity * priceOf(good) } - debtRupees

    /** The title this run earns if it ended now. */
    val rank: RankId
        get() = GameConstants.RANK_THRESHOLDS.first { netWorthRupees >= it.first }.second

    /** The most of [good] that cash and luggage space allow right now. */
    fun maxBuyable(good: GoodId): Int {
        val affordable = cashRupees / priceOf(good)
        val space = (luggageCapacity - luggageUsed).toLong()
        return minOf(affordable, space).coerceAtLeast(0).toInt()
    }

    companion object {
        fun newRun(seed: Long, runLengthDays: Int = GameConstants.DEFAULT_RUN_LENGTH_DAYS) = RunState(
            seed = seed,
            day = 1,
            runLengthDays = runLengthDays,
            city = GameConstants.STARTING_CITY,
            cashRupees = GameConstants.STARTING_CASH_RUPEES,
            debtRupees = GameConstants.STARTING_DEBT_RUPEES,
            bankRupees = 0,
            transport = GameConstants.STARTING_TRANSPORT,
            holdings = emptyMap(),
        )
    }
}
