package com.purplevarun.trader.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RupeesTest {

    @Test
    fun `amounts under a thousand have no separators`() {
        assertEquals("₹0", formatRupees(0))
        assertEquals("₹420", formatRupees(420))
    }

    @Test
    fun `thousands get one separator`() {
        assertEquals("₹1,825", formatRupees(1_825))
        assertEquals("₹91,400", formatRupees(91_400))
    }

    @Test
    fun `lakhs and crores group in twos after the first three digits`() {
        assertEquals("₹1,00,000", formatRupees(100_000))
        assertEquals("₹12,84,000", formatRupees(1_284_000))
        assertEquals("₹1,23,45,678", formatRupees(12_345_678))
    }

    @Test
    fun `a negative amount keeps its sign in front of the rupee sign`() {
        assertEquals("-₹12,400", formatRupees(-12_400))
    }
}
