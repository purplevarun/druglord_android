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

    // Run length
    val RUN_LENGTH_OPTIONS_DAYS = listOf(30, 60, 90)
    const val DEFAULT_RUN_LENGTH_DAYS = 60

    /** A run with this many days never ends on its own: the trader retires when they choose. */
    const val ENDLESS_RUN_LENGTH_DAYS = Int.MAX_VALUE

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
    const val LUGGAGE_CAPACITY = 100

    /**
     * Base prices at year-2000 rupee levels, per trade unit.
     * SOURCED: cotton (2000-01 support price, RBI and Economic Survey), masala (black pepper,
     * about Rs 215/kg in 1999-2000), gold (Rs 4,300 to 4,800 per 10 g in 2000, RBI).
     * ESTIMATE: no year-2000 source found yet; verify before treating as final.
     * Volatility is game tuning, not history: onions were notoriously jumpy, gold barely moved.
     */
    val GOODS: List<Good> = listOf(
        Good(GoodId.ONIONS, TradeUnit.QUINTAL, 400, PriceBasis.ESTIMATE, volatility = 0.45),
        Good(GoodId.COTTON, TradeUnit.QUINTAL, 1_825, PriceBasis.SOURCED, volatility = 0.15),
        Good(GoodId.BASMATI, TradeUnit.QUINTAL, 3_000, PriceBasis.ESTIMATE, volatility = 0.15),
        Good(GoodId.CHAI, TradeUnit.QUINTAL, 6_500, PriceBasis.ESTIMATE, volatility = 0.2),
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
        CityId.MUMBAI to mapOf(GoodId.ONIONS to 0.7, GoodId.GOLD to 0.95, GoodId.KESAR to 1.3, GoodId.CHAI to 1.15),
        CityId.DELHI to mapOf(GoodId.BASMATI to 0.7, GoodId.SILK_SAREES to 1.3, GoodId.MASALA to 1.2, GoodId.GOLD to 1.05),
        CityId.CALCUTTA to mapOf(GoodId.CHAI to 0.6, GoodId.ONIONS to 1.3, GoodId.COTTON to 1.2, GoodId.KESAR to 1.15),
        CityId.SRINAGAR to mapOf(
            GoodId.KESAR to 0.6, GoodId.ONIONS to 1.4, GoodId.MASALA to 1.35, GoodId.COTTON to 1.25, GoodId.CHAI to 1.2,
        ),
        CityId.BANGALORE to mapOf(
            GoodId.SILK_SAREES to 0.65, GoodId.MASALA to 0.75, GoodId.BASMATI to 1.25, GoodId.KESAR to 1.2,
        ),
        CityId.HYDERABAD to mapOf(GoodId.COTTON to 0.7, GoodId.BASMATI to 1.2, GoodId.SILK_SAREES to 1.15),
    )

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
