package com.purplevarun.trader.engine

object GameCalendar {

    private const val FEBRUARY = 2
    private const val DECEMBER = 12
    private val DAYS_IN_MONTH = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    /** The calendar date of the given day of a run. Day 1 is the start date. */
    fun dateOn(dayOfRun: Int): GameDate {
        require(dayOfRun >= 1) { "Runs start on day 1, got $dayOfRun" }
        var year = GameConstants.START_YEAR
        var month = GameConstants.START_MONTH
        var day = GameConstants.START_DAY_OF_MONTH + (dayOfRun - 1)
        while (day > daysIn(month, year)) {
            day -= daysIn(month, year)
            if (month == DECEMBER) {
                month = 1
                year++
            } else {
                month++
            }
        }
        return GameDate(day, month, year)
    }

    private fun daysIn(month: Int, year: Int): Int =
        if (month == FEBRUARY && isLeapYear(year)) 29 else DAYS_IN_MONTH[month - 1]

    private fun isLeapYear(year: Int): Boolean =
        year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
}
