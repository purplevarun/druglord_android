package com.purplevarun.trader.engine

import kotlin.math.pow
import kotlin.math.roundToLong

sealed interface Action {
    data class Buy(val good: GoodId, val quantity: Int) : Action
    data class Sell(val good: GoodId, val quantity: Int) : Action
    data class Travel(val to: CityId) : Action
    data class Borrow(val amountRupees: Long) : Action
    data class Repay(val amountRupees: Long) : Action
    data class Deposit(val amountRupees: Long) : Action
    data class Withdraw(val amountRupees: Long) : Action
    data class BuyTransport(val transport: TransportId) : Action
}

enum class Rejection {
    NOT_A_POSITIVE_QUANTITY,
    NOT_ENOUGH_CASH,
    NOT_ENOUGH_LUGGAGE_SPACE,
    NOT_ENOUGH_HELD,
    ALREADY_THERE,
    NOT_ENOUGH_DAYS,
    NOT_ENOUGH_SAVED,
    REPAYS_MORE_THAN_OWED,
    NOT_AN_UPGRADE,
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
        is Action.Borrow -> borrow(state, action.amountRupees)
        is Action.Repay -> repay(state, action.amountRupees)
        is Action.Deposit -> deposit(state, action.amountRupees)
        is Action.Withdraw -> withdraw(state, action.amountRupees)
        is Action.BuyTransport -> buyTransport(state, action.transport)
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
        // Every day on the rails the debt grows heavier and the savings a little richer.
        return Outcome.Done(
            state.copy(
                city = to,
                day = state.day + days,
                debtRupees = state.debtRupees.withInterest(GameConstants.SAHUKAR_DAILY_INTEREST, days),
                bankRupees = state.bankRupees.withInterest(GameConstants.BANK_DAILY_INTEREST, days),
            ),
        )
    }

    private fun borrow(state: RunState, amount: Long): Outcome {
        if (amount <= 0) return Outcome.Rejected(Rejection.NOT_A_POSITIVE_QUANTITY)
        return Outcome.Done(
            state.copy(
                cashRupees = state.cashRupees + amount,
                debtRupees = state.debtRupees + amount,
            ),
        )
    }

    private fun repay(state: RunState, amount: Long): Outcome {
        if (amount <= 0) return Outcome.Rejected(Rejection.NOT_A_POSITIVE_QUANTITY)
        if (amount > state.debtRupees) return Outcome.Rejected(Rejection.REPAYS_MORE_THAN_OWED)
        if (amount > state.cashRupees) return Outcome.Rejected(Rejection.NOT_ENOUGH_CASH)
        return Outcome.Done(
            state.copy(
                cashRupees = state.cashRupees - amount,
                debtRupees = state.debtRupees - amount,
            ),
        )
    }

    private fun deposit(state: RunState, amount: Long): Outcome {
        if (amount <= 0) return Outcome.Rejected(Rejection.NOT_A_POSITIVE_QUANTITY)
        if (amount > state.cashRupees) return Outcome.Rejected(Rejection.NOT_ENOUGH_CASH)
        return Outcome.Done(
            state.copy(
                cashRupees = state.cashRupees - amount,
                bankRupees = state.bankRupees + amount,
            ),
        )
    }

    private fun withdraw(state: RunState, amount: Long): Outcome {
        if (amount <= 0) return Outcome.Rejected(Rejection.NOT_A_POSITIVE_QUANTITY)
        if (amount > state.bankRupees) return Outcome.Rejected(Rejection.NOT_ENOUGH_SAVED)
        return Outcome.Done(
            state.copy(
                cashRupees = state.cashRupees + amount,
                bankRupees = state.bankRupees - amount,
            ),
        )
    }

    private fun buyTransport(state: RunState, transport: TransportId): Outcome {
        val upgrade = GameConstants.transport(transport)
        if (upgrade.capacity <= state.luggageCapacity) return Outcome.Rejected(Rejection.NOT_AN_UPGRADE)
        if (upgrade.priceRupees > state.cashRupees) return Outcome.Rejected(Rejection.NOT_ENOUGH_CASH)
        return Outcome.Done(
            state.copy(
                cashRupees = state.cashRupees - upgrade.priceRupees,
                transport = transport,
            ),
        )
    }

    private fun Long.withInterest(dailyRate: Double, days: Int): Long =
        if (this == 0L) 0L else (this * (1 + dailyRate).pow(days)).roundToLong()
}
