package com.purplevarun.trader.engine

/**
 * Every tunable number and table in the game rules lives here.
 * Display names are not here: they are Android string resources keyed by these ids.
 */
object GameConstants {

    // Calendar: day 1 of a run is this date.
    const val START_YEAR = 2000
    const val START_MONTH = 1
    const val START_DAY_OF_MONTH = 1

    /**
     * What a day of living costs in each city, billed on every move (and per day on the
     * rails at the destination's rate). Game tuning, not history; Srinagar is cheapest.
     */
    val LIVING_COST_RUPEES: Map<CityId, Long> = mapOf(
        CityId.MUMBAI to 120L,
        CityId.DELHI to 100L,
        CityId.CALCUTTA to 80L,
        CityId.SRINAGAR to 60L,
        CityId.BANGALORE to 90L,
        CityId.HYDERABAD to 70L,
    )

    /** Days after a loan begins before the collector comes for the whole debt. */
    const val COLLECTOR_AFTER_DAYS = 10

    /** What the sahukar lends a trader with nothing left: enough to climb back, one shot. */
    const val SURVIVAL_LOAN_RUPEES = 5_000L

    /** The most the sahukar will stake a trader at once, whatever he is asked. */
    const val MAX_LOAN_RUPEES = 50_000L

    /**
     * Final net worth to rank: the first threshold the score reaches or passes.
     * Game tuning, not history; the balance pass will set real cutoffs.
     */
    val RANK_THRESHOLDS: List<Pair<Long, RankId>> = listOf(
        10_000_000L to RankId.CROREPATI,
        1_000_000L to RankId.SETH,
        100_000L to RankId.SAUDAGAR,
        25_000L to RankId.DUKANDAAR,
        0L to RankId.THELAWALA,
        Long.MIN_VALUE to RankId.DIVALA,
    )

    // Travel
    const val DAYS_PER_RAIL_LINK = 1

    // A new run
    val STARTING_CITY = CityId.MUMBAI
    const val STARTING_CASH_RUPEES = 10_000L
    const val STARTING_DEBT_RUPEES = 5_000L
    val STARTING_TRANSPORT = TransportId.JHOLA

    // The facilities, in every city
    /** What the sahukar adds to a debt each day that passes. */
    const val SAHUKAR_DAILY_INTEREST = 0.08
    /** What the bank adds to savings each day that passes. */
    const val BANK_DAILY_INTEREST = 0.01
    /** The amounts the facility sheets offer, one tap at a time. */
    val TRANSACTION_STEPS_RUPEES = listOf(1_000L, 10_000L)

    /**
     * The luggage ladder, cheapest first. Jhola is where every run starts and is not for sale.
     * Game tuning, not history; the balance pass will set real prices and sizes.
     */
    val TRANSPORTS: List<Transport> = listOf(
        Transport(TransportId.JHOLA, capacity = 100, priceRupees = 0),
        Transport(TransportId.TRUNK, capacity = 250, priceRupees = 6_000),
        Transport(TransportId.TEMPO, capacity = 600, priceRupees = 25_000),
        Transport(TransportId.TRUCK, capacity = 1_500, priceRupees = 90_000),
    )

    fun transport(id: TransportId): Transport = TRANSPORTS.first { it.id == id }

    // Random events: the odds are first guesses, tuned in the balance pass
    /** Chance of an event on each day spent on the rails. */
    const val EVENT_CHANCE_PER_TRAVEL_DAY = 0.22
    /** Chance of an event on each non-travel action, while you are in town. */
    const val EVENT_CHANCE_PER_CITY_ACTION = 0.05

    // What the dice can bring, and what it costs
    const val PICKPOCKET_SHARE_MIN = 0.10
    const val PICKPOCKET_SHARE_MAX = 0.30
    const val CHECKPOST_BRIBE_SHARE = 0.15
    const val CHECKPOST_MIN_BRIBE_RUPEES = 500L
    const val CHECKPOST_SEIZE_SHARE = 0.10
    const val BANDH_EXTRA_DAYS = 1

    // Rumours: word on the street that a price will swing within days
    /** The share of rumours that are duds: told, but nothing ever lands. */
    const val RUMOUR_DUD_CHANCE = 0.25
    /** The share of rumours that call a fall rather than a rise. */
    const val RUMOUR_DOWN_CHANCE = 0.3
    /** How many days out a rumour lands, at random between these. */
    const val RUMOUR_DAYS_MIN = 2
    const val RUMOUR_DAYS_MAX = 5
    /** The swing a true rumour lands, up or down, in basis points. */
    const val RUMOUR_BP_MIN = 3_000
    const val RUMOUR_BP_MAX = 9_000

    // Price wobbles, in basis points (10,000 = the price itself)
    /** A spike adds this much at random; a crash takes this much away. */
    const val PRICE_SPIKE_BP_MIN = 4_000
    const val PRICE_SPIKE_BP_MAX = 12_000
    const val PRICE_CRASH_BP_MIN = 2_000
    const val PRICE_CRASH_BP_MAX = 6_000
    /** Each unit bought or sold nudges the local price up or down by this much. */
    const val SATURATION_BP_PER_UNIT = 10
    /** Saturation and news together never move a price past this far. */
    const val PRICE_SHIFT_CAP_BP = 6_000
    /** What is left of a shift each day, and the stub too small to keep. */
    const val PRICE_SHIFT_DAILY_DECAY = 0.5
    const val PRICE_SHIFT_MIN_BP = 50

