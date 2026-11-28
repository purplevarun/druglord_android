package com.purplevarun.trader.engine

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random

/** What the trader tries to do. Every action goes through [Game.apply]. */
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
    /** The move went through. [events] is what happened along the way, in order. */
    data class Done(val state: RunState, val events: List<GameEvent> = emptyList()) : Outcome
    data class Rejected(val reason: Rejection) : Outcome
}

/**
 * The rules of the game. [check] answers "would this move be allowed?" without touching
 * the dice; [apply] performs it — checks it, applies it, then rolls for the events the
 * day(s) brought. Every roll is drawn from `seed + rngRolls`, so the same run of moves
 * always produces the same events.
 */
object Game {

    fun check(state: RunState, action: Action): Rejection? = when (action) {
        is Action.Buy -> when {
            action.quantity <= 0 -> Rejection.NOT_A_POSITIVE_QUANTITY
            saturating(state, state.city, action.good, action.quantity * GameConstants.SATURATION_BP_PER_UNIT)
                .priceOf(action.good) * action.quantity > state.cashRupees -> Rejection.NOT_ENOUGH_CASH
            action.quantity > state.luggageCapacity - state.luggageUsed -> Rejection.NOT_ENOUGH_LUGGAGE_SPACE
            else -> null
        }
        is Action.Sell -> when {
            action.quantity <= 0 -> Rejection.NOT_A_POSITIVE_QUANTITY
            action.quantity > state.held(action.good) -> Rejection.NOT_ENOUGH_HELD
            else -> null
        }
        is Action.Travel -> when {
            action.to == state.city -> Rejection.ALREADY_THERE
            RailNetwork.travelDays(state.city, action.to) > state.daysLeft -> Rejection.NOT_ENOUGH_DAYS
            else -> null
        }
        is Action.Borrow ->
            if (action.amountRupees <= 0) Rejection.NOT_A_POSITIVE_QUANTITY else null
        is Action.Repay -> when {
            action.amountRupees <= 0 -> Rejection.NOT_A_POSITIVE_QUANTITY
            action.amountRupees > state.debtRupees -> Rejection.REPAYS_MORE_THAN_OWED
            action.amountRupees > state.cashRupees -> Rejection.NOT_ENOUGH_CASH
            else -> null
        }
        is Action.Deposit -> when {
            action.amountRupees <= 0 -> Rejection.NOT_A_POSITIVE_QUANTITY
            action.amountRupees > state.cashRupees -> Rejection.NOT_ENOUGH_CASH
            else -> null
        }
        is Action.Withdraw -> when {
            action.amountRupees <= 0 -> Rejection.NOT_A_POSITIVE_QUANTITY
            action.amountRupees > state.bankRupees -> Rejection.NOT_ENOUGH_SAVED
            else -> null
        }
        is Action.BuyTransport -> when {
            GameConstants.transport(action.transport).capacity <= state.luggageCapacity -> Rejection.NOT_AN_UPGRADE
            GameConstants.transport(action.transport).priceRupees > state.cashRupees -> Rejection.NOT_ENOUGH_CASH
            else -> null
        }
    }

    fun apply(state: RunState, action: Action): Outcome {
        check(state, action)?.let { return Outcome.Rejected(it) }

        var rolled = applyMove(state, action)
        val events = mutableListOf<GameEvent>()
        if (action is Action.Travel) {
            repeat(rolled.day - state.day) {
                val (next, event) = roll(rolled, onTheRails = true)
                rolled = next
                event?.let(events::add)
            }
        } else {
            val (next, event) = roll(rolled, onTheRails = false)
            rolled = next
            event?.let(events::add)
        }
        return Outcome.Done(rolled, events)
    }

    /**
     * Check plus move, without rolling the dice. For tests that want the move's own
     * outcome and nothing the day might bring; production goes through [apply].
     */
    internal fun applyQuietly(state: RunState, action: Action): Outcome =
        check(state, action)?.let { Outcome.Rejected(it) } ?: Outcome.Done(applyMove(state, action))

    // --- The moves, no dice ---

    private fun applyMove(state: RunState, action: Action): RunState = when (action) {
        is Action.Buy -> {
            // A heavy order moves the price first, then pays the moved price: buying a
            // lot never leaves you richer for free, and selling a lot eats its own dump.
            val moved = saturating(state, state.city, action.good, action.quantity * GameConstants.SATURATION_BP_PER_UNIT)
            moved.copy(
                cashRupees = moved.cashRupees - moved.priceOf(action.good) * action.quantity,
                holdings = moved.holdings + (action.good to moved.held(action.good) + action.quantity),
            )
        }
        is Action.Sell -> {
            val moved = saturating(state, state.city, action.good, -action.quantity * GameConstants.SATURATION_BP_PER_UNIT)
            val left = moved.held(action.good) - action.quantity
            moved.copy(
                cashRupees = moved.cashRupees + moved.priceOf(action.good) * action.quantity,
                holdings = if (left > 0) moved.holdings + (action.good to left) else moved.holdings - action.good,
            )
        }
        is Action.Travel -> {
            val days = RailNetwork.travelDays(state.city, action.to)
            // Every day on the rails the debt grows heavier, the savings a little richer,
            // and old news and saturation fade a step further.
            state.copy(
                city = action.to,
                day = state.day + days,
                debtRupees = state.debtRupees.withInterest(GameConstants.SAHUKAR_DAILY_INTEREST, days),
                bankRupees = state.bankRupees.withInterest(GameConstants.BANK_DAILY_INTEREST, days),
                priceShifts = state.priceShifts
                    .map { it.copy(basisPoints = (it.basisPoints * GameConstants.PRICE_SHIFT_DAILY_DECAY.pow(days)).toInt()) }
                    .filter { kotlin.math.abs(it.basisPoints) >= GameConstants.PRICE_SHIFT_MIN_BP },
            )
        }
        is Action.Borrow -> state.copy(
            cashRupees = state.cashRupees + action.amountRupees,
            debtRupees = state.debtRupees + action.amountRupees,
        )
        is Action.Repay -> state.copy(
            cashRupees = state.cashRupees - action.amountRupees,
            debtRupees = state.debtRupees - action.amountRupees,
        )
        is Action.Deposit -> state.copy(
            cashRupees = state.cashRupees - action.amountRupees,
            bankRupees = state.bankRupees + action.amountRupees,
        )
        is Action.Withdraw -> state.copy(
            cashRupees = state.cashRupees + action.amountRupees,
            bankRupees = state.bankRupees - action.amountRupees,
        )
        is Action.BuyTransport -> state.copy(
            cashRupees = state.cashRupees - GameConstants.transport(action.transport).priceRupees,
            transport = action.transport,
        )
    }

    /** One unit traded nudges the local price [shiftBp] basis points; same-shift merges. */
    private fun saturating(state: RunState, city: CityId, good: GoodId, shiftBp: Int): RunState {
        if (shiftBp == 0) return state
        val rest = state.priceShifts.filterNot { it.city == city && it.good == good }
        val merged = (state.priceShifts.firstOrNull { it.city == city && it.good == good }?.basisPoints ?: 0) + shiftBp
        return state.copy(priceShifts = rest + PriceShift(city, good, merged))
    }

    // --- The dice ---

    /** One roll of the dice: may produce an event, always spends the roll. */
    private fun roll(state: RunState, onTheRails: Boolean): Pair<RunState, GameEvent?> {
        val rolled = state.copy(rngRolls = state.rngRolls + 1)
        val random = Random(rolled.seed * 31 + rolled.rngRolls)
        val chance = if (onTheRails) {
            GameConstants.EVENT_CHANCE_PER_TRAVEL_DAY
        } else {
            GameConstants.EVENT_CHANCE_PER_CITY_ACTION
        }
        if (random.nextDouble() >= chance) return rolled to null
        val event = drawEvent(rolled, onTheRails, random) ?: return rolled to null
        return applyEvent(rolled, event) to event
    }

    /** What the dice brought, given where the trader is. Null when nothing fits today. */
    private fun drawEvent(state: RunState, onTheRails: Boolean, random: Random): GameEvent? {
        val pick = random.nextInt(100)
        if (onTheRails) {
            if (pick < 30) return pickpocket(state, random)
            if (pick < 50) return checkpost(state, random)
            if (pick < 70) return bandh(state)
            return priceSwing(state, random)
        }
        if (pick < 40) return pickpocket(state, random)
        return priceSwing(state, random)
    }

    private fun pickpocket(state: RunState, random: Random): GameEvent? {
        if (state.cashRupees <= 0) return null
        val share = GameConstants.PICKPOCKET_SHARE_MIN +
            random.nextDouble() * (GameConstants.PICKPOCKET_SHARE_MAX - GameConstants.PICKPOCKET_SHARE_MIN)
        return GameEvent.Pickpocket((state.cashRupees * share).roundToLong())
    }

    private fun checkpost(state: RunState, random: Random): GameEvent? {
        if (state.cashRupees >= GameConstants.CHECKPOST_MIN_BRIBE_RUPEES) {
            val bribe = (state.cashRupees * GameConstants.CHECKPOST_BRIBE_SHARE).roundToLong()
            return GameEvent.Checkpost(bribeRupees = bribe, seizedGood = null, seizedUnits = 0)
        }
        val good = state.holdings.keys.randomOrNull(random) ?: return null
        val units = (state.held(good) * GameConstants.CHECKPOST_SEIZE_SHARE).roundToInt().coerceAtLeast(1)
        return GameEvent.Checkpost(bribeRupees = 0, seizedGood = good, seizedUnits = units)
    }

    private fun bandh(state: RunState): GameEvent? =
        if (state.day < state.runLengthDays) GameEvent.Bandh(GameConstants.BANDH_EXTRA_DAYS) else null

    private fun priceSwing(state: RunState, random: Random): GameEvent {
        val headline = EventHeadline.entries.random(random)
        val basisPoints = if (headline == EventHeadline.BUMPER_HARVEST) {
            -random.nextInt(GameConstants.PRICE_CRASH_BP_MIN, GameConstants.PRICE_CRASH_BP_MAX + 1)
        } else {
            random.nextInt(GameConstants.PRICE_SPIKE_BP_MIN, GameConstants.PRICE_SPIKE_BP_MAX + 1)
        }
        return GameEvent.PriceSwing(state.city, GoodId.entries.random(random), basisPoints, headline)
    }

    /** What an event does to the run. Exposed for tests; events arrive via [apply]. */
    internal fun applyEvent(state: RunState, event: GameEvent): RunState = when (event) {
        is GameEvent.Pickpocket -> state.copy(cashRupees = state.cashRupees - event.lostRupees)
        is GameEvent.Checkpost -> {
            val seized = if (event.seizedGood == null) {
                state.holdings
            } else {
                val left = state.held(event.seizedGood) - event.seizedUnits
                if (left > 0) state.holdings + (event.seizedGood to left) else state.holdings - event.seizedGood
            }
            state.copy(cashRupees = state.cashRupees - event.bribeRupees, holdings = seized)
        }
        is GameEvent.Bandh ->
            // The extra day costs what a day costs: a day, and a day's interest on both sides.
            if (state.day >= state.runLengthDays) {
                state
            } else {
                state.copy(
                    day = state.day + event.extraDays,
                    debtRupees = state.debtRupees.withInterest(GameConstants.SAHUKAR_DAILY_INTEREST, event.extraDays),
                    bankRupees = state.bankRupees.withInterest(GameConstants.BANK_DAILY_INTEREST, event.extraDays),
                )
            }
        is GameEvent.PriceSwing -> saturating(state, event.city, event.good, event.basisPoints)
    }

    private fun Long.withInterest(dailyRate: Double, days: Int): Long =
        if (this == 0L) 0L else (this * (1 + dailyRate).pow(days)).roundToLong()
}
