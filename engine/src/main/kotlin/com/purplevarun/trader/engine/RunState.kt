package com.purplevarun.trader.engine

import kotlin.math.abs

/** Everything about a run in progress. Prices are not stored: they follow from the seed. */
data class RunState(
    val seed: Long,
    val day: Int,
    val city: CityId,
    val cashRupees: Long,
    val debtRupees: Long,
    val bankRupees: Long,
    val transport: TransportId,
    /** The day the collector comes for the debt, or 0 while nothing is owed. */
    val debtDueDay: Int,
    /** How many event rolls this run has made; the next roll's luck comes from seed + rngRolls. */
    val rngRolls: Long,
    /** Local price wobbles from news and saturation, still in effect. */
    val priceShifts: List<PriceShift>,
    /** Goods parked in godowns, per city. Only cities with goods stored are listed. */
    val godowns: Map<CityId, Map<GoodId, Int>>,
    /** Only goods actually carried are listed, each with a quantity of at least one. */
    val holdings: Map<GoodId, Int>,
    /** Word on the street, still waiting for its day. */
    val rumours: List<Rumour>,
) {
    val luggageCapacity: Int get() = GameConstants.transport(transport).capacity

    val luggageUsed: Int get() = holdings.values.sum()

    fun held(good: GoodId): Int = holdings[good] ?: 0

    fun stored(city: CityId, good: GoodId): Int = godowns[city]?.get(good) ?: 0

    fun godownUsed(city: CityId): Int = godowns[city]?.values?.sum() ?: 0

    /** Cities where goods sit, each owing rent per day that passes. */
    val godownsInUse: Int get() = godowns.keys.size

    /** Today's rent across all godowns in use. */
    val godownRentPerDay: Long get() = GameConstants.GODOWN_DAILY_RENT_RUPEES * godownsInUse

    /** What a day of living costs where you are now. */
    val livingCostPerDay: Long get() = GameConstants.LIVING_COST_RUPEES.getValue(city)

    /** The collector has found you: the deadline passed while debt stands. */
    val collectorDue: Boolean get() = debtRupees > 0 && debtDueDay > 0 && day >= debtDueDay

    /** Nothing left at all: no money anywhere and nothing to sell. */
    val isBroke: Boolean
        get() = cashRupees <= 0 && bankRupees <= 0 && holdings.isEmpty() && godowns.isEmpty()

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

    /** The goods that moved most against yesterday here, biggest first, zero moves left out. */
    fun movers(): List<Pair<GoodId, Int>> = GoodId.entries
        .map { it to changePercentOf(it) }
        .filter { (_, percent) -> percent != 0 }
        .sortedByDescending { (_, percent) -> abs(percent) }

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

    /**
     * The most of [good] that cash and luggage space allow right now. The order itself moves
     * the price: unit q costs what it costs at the shifted quote, so the answer solves
     * q·(price + q·base·bp) ≤ cash — not a plain division, which would overshoot.
     */
    fun maxBuyable(good: GoodId): Int {
        val space = (luggageCapacity - luggageUsed).toLong().coerceAtLeast(0)
        if (space == 0L || cashRupees <= 0) return 0
        val price = priceOf(good)
        val base = Market.price(seed, day, city, good)
        // Cost of q units bought at once: q·price + q·(shifted price per extra unit),
        // which is q·price + q²·base·SATURATION_BP/10000. Solve the quadratic for q.
        val rise = base * GameConstants.SATURATION_BP_PER_UNIT / 10_000.0
        val affordable = if (rise <= 0.0) {
            cashRupees / price
        } else {
            ((-price + kotlin.math.sqrt(price.toDouble() * price + 4 * rise * cashRupees)) / (2 * rise))
                .toLong()
        }
        return minOf(affordable, space).toInt()
    }

    companion object {
        fun newRun(seed: Long) = RunState(
            seed = seed,
            day = 1,
            city = GameConstants.STARTING_CITY,
            cashRupees = GameConstants.STARTING_CASH_RUPEES,
            debtRupees = GameConstants.STARTING_DEBT_RUPEES,
            bankRupees = 0,
            transport = GameConstants.STARTING_TRANSPORT,
            // The starting debt is the collector's first deadline: due on day 1 + 10.
            debtDueDay = 1 + GameConstants.COLLECTOR_AFTER_DAYS,
            rngRolls = 0,
            priceShifts = emptyList(),
            godowns = emptyMap(),
            holdings = emptyMap(),
            rumours = emptyList(),
        )
    }
}
