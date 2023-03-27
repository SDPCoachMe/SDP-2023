package com.github.sdpcoachme

import android.content.Context
import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapView(modifier: Modifier, userLocation: Location?) {
    val campus = LatLng(46.520536,6.568318)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(campus, 15f)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            // while true (= while location is available), continuously draws an indication
            // of the user's current location on the map, does not retrieve the location data
            // for logic computation
            isMyLocationEnabled = userLocation != null,
            mapType = MapType.NORMAL
        )
    )
}