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
    data class Store(val good: GoodId, val quantity: Int) : Action
    data class Retrieve(val good: GoodId, val quantity: Int) : Action
    data class SellStored(val good: GoodId, val quantity: Int) : Action
    /** Pay the collector the whole debt, cash first then the bank. */
    data object PayCollector : Action
}

enum class Rejection {
    NOT_A_POSITIVE_QUANTITY,
    NOT_ENOUGH_CASH,
    NOT_ENOUGH_LUGGAGE_SPACE,
    NOT_ENOUGH_HELD,
    ALREADY_THERE,
    NOT_ENOUGH_SAVED,
    ALREADY_OWES,
    ASKS_TOO_MUCH,
    NOTHING_OWED,
    REPAYS_MORE_THAN_OWED,
    NOT_AN_UPGRADE,
    NOT_ENOUGH_STORED,
    GODOWN_FULL,
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
        is Action.Travel ->
            if (action.to == state.city) Rejection.ALREADY_THERE else null
        is Action.Borrow -> when {
            action.amountRupees <= 0 -> Rejection.NOT_A_POSITIVE_QUANTITY
            action.amountRupees > GameConstants.MAX_LOAN_RUPEES -> Rejection.ASKS_TOO_MUCH
            // The sahukar lends once: while you owe anything there is no more.
            state.debtRupees > 0 -> Rejection.ALREADY_OWES
            else -> null
        }
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
        is Action.Store -> when {
            action.quantity <= 0 -> Rejection.NOT_A_POSITIVE_QUANTITY
            action.quantity > state.held(action.good) -> Rejection.NOT_ENOUGH_HELD
            state.godownUsed(state.city) + action.quantity > GameConstants.GODOWN_CAPACITY_UNITS -> Rejection.GODOWN_FULL
            else -> null
        }
        is Action.Retrieve -> when {
            action.quantity <= 0 -> Rejection.NOT_A_POSITIVE_QUANTITY
            action.quantity > state.stored(state.city, action.good) -> Rejection.NOT_ENOUGH_STORED
            action.quantity > state.luggageCapacity - state.luggageUsed -> Rejection.NOT_ENOUGH_LUGGAGE_SPACE
            else -> null
        }
        is Action.SellStored -> when {
            action.quantity <= 0 -> Rejection.NOT_A_POSITIVE_QUANTITY
            action.quantity > state.stored(state.city, action.good) -> Rejection.NOT_ENOUGH_STORED
            else -> null
        }
        Action.PayCollector -> when {
            state.debtRupees == 0L -> Rejection.NOTHING_OWED
            // The visit settles the debt and the day's keep in one go.
            state.cashRupees + state.bankRupees < state.debtRupees + state.livingCostPerDay ->
                Rejection.NOT_ENOUGH_CASH
            else -> null
        }
    }

    fun apply(state: RunState, action: Action): Outcome {
        check(state, action)?.let { return Outcome.Rejected(it) }

        var rolled = applyMove(state, action)
        val events = mutableListOf<GameEvent>()
        // Rumours whose day came land before the day rolls its own luck.
        val (landed, rumourEvents) = landingRumours(rolled)
        rolled = landed
        events += rumourEvents
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
     * Check plus move, without rolling the dice: rumour landings still happen (they are
     * the day's doing, not luck). For tests that want the move's own outcome and nothing
     * the dice might bring; production goes through [apply].
     */
    internal fun applyQuietly(state: RunState, action: Action): Outcome =
        check(state, action)?.let { Outcome.Rejected(it) } ?: run {
            val (moved, rumourEvents) = landingRumours(applyMove(state, action))
            Outcome.Done(moved, rumourEvents)
        }

    // --- The moves, no dice ---

    /**
     * Every move takes your day: food and lodging at the city you end it in. What cash
     * cannot cover lands on the sahukar debt — and its interest — like unpaid rent.
     */
    private fun chargingLiving(state: RunState, days: Int): RunState {
        val cost = state.livingCostPerDay * days
        if (cost <= 0) return state
        val paid = minOf(cost, state.cashRupees)
        return state.copy(
            cashRupees = state.cashRupees - paid,
            debtRupees = state.debtRupees + (cost - paid),
            debtDueDay = debtDueDayAfter(state, cost - paid),
        )
    }

    private fun applyMove(state: RunState, action: Action): RunState {
        val moved = performMove(state, action)
        // A trip lives at the destination's rate for each rail day; any other move takes
        // one day of living where you are — except the collector's visit, which settled
        // the day's keep inside the payment itself.
        return if (action is Action.Travel || action is Action.PayCollector) {
            moved
        } else {
            chargingLiving(moved, 1)
        }
    }

    private fun performMove(state: RunState, action: Action): RunState = when (action) {
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
            // the godowns bill their rent, the destination bills your keep, and old news
            // and saturation fade a step further.
            val moved = chargingRent(
                state.copy(
                    city = action.to,
                    day = state.day + days,
                    debtRupees = state.debtRupees.withInterest(GameConstants.SAHUKAR_DAILY_INTEREST, days),
                    bankRupees = state.bankRupees.withInterest(GameConstants.BANK_DAILY_INTEREST, days),
                    priceShifts = state.priceShifts
                        .map { it.copy(basisPoints = (it.basisPoints * GameConstants.PRICE_SHIFT_DAILY_DECAY.pow(days)).toInt()) }
                        .filter { kotlin.math.abs(it.basisPoints) >= GameConstants.PRICE_SHIFT_MIN_BP },
                ),
                days,
            )
            chargingLiving(moved, days)
        }
        is Action.Borrow -> state.copy(
            // The loan starts the collector's clock: ten days to turn it into the money back.
            cashRupees = state.cashRupees + action.amountRupees,
            debtRupees = state.debtRupees + action.amountRupees,
            debtDueDay = state.day + GameConstants.COLLECTOR_AFTER_DAYS,
        )
        is Action.Repay -> {
            val debtLeft = state.debtRupees - action.amountRupees
            // Clearing the debt calls the collector off entirely.
            state.copy(
                cashRupees = state.cashRupees - action.amountRupees,
                debtRupees = debtLeft,
                debtDueDay = if (debtLeft == 0L) 0 else state.debtDueDay,
            )
        }
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
        is Action.Store -> {
            val left = state.held(action.good) - action.quantity
            val godown = state.godowns[state.city].orEmpty() +
                (action.good to state.stored(state.city, action.good) + action.quantity)
            state.copy(
                holdings = if (left > 0) state.holdings + (action.good to left) else state.holdings - action.good,
                godowns = state.godowns + (state.city to godown),
            )
        }
        is Action.Retrieve -> {
            val left = state.stored(state.city, action.good) - action.quantity
            val godown = if (left > 0) {
                state.godowns.getValue(state.city) + (action.good to left)
            } else {
                state.godowns.getValue(state.city) - action.good
            }
            state.copy(
                holdings = state.holdings + (action.good to state.held(action.good) + action.quantity),
                godowns = if (godown.isEmpty()) state.godowns - state.city else state.godowns + (state.city to godown),
            )
        }
        is Action.SellStored -> {
            // Selling from the store hits the same mandi: it moves the price, then pays it.
            val moved = saturating(state, state.city, action.good, -action.quantity * GameConstants.SATURATION_BP_PER_UNIT)
            val left = moved.stored(state.city, action.good) - action.quantity
            val godown = if (left > 0) {
                moved.godowns.getValue(state.city) + (action.good to left)
            } else {
                moved.godowns.getValue(state.city) - action.good
            }
            moved.copy(
                cashRupees = moved.cashRupees + moved.priceOf(action.good) * action.quantity,
                godowns = if (godown.isEmpty()) moved.godowns - state.city else moved.godowns + (state.city to godown),
            )
        }
        Action.PayCollector -> {
            // Cash first, then the bank — the collector takes the whole debt and the
            // day's keep in one visit, so paying with an empty pocket cannot re-indebt.
            val total = state.debtRupees + state.livingCostPerDay
            val fromCash = minOf(state.cashRupees, total)
            state.copy(
                cashRupees = state.cashRupees - fromCash,
                bankRupees = state.bankRupees - (total - fromCash),
                debtRupees = 0,
                debtDueDay = 0,
            )
        }
    }

    /**
     * The godowns bill per day that passes. Cash pays what it can; the godown man knows a
     * collector, so what cash cannot cover lands on the sahukar debt — at its interest.
     */
    private fun chargingRent(state: RunState, days: Int): RunState {
        val rent = state.godownRentPerDay * days
        if (rent <= 0) return state
        val paid = minOf(rent, state.cashRupees)
        return state.copy(
            cashRupees = state.cashRupees - paid,
            debtRupees = state.debtRupees + (rent - paid),
            debtDueDay = debtDueDayAfter(state, rent - paid),
        )
    }

    /**
     * Debt taken without borrowing — unpaid keep, unpaid rent — starts the collector's
     * clock like a loan does. A clock already ticking keeps the sooner day.
     */
    private fun debtDueDayAfter(state: RunState, shortfall: Long): Int =
        if (shortfall > 0 && state.debtDueDay == 0) {
            state.day + GameConstants.COLLECTOR_AFTER_DAYS
        } else {
            state.debtDueDay
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
            if (pick < 65) return bandh(state)
            if (pick < 85) return rumour(state, random)
            return priceSwing(state, random)
        }
        if (pick < 40) return pickpocket(state, random)
        if (pick < 70) return rumour(state, random)
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

    private fun bandh(state: RunState): GameEvent =
        GameEvent.Bandh(GameConstants.BANDH_EXTRA_DAYS)

    private fun priceSwing(state: RunState, random: Random): GameEvent {
        val headline = EventHeadline.entries.random(random)
        val basisPoints = if (headline == EventHeadline.BUMPER_HARVEST) {
            -random.nextInt(GameConstants.PRICE_CRASH_BP_MIN, GameConstants.PRICE_CRASH_BP_MAX + 1)
        } else {
            random.nextInt(GameConstants.PRICE_SPIKE_BP_MIN, GameConstants.PRICE_SPIKE_BP_MAX + 1)
        }
        return GameEvent.PriceSwing(state.city, GoodId.entries.random(random), basisPoints, headline)
    }

    /**
     * Word on the street about any city, any good: usually dear, sometimes cheap, lands in
     * a few days — and a quarter of the time it is only talk. [RumourTip.lands] is what
     * the trader cannot know until the day comes.
     */
    private fun rumour(state: RunState, random: Random): GameEvent.RumourTip {
        val up = random.nextDouble() >= GameConstants.RUMOUR_DOWN_CHANCE
        val basisPoints = random.nextInt(GameConstants.RUMOUR_BP_MIN, GameConstants.RUMOUR_BP_MAX + 1) *
            (if (up) 1 else -1)
        return GameEvent.RumourTip(
            city = CityId.entries.random(random),
            good = GoodId.entries.random(random),
            up = up,
            landsOnDay = state.day + random.nextInt(GameConstants.RUMOUR_DAYS_MIN, GameConstants.RUMOUR_DAYS_MAX + 1),
            basisPoints = basisPoints,
            lands = random.nextDouble() >= GameConstants.RUMOUR_DUD_CHANCE,
        )
    }

    /** Rumours whose day arrived become real shifts, told as landings; the rest keep waiting. */
    private fun landingRumours(state: RunState): Pair<RunState, List<GameEvent>> {
        val due = state.rumours.filter { it.landsOnDay <= state.day }
        if (due.isEmpty()) return state to emptyList()
        var moved = state.copy(rumours = state.rumours - due)
        for (rumour in due) {
            moved = saturating(moved, rumour.city, rumour.good, rumour.basisPoints)
        }
        return moved to due.map { GameEvent.RumourLanded(it.city, it.good, it.basisPoints) }
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
            // The extra day costs what a day costs: a day, a day's interest on both sides,
            // a day's rent on whatever the godowns hold, and a day's keep where you're stuck.
            chargingLiving(
                chargingRent(
                    state.copy(
                        day = state.day + event.extraDays,
                        debtRupees = state.debtRupees.withInterest(GameConstants.SAHUKAR_DAILY_INTEREST, event.extraDays),
                        bankRupees = state.bankRupees.withInterest(GameConstants.BANK_DAILY_INTEREST, event.extraDays),
                    ),
                    event.extraDays,
                ),
                event.extraDays,
            )
        is GameEvent.PriceSwing -> saturating(state, event.city, event.good, event.basisPoints)
        is GameEvent.RumourTip ->
            // A true rumour joins the waiting list; a dud was never anything but talk.
            if (event.lands) {
                state.copy(
                    rumours = state.rumours +
                        Rumour(event.city, event.good, event.landsOnDay, event.basisPoints),
                )
            } else {
                state
            }
        is GameEvent.RumourLanded -> state // only ever told, never drawn
    }

    private fun Long.withInterest(dailyRate: Double, days: Int): Long =
        if (this == 0L) 0L else (this * (1 + dailyRate).pow(days)).roundToLong()
}
