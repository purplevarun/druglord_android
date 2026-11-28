package com.purplevarun.trader

import android.content.Context
import com.purplevarun.trader.engine.CityId
import com.purplevarun.trader.engine.GameConstants
import com.purplevarun.trader.engine.GoodId
import com.purplevarun.trader.engine.PriceShift
import com.purplevarun.trader.engine.RankId
import com.purplevarun.trader.engine.RunState
import com.purplevarun.trader.engine.TransportId

/** One finished run that earned a place on the best-runs list, with the rank it earned then. */
data class BestRun(
    val netWorthRupees: Long,
    val daysPlayed: Int,
    val rank: RankId,
)

/**
 * The run in progress and the best-runs list, kept in SharedPreferences so both survive
 * the app being closed. No libraries: the file format is hand-rolled and versioned.
 * Format 1 predates the facilities: a run saved then loads with no savings and a jhola.
 * Format 2 predates the dice and the news: a run saved then loads untouched by either.
 * Format 3 predates the godowns: a run saved then loads with nothing stored.
 * Format 4 predates the survival loop: a run saved then is endless now, and any debt it
 *   owes gets a fresh ten-day clock before the collector comes.
 */
class RunStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The run last saved, or null when none was saved or the file is not understood. */
    fun loadRun(): RunState? = runCatching {
        val version = prefs.getInt(KEY_VERSION, 0)
        if (version < 1 || version > FORMAT_VERSION) return null
        if (!prefs.getBoolean(KEY_RUN_PRESENT, false)) return null
        val day = prefs.getInt(KEY_RUN_DAY, 1)
        val debt = prefs.getLong(KEY_RUN_DEBT, 0)
        RunState(
            seed = prefs.getLong(KEY_RUN_SEED, 0),
            day = day,
            city = CityId.valueOf(prefs.getString(KEY_RUN_CITY, null)!!),
            cashRupees = prefs.getLong(KEY_RUN_CASH, 0),
            debtRupees = debt,
            debtDueDay = when {
                version >= 5 -> prefs.getInt(KEY_RUN_DUE_DAY, 0)
                debt > 0 -> day + GameConstants.COLLECTOR_AFTER_DAYS
                else -> 0
            },
            bankRupees = prefs.getLong(KEY_RUN_BANK, 0),
            transport = prefs.getString(KEY_RUN_TRANSPORT, null)
                ?.let { TransportId.valueOf(it) }
                ?: GameConstants.STARTING_TRANSPORT,
            rngRolls = prefs.getLong(KEY_RUN_RNG, 0),
            priceShifts = shiftsFromString(prefs.getString(KEY_RUN_SHIFTS, "")!!),
            godowns = godownsFromString(prefs.getString(KEY_RUN_GODOWNS, "")!!),
            holdings = holdingsFromString(prefs.getString(KEY_RUN_HOLDINGS, "")!!),
        )
    }.getOrNull()

    fun saveRun(run: RunState) {
        prefs.edit()
            .putInt(KEY_VERSION, FORMAT_VERSION)
            .putBoolean(KEY_RUN_PRESENT, true)
            .putLong(KEY_RUN_SEED, run.seed)
            .putInt(KEY_RUN_DAY, run.day)
            .putString(KEY_RUN_CITY, run.city.name)
            .putLong(KEY_RUN_CASH, run.cashRupees)
            .putLong(KEY_RUN_DEBT, run.debtRupees)
            .putInt(KEY_RUN_DUE_DAY, run.debtDueDay)
            .putLong(KEY_RUN_BANK, run.bankRupees)
            .putString(KEY_RUN_TRANSPORT, run.transport.name)
            .putLong(KEY_RUN_RNG, run.rngRolls)
            .putString(KEY_RUN_SHIFTS, shiftsToString(run.priceShifts))
            .putString(KEY_RUN_GODOWNS, godownsToString(run.godowns))
            .putString(KEY_RUN_HOLDINGS, holdingsToString(run.holdings))
            .apply()
    }

    fun clearRun() {
        prefs.edit().putBoolean(KEY_RUN_PRESENT, false).apply()
    }

    /** The best finished runs so far, highest net worth first, at most [MAX_BEST_RUNS]. */
    fun bestRuns(): List<BestRun> {
        val count = prefs.getInt(KEY_BEST_COUNT, 0)
        return (0 until count).map { i ->
            BestRun(
                netWorthRupees = prefs.getLong(bestKey(KEY_BEST_NET, i), 0),
                daysPlayed = prefs.getInt(bestKey(KEY_BEST_DAYS, i), 0),
                rank = RankId.valueOf(prefs.getString(bestKey(KEY_BEST_RANK, i), null)!!),
            )
        }
    }

    /** Adds a finished run to the best-runs list, keeping only the highest scorers. */
    fun recordRun(run: RunState) {
        val entry = BestRun(run.netWorthRupees, run.day, run.rank)
        val best = (bestRuns() + entry).sortedByDescending { it.netWorthRupees }.take(MAX_BEST_RUNS)
        prefs.edit().apply {
            putInt(KEY_BEST_COUNT, best.size)
            best.forEachIndexed { i, entry ->
                putLong(bestKey(KEY_BEST_NET, i), entry.netWorthRupees)
                putInt(bestKey(KEY_BEST_DAYS, i), entry.daysPlayed)
                putString(bestKey(KEY_BEST_RANK, i), entry.rank.name)
            }
            apply()
        }
    }

    private fun bestKey(prefix: String, index: Int) = "$prefix.$index"

    private fun shiftsToString(shifts: List<PriceShift>): String =
        shifts.joinToString(",") { "${it.city.name}:${it.good.name}:${it.basisPoints}" }

    private fun shiftsFromString(saved: String): List<PriceShift> =
        saved.split(",")
            .filter { it.isNotEmpty() }
            .map { entry ->
                val (city, good, basisPoints) = entry.split(":")
                PriceShift(CityId.valueOf(city), GoodId.valueOf(good), basisPoints.toInt())
            }

    /** "DELHI>KESAR:2,ONIONS:3;MUMBAI>GOLD:1" — cities split by ';', their goods the usual way. */
    private fun godownsToString(godowns: Map<CityId, Map<GoodId, Int>>): String =
        godowns.entries.joinToString(";") { (city, goods) ->
            "${city.name}>" + goods.entries.joinToString(",") { (good, quantity) -> "${good.name}:$quantity" }
        }

    private fun godownsFromString(saved: String): Map<CityId, Map<GoodId, Int>> =
        saved.split(";")
            .filter { it.isNotEmpty() }
            .associate { entry ->
                val (city, goods) = entry.split(">")
                CityId.valueOf(city) to holdingsFromString(goods)
            }

    private fun holdingsToString(holdings: Map<GoodId, Int>): String =
        holdings.entries.joinToString(",") { (good, quantity) -> "${good.name}:$quantity" }

    private fun holdingsFromString(saved: String): Map<GoodId, Int> =
        saved.split(",")
            .filter { it.isNotEmpty() }
            .associate { entry ->
                val (good, quantity) = entry.split(":")
                GoodId.valueOf(good) to quantity.toInt()
            }

    companion object {
        const val MAX_BEST_RUNS = 5
        private const val PREFS_NAME = "runs"
        private const val FORMAT_VERSION = 5
        private const val KEY_VERSION = "version"
        private const val KEY_RUN_PRESENT = "run.present"
        private const val KEY_RUN_SEED = "run.seed"
        private const val KEY_RUN_DAY = "run.day"
        private const val KEY_RUN_CITY = "run.city"
        private const val KEY_RUN_CASH = "run.cash"
        private const val KEY_RUN_DEBT = "run.debt"
        private const val KEY_RUN_DUE_DAY = "run.dueDay"
        private const val KEY_RUN_BANK = "run.bank"
        private const val KEY_RUN_TRANSPORT = "run.transport"
        private const val KEY_RUN_RNG = "run.rng"
        private const val KEY_RUN_SHIFTS = "run.shifts"
        private const val KEY_RUN_GODOWNS = "run.godowns"
        private const val KEY_RUN_HOLDINGS = "run.holdings"
        private const val KEY_BEST_COUNT = "best.count"
        private const val KEY_BEST_NET = "best.net"
        private const val KEY_BEST_DAYS = "best.days"
        private const val KEY_BEST_RANK = "best.rank"
    }
}
