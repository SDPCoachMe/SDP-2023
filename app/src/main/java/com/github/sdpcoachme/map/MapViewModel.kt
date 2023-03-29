package com.github.sdpcoachme.map

import android.annotation.SuppressLint
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.github.sdpcoachme.data.MapState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng

/**
 * View model for the MapView. Holds and updates a MapState which contains the last known
 * user location.
 */
class MapViewModel : ViewModel() {

    val mapState: MutableState<MapState> = mutableStateOf(MapState(lastKnownLocation = null))

    @SuppressLint("MissingPermission") //permission is checked before the call
    fun getDeviceLocation(fusedLocationProviderClient: FusedLocationProviderClient) {
        try {
            fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.result != null) {
                        println("Last location has been successfully retrieved")
                        mapState.value = mapState.value.copy(lastKnownLocation = LatLng(
                            task.result.latitude,
                            task.result.longitude
                        ))
                    } else {
                        println("Location is disabled on the device")
                        // TODO handle this case
                    }
                }
            }
        } catch (e: SecurityException) {
            error("getDeviceLocation was called without correct permissions : ${e.message}")
        }
    }
}