package com.purplevarun.trader.engine

enum class GoodId { ONIONS, COTTON, BASMATI, CHAI, MASALA, SILK_SAREES, KESAR, GOLD }

enum class CityId { MUMBAI, DELHI, CALCUTTA, SRINAGAR, BANGALORE, HYDERABAD }

enum class TradeUnit { QUINTAL, BUNDLE_OF_TEN, KILOGRAM, HUNDRED_GRAMS }

/** Whether a base price comes from a cited year-2000 source or is still an estimate to verify. */
enum class PriceBasis { SOURCED, ESTIMATE }

data class Good(
    val id: GoodId,
    val unit: TradeUnit,
    val basePriceRupees: Int,
    val basis: PriceBasis,
    /** How far the price can swing either way on any day, as a fraction of its usual price. */
    val volatility: Double,
)

data class City(
    val id: CityId,
    val latitude: Double,
    val longitude: Double,
)

data class RailLink(val a: CityId, val b: CityId)

data class GameDate(val day: Int, val month: Int, val year: Int)

/**
 * The title a finished run earns, lowest to highest. Display names are Android string
 * resources keyed by these ids; the names are placeholders until the naming pass.
 */
enum class RankId { DIVALA, THELAWALA, DUKANDAAR, SAUDAGAR, SETH, CROREPATI }

/**
 * What the trader hauls their goods in. Display names are Android string resources keyed by
 * these ids. Each bigger transport is a strict upgrade: more slots for more money.
 */
enum class TransportId { JHOLA, TRUNK, TEMPO, TRUCK }

data class Transport(
    val id: TransportId,
    /** How many units of goods it holds. */
    val capacity: Int,
    val priceRupees: Long,
)

/**
 * A local price wobble, in basis points (1,000 = +10%), keyed to one good in one city.
 * Saturation adds small shifts; news adds big ones. Both halve every day on the rails.
 */
data class PriceShift(
    val city: CityId,
    val good: GoodId,
    val basisPoints: Int,
)

/** What a price event reads like in the papers. */
enum class EventHeadline { MONSOON, BUMPER_HARVEST, DIWALI, WEDDING_SEASON }

/**
 * Something that happened to the trader on a day that went wrong (or lucky). The state
 * is already changed by the time the event is shown; it exists only to be narrated.
 */
sealed interface GameEvent {
    /** On the train they lifted some of the cash. The bank's money is never touched. */
    data class Pickpocket(val lostRupees: Long) : GameEvent

    /** The police at a checkpost: a cash bribe, or seized goods when the pocket was empty. */
    data class Checkpost(
        val bribeRupees: Long,
        val seizedGood: GoodId?,
        val seizedUnits: Int,
    ) : GameEvent

    /** The train (or the town) stood still an extra day. The debt kept growing meanwhile. */
    data class Bandh(val extraDays: Int) : GameEvent

    /** News moved a price in one city: basis points (1,000 = +10%, negative = down). */
    data class PriceSwing(
        val city: CityId,
        val good: GoodId,
        val basisPoints: Int,
        val headline: EventHeadline,
    ) : GameEvent
}
