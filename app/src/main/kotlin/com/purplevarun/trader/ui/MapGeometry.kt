package com.purplevarun.trader.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.purplevarun.trader.engine.CityId
import kotlin.math.min

/** How real coordinates become points on the map, and the outline of the country. */
object MapGeometry {

    // The window of the world the map shows, in degrees.
    private const val WEST = 67.5
    private const val EAST = 97.5
    private const val SOUTH = 7.5
    private const val NORTH = 37.5

    /** Width over height. A degree of longitude is shorter than a degree of latitude at these latitudes. */
    const val ASPECT_RATIO = 0.92f

    enum class LabelSide { START, END, ABOVE, ABOVE_END }

    /** Which side of its dot each city's name sits on, chosen so names stay clear of the rail lines. */
    val LABEL_SIDE: Map<CityId, LabelSide> = mapOf(
        CityId.MUMBAI to LabelSide.END,
        CityId.DELHI to LabelSide.ABOVE_END,
        CityId.CALCUTTA to LabelSide.END,
        CityId.CHENNAI to LabelSide.END,
        CityId.JAIPUR to LabelSide.START,
        CityId.KOCHI to LabelSide.START,
        CityId.VARANASI to LabelSide.ABOVE,
        CityId.SRINAGAR to LabelSide.END,
    )

    /** A simplified outline of India as longitude to latitude, clockwise from the north. */
    val INDIA_OUTLINE: List<Pair<Double, Double>> = listOf(
        74.0 to 37.0, 78.0 to 35.5, 79.0 to 32.5, 81.0 to 30.2, 84.0 to 28.5, 88.0 to 27.5,
        89.0 to 26.5, 92.0 to 27.8, 96.0 to 29.3, 97.0 to 27.5, 95.0 to 25.0, 93.5 to 22.0,
        92.0 to 23.5, 91.5 to 25.0, 89.5 to 25.5, 88.8 to 24.0, 89.0 to 21.8, 87.0 to 21.3,
        85.0 to 19.5, 82.3 to 16.6, 80.2 to 15.0, 80.3 to 13.0, 79.8 to 10.3, 77.5 to 8.1,
        76.3 to 9.5, 74.8 to 13.0, 73.3 to 17.0, 72.8 to 20.0, 72.6 to 21.2, 70.5 to 20.8,
        69.0 to 22.3, 68.3 to 23.6, 70.5 to 24.5, 70.0 to 27.0, 72.5 to 29.5, 74.3 to 31.5,
        74.0 to 34.0, 73.5 to 36.0,
    )

    /** The largest rectangle of the map's shape that fits inside [size] with [padding] on every side, centred. */
    fun fit(size: Size, padding: Float): Rect {
        val availableWidth = (size.width - 2 * padding).coerceAtLeast(0f)
        val availableHeight = (size.height - 2 * padding).coerceAtLeast(0f)
        val width = min(availableWidth, availableHeight * ASPECT_RATIO)
        val height = width / ASPECT_RATIO
        val left = (size.width - width) / 2
        val top = (size.height - height) / 2
        return Rect(left, top, left + width, top + height)
    }

    fun project(longitude: Double, latitude: Double, bounds: Rect): Offset = Offset(
        x = bounds.left + ((longitude - WEST) / (EAST - WEST)).toFloat() * bounds.width,
        y = bounds.top + ((NORTH - latitude) / (NORTH - SOUTH)).toFloat() * bounds.height,
    )
}
