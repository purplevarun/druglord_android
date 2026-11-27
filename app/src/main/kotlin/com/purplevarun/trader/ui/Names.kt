package com.purplevarun.trader.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.purplevarun.trader.R
import com.purplevarun.trader.engine.CityId
import com.purplevarun.trader.engine.GameCalendar
import com.purplevarun.trader.engine.GoodId
import com.purplevarun.trader.engine.TradeUnit

@StringRes
fun cityNameRes(id: CityId): Int = when (id) {
    CityId.MUMBAI -> R.string.city_mumbai
    CityId.DELHI -> R.string.city_delhi
    CityId.CALCUTTA -> R.string.city_calcutta
    CityId.SRINAGAR -> R.string.city_srinagar
    CityId.BANGALORE -> R.string.city_bangalore
    CityId.HYDERABAD -> R.string.city_hyderabad
}

@StringRes
fun goodNameRes(id: GoodId): Int = when (id) {
    GoodId.ONIONS -> R.string.good_onions
    GoodId.COTTON -> R.string.good_cotton
    GoodId.BASMATI -> R.string.good_basmati
    GoodId.CHAI -> R.string.good_chai
    GoodId.MASALA -> R.string.good_masala
    GoodId.SILK_SAREES -> R.string.good_silk_sarees
    GoodId.KESAR -> R.string.good_kesar
    GoodId.GOLD -> R.string.good_gold
}

@StringRes
fun unitNameRes(unit: TradeUnit): Int = when (unit) {
    TradeUnit.QUINTAL -> R.string.unit_quintal
    TradeUnit.BUNDLE_OF_TEN -> R.string.unit_bundle_of_ten
    TradeUnit.KILOGRAM -> R.string.unit_kilogram
    TradeUnit.HUNDRED_GRAMS -> R.string.unit_hundred_grams
}

/** The calendar date of a day of the run, such as "12 Jan 2000". */
@Composable
fun dateText(dayOfRun: Int): String {
    val date = GameCalendar.dateOn(dayOfRun)
    val month = stringArrayResource(R.array.month_abbreviations)[date.month - 1]
    return stringResource(R.string.date_format, date.day, month, date.year)
}
