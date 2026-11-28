package com.purplevarun.trader.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketTest {

    private fun pricesOn(seed: Long, day: Int, city: CityId) =
        GoodId.entries.map { Market.price(seed, day, city, it) }

    @Test
    fun `the same seed always gives the same prices`() {
        assertEquals(pricesOn(9, 4, CityId.DELHI), pricesOn(9, 4, CityId.DELHI))
        assertEquals(
            GoodId.entries.map { RunState.newRun(seed = 9).priceOf(it) },
            GoodId.entries.map { RunState.newRun(seed = 9).priceOf(it) },
        )
    }

    @Test
    fun `different seeds give different markets`() {
        assertNotEquals(pricesOn(1, 1, CityId.MUMBAI), pricesOn(2, 1, CityId.MUMBAI))
    }

    @Test
    fun `prices move from one day to the next`() {
        assertNotEquals(pricesOn(1, 1, CityId.MUMBAI), pricesOn(1, 2, CityId.MUMBAI))
    }

    @Test
    fun `gold is steady and onions swing wildly`() {
        val days = 1..200
        val gold = days.map { Market.price(3, it, CityId.DELHI, GoodId.GOLD) }
        val onions = days.map { Market.price(3, it, CityId.DELHI, GoodId.ONIONS) }
        assertTrue("gold should move less than 15% top to bottom", gold.max() < gold.min() * 1.15)
        assertTrue("onions should move more than 80% top to bottom", onions.max() > onions.min() * 1.8)
    }

    @Test
    fun `each city's speciality is cheapest there`() {
        val specialities = mapOf(
            GoodId.KESAR to CityId.SRINAGAR,
            GoodId.CHAI to CityId.CALCUTTA,
            GoodId.SILK_SAREES to CityId.BANGALORE,
            GoodId.BASMATI to CityId.DELHI,
            GoodId.COTTON to CityId.HYDERABAD,
            GoodId.ONIONS to CityId.MUMBAI,
        )
        for ((good, home) in specialities) {
            val cheapest = CityId.entries.minBy { city -> (1..300).map { Market.price(5, it, city, good) }.average() }
            assertEquals("$good should be cheapest in $home", home, cheapest)
        }
    }

    @Test
    fun `the daily change shows which way the price moved since yesterday`() {
        for (day in 2..60) {
            for (good in GoodId.entries) {
                val today = Market.price(11, day, CityId.CALCUTTA, good)
                val yesterday = Market.price(11, day - 1, CityId.CALCUTTA, good)
                val change = Market.changePercent(11, day, CityId.CALCUTTA, good)
                if (today > yesterday) assertTrue("$good rose on day $day but shows $change%", change >= 0)
                if (today < yesterday) assertTrue("$good fell on day $day but shows $change%", change <= 0)
                if (today == yesterday) assertEquals(0, change)
            }
        }
    }

    @Test
    fun `a city's speciality is the good its factor prices lowest`() {
        assertEquals(GoodId.KESAR, GameConstants.specialityOf(CityId.SRINAGAR))
        assertEquals(GoodId.ONIONS, GameConstants.specialityOf(CityId.MUMBAI))
        for (city in CityId.entries) {
            val speciality = GameConstants.specialityOf(city)!!
            val factors = GameConstants.CITY_PRICE_FACTORS.getValue(city)
            assertEquals(factors.minByOrNull { (_, factor) -> factor }!!.key, speciality)
        }
    }

    @Test
    fun `the movers list ranks the day's biggest swings`() {
        val state = RunState.newRun(seed = 7)
        val movers = state.movers()
        assertTrue(movers.isNotEmpty())
        assertTrue(movers.none { (_, percent) -> percent == 0 })
        assertEquals(
            movers.map { kotlin.math.abs(it.second) },
            movers.map { kotlin.math.abs(it.second) }.sortedDescending(),
        )
        assertEquals(
            GoodId.entries.count { state.changePercentOf(it) != 0 },
            movers.size,
        )
    }
}
