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
import com.purplevarun.trader.engine.GameEvent
import com.purplevarun.trader.engine.GoodId
import com.purplevarun.trader.engine.Outcome
import com.purplevarun.trader.engine.PriceShift
import com.purplevarun.trader.engine.RailNetwork
import com.purplevarun.trader.engine.Rumour
import com.purplevarun.trader.engine.RunState
import com.purplevarun.trader.engine.TransportId

/** Where the game is: picking a new run, mid-run, or looking at how a run ended. */
sealed interface Phase {
    data object NewRun : Phase

    /**
     * Mid-run. [pendingEvents] are what the day brought, still waiting to be told; their
     * effects are already in [run]. They are not saved: restoring mid-run skips the telling.
     */
    data class Playing(
        val run: RunState,
        val overlay: Overlay,
        val pendingEvents: List<GameEvent> = emptyList(),
        val keypad: KeypadSpec? = null,
    ) : Phase
    data class Ended(val run: RunState, val ending: Ending) : Phase
}

/** How a run ended: walked away rich, out of everything, or collected by the sahukar's man. */
enum class Ending { RETIRED, BROKE, COLLECTED }

/** What is shown over the map, if anything. */
sealed interface Overlay {
    data object None : Overlay
    data object Market : Overlay
    data object Sahukar : Overlay
    data object Bank : Overlay
    data object Luggage : Overlay
    data object Godown : Overlay
    data object EndRun : Overlay
    data class Travel(val to: CityId) : Overlay
}

/**
 * The game. Holds where the run is and what is on screen, keeps both through rotation and
 * through Android stopping the app in the background, and writes the run to [store] as it
 * changes so closing the app loses nothing. Every move goes through the engine.
 */
