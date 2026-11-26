package com.purplevarun.trader.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class RailNetworkTest {

    @Test
    fun `neighbouring cities are one day apart`() {
        assertEquals(1, RailNetwork.travelDays(CityId.MUMBAI, CityId.CHENNAI))
    }

    @Test
    fun `a trip with one change takes two days`() {
        assertEquals(2, RailNetwork.travelDays(CityId.MUMBAI, CityId.CALCUTTA))
    }

    @Test
    fun `the route lists every city passed through in order`() {
        assertEquals(
            listOf(CityId.MUMBAI, CityId.CHENNAI, CityId.CALCUTTA),
            RailNetwork.route(CityId.MUMBAI, CityId.CALCUTTA),
        )
    }

    @Test
    fun `the far corners of the map are four days apart`() {
        assertEquals(4, RailNetwork.travelDays(CityId.KOCHI, CityId.SRINAGAR))
    }

    @Test
    fun `a trip takes as long in either direction`() {
        assertEquals(3, RailNetwork.travelDays(CityId.MUMBAI, CityId.SRINAGAR))
        assertEquals(3, RailNetwork.travelDays(CityId.SRINAGAR, CityId.MUMBAI))
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
}
