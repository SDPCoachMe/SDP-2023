package com.github.sdpcoachme

import android.annotation.SuppressLint
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.github.sdpcoachme.data.MapState
import com.google.android.gms.location.FusedLocationProviderClient

class MapViewModel : ViewModel() {

    val mapState: MutableState<MapState> = mutableStateOf(MapState(lastKnownLocation = null))

    @SuppressLint("MissingPermission") //permission is checked before the call
    fun getDeviceLocation(fusedLocationProviderClient: FusedLocationProviderClient) {
        try {
            fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("new location found !")
                    mapState.value = mapState.value.copy(lastKnownLocation = task.result)
                }
            }
        } catch (e: SecurityException) {
            error("getDeviceLocation was called without correct permissions : ${e.message}")
        }
    }
}