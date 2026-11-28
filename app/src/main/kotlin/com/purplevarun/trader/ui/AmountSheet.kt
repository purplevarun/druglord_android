package com.purplevarun.trader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.purplevarun.trader.R
import com.purplevarun.trader.engine.Action
import com.purplevarun.trader.engine.Game
import com.purplevarun.trader.engine.GameConstants
import com.purplevarun.trader.engine.GoodId
import com.purplevarun.trader.engine.RunState

/** What the keypad is counting and what confirming it does. */
sealed interface KeypadSpec {
    /** A market good: both "Buy N" and "Sell N" are offered. */
    data class Market(val good: GoodId) : KeypadSpec

    /** Rupees at the sahukar or the bank. */
    data class Money(val verb: MoneyVerb) : KeypadSpec

    /** Units of one good in or out of this city's godown, or sold straight from it. */
    data class Godown(val good: GoodId, val verb: GodownVerb) : KeypadSpec
}

enum class MoneyVerb { BORROW, REPAY, DEPOSIT, WITHDRAW }
enum class GodownVerb { STORE, TAKE, SELL }

private const val MAX_DIGITS = 12

/**
 * The exact-amount keypad: digits tapped into a number, the count shown beside the
 * name, and one or two confirm buttons that light up only when the engine would
 * take the move. Opened from an "exact" chip or by holding an amount chip.
 */
@Composable
fun AmountSheet(
    state: RunState,
    spec: KeypadSpec,
    onAction: (Action) -> Unit,
    onDismiss: () -> Unit,
) {
    var digits by rememberSaveable { mutableStateOf("") }
    val amount = digits.toLongOrNull() ?: 0L
    val confirms = confirmsFor(spec, amount)
    Sheet(
        onDismiss = onDismiss,
        testTag = TestTags.KEYPAD_SHEET,
        content = {
            TitleLine(title = keypadTitle(spec), status = amountStatus(spec, amount))
            Spacer(Modifier.height(UiDimens.SectionGap))
            KeypadGrid(
                onDigit = { if (digits.length < MAX_DIGITS) digits += it },
                onDelete = { digits = digits.dropLast(1) },
            )
        },
        actions = {
            confirms.forEachIndexed { index, confirm ->
                PrimaryButton(
                    text = confirm.text,
                    enabled = Game.check(state, confirm.action) == null,
                    onClick = { onAction(confirm.action); digits = "" },
                    modifier = Modifier.testTag(
                        if (index == 0) TestTags.AMOUNT_CONFIRM else TestTags.AMOUNT_CONFIRM_2
                    ),
                )
                Spacer(Modifier.height(UiDimens.SheetLineGap))
            }
            TextButton(
                text = stringResource(R.string.close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

private data class AmountConfirm(val text: String, val action: Action)

@Composable
private fun confirmsFor(spec: KeypadSpec, amount: Long): List<AmountConfirm> {
    val units = amount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    return when (spec) {
        is KeypadSpec.Market -> {
            val name = stringResource(goodNameRes(spec.good))
            listOf(
                AmountConfirm(
                    stringResource(R.string.buy_amount_units, units, name),
                    Action.Buy(spec.good, units),
                ),
                AmountConfirm(
                    stringResource(R.string.sell_amount_units, units, name),
                    Action.Sell(spec.good, units),
                ),
            )
        }
        is KeypadSpec.Money -> {
            val (textRes, action) = when (spec.verb) {
                MoneyVerb.BORROW -> R.string.borrow_amount to Action.Borrow(amount)
                MoneyVerb.REPAY -> R.string.repay_amount to Action.Repay(amount)
                MoneyVerb.DEPOSIT -> R.string.deposit_amount to Action.Deposit(amount)
                MoneyVerb.WITHDRAW -> R.string.withdraw_amount to Action.Withdraw(amount)
            }
            listOf(AmountConfirm(stringResource(textRes, formatRupees(amount)), action))
        }
        is KeypadSpec.Godown -> {
            val name = stringResource(goodNameRes(spec.good))
            val (textRes, action) = when (spec.verb) {
                GodownVerb.STORE -> R.string.store_units to Action.Store(spec.good, units)
                GodownVerb.TAKE -> R.string.take_units to Action.Retrieve(spec.good, units)
                GodownVerb.SELL -> R.string.sell_stored_amount to Action.SellStored(spec.good, units)
            }
            listOf(AmountConfirm(stringResource(textRes, units, name), action))
        }
    }
}

@Composable
private fun keypadTitle(spec: KeypadSpec): String = when (spec) {
    is KeypadSpec.Market -> stringResource(goodNameRes(spec.good))
    is KeypadSpec.Money -> stringResource(
        when (spec.verb) {
            MoneyVerb.BORROW -> R.string.action_borrow
            MoneyVerb.REPAY -> R.string.action_repay
            MoneyVerb.DEPOSIT -> R.string.action_deposit
            MoneyVerb.WITHDRAW -> R.string.action_withdraw
        }
    )
    is KeypadSpec.Godown -> stringResource(goodNameRes(spec.good))
}

@Composable
private fun amountStatus(spec: KeypadSpec, amount: Long): String {
    val goodId = when (spec) {
        is KeypadSpec.Market -> spec.good
        is KeypadSpec.Godown -> spec.good
        is KeypadSpec.Money -> return formatRupees(amount)
    }
    val good = GameConstants.GOODS.first { it.id == goodId }
    return "$amount ${stringResource(unitNameRes(good.unit))}"
}

@Composable
private fun KeypadGrid(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
    for (row in rows + listOf(listOf(null, "0", "del"))) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UiDimens.ChipGap, Alignment.CenterHorizontally),
        ) {
            for (key in row) {
                when (key) {
                    null -> Spacer(Modifier.width(UiDimens.MinTouchTarget))
                    "del" -> AmountChip(
                        text = stringResource(R.string.keypad_delete),
                        label = stringResource(R.string.keypad_delete),
                        enabled = true,
                        onClick = onDelete,
                        modifier = Modifier.testTag(TestTags.key("del")),
                    )
                    else -> AmountChip(
                        text = key,
                        enabled = true,
                        onClick = { onDigit(key) },
                        modifier = Modifier.testTag(TestTags.key(key)),
                    )
                }
            }
        }
        Spacer(Modifier.height(UiDimens.ChipGap))
    }
}
