package com.github.sdpcoachme.location.provider

import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import com.github.sdpcoachme.data.UserInfo
import com.google.android.gms.maps.model.LatLng
import java.util.concurrent.CompletableFuture

/**
 * Interface of a location-related main services provider.
 */
interface LocationProvider {

    /**
     * Checks if location-service is set on the device.
     */
    fun checkLocationSetting()

    /**
     * Checks correct permission are granted for location retrieval.
     */
    fun locationIsPermitted(): Boolean

    /**
     * Requests permission to retrieve location.
     */
    fun requestPermission()

    /**
     * Retrieves last application-wide known location.
     */
    fun getLastLocation(): MutableState<LatLng?>

    /**
     * Initializes the location provider with an activity context and a user info future.
     */
    fun init(context: ComponentActivity, userInfo: CompletableFuture<UserInfo>)
}