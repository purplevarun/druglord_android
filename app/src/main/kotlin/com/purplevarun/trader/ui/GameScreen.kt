package com.purplevarun.trader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.purplevarun.trader.BestRun
import com.purplevarun.trader.RunStore
import com.purplevarun.trader.engine.Action
import com.purplevarun.trader.engine.CityId
import com.purplevarun.trader.engine.Game
import com.purplevarun.trader.engine.GoodId
import com.purplevarun.trader.engine.Outcome
import com.purplevarun.trader.engine.RailNetwork
import com.purplevarun.trader.engine.RunState

/** Where the game is: picking a new run, mid-run, or looking at how a run ended. */
sealed interface Phase {
    data object NewRun : Phase
    data class Playing(val run: RunState, val overlay: Overlay) : Phase
    data class Ended(val run: RunState) : Phase
}

/** What is shown over the map, if anything. */
sealed interface Overlay {
    data object None : Overlay
    data object Market : Overlay
    data object EndRun : Overlay
    data class Travel(val to: CityId) : Overlay
}

/**
 * The game. Holds where the run is and what is on screen, keeps both through rotation and
 * through Android stopping the app in the background, and writes the run to [store] as it
 * changes so closing the app loses nothing. Every move goes through the engine.
 */
@Composable
fun GameScreen(store: RunStore, newRun: (Int) -> RunState) {
    var phase by rememberSaveable(stateSaver = PhaseSaver) {
        mutableStateOf(store.loadRun()?.let { Phase.Playing(it, Overlay.None) } ?: Phase.NewRun)
    }

    LaunchedEffect(phase) {
        (phase as? Phase.Playing)?.let { store.saveRun(it.run) }
    }

    val playing = phase as? Phase.Playing
    BackHandler(enabled = playing != null && playing.overlay != Overlay.None) {
        playing?.let { phase = it.copy(overlay = Overlay.None) }
    }

    GameScreenContent(
        phase = phase,
        bestRuns = store.bestRuns(),
        onAction = { action ->
            val current = phase as? Phase.Playing ?: return@GameScreenContent
            val outcome = Game.apply(current.run, action)
            if (outcome is Outcome.Done) phase = current.copy(run = outcome.state)
        },
        onOverlayChange = { overlay ->
            (phase as? Phase.Playing)?.let { phase = it.copy(overlay = overlay) }
        },
        onStartRun = { days -> phase = Phase.Playing(newRun(days), Overlay.None) },
        onEndRun = {
            (phase as? Phase.Playing)?.let { current ->
                store.recordRun(current.run)
                store.clearRun()
                phase = Phase.Ended(current.run)
            }
        },
        onNewRun = { phase = Phase.NewRun },
    )
}

/** The game's screens for a given phase, with no state of their own. */
@Composable
fun GameScreenContent(
    phase: Phase,
    bestRuns: List<BestRun>,
    onAction: (Action) -> Unit,
    onOverlayChange: (Overlay) -> Unit,
    onStartRun: (Int) -> Unit,
    onEndRun: () -> Unit,
    onNewRun: () -> Unit,
) {
    Box(Modifier.fillMaxSize().testTag(TestTags.SCREEN)) {
        when (phase) {
            Phase.NewRun -> NewRunScreen(bestRuns = bestRuns, onStart = onStartRun)
            is Phase.Ended -> EndScreen(run = phase.run, onNewRun = onNewRun)
            is Phase.Playing -> {
                val run = phase.run
                if (phase.overlay == Overlay.Market) {
                    MarketScreen(
                        state = run,
                        onBuy = { onAction(Action.Buy(it, 1)) },
                        onSell = { onAction(Action.Sell(it, 1)) },
                        onClose = { onOverlayChange(Overlay.None) },
                    )
                } else {
                    HomeScreen(
                        state = run,
                        route = if (phase.overlay is Overlay.Travel) {
                            RailNetwork.route(run.city, phase.overlay.to)
                        } else {
                            emptyList()
                        },
                        onOpenMarket = { onOverlayChange(Overlay.Market) },
                        onCityTap = { city ->
                            onOverlayChange(if (city == run.city) Overlay.Market else Overlay.Travel(city))
                        },
                        onEndRun = { onOverlayChange(Overlay.EndRun) },
                    )
                }
                when (val overlay = phase.overlay) {
                    is Overlay.Travel -> TravelSheet(
                        state = run,
                        destination = overlay.to,
                        onBoard = {
                            onAction(Action.Travel(overlay.to))
                            onOverlayChange(Overlay.None)
                        },
                        onDismiss = { onOverlayChange(Overlay.None) },
                    )
                    Overlay.EndRun -> EndRunSheet(
                        state = run,
                        onConfirm = onEndRun,
                        onDismiss = { onOverlayChange(Overlay.None) },
                    )
                    else -> {}
                }
            }
        }
    }
}

private const val PHASE_NEW_RUN = "newRun"
private const val PHASE_PLAYING = "playing"
private const val PHASE_ENDED = "ended"
private const val SAVED_FIELDS_BEFORE_HOLDINGS = 7

private val PhaseSaver = listSaver<Phase, Any>(
    save = { phase ->
        when (phase) {
            Phase.NewRun -> listOf(PHASE_NEW_RUN)
            is Phase.Playing -> listOf<Any>(PHASE_PLAYING, overlayToString(phase.overlay)) + runToFields(phase.run)
            is Phase.Ended -> listOf<Any>(PHASE_ENDED) + runToFields(phase.run)
        }
    },
    restore = { saved ->
        when (saved[0]) {
            PHASE_PLAYING -> Phase.Playing(
                run = fieldsToRun(saved.drop(2)),
                overlay = stringToOverlay(saved[1] as String),
            )
            PHASE_ENDED -> Phase.Ended(fieldsToRun(saved.drop(1)))
            else -> Phase.NewRun
        }
    },
)

private const val TRAVEL_PREFIX = "travel:"

private fun overlayToString(overlay: Overlay): String = when (overlay) {
    Overlay.None -> "none"
    Overlay.Market -> "market"
    Overlay.EndRun -> "endRun"
    is Overlay.Travel -> TRAVEL_PREFIX + overlay.to.name
}

private fun stringToOverlay(saved: String): Overlay = when {
    saved == "market" -> Overlay.Market
    saved == "endRun" -> Overlay.EndRun
    saved.startsWith(TRAVEL_PREFIX) -> Overlay.Travel(CityId.valueOf(saved.removePrefix(TRAVEL_PREFIX)))
    else -> Overlay.None
}

private fun runToFields(run: RunState): List<Any> = listOf<Any>(
    run.seed, run.day, run.runLengthDays, run.city.name,
    run.cashRupees, run.debtRupees, run.luggageCapacity,
) + GoodId.entries.map { run.held(it) }

private fun fieldsToRun(saved: List<Any>): RunState = RunState(
    seed = saved[0] as Long,
    day = saved[1] as Int,
    runLengthDays = saved[2] as Int,
    city = CityId.valueOf(saved[3] as String),
    cashRupees = saved[4] as Long,
    debtRupees = saved[5] as Long,
    luggageCapacity = saved[6] as Int,
    holdings = GoodId.entries
        .mapIndexed { index, good -> good to saved[SAVED_FIELDS_BEFORE_HOLDINGS + index] as Int }
        .filter { (_, quantity) -> quantity > 0 }
        .toMap(),
)
