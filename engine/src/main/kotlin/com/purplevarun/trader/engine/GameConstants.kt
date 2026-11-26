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

    // Travel
    const val DAYS_PER_RAIL_LINK = 1

    /**
     * Base prices at year-2000 rupee levels, per trade unit.
     * SOURCED: cotton (2000-01 support price, RBI and Economic Survey), masala (black pepper,
     * about Rs 215/kg in 1999-2000), gold (Rs 4,300 to 4,800 per 10 g in 2000, RBI).
     * ESTIMATE: no year-2000 source found yet; verify before treating as final.
     */
    val GOODS: List<Good> = listOf(
        Good(GoodId.ONIONS, TradeUnit.QUINTAL, 400, PriceBasis.ESTIMATE),
        Good(GoodId.COTTON, TradeUnit.QUINTAL, 1_825, PriceBasis.SOURCED),
        Good(GoodId.BASMATI, TradeUnit.QUINTAL, 3_000, PriceBasis.ESTIMATE),
        Good(GoodId.CHAI, TradeUnit.QUINTAL, 6_500, PriceBasis.ESTIMATE),
        Good(GoodId.MASALA, TradeUnit.QUINTAL, 21_500, PriceBasis.SOURCED),
        Good(GoodId.SILK_SAREES, TradeUnit.BUNDLE_OF_TEN, 25_000, PriceBasis.ESTIMATE),
        Good(GoodId.KESAR, TradeUnit.KILOGRAM, 40_000, PriceBasis.ESTIMATE),
        Good(GoodId.GOLD, TradeUnit.HUNDRED_GRAMS, 45_000, PriceBasis.SOURCED),
    )

    /** Real coordinates, in degrees. The app projects these onto the map. */
    val CITIES: List<City> = listOf(
        City(CityId.MUMBAI, latitude = 19.1, longitude = 72.9),
        City(CityId.DELHI, latitude = 28.6, longitude = 77.2),
        City(CityId.CALCUTTA, latitude = 22.6, longitude = 88.4),
        City(CityId.CHENNAI, latitude = 13.1, longitude = 80.3),
        City(CityId.JAIPUR, latitude = 26.9, longitude = 75.8),
        City(CityId.KOCHI, latitude = 9.9, longitude = 76.3),
        City(CityId.VARANASI, latitude = 25.3, longitude = 83.0),
        City(CityId.SRINAGAR, latitude = 34.1, longitude = 74.8),
    )

    val RAIL_LINKS: List<RailLink> = listOf(
        RailLink(CityId.SRINAGAR, CityId.DELHI),
        RailLink(CityId.DELHI, CityId.JAIPUR),
        RailLink(CityId.JAIPUR, CityId.MUMBAI),
        RailLink(CityId.DELHI, CityId.VARANASI),
        RailLink(CityId.VARANASI, CityId.CALCUTTA),
        RailLink(CityId.MUMBAI, CityId.CHENNAI),
        RailLink(CityId.CHENNAI, CityId.KOCHI),
        RailLink(CityId.CALCUTTA, CityId.CHENNAI),
        RailLink(CityId.MUMBAI, CityId.KOCHI),
    )
}
