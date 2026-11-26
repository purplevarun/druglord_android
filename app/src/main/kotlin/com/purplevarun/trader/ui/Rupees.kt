package com.purplevarun.trader.ui

import kotlin.math.abs

private const val RUPEE_SIGN = "₹"
private const val SEPARATOR = ","
private const val FIRST_GROUP_DIGITS = 3
private const val LATER_GROUP_DIGITS = 2

/** Formats an amount the Indian way: the last three digits, then groups of two (12,84,000). */
fun formatRupees(amount: Long): String {
    val digits = abs(amount).toString()
    val grouped = if (digits.length <= FIRST_GROUP_DIGITS) {
        digits
    } else {
        val lastThree = digits.takeLast(FIRST_GROUP_DIGITS)
        val rest = digits.dropLast(FIRST_GROUP_DIGITS)
        val restGrouped = rest.reversed().chunked(LATER_GROUP_DIGITS).joinToString(SEPARATOR).reversed()
        restGrouped + SEPARATOR + lastThree
    }
    val sign = if (amount < 0) "-" else ""
    return sign + RUPEE_SIGN + grouped
}
