package com.purplevarun.trader

import androidx.test.core.app.ApplicationProvider
import com.purplevarun.trader.engine.CityId
import com.purplevarun.trader.engine.GameConstants
import com.purplevarun.trader.engine.GoodId
import com.purplevarun.trader.engine.PriceShift
import com.purplevarun.trader.engine.RunState
import com.purplevarun.trader.engine.TransportId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The run and the best-runs list, kept between sessions in SharedPreferences. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RunStoreTest {

    private val store = RunStore(ApplicationProvider.getApplicationContext())

    private val run = RunState(
        seed = 42,
        day = 12,
        runLengthDays = 90,
        city = CityId.DELHI,
        cashRupees = 42_000,
        debtRupees = 5_000,
        bankRupees = 8_000,
        transport = TransportId.TRUNK,
        rngRolls = 7,
        priceShifts = listOf(PriceShift(CityId.MUMBAI, GoodId.ONIONS, 2_500)),
        godowns = mapOf(CityId.DELHI to mapOf(GoodId.KESAR to 2)),
        holdings = mapOf(GoodId.ONIONS to 3, GoodId.GOLD to 1),
    )

    @Test
    fun `with nothing saved there is no run and no best runs`() {
        assertNull(store.loadRun())
        assertTrue(store.bestRuns().isEmpty())
    }

    @Test
    fun `a saved run comes back whole, even in a new store`() {
        store.saveRun(run)

        assertEquals(run, RunStore(ApplicationProvider.getApplicationContext()).loadRun())
    }

    @Test
    fun `a run saved before bank and transport existed still loads, with none of either`() {
        // What format version 1 wrote: no bank, no transport.
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getSharedPreferences("runs", 0)
            .edit()
            .putInt("version", 1)
            .putBoolean("run.present", true)
            .putLong("run.seed", 42)
            .putInt("run.day", 12)
            .putInt("run.length", 90)
            .putString("run.city", "DELHI")
            .putLong("run.cash", 42_000)
            .putLong("run.debt", 5_000)
            .putInt("run.capacity", 100)
            .putString("run.holdings", "ONIONS:3,GOLD:1")
            .commit()

        val loaded = store.loadRun()!!

        assertEquals(0L, loaded.bankRupees)
        assertEquals(TransportId.JHOLA, loaded.transport)
        assertEquals(42_000L, loaded.cashRupees)
        assertEquals(3, loaded.held(GoodId.ONIONS))
    }

    @Test
    fun `a run saved before the dice and the news still loads, untouched by either`() {
        // What format version 2 wrote: bank and transport, no rolls and no price shifts.
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getSharedPreferences("runs", 0)
            .edit()
            .putInt("version", 2)
            .putBoolean("run.present", true)
            .putLong("run.seed", 42)
            .putInt("run.day", 12)
            .putInt("run.length", 90)
            .putString("run.city", "DELHI")
            .putLong("run.cash", 42_000)
            .putLong("run.debt", 5_000)
            .putLong("run.bank", 8_000)
            .putString("run.transport", "TRUNK")
            .putString("run.holdings", "ONIONS:3,GOLD:1")
            .commit()

        val loaded = store.loadRun()!!

        assertEquals(0L, loaded.rngRolls)
        assertTrue(loaded.priceShifts.isEmpty())
        assertEquals(8_000L, loaded.bankRupees)
        assertEquals(TransportId.TRUNK, loaded.transport)
    }

    @Test
    fun `a run saved before the godowns still loads, with nothing stored`() {
        // What format version 3 wrote: dice and news, no godowns.
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getSharedPreferences("runs", 0)
            .edit()
            .putInt("version", 3)
            .putBoolean("run.present", true)
            .putLong("run.seed", 42)
            .putInt("run.day", 12)
            .putInt("run.length", 90)
            .putString("run.city", "DELHI")
            .putLong("run.cash", 42_000)
            .putLong("run.debt", 5_000)
            .putLong("run.bank", 8_000)
            .putString("run.transport", "TRUNK")
            .putLong("run.rng", 7)
            .putString("run.shifts", "MUMBAI:ONIONS:2500")
            .putString("run.holdings", "ONIONS:3,GOLD:1")
            .commit()

        val loaded = store.loadRun()!!

        assertTrue(loaded.godowns.isEmpty())
        assertEquals(7L, loaded.rngRolls)
        assertEquals(2_500, loaded.shiftForIn(CityId.MUMBAI, GoodId.ONIONS))
    }

    @Test
    fun `clearing the run removes it but keeps the best runs`() {
        store.saveRun(run)
        store.recordRun(run)

        store.clearRun()

        assertNull(store.loadRun())
        assertEquals(1, store.bestRuns().size)
    }

    @Test
    fun `finished runs are listed richest first and the list keeps only the top few`() {
        val scores = listOf(5_000L, 900_000L, 50_000L, 2_000_000L, 500L, 70_000L, 11_000_000L)
        for (score in scores) {
            store.recordRun(
                run.copy(
                    cashRupees = score + run.debtRupees,
                    bankRupees = 0,
                    godowns = emptyMap(),
                    holdings = emptyMap(),
                ),
            )
        }

        val best = store.bestRuns()
        assertEquals(RunStore.MAX_BEST_RUNS, best.size)
        assertEquals(listOf(11_000_000L, 2_000_000L, 900_000L, 70_000L, 50_000L), best.map { it.netWorthRupees })
    }

    @Test
    fun `a recorded run remembers its length and how far it got`() {
        val endlessDay400 = run.copy(runLengthDays = GameConstants.ENDLESS_RUN_LENGTH_DAYS, day = 400)
        store.recordRun(endlessDay400)

        val best = store.bestRuns().single()
        assertEquals(GameConstants.ENDLESS_RUN_LENGTH_DAYS, best.runLengthDays)
        assertEquals(400, best.daysPlayed)
    }
}
