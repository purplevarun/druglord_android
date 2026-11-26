package com.purplevarun.trader.engine

enum class GoodId { ONIONS, COTTON, BASMATI, CHAI, MASALA, SILK_SAREES, KESAR, GOLD }

enum class CityId { MUMBAI, DELHI, CALCUTTA, CHENNAI, JAIPUR, KOCHI, VARANASI, SRINAGAR }

enum class TradeUnit { QUINTAL, BUNDLE_OF_TEN, KILOGRAM, HUNDRED_GRAMS }

/** Whether a base price comes from a cited year-2000 source or is still an estimate to verify. */
enum class PriceBasis { SOURCED, ESTIMATE }

data class Good(
    val id: GoodId,
    val unit: TradeUnit,
    val basePriceRupees: Int,
    val basis: PriceBasis,
)

data class City(
    val id: CityId,
    val latitude: Double,
    val longitude: Double,
)

data class RailLink(val a: CityId, val b: CityId)

data class GameDate(val day: Int, val month: Int, val year: Int)
