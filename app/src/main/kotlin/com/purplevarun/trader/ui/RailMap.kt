package com.purplevarun.trader.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.purplevarun.trader.R
import com.purplevarun.trader.engine.City
import com.purplevarun.trader.engine.CityId
import com.purplevarun.trader.engine.GameConstants
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The country, its rail links and the cities, scaled to whatever space it is given.
 * A tap anywhere on the map picks the nearest city; screen readers get one button per city name.
 * [route] is drawn highlighted, ending at its last city.
 */
@Composable
fun RailMap(
    currentCity: CityId,
    onCityTap: (CityId) -> Unit,
    modifier: Modifier = Modifier,
    route: List<CityId> = emptyList(),
) {
    val tap by rememberUpdatedState(onCityTap)
    val density = LocalDensity.current
    val labelDensity = remember(density) {
        Density(density.density, min(density.fontScale, UiType.MAP_LABEL_MAX_FONT_SCALE))
    }
    Layout(
        modifier = modifier
            .testTag(TestTags.RAIL_MAP)
            .drawBehind { drawMap(currentCity, route) }
            .pointerInput(Unit) {
                detectTapGestures { position ->
                    val bounds = MapGeometry.fit(
                        Size(size.width.toFloat(), size.height.toFloat()),
                        UiDimens.MapPadding.toPx(),
                    )
                    val nearest = GameConstants.CITIES.minBy { (it.point(bounds) - position).getDistance() }
                    if ((nearest.point(bounds) - position).getDistance() <= UiDimens.CityTapRadius.toPx()) {
                        tap(nearest.id)
                    }
                }
            },
        content = {
            CompositionLocalProvider(LocalDensity provides labelDensity) {
                GameConstants.CITIES.forEach { city ->
                    val name = stringResource(cityNameRes(city.id))
                    val action = if (city.id == currentCity) {
                        stringResource(R.string.open_market)
                    } else {
                        stringResource(R.string.travel_to, name)
                    }
                    BasicText(
                        text = name,
                        style = if (city.id == currentCity) UiType.MapLabelCurrent else UiType.MapLabel,
                        modifier = Modifier
                            .layoutId(city.id)
                            .testTag(TestTags.cityLabel(city.id))
                            .semantics {
                                role = Role.Button
                                onClick(label = action) {
                                    tap(city.id)
                                    true
                                }
                            },
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
                val dot = GameConstants.CITIES.first { it.id == id }.point(bounds)
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

private fun City.point(bounds: Rect): Offset = MapGeometry.project(longitude, latitude, bounds)

private fun DrawScope.drawMap(currentCity: CityId, route: List<CityId>) {
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

    val points = GameConstants.CITIES.associate { it.id to it.point(bounds) }
    val routeLinks = route.zipWithNext().map { (a, b) -> setOf(a, b) }.toSet()
    val ties = PathEffect.dashPathEffect(floatArrayOf(UiDimens.RailTieThickness.toPx(), UiDimens.RailTieSpacing.toPx()))

    fun drawRail(from: Offset, to: Offset, color: Color, stroke: Float, tieLength: Float) {
        drawLine(color, from, to, strokeWidth = stroke)
        drawLine(color, from, to, strokeWidth = tieLength, pathEffect = ties)
    }
    for (link in GameConstants.RAIL_LINKS.filter { setOf(it.a, it.b) !in routeLinks }) {
        drawRail(points.getValue(link.a), points.getValue(link.b), UiColors.Mute, UiDimens.RailStroke.toPx(), UiDimens.RailTieLength.toPx())
    }
    for ((a, b) in route.zipWithNext()) {
        drawRail(points.getValue(a), points.getValue(b), UiColors.Accent, UiDimens.RouteStroke.toPx(), UiDimens.RouteTieLength.toPx())
    }

    val destination = route.lastOrNull()?.takeIf { it != currentCity }
    for ((id, point) in points) {
        val isCurrent = id == currentCity
        val radius = (if (isCurrent) UiDimens.CurrentCityDotRadius else UiDimens.CityDotRadius).toPx()
        drawCircle(if (isCurrent) UiColors.Accent else UiColors.Panel, radius, point)
        if (id == destination) {
            drawCircle(UiColors.Accent, radius, point, style = Stroke(UiDimens.DestinationRingStroke.toPx()))
        } else {
            drawCircle(UiColors.Ink, radius, point, style = Stroke(UiDimens.CityDotStroke.toPx()))
        }
    }
}
