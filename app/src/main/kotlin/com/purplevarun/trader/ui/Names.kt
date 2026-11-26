package com.purplevarun.trader.ui

import androidx.annotation.StringRes
import com.purplevarun.trader.R
import com.purplevarun.trader.engine.CityId

@StringRes
fun cityNameRes(id: CityId): Int = when (id) {
    CityId.MUMBAI -> R.string.city_mumbai
    CityId.DELHI -> R.string.city_delhi
    CityId.CALCUTTA -> R.string.city_calcutta
    CityId.CHENNAI -> R.string.city_chennai
    CityId.JAIPUR -> R.string.city_jaipur
    CityId.KOCHI -> R.string.city_kochi
    CityId.VARANASI -> R.string.city_varanasi
    CityId.SRINAGAR -> R.string.city_srinagar
}
