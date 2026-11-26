package com.purplevarun.trader.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GameCalendarTest {

    @Test
    fun `day 1 is 1 January 2000`() {
        assertEquals(GameDate(day = 1, month = 1, year = 2000), GameCalendar.dateOn(1))
    }

    @Test
    fun `day 12 is 12 January 2000`() {
        assertEquals(GameDate(day = 12, month = 1, year = 2000), GameCalendar.dateOn(12))
    }

    @Test
    fun `day 32 rolls over into February`() {
        assertEquals(GameDate(day = 1, month = 2, year = 2000), GameCalendar.dateOn(32))
    }

    @Test
    fun `2000 is a leap year so day 60 is 29 February`() {
        assertEquals(GameDate(day = 29, month = 2, year = 2000), GameCalendar.dateOn(60))
        assertEquals(GameDate(day = 1, month = 3, year = 2000), GameCalendar.dateOn(61))
    }

    @Test
    fun `an endless run rolls over into 2001, which is not a leap year`() {
        assertEquals(GameDate(day = 31, month = 12, year = 2000), GameCalendar.dateOn(366))
        assertEquals(GameDate(day = 1, month = 1, year = 2001), GameCalendar.dateOn(367))
        assertEquals(GameDate(day = 1, month = 3, year = 2001), GameCalendar.dateOn(366 + 31 + 28 + 1))
    }

    @Test
    fun `there is no day zero`() {
        assertThrows(IllegalArgumentException::class.java) { GameCalendar.dateOn(0) }
    }
}
