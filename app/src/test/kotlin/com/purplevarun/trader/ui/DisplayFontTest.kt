package com.purplevarun.trader.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.rememberTextMeasurer
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The heading font is one variable font file holding every weight. If the bold weight is not
 * actually selected from it, bold text silently renders as regular; bold letters are wider, so
 * the widths tell the two apart.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class DisplayFontTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the current city's name is drawn heavier than the other cities`() {
        var regularWidth = 0
        var boldWidth = 0
        compose.setContent {
            val measurer = rememberTextMeasurer()
            regularWidth = measurer.measure(SAMPLE, UiType.MapLabel).size.width
            boldWidth = measurer.measure(SAMPLE, UiType.MapLabelCurrent).size.width
        }
        compose.waitForIdle()

        assertTrue(
            "bold text ($boldWidth px) should be wider than regular text ($regularWidth px)",
            boldWidth > regularWidth,
        )
    }

    private companion object {
        const val SAMPLE = "Hyderabad Bangalore"
    }
}
