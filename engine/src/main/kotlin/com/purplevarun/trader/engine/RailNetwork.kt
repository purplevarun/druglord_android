package com.purplevarun.trader.engine

object RailNetwork {

    private val neighbours: Map<CityId, List<CityId>> = CityId.entries.associateWith { city ->
        GameConstants.RAIL_LINKS.mapNotNull { link ->
            when (city) {
                link.a -> link.b
                link.b -> link.a
                else -> null
            }
        }
    }

    /** The cities passed through, from [from] to [to] inclusive, using the fewest rail links. */
    fun route(from: CityId, to: CityId): List<CityId> {
        val cameFrom = mutableMapOf<CityId, CityId?>(from to null)
        val queue = ArrayDeque(listOf(from))
        while (queue.isNotEmpty()) {
            val city = queue.removeFirst()
            if (city == to) break
            for (next in neighbours.getValue(city)) {
                if (next !in cameFrom) {
                    cameFrom[next] = city
                    queue.addLast(next)
                }
            }
        }
        check(to in cameFrom) { "No rail route from $from to $to" }
        return generateSequence(to) { cameFrom[it] }.toList().reversed()
    }

    fun travelDays(from: CityId, to: CityId): Int =
        (route(from, to).size - 1) * GameConstants.DAYS_PER_RAIL_LINK
}