@Composable
fun GameScreen(store: RunStore, newRun: () -> RunState) {
    var phase by rememberSaveable(stateSaver = PhaseSaver) {
        mutableStateOf(store.loadRun()?.let { Phase.Playing(it, Overlay.None) } ?: Phase.NewRun)
    }

    LaunchedEffect(phase) {
        (phase as? Phase.Playing)?.let { store.saveRun(it.run) }
    }

    val playing = phase as? Phase.Playing
    BackHandler(enabled = playing != null && (playing.overlay != Overlay.None || playing.keypad != null)) {
        playing?.let {
            phase = if (it.keypad != null) it.copy(keypad = null) else it.copy(overlay = Overlay.None)
        }
    }

    GameScreenContent(
        phase = phase,
        bestRuns = store.bestRuns(),
        onAction = { action ->
            val current = phase as? Phase.Playing ?: return@GameScreenContent
            val outcome = Game.apply(current.run, action)
            if (outcome is Outcome.Done) {
                phase = current.copy(
                    run = outcome.state,
                    pendingEvents = current.pendingEvents + outcome.events,
                )
            }
        },
        onOverlayChange = { overlay ->
            (phase as? Phase.Playing)?.let { phase = it.copy(overlay = overlay) }
        },
        onKeypad = { spec ->
            (phase as? Phase.Playing)?.let { phase = it.copy(keypad = spec) }
        },
        onEventDismissed = {
            (phase as? Phase.Playing)?.let { phase = it.copy(pendingEvents = it.pendingEvents.drop(1)) }
        },
        onStartRun = { phase = Phase.Playing(newRun(), Overlay.None) },
        onEndRun = { ending ->
            (phase as? Phase.Playing)?.let { current ->
                store.recordRun(current.run)
                store.clearRun()
                phase = Phase.Ended(current.run, ending)
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
    onKeypad: (KeypadSpec?) -> Unit,
    onEventDismissed: () -> Unit,
    onStartRun: () -> Unit,
    onEndRun: (Ending) -> Unit,
    onNewRun: () -> Unit,
) {
    Box(Modifier.fillMaxSize().testTag(TestTags.SCREEN)) {
        when (phase) {
            Phase.NewRun -> NewRunScreen(bestRuns = bestRuns, onStart = onStartRun)
            is Phase.Ended -> EndScreen(run = phase.run, ending = phase.ending, onNewRun = onNewRun)
            is Phase.Playing -> {
                val run = phase.run
                if (phase.overlay == Overlay.Market) {
                    MarketScreen(
                        state = run,
                        onBuy = { onAction(Action.Buy(it, 1)) },
                        onSell = { onAction(Action.Sell(it, 1)) },
                        onExact = { onKeypad(KeypadSpec.Market(it)) },
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
                        onOpenPlace = { onOverlayChange(it) },
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
                    Overlay.Sahukar -> SahukarSheet(
                        state = run,
                        onAction = onAction,
                        onExact = onKeypad,
                        onDismiss = { onOverlayChange(Overlay.None) },
                    )
                    Overlay.Bank -> BankSheet(
                        state = run,
                        onAction = onAction,
                        onExact = onKeypad,
                        onDismiss = { onOverlayChange(Overlay.None) },
                    )
                    Overlay.Luggage -> LuggageSheet(
                        state = run,
                        onAction = onAction,
                        onDismiss = { onOverlayChange(Overlay.None) },
                    )
                    Overlay.Godown -> GodownSheet(
                        state = run,
                        onAction = onAction,
                        onExact = onKeypad,
                        onDismiss = { onOverlayChange(Overlay.None) },
                    )
                    Overlay.EndRun -> EndRunSheet(
                        state = run,
                        onConfirm = { onEndRun(Ending.RETIRED) },
                        onDismiss = { onOverlayChange(Overlay.None) },
                    )
                    else -> {}
                }
                // The keypad floats over the screen or sheet it was opened from; what the
                // day brought is told on top of that, and what the day demands — the
                // collector, then the end of options — on top of everything.
                phase.keypad?.let { spec ->
                    AmountSheet(
                        state = run,
                        spec = spec,
                        onAction = onAction,
                        onDismiss = { onKeypad(null) },
                    )
                }
                phase.pendingEvents.firstOrNull()?.let { event ->
                    EventSheet(
                        event = event,
                        moreAfter = phase.pendingEvents.size - 1,
                        onDismissed = onEventDismissed,
                    )
                }
                if (run.collectorDue) {
                    CollectorSheet(
                        state = run,
                        onAction = onAction,
                        onDeath = { onEndRun(Ending.COLLECTED) },
                    )
                } else if (run.isBroke) {
                    BrokeSheet(
                        state = run,
                        onAction = onAction,
                        onDeath = { onEndRun(Ending.BROKE) },
                    )
                }
            }
        }
    }
}

private const val PHASE_NEW_RUN = "newRun"
private const val PHASE_PLAYING = "playing"
private const val PHASE_ENDED = "ended"
private const val SAVED_FIELDS_BEFORE_HOLDINGS = 12

private val PhaseSaver = listSaver<Phase, Any>(
    save = { phase ->
        when (phase) {
            Phase.NewRun -> listOf(PHASE_NEW_RUN)
            is Phase.Playing -> listOf<Any>(
                PHASE_PLAYING,
                overlayToString(phase.overlay),
                keypadToString(phase.keypad),
            ) + runToFields(phase.run)
            is Phase.Ended -> listOf<Any>(PHASE_ENDED, phase.ending.name) + runToFields(phase.run)
        }
    },
    restore = { saved ->
        when (saved[0]) {
            PHASE_PLAYING -> Phase.Playing(
                run = fieldsToRun(saved.drop(3)),
                overlay = stringToOverlay(saved[1] as String),
                keypad = stringToKeypad(saved[2] as String),
            )
            PHASE_ENDED -> Phase.Ended(fieldsToRun(saved.drop(2)), Ending.valueOf(saved[1] as String))
            else -> Phase.NewRun
        }
    },
)

private const val TRAVEL_PREFIX = "travel:"

private fun overlayToString(overlay: Overlay): String = when (overlay) {
    Overlay.None -> "none"
    Overlay.Market -> "market"
    Overlay.Sahukar -> "sahukar"
    Overlay.Bank -> "bank"
    Overlay.Luggage -> "luggage"
    Overlay.Godown -> "godown"
    Overlay.EndRun -> "endRun"
    is Overlay.Travel -> TRAVEL_PREFIX + overlay.to.name
}

private fun stringToOverlay(saved: String): Overlay = when {
    saved == "market" -> Overlay.Market
    saved == "sahukar" -> Overlay.Sahukar
    saved == "bank" -> Overlay.Bank
    saved == "luggage" -> Overlay.Luggage
    saved == "godown" -> Overlay.Godown
    saved == "endRun" -> Overlay.EndRun
    saved.startsWith(TRAVEL_PREFIX) -> Overlay.Travel(CityId.valueOf(saved.removePrefix(TRAVEL_PREFIX)))
    else -> Overlay.None
}

private const val KEYPAD_PREFIX = "keypad:"

private fun keypadToString(spec: KeypadSpec?): String = when (spec) {
    null -> KEYPAD_PREFIX + "none"
    is KeypadSpec.Market -> KEYPAD_PREFIX + "market:" + spec.good.name
    is KeypadSpec.Money -> KEYPAD_PREFIX + "money:" + spec.verb.name
    is KeypadSpec.Godown -> KEYPAD_PREFIX + "godown:" + spec.good.name + ":" + spec.verb.name
}

private fun stringToKeypad(saved: String): KeypadSpec? {
    val parts = saved.removePrefix(KEYPAD_PREFIX).split(":")
    return when (parts[0]) {
        "market" -> KeypadSpec.Market(GoodId.valueOf(parts[1]))
        "money" -> KeypadSpec.Money(MoneyVerb.valueOf(parts[1]))
        "godown" -> KeypadSpec.Godown(GoodId.valueOf(parts[1]), GodownVerb.valueOf(parts[2]))
        else -> null
    }
}

private fun runToFields(run: RunState): List<Any> = listOf<Any>(
    run.seed, run.day, run.city.name,
    run.cashRupees, run.debtRupees, run.debtDueDay, run.bankRupees, run.transport.name,
    run.rngRolls,
    run.priceShifts.joinToString(",") { "${it.city.name}:${it.good.name}:${it.basisPoints}" },
    run.godowns.entries.joinToString(";") { (city, goods) ->
        "${city.name}>" + goods.entries.joinToString(",") { (good, quantity) -> "${good.name}:$quantity" }
    },
    run.rumours.joinToString(";") { "${it.city.name}>${it.good.name}:${it.landsOnDay}:${it.basisPoints}" },
) + GoodId.entries.map { run.held(it) }

private fun fieldsToRun(saved: List<Any>): RunState = RunState(
    seed = saved[0] as Long,
    day = saved[1] as Int,
    city = CityId.valueOf(saved[2] as String),
    cashRupees = saved[3] as Long,
    debtRupees = saved[4] as Long,
    debtDueDay = saved[5] as Int,
    bankRupees = saved[6] as Long,
    transport = TransportId.valueOf(saved[7] as String),
    rngRolls = saved[8] as Long,
    priceShifts = (saved[9] as String).split(",")
        .filter { it.isNotEmpty() }
        .map { entry ->
            val (city, good, basisPoints) = entry.split(":")
            PriceShift(CityId.valueOf(city), GoodId.valueOf(good), basisPoints.toInt())
        },
    godowns = (saved[10] as String).split(";")
        .filter { it.isNotEmpty() }
        .associate { entry ->
            val (city, goods) = entry.split(">")
            CityId.valueOf(city) to goods.split(",")
                .filter { it.isNotEmpty() }
                .associate { item ->
                    val (good, quantity) = item.split(":")
                    GoodId.valueOf(good) to quantity.toInt()
                }
        },
    holdings = GoodId.entries
        .mapIndexed { index, good -> good to saved[SAVED_FIELDS_BEFORE_HOLDINGS + index] as Int }
        .filter { (_, quantity) -> quantity > 0 }
        .toMap(),
    rumours = (saved[11] as String).split(";")
        .filter { it.isNotEmpty() }
        .map { entry ->
            val (city, rest) = entry.split(">")
            val (good, landsOnDay, basisPoints) = rest.split(":")
            Rumour(CityId.valueOf(city), GoodId.valueOf(good), landsOnDay.toInt(), basisPoints.toInt())
        },
)
