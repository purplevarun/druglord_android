package com.purplevarun.trader.engine

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Today's price of every good in every city, worked out from the run's seed so it never needs storing. */
object Market {

    private val goods: Map<GoodId, Good> = GameConstants.GOODS.associateBy { it.id }

    fun price(seed: Long, day: Int, city: CityId, good: GoodId): Long {
        val spec = goods.getValue(good)
        val cityFactor = GameConstants.CITY_PRICE_FACTORS[city]?.get(good) ?: 1.0
        val swing = (noise(seed, day, city, good) * 2 - 1) * spec.volatility
        return max(1L, (spec.basePriceRupees * cityFactor * (1 + swing)).roundToLong())
    }

    /** Today's price against yesterday's in the same city, as a whole percentage. */
    fun changePercent(seed: Long, day: Int, city: CityId, good: GoodId): Int {
        val today = price(seed, day, city, good)
        val yesterday = price(seed, day - 1, city, good)
        return ((today - yesterday) * 100.0 / yesterday).roundToInt()
    }

    /**
     * A number from 0 up to 1 that looks random but is fixed by its inputs. Names, not enum
     * positions, feed it, so reordering the enums does not change the prices of a saved run.
     */
    private fun noise(seed: Long, day: Int, city: CityId, good: GoodId): Double {
        var x = mix(seed)
        x = mix(x xor day.toLong())
        x = mix(x xor city.name.hashCode().toLong())
        x = mix(x xor good.name.hashCode().toLong())
        return (x ushr 11).toDouble() / (1L shl 53).toDouble()
    }

    /** One step of the SplitMix64 generator. */
    private fun mix(value: Long): Long {
        var z = value + 0x9E3779B97F4A7C15uL.toLong()
        z = (z xor (z ushr 30)) * 0xBF58476D1CE4E5B9uL.toLong()
        z = (z xor (z ushr 27)) * 0x94D049BB133111EBuL.toLong()
        return z xor (z ushr 31)
    }
}
