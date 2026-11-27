package com.purplevarun.trader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.purplevarun.trader.engine.Action
import com.purplevarun.trader.engine.CityId
import com.purplevarun.trader.engine.Game
import com.purplevarun.trader.engine.GoodId
import com.purplevarun.trader.engine.Outcome
import com.purplevarun.trader.engine.RailNetwork
import com.purplevarun.trader.engine.RunState

/** What is shown over the map, if anything. */
sealed interface Overlay {
    data object None : Overlay
    data object Market : Overlay
    data class Travel(val to: CityId) : Overlay
}

/**
 * The game. Holds the run and what is on screen, and keeps both through rotation and through
 * Android stopping the app in the background. Every move goes through the engine.
 */
@Composable
fun GameScreen(newRun: () -> RunState) {
    var state by rememberSaveable(stateSaver = RunStateSaver) { mutableStateOf(newRun()) }
    var overlay by rememberSaveable(stateSaver = OverlaySaver) { mutableStateOf<Overlay>(Overlay.None) }

    BackHandler(enabled = overlay != Overlay.None) { overlay = Overlay.None }

    GameScreenContent(
        state = state,
        overlay = overlay,
        onAction = { action ->
            val outcome = Game.apply(state, action)
            if (outcome is Outcome.Done) state = outcome.state
        },
        onOverlayChange = { overlay = it },
    )
}

/** The game's screens for a given run and overlay, with no state of their own. */
@Composable
fun GameScreenContent(
    state: RunState,
    overlay: Overlay,
    onAction: (Action) -> Unit,
    onOverlayChange: (Overlay) -> Unit,
) {
    Box(Modifier.fillMaxSize().testTag(TestTags.SCREEN)) {
        if (overlay == Overlay.Market) {
            MarketScreen(
                state = state,
                onBuy = { onAction(Action.Buy(it, 1)) },
                onSell = { onAction(Action.Sell(it, 1)) },
                onClose = { onOverlayChange(Overlay.None) },
            )
        } else {
            HomeScreen(
                state = state,
                route = if (overlay is Overlay.Travel) RailNetwork.route(state.city, overlay.to) else emptyList(),
                onOpenMarket = { onOverlayChange(Overlay.Market) },
                onCityTap = { city ->
                    onOverlayChange(if (city == state.city) Overlay.Market else Overlay.Travel(city))
                },
            )
        }
        if (overlay is Overlay.Travel) {
            TravelSheet(
                state = state,
                destination = overlay.to,
                onBoard = {
                    onAction(Action.Travel(overlay.to))
                    onOverlayChange(Overlay.None)
                },
                onDismiss = { onOverlayChange(Overlay.None) },
            )
        }
    }
}

private const val SAVED_FIELDS_BEFORE_HOLDINGS = 7

private val RunStateSaver = listSaver<RunState, Any>(
    save = { run ->
        listOf<Any>(
            run.seed, run.day, run.runLengthDays, run.city.name,
            run.cashRupees, run.debtRupees, run.luggageCapacity,
        ) + GoodId.entries.map { run.held(it) }
    },
    restore = { saved ->
        RunState(
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
    },
)

private const val TRAVEL_PREFIX = "travel:"

private val OverlaySaver = Saver<Overlay, String>(
    save = { overlay ->
        when (overlay) {
            Overlay.None -> "none"
            Overlay.Market -> "market"
            is Overlay.Travel -> TRAVEL_PREFIX + overlay.to.name
        }
    },
    restore = { saved ->
        when {
            saved == "market" -> Overlay.Market
            saved.startsWith(TRAVEL_PREFIX) -> Overlay.Travel(CityId.valueOf(saved.removePrefix(TRAVEL_PREFIX)))
            else -> Overlay.None
        }
    },
)
