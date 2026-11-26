package com.purplevarun.trader.ui

import androidx.annotation.StringRes
import com.purplevarun.trader.R
import com.purplevarun.trader.engine.CityId

@StringRes
fun cityNameRes(id: CityId): Int = when (id) {
    CityId.MUMBAI -> R.string.city_mumbai
    CityId.DELHI -> R.string.city_delhi
    CityId.CALCUTTA -> R.string.city_calcutta
    CityId.SRINAGAR -> R.string.city_srinagar
    CityId.BANGALORE -> R.string.city_bangalore
    CityId.HYDERABAD -> R.string.city_hyderabad
}
