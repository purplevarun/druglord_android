package com.purplevarun.trader

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.purplevarun.trader.engine.RunState
import com.purplevarun.trader.ui.GameScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // The paper background is always light, so the system bar icons must always be dark.
        val darkIcons = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = darkIcons, navigationBarStyle = darkIcons)
        super.onCreate(savedInstanceState)
        setContent { GameScreen(newRun = { RunState.newRun(seed = System.nanoTime()) }) }
    }
}
