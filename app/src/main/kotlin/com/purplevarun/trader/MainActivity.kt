package com.purplevarun.trader

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.purplevarun.trader.engine.CityId
import com.purplevarun.trader.engine.GameConstants
import com.purplevarun.trader.ui.HomeScreen
import com.purplevarun.trader.ui.HomeUiState

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // The paper background is always light, so the system bar icons must always be dark.
        val darkIcons = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = darkIcons, navigationBarStyle = darkIcons)
        super.onCreate(savedInstanceState)
        setContent { HomeScreen(PLACEHOLDER_STATE) }
    }

    private companion object {
        /** Stand-in figures until the engine tracks a real run. */
        val PLACEHOLDER_STATE = HomeUiState(
            netWorthRupees = 91_400,
            cashRupees = 48_250,
            debtRupees = 12_400,
            dayOfRun = 12,
            runLengthDays = GameConstants.DEFAULT_RUN_LENGTH_DAYS,
            currentCity = CityId.MUMBAI,
        )
    }
}
