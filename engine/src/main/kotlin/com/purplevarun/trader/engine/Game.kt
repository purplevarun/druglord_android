package com.purplevarun.trader.engine

sealed interface Action {
    data class Buy(val good: GoodId, val quantity: Int) : Action
    data class Sell(val good: GoodId, val quantity: Int) : Action
    data class Travel(val to: CityId) : Action
}

enum class Rejection {
    NOT_A_POSITIVE_QUANTITY,
    NOT_ENOUGH_CASH,
    NOT_ENOUGH_LUGGAGE_SPACE,
    NOT_ENOUGH_HELD,
    ALREADY_THERE,
    NOT_ENOUGH_DAYS,
}

sealed interface Outcome {
    data class Done(val state: RunState) : Outcome
    data class Rejected(val reason: Rejection) : Outcome
}

/** The rules of a run: the one place a move is checked and applied. */
object Game {
    fun apply(state: RunState, action: Action): Outcome = when (action) {
        is Action.Buy -> buy(state, action.good, action.quantity)
        is Action.Sell -> sell(state, action.good, action.quantity)
        is Action.Travel -> travel(state, action.to)
    }

    private fun buy(state: RunState, good: GoodId, quantity: Int): Outcome {
        if (quantity <= 0) return Outcome.Rejected(Rejection.NOT_A_POSITIVE_QUANTITY)
        val cost = state.priceOf(good) * quantity
        if (cost > state.cashRupees) return Outcome.Rejected(Rejection.NOT_ENOUGH_CASH)
        if (state.luggageUsed + quantity > state.luggageCapacity) {
            return Outcome.Rejected(Rejection.NOT_ENOUGH_LUGGAGE_SPACE)
        }
        return Outcome.Done(
            state.copy(
                cashRupees = state.cashRupees - cost,
                holdings = state.holdings + (good to state.held(good) + quantity),
            ),
        )
    }

    private fun sell(state: RunState, good: GoodId, quantity: Int): Outcome {
        if (quantity <= 0) return Outcome.Rejected(Rejection.NOT_A_POSITIVE_QUANTITY)
        if (quantity > state.held(good)) return Outcome.Rejected(Rejection.NOT_ENOUGH_HELD)
        val remaining = state.held(good) - quantity
        return Outcome.Done(
            state.copy(
                cashRupees = state.cashRupees + state.priceOf(good) * quantity,
                holdings = if (remaining == 0) state.holdings - good else state.holdings + (good to remaining),
            ),
        )
    }

    private fun travel(state: RunState, to: CityId): Outcome {
        if (to == state.city) return Outcome.Rejected(Rejection.ALREADY_THERE)
        val days = RailNetwork.travelDays(state.city, to)
        if (days > state.daysLeft) return Outcome.Rejected(Rejection.NOT_ENOUGH_DAYS)
        return Outcome.Done(state.copy(city = to, day = state.day + days))
    }
}
