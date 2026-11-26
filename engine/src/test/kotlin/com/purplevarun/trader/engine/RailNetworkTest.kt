package com.purplevarun.trader.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class RailNetworkTest {

    @Test
    fun `neighbouring cities are one day apart`() {
        assertEquals(1, RailNetwork.travelDays(CityId.MUMBAI, CityId.BANGALORE))
    }

    @Test
    fun `a trip with one change takes two days`() {
        assertEquals(2, RailNetwork.travelDays(CityId.BANGALORE, CityId.CALCUTTA))
    }

    @Test
    fun `the route lists every city passed through in order`() {
        assertEquals(
            listOf(CityId.BANGALORE, CityId.HYDERABAD, CityId.CALCUTTA),
            RailNetwork.route(CityId.BANGALORE, CityId.CALCUTTA),
        )
        assertEquals(
            listOf(CityId.BANGALORE, CityId.MUMBAI, CityId.DELHI, CityId.SRINAGAR),
            RailNetwork.route(CityId.BANGALORE, CityId.SRINAGAR),
        )
    }

    @Test
    fun `the far ends of the map are three days apart`() {
        assertEquals(3, RailNetwork.travelDays(CityId.BANGALORE, CityId.SRINAGAR))
    }

    @Test
    fun `a trip takes as long in either direction`() {
        assertEquals(3, RailNetwork.travelDays(CityId.HYDERABAD, CityId.SRINAGAR))
        assertEquals(3, RailNetwork.travelDays(CityId.SRINAGAR, CityId.HYDERABAD))
    }

    @Test
    fun `staying put takes no time`() {
        assertEquals(0, RailNetwork.travelDays(CityId.DELHI, CityId.DELHI))
        assertEquals(listOf(CityId.DELHI), RailNetwork.route(CityId.DELHI, CityId.DELHI))
    }

    @Test
    fun `every city can be reached from every other city`() {
        for (from in CityId.entries) {
            for (to in CityId.entries) {
                val route = RailNetwork.route(from, to)
                assertEquals("route $from to $to starts at the origin", from, route.first())
                assertEquals("route $from to $to ends at the destination", to, route.last())
            }
        }
    }

    @Test
    fun `the map has exactly the six agreed cities`() {
        assertEquals(
            setOf(
                CityId.MUMBAI, CityId.DELHI, CityId.CALCUTTA,
                CityId.SRINAGAR, CityId.BANGALORE, CityId.HYDERABAD,
            ),
            GameConstants.CITIES.map { it.id }.toSet(),
        )
    }
}
