package com.purplevarun.trader

import android.content.Context
import com.purplevarun.trader.engine.CityId
import com.purplevarun.trader.engine.GameConstants
import com.purplevarun.trader.engine.GoodId
import com.purplevarun.trader.engine.RankId
import com.purplevarun.trader.engine.RunState

/** One finished run that earned a place on the best-runs list, with the rank it earned then. */
data class BestRun(
    val netWorthRupees: Long,
    val runLengthDays: Int,
    val daysPlayed: Int,
    val rank: RankId,
)

/**
 * The run in progress and the best-runs list, kept in SharedPreferences so both survive
 * the app being closed. No libraries: the file format is hand-rolled and versioned.
 */
class RunStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The run last saved, or null when none was saved or the file is not understood. */
    fun loadRun(): RunState? = runCatching {
        if (prefs.getInt(KEY_VERSION, 0) != FORMAT_VERSION) return null
        if (!prefs.getBoolean(KEY_RUN_PRESENT, false)) return null
        RunState(
            seed = prefs.getLong(KEY_RUN_SEED, 0),
            day = prefs.getInt(KEY_RUN_DAY, 1),
            runLengthDays = prefs.getInt(KEY_RUN_LENGTH, GameConstants.DEFAULT_RUN_LENGTH_DAYS),
            city = CityId.valueOf(prefs.getString(KEY_RUN_CITY, null)!!),
            cashRupees = prefs.getLong(KEY_RUN_CASH, 0),
            debtRupees = prefs.getLong(KEY_RUN_DEBT, 0),
            luggageCapacity = prefs.getInt(KEY_RUN_CAPACITY, 0),
            holdings = holdingsFromString(prefs.getString(KEY_RUN_HOLDINGS, "")!!),
        )
    }.getOrNull()

    fun saveRun(run: RunState) {
        prefs.edit()
            .putInt(KEY_VERSION, FORMAT_VERSION)
            .putBoolean(KEY_RUN_PRESENT, true)
            .putLong(KEY_RUN_SEED, run.seed)
            .putInt(KEY_RUN_DAY, run.day)
            .putInt(KEY_RUN_LENGTH, run.runLengthDays)
            .putString(KEY_RUN_CITY, run.city.name)
            .putLong(KEY_RUN_CASH, run.cashRupees)
            .putLong(KEY_RUN_DEBT, run.debtRupees)
            .putInt(KEY_RUN_CAPACITY, run.luggageCapacity)
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
                runLengthDays = prefs.getInt(bestKey(KEY_BEST_LENGTH, i), 0),
                daysPlayed = prefs.getInt(bestKey(KEY_BEST_DAYS, i), 0),
                rank = RankId.valueOf(prefs.getString(bestKey(KEY_BEST_RANK, i), null)!!),
            )
        }
    }

    /** Adds a finished run to the best-runs list, keeping only the highest scorers. */
    fun recordRun(run: RunState) {
        val entry = BestRun(run.netWorthRupees, run.runLengthDays, run.day, run.rank)
        val best = (bestRuns() + entry).sortedByDescending { it.netWorthRupees }.take(MAX_BEST_RUNS)
        prefs.edit().apply {
            putInt(KEY_BEST_COUNT, best.size)
            best.forEachIndexed { i, entry ->
                putLong(bestKey(KEY_BEST_NET, i), entry.netWorthRupees)
                putInt(bestKey(KEY_BEST_LENGTH, i), entry.runLengthDays)
                putInt(bestKey(KEY_BEST_DAYS, i), entry.daysPlayed)
                putString(bestKey(KEY_BEST_RANK, i), entry.rank.name)
            }
            apply()
        }
    }

    private fun bestKey(prefix: String, index: Int) = "$prefix.$index"

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
        private const val FORMAT_VERSION = 1
        private const val KEY_VERSION = "version"
        private const val KEY_RUN_PRESENT = "run.present"
        private const val KEY_RUN_SEED = "run.seed"
        private const val KEY_RUN_DAY = "run.day"
        private const val KEY_RUN_LENGTH = "run.length"
        private const val KEY_RUN_CITY = "run.city"
        private const val KEY_RUN_CASH = "run.cash"
        private const val KEY_RUN_DEBT = "run.debt"
        private const val KEY_RUN_CAPACITY = "run.capacity"
        private const val KEY_RUN_HOLDINGS = "run.holdings"
        private const val KEY_BEST_COUNT = "best.count"
        private const val KEY_BEST_NET = "best.net"
        private const val KEY_BEST_LENGTH = "best.length"
        private const val KEY_BEST_DAYS = "best.days"
        private const val KEY_BEST_RANK = "best.rank"
    }
}
