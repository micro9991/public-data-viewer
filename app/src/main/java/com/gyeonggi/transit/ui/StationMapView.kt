package com.gyeonggi.transit.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.gyeonggi.transit.data.NearbyStation
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

data class StationMapPoint(
    val station: NearbyStation,
    val lat: Double,
    val lon: Double
)

private const val COLOR_USER = 0xFFE53935.toInt()
private const val COLOR_STATION = 0xFF5E35B1.toInt()
private const val COLOR_STATION_SELECTED = 0xFFFF8F00.toInt()

@Composable
fun StationMapView(
    userLat: Double,
    userLon: Double,
    points: List<StationMapPoint>,
    selectedUniId: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember { createMapView(context) }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            view.overlays.clear()

            val userPoint = GeoPoint(userLat, userLon)
            view.overlays.add(
                Marker(view).apply {
                    position = userPoint
                    title = "내 위치"
                    icon = circleDrawable(context, COLOR_USER, sizeDp = 22, ringDp = 3)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
            )

            points.forEach { p ->
                val isSelected = p.station.uniId == selectedUniId
                view.overlays.add(
                    Marker(view).apply {
                        position = GeoPoint(p.lat, p.lon)
                        icon = circleDrawable(
                            context,
                            if (isSelected) COLOR_STATION_SELECTED else COLOR_STATION,
                            sizeDp = if (isSelected) 26 else 18,
                            ringDp = 2
                        )
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                )
            }

            view.controller.setZoom(14.5)
            view.controller.setCenter(
                points.firstOrNull { it.station.uniId == selectedUniId }
                    ?.let { GeoPoint(it.lat, it.lon) }
                    ?: userPoint
            )
            view.invalidate()
        }
    )
}

private fun createMapView(context: Context): MapView {
    Configuration.getInstance().load(
        context,
        context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
    )
    Configuration.getInstance().userAgentValue = context.packageName
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
    }
}

private fun circleDrawable(context: Context, colorInt: Int, sizeDp: Int, ringDp: Int): GradientDrawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt()
    val ringPx = (ringDp * density).toInt()
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(colorInt)
        setStroke(ringPx, 0xFFFFFFFF.toInt())
        setSize(sizePx, sizePx)
    }
}
