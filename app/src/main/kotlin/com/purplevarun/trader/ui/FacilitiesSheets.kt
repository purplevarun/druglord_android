package com.purplevarun.trader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.purplevarun.trader.R
import com.purplevarun.trader.engine.Action
import com.purplevarun.trader.engine.CityId
import com.purplevarun.trader.engine.Game
import com.purplevarun.trader.engine.GameConstants
import com.purplevarun.trader.engine.GoodId
import com.purplevarun.trader.engine.Outcome
import com.purplevarun.trader.engine.RunState

/**
 * The sahukar's sheet: how much you owe, what the days cost, and the amounts he will
 * lend or take back in a tap.
 */
@Composable
fun SahukarSheet(state: RunState, onAction: (Action) -> Unit, onDismiss: () -> Unit) {
    Sheet(
        onDismiss = onDismiss,
        testTag = TestTags.SAHUKAR_SHEET,
        content = {
            TitleLine(
                title = stringResource(R.string.place_sahukar),
                status = if (state.debtRupees > 0) {
                    stringResource(R.string.you_owe, formatRupees(state.debtRupees))
                } else {
                    stringResource(R.string.you_owe_nothing)
                },
            )
            Spacer(Modifier.height(UiDimens.SheetLineGap))
            BasicText(stringResource(R.string.sahukar_blurb), style = UiType.Body)
            Spacer(Modifier.height(UiDimens.SheetLineGap))
            BasicText(
                stringResource(R.string.cash_you_carry, formatRupees(state.cashRupees)),
                style = UiType.Body,
            )
        },
        actions = {
            AmountRow(verb = stringResource(R.string.action_borrow)) {
                for (step in GameConstants.TRANSACTION_STEPS_RUPEES) {
                    AmountChip(
                        text = formatRupees(step),
                        label = stringResource(R.string.borrow_amount, formatRupees(step)),
                        enabled = Game.check(state, Action.Borrow(step)) == null,
                        onClick = { onAction(Action.Borrow(step)) },
                        modifier = Modifier.testTag(TestTags.borrow(step)),
                    )
                }
            }
            AmountRow(verb = stringResource(R.string.action_repay)) {
                for (step in GameConstants.TRANSACTION_STEPS_RUPEES) {
                    AmountChip(
                        text = formatRupees(step),
                        label = stringResource(R.string.repay_amount, formatRupees(step)),
                        enabled = Game.check(state, Action.Repay(step)) == null,
                        onClick = { onAction(Action.Repay(step)) },
                        modifier = Modifier.testTag(TestTags.repay(step)),
                    )
                }
                val all = minOf(state.cashRupees, state.debtRupees)
                AmountChip(
                    text = stringResource(R.string.action_all),
                    label = stringResource(R.string.repay_all, formatRupees(all)),
                    enabled = all > 0,
                    onClick = { onAction(Action.Repay(all)) },
                    modifier = Modifier.testTag(TestTags.REPAY_ALL),
                )
            }
            TextButton(
                text = stringResource(R.string.close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

/**
 * The bank's sheet: what is saved, what it earns, and the amounts that move in or out.
 * Savings travel with the account, not the pocket: they are safe wherever you are.
 */
@Composable
fun BankSheet(state: RunState, onAction: (Action) -> Unit, onDismiss: () -> Unit) {
    Sheet(
        onDismiss = onDismiss,
        testTag = TestTags.BANK_SHEET,
        content = {
            TitleLine(
                title = stringResource(R.string.place_bank),
                status = if (state.bankRupees > 0) {
                    stringResource(R.string.in_the_bank, formatRupees(state.bankRupees))
                } else {
                    stringResource(R.string.in_the_bank_nothing)
                },
            )
            Spacer(Modifier.height(UiDimens.SheetLineGap))
            BasicText(stringResource(R.string.bank_blurb), style = UiType.Body)
            Spacer(Modifier.height(UiDimens.SheetLineGap))
            BasicText(
                stringResource(R.string.cash_you_carry, formatRupees(state.cashRupees)),
                style = UiType.Body,
            )
        },
        actions = {
            AmountRow(verb = stringResource(R.string.action_deposit)) {
                for (step in GameConstants.TRANSACTION_STEPS_RUPEES) {
                    AmountChip(
                        text = formatRupees(step),
                        label = stringResource(R.string.deposit_amount, formatRupees(step)),
                        enabled = Game.check(state, Action.Deposit(step)) == null,
                        onClick = { onAction(Action.Deposit(step)) },
                        modifier = Modifier.testTag(TestTags.deposit(step)),
                    )
                }
                AmountChip(
                    text = stringResource(R.string.action_all),
                    label = stringResource(R.string.deposit_all, formatRupees(state.cashRupees)),
                    enabled = state.cashRupees > 0,
                    onClick = { onAction(Action.Deposit(state.cashRupees)) },
                    modifier = Modifier.testTag(TestTags.DEPOSIT_ALL),
                )
            }
            AmountRow(verb = stringResource(R.string.action_withdraw)) {
                for (step in GameConstants.TRANSACTION_STEPS_RUPEES) {
                    AmountChip(
                        text = formatRupees(step),
                        label = stringResource(R.string.withdraw_amount, formatRupees(step)),
                        enabled = Game.check(state, Action.Withdraw(step)) == null,
                        onClick = { onAction(Action.Withdraw(step)) },
                        modifier = Modifier.testTag(TestTags.withdraw(step)),
                    )
                }
                AmountChip(
                    text = stringResource(R.string.action_all),
                    label = stringResource(R.string.withdraw_all, formatRupees(state.bankRupees)),
                    enabled = state.bankRupees > 0,
                    onClick = { onAction(Action.Withdraw(state.bankRupees)) },
                    modifier = Modifier.testTag(TestTags.WITHDRAW_ALL),
                )
            }
            TextButton(
                text = stringResource(R.string.close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

/**
 * The luggage shop's sheet: what you carry in now, and what bigger transports cost.
 * Only upgrades are for sale; the shop does not trade down.
 */
@Composable
fun LuggageSheet(state: RunState, onAction: (Action) -> Unit, onDismiss: () -> Unit) {
    Sheet(
        onDismiss = onDismiss,
        testTag = TestTags.LUGGAGE_SHEET,
        content = {
            TitleLine(
                title = stringResource(R.string.place_luggage),
                status = stringResource(
                    R.string.carrying_now,
                    stringResource(transportNameRes(state.transport)),
                    state.luggageUsed,
                    state.luggageCapacity,
                ),
            )
            Spacer(Modifier.height(UiDimens.SheetLineGap))
            val upgrades = GameConstants.TRANSPORTS.filter { it.capacity > state.luggageCapacity }
            if (upgrades.isEmpty()) {
                BasicText(stringResource(R.string.nothing_bigger), style = UiType.Body)
            } else {
                for (upgrade in upgrades) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = UiDimens.UpgradeRowGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            BasicText(
                                stringResource(transportNameRes(upgrade.id)),
                                style = UiType.GoodName,
                            )
                            BasicText(
                                stringResource(
                                    R.string.upgrade_line,
                                    upgrade.capacity,
                                    formatRupees(upgrade.priceRupees),
                                ),
                                style = UiType.Body,
                            )
                        }
                        val name = stringResource(transportNameRes(upgrade.id))
                        AmountChip(
                            text = stringResource(R.string.action_buy),
                            label = stringResource(R.string.buy_transport, name),
                            enabled = Game.check(state, Action.BuyTransport(upgrade.id)) == null,
                            onClick = { onAction(Action.BuyTransport(upgrade.id)) },
                            modifier = Modifier.testTag(TestTags.buyTransport(upgrade.id)),
                        )
                    }
                }
            }
        },
        actions = {
            TextButton(
                text = stringResource(R.string.close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

/**
 * The godown's sheet: what is stored in this city (and what it costs per day), what you
 * carry that could be stored, and where else goods sit — those are only looked at here,
 * never touched from afar.
 */
@Composable
fun GodownSheet(state: RunState, onAction: (Action) -> Unit, onDismiss: () -> Unit) {
    val storedHere = state.godowns[state.city].orEmpty()
    Sheet(
        onDismiss = onDismiss,
        testTag = TestTags.GODOWN_SHEET,
        content = {
            TitleLine(
                title = stringResource(R.string.place_godown),
                status = if (storedHere.isEmpty()) {
                    stringResource(R.string.godown_nothing)
                } else {
                    stringResource(
                        R.string.godown_stored,
                        state.godownUsed(state.city),
                        GameConstants.GODOWN_DAILY_RENT_RUPEES,
                    )
                },
            )
            Spacer(Modifier.height(UiDimens.SheetLineGap))
            BasicText(
                stringResource(R.string.godown_blurb, GameConstants.GODOWN_DAILY_RENT_RUPEES),
                style = UiType.Body,
            )

            Spacer(Modifier.height(UiDimens.SectionGap))
            BasicText(stringResource(R.string.godown_here), style = UiType.Overline)
            Spacer(Modifier.height(UiDimens.SheetLineGap))
            if (storedHere.isEmpty()) {
                BasicText(stringResource(R.string.godown_here_empty), style = UiType.Body)
            } else {
                for ((good, quantity) in storedHere) {
                    Spacer(Modifier.height(UiDimens.SheetLineGap))
                    BasicText(
                        stringResource(R.string.stored_count, stringResource(goodNameRes(good)), quantity),
                        style = UiType.GoodName,
                    )
                    AmountRow(verb = stringResource(R.string.action_take)) {
                        for (units in GameConstants.GODOWN_STEPS_UNITS) {
                            AmountChip(
                                text = "$units",
                                label = stringResource(R.string.take_units, units, stringResource(goodNameRes(good))),
                                enabled = Game.check(state, Action.Retrieve(good, units)) == null,
                                onClick = { onAction(Action.Retrieve(good, units)) },
                                modifier = Modifier.testTag(TestTags.take(good, units)),
                            )
                        }
                        AmountChip(
                            text = stringResource(R.string.action_all),
                            label = stringResource(R.string.take_all, stringResource(goodNameRes(good))),
                            enabled = Game.check(state, Action.Retrieve(good, quantity)) == null,
                            onClick = { onAction(Action.Retrieve(good, quantity)) },
                            modifier = Modifier.testTag(TestTags.takeAll(good)),
                        )
                    }
                    AmountRow(verb = stringResource(R.string.action_sell)) {
                        AmountChip(
                            text = stringResource(R.string.action_all),
                            label = stringResource(R.string.sell_stored_all, stringResource(goodNameRes(good))),
                            enabled = Game.check(state, Action.SellStored(good, quantity)) == null,
                            onClick = { onAction(Action.SellStored(good, quantity)) },
                            modifier = Modifier.testTag(TestTags.sellAllStored(good)),
                        )
                    }
                }
            }

            Spacer(Modifier.height(UiDimens.SectionGap))
            BasicText(
                stringResource(R.string.godown_carried, stringResource(transportNameRes(state.transport)).lowercase()),
                style = UiType.Overline,
            )
            Spacer(Modifier.height(UiDimens.SheetLineGap))
            if (state.holdings.isEmpty()) {
                BasicText(stringResource(R.string.godown_carried_empty), style = UiType.Body)
            } else {
                for ((good, quantity) in state.holdings) {
                    Spacer(Modifier.height(UiDimens.SheetLineGap))
                    BasicText(
                        stringResource(R.string.carried_count, stringResource(goodNameRes(good)), quantity),
                        style = UiType.GoodName,
                    )
                    AmountRow(verb = stringResource(R.string.action_store)) {
                        for (units in GameConstants.GODOWN_STEPS_UNITS) {
                            AmountChip(
                                text = "$units",
                                label = stringResource(R.string.store_units, units, stringResource(goodNameRes(good))),
                                enabled = Game.check(state, Action.Store(good, units)) == null,
                                onClick = { onAction(Action.Store(good, units)) },
                                modifier = Modifier.testTag(TestTags.store(good, units)),
                            )
                        }
                        AmountChip(
                            text = stringResource(R.string.action_all),
                            label = stringResource(R.string.store_all, stringResource(goodNameRes(good))),
                            enabled = Game.check(state, Action.Store(good, quantity)) == null,
                            onClick = { onAction(Action.Store(good, quantity)) },
                            modifier = Modifier.testTag(TestTags.storeAll(good)),
                        )
                    }
                }
            }

            val elsewhere = state.godowns.filterKeys { it != state.city }
            if (elsewhere.isNotEmpty()) {
                Spacer(Modifier.height(UiDimens.SectionGap))
                BasicText(
                    stringResource(R.string.godown_elsewhere, elsewhereSummary(elsewhere)),
                    style = UiType.Body,
                )
            }
        },
        actions = {
            TextButton(
                text = stringResource(R.string.close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.GODOWN_CLOSE),
            )
        },
    )
}

/** "Delhi · onions ×30, kesar ×5; Mumbai · gold ×1" — what sits in godowns elsewhere. */
@Composable
private fun elsewhereSummary(elsewhere: Map<CityId, Map<GoodId, Int>>): String {
    val separator = stringResource(R.string.list_separator)
    val parts = mutableListOf<String>()
    for ((city, goods) in elsewhere) {
        val goodsParts = mutableListOf<String>()
        for ((good, quantity) in goods) {
            goodsParts += stringResource(
                R.string.godown_goods_count,
                stringResource(goodNameRes(good)),
                quantity,
            )
        }
        parts += stringResource(
            R.string.godown_elsewhere_entry,
            stringResource(cityNameRes(city)),
            goodsParts.joinToString(separator),
        )
    }
    return parts.joinToString(separator)
}

/**
 * One money action as it reads in a sheet: the verb, then the amounts it comes in.
 * Chips flow to the next line when the row runs out of room.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmountRow(verb: String, chips: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UiDimens.ChipGap),
        verticalArrangement = Arrangement.spacedBy(UiDimens.ChipGap),
    ) {
        BasicText(
            verb,
            style = UiType.Price,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        chips()
    }
}

/** A sheet's name on the left and its standing on the right, wrapping when narrow. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TitleLine(title: String, status: String) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(UiDimens.SheetLineGap),
    ) {
        BasicText(title, style = UiType.SheetTitle, modifier = Modifier.align(Alignment.CenterVertically))
        BasicText(status, style = UiType.Body, modifier = Modifier.align(Alignment.CenterVertically))
    }
}
