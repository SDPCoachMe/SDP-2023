package com.github.sdpcoachme.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.github.sdpcoachme.data.MapState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

/**
 * Displays the map with the last known user location on creation.
 * The map API displays the current location but does not explicitly give the location data.
 * This is done by given a MapState via its lastKnownLocation attribute that retrieves the
 * location data. The MapState is updated by the MapViewModel in the DashboardActivity.
 */
@Composable
fun MapView(modifier: Modifier, mapState: MapState) {

    val campus = LatLng(46.520536,6.568318)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(campus, 15f)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = mapState.lastKnownLocation != null,
            mapType = MapType.NORMAL
        )
    ) {
        // moves camera to last known location
        if (mapState.lastKnownLocation != null) {
            // relaunches the effect when the key changes. So here only once
            LaunchedEffect(true) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(mapState.lastKnownLocation, 17f)
                )
            }
        }

        // places a marker at the last known position, if location is null, places at campus
        Marker(state = MarkerState(mapState.lastKnownLocation?:campus))
    }
}