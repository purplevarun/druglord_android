package com.purplevarun.trader.engine

import kotlin.math.abs

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
    /** How many event rolls this run has made; the next roll's luck comes from seed + rngRolls. */
    val rngRolls: Long,
    /** Local price wobbles from news and saturation, still in effect. */
    val priceShifts: List<PriceShift>,
    /** Goods parked in godowns, per city. Only cities with goods stored are listed. */
    val godowns: Map<CityId, Map<GoodId, Int>>,
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

    fun stored(city: CityId, good: GoodId): Int = godowns[city]?.get(good) ?: 0

    fun godownUsed(city: CityId): Int = godowns[city]?.values?.sum() ?: 0

    /** Cities where goods sit, each owing rent per day that passes. */
    val godownsInUse: Int get() = godowns.keys.size

    /** Today's rent across all godowns in use. */
    val godownRentPerDay: Long get() = GameConstants.GODOWN_DAILY_RENT_RUPEES * godownsInUse

    /** The total shift on [good] in this city right now, capped so a price stays sane. */
    fun shiftFor(good: GoodId): Int = shiftForIn(city, good)

    fun shiftForIn(city: CityId, good: GoodId): Int = priceShifts
        .filter { it.city == city && it.good == good }
        .sumOf { it.basisPoints }
        .coerceIn(-GameConstants.PRICE_SHIFT_CAP_BP, GameConstants.PRICE_SHIFT_CAP_BP)

    fun priceOf(good: GoodId): Long = priceOfIn(city, good)

    fun priceOfIn(city: CityId, good: GoodId): Long {
        val base = Market.price(seed, day, city, good)
        return (base * (10_000 + shiftForIn(city, good)) / 10_000).coerceAtLeast(1)
    }

    fun changePercentOf(good: GoodId): Int = Market.changePercent(seed, day, city, good)

    /**
     * Cash, plus savings and everything owned, less debt. Carried goods are worth today's
     * local price; stashed goods are worth the price in the city they are stored in.
     */
    val netWorthRupees: Long
        get() = cashRupees + bankRupees +
            holdings.entries.sumOf { (good, quantity) -> quantity * priceOf(good) } +
            godowns.entries.sumOf { (city, goods) ->
                goods.entries.sumOf { (good, quantity) -> quantity * priceOfIn(city, good) }
            } - debtRupees

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
            rngRolls = 0,
            priceShifts = emptyList(),
            godowns = emptyMap(),
            holdings = emptyMap(),
        )
    }
}
