package com.purplevarun.trader.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.purplevarun.trader.engine.CityId
import com.purplevarun.trader.engine.GameConstants
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The country, its rail links and the cities, scaled to whatever space it is given. */
@Composable
fun RailMap(currentCity: CityId, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val labelDensity = remember(density) {
        Density(density.density, min(density.fontScale, UiType.MAP_LABEL_MAX_FONT_SCALE))
    }
    Layout(
        modifier = modifier
            .testTag(TestTags.RAIL_MAP)
            .drawBehind { drawMap(currentCity) },
        content = {
            CompositionLocalProvider(LocalDensity provides labelDensity) {
                GameConstants.CITIES.forEach { city ->
                    BasicText(
                        text = stringResource(cityNameRes(city.id)),
                        style = if (city.id == currentCity) UiType.MapLabelCurrent else UiType.MapLabel,
                        modifier = Modifier.layoutId(city.id),
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            (width / MapGeometry.ASPECT_RATIO).roundToInt()
        }
        val bounds = MapGeometry.fit(Size(width.toFloat(), height.toFloat()), UiDimens.MapPadding.toPx())
        val gap = (UiDimens.CurrentCityDotRadius + UiDimens.CityLabelGap).toPx()
        val labels = measurables.map { measurable ->
            measurable.layoutId as CityId to measurable.measure(Constraints(maxWidth = width, maxHeight = height))
        }
        layout(width, height) {
            for ((id, label) in labels) {
                val city = GameConstants.CITIES.first { it.id == id }
                val dot = MapGeometry.project(city.longitude, city.latitude, bounds)
                val (x, y) = when (MapGeometry.LABEL_SIDE.getValue(id)) {
                    MapGeometry.LabelSide.END -> dot.x + gap to dot.y - label.height / 2f
                    MapGeometry.LabelSide.START -> dot.x - gap - label.width to dot.y - label.height / 2f
                    MapGeometry.LabelSide.ABOVE_END -> dot.x + gap / 2 to dot.y - gap / 2 - label.height
                    MapGeometry.LabelSide.BELOW_END -> dot.x + gap / 2 to dot.y + gap / 2
                }
                // A name never leaves the map, however narrow the screen or large the text.
                label.place(
                    x.roundToInt().coerceIn(0, max(0, width - label.width)),
                    y.roundToInt().coerceIn(0, max(0, height - label.height)),
                )
            }
        }
    }
}

private fun DrawScope.drawMap(currentCity: CityId) {
    val bounds = MapGeometry.fit(size, UiDimens.MapPadding.toPx())

    val land = Path().apply {
        MapGeometry.INDIA_OUTLINE.forEachIndexed { index, (longitude, latitude) ->
            val point = MapGeometry.project(longitude, latitude, bounds)
            if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
        close()
    }
    drawPath(land, UiColors.Land)
    drawPath(land, UiColors.Line, style = Stroke(UiDimens.LandStroke.toPx(), join = StrokeJoin.Round))

    val cityPoints = GameConstants.CITIES.associate { it.id to MapGeometry.project(it.longitude, it.latitude, bounds) }
    val ties = PathEffect.dashPathEffect(
        floatArrayOf(UiDimens.RailTieThickness.toPx(), UiDimens.RailTieSpacing.toPx()),
    )
    for (link in GameConstants.RAIL_LINKS) {
        val from = cityPoints.getValue(link.a)
        val to = cityPoints.getValue(link.b)
        drawLine(UiColors.Mute, from, to, strokeWidth = UiDimens.RailStroke.toPx())
        drawLine(UiColors.Mute, from, to, strokeWidth = UiDimens.RailTieLength.toPx(), pathEffect = ties)
    }

    for ((id, point) in cityPoints) {
        val isCurrent = id == currentCity
        val radius = (if (isCurrent) UiDimens.CurrentCityDotRadius else UiDimens.CityDotRadius).toPx()
        drawCircle(if (isCurrent) UiColors.Accent else UiColors.Panel, radius, point)
        drawCircle(UiColors.Ink, radius, point, style = Stroke(UiDimens.CityDotStroke.toPx()))
    }
}