    // The godowns: rent while goods sit, and how much one holds
    /** Rent per day for each city where goods are stored, billed on days that pass. */
    const val GODOWN_DAILY_RENT_RUPEES = 50L
    /** How many units one city's godown holds, whatever the goods. */
    const val GODOWN_CAPACITY_UNITS = 500
    /** The one-tap unit amounts the godown sheet offers, before the All chip. */
    val GODOWN_STEPS_UNITS = listOf(1, 10)

    /**
     * Base prices at year-2000 rupee levels, per trade unit.
     * SOURCED: cotton (2000-01 support price, RBI and Economic Survey), masala (black pepper,
     * about Rs 215/kg in 1999-2000), gold (Rs 4,300 to 4,800 per 10 g in 2000, RBI), chai
     * (Calcutta auction averages ran about Rs 60 to 103/kg through 2000, Tea Board figures).
     * ESTIMATE: no year-2000 source found; plausible but unverified.
     * Volatility is game tuning, not history: onions were notoriously jumpy, gold barely moved.
     */
    val GOODS: List<Good> = listOf(
        Good(GoodId.ONIONS, TradeUnit.QUINTAL, 400, PriceBasis.ESTIMATE, volatility = 0.45),
        Good(GoodId.COTTON, TradeUnit.QUINTAL, 1_825, PriceBasis.SOURCED, volatility = 0.15),
        Good(GoodId.BASMATI, TradeUnit.QUINTAL, 3_000, PriceBasis.ESTIMATE, volatility = 0.15),
        Good(GoodId.CHAI, TradeUnit.QUINTAL, 6_500, PriceBasis.SOURCED, volatility = 0.2),
        Good(GoodId.MASALA, TradeUnit.QUINTAL, 21_500, PriceBasis.SOURCED, volatility = 0.25),
        Good(GoodId.SILK_SAREES, TradeUnit.BUNDLE_OF_TEN, 25_000, PriceBasis.ESTIMATE, volatility = 0.2),
        Good(GoodId.KESAR, TradeUnit.KILOGRAM, 40_000, PriceBasis.ESTIMATE, volatility = 0.3),
        Good(GoodId.GOLD, TradeUnit.HUNDRED_GRAMS, 45_000, PriceBasis.SOURCED, volatility = 0.06),
    )

    /**
     * How each city's usual price compares with the base price. Below 1 is cheap there, above 1
     * is dear; a good not listed for a city sells at its base price. Game tuning loosely based on
     * where things come from (saffron from Kashmir, tea through Calcutta, silk from Karnataka).
     */
    val CITY_PRICE_FACTORS: Map<CityId, Map<GoodId, Double>> = mapOf(
        CityId.MUMBAI to mapOf(GoodId.ONIONS to 0.75, GoodId.GOLD to 0.95, GoodId.KESAR to 1.2, GoodId.CHAI to 1.1),
        CityId.DELHI to mapOf(GoodId.BASMATI to 0.75, GoodId.SILK_SAREES to 1.2, GoodId.MASALA to 1.15, GoodId.GOLD to 1.05),
        CityId.CALCUTTA to mapOf(GoodId.CHAI to 0.7, GoodId.ONIONS to 1.2, GoodId.COTTON to 1.15, GoodId.KESAR to 1.1),
        CityId.SRINAGAR to mapOf(
            GoodId.KESAR to 0.7, GoodId.ONIONS to 1.25, GoodId.MASALA to 1.2, GoodId.COTTON to 1.15, GoodId.CHAI to 1.15,
        ),
        CityId.BANGALORE to mapOf(
            GoodId.SILK_SAREES to 0.75, GoodId.MASALA to 0.8, GoodId.BASMATI to 1.15, GoodId.KESAR to 1.15,
        ),
        CityId.HYDERABAD to mapOf(GoodId.COTTON to 0.75, GoodId.BASMATI to 1.15, GoodId.SILK_SAREES to 1.1),
    )

    /** The good a city prices lowest — its speciality, the thing that comes cheap here. */
    fun specialityOf(city: CityId): GoodId? =
        CITY_PRICE_FACTORS[city]?.minByOrNull { (_, factor) -> factor }?.key

    /** Real coordinates, in degrees. The app projects these onto the map. */
    val CITIES: List<City> = listOf(
        City(CityId.MUMBAI, latitude = 19.1, longitude = 72.9),
        City(CityId.DELHI, latitude = 28.6, longitude = 77.2),
        City(CityId.CALCUTTA, latitude = 22.6, longitude = 88.4),
        City(CityId.SRINAGAR, latitude = 34.1, longitude = 74.8),
        City(CityId.BANGALORE, latitude = 13.0, longitude = 77.6),
        City(CityId.HYDERABAD, latitude = 17.4, longitude = 78.5),
    )

    val RAIL_LINKS: List<RailLink> = listOf(
        RailLink(CityId.SRINAGAR, CityId.DELHI),
        RailLink(CityId.DELHI, CityId.MUMBAI),
        RailLink(CityId.DELHI, CityId.CALCUTTA),
        RailLink(CityId.MUMBAI, CityId.HYDERABAD),
        RailLink(CityId.MUMBAI, CityId.BANGALORE),
        RailLink(CityId.HYDERABAD, CityId.BANGALORE),
        RailLink(CityId.HYDERABAD, CityId.CALCUTTA),
    )
}
