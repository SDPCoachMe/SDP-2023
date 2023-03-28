package com.github.sdpcoachme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.sdpcoachme.data.MapState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapView(modifier: Modifier, mapState: MapState) {
    val campus = LatLng(46.520536,6.568318)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(campus, 15f)
    }
    val lastKnownLatLng = mapState.lastKnownLocation?.let {
        LatLng(it.latitude, mapState.lastKnownLocation.longitude)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            // while true (= while location is available), continuously draws an indication
            // of the user's current location on the map, does not retrieve the location data
            // for logic computation
            isMyLocationEnabled = mapState.lastKnownLocation != null,
            mapType = MapType.NORMAL
        )
    ) {
        // places a marker on the last known position, does nothing if location is null
        lastKnownLatLng?.let { MarkerState(it) }?.let { Marker(state = it) }
    }
}