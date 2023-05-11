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
     *
     * @return true if ACCESS_FINE_LOCATION has been granted to the current application.
     */
    fun locationIsPermitted(): Boolean

    /**
     * Requests permission to retrieve location.
     */
    fun requestPermission()

    /**
     * Retrieves last application-wide known location. MutableState to provide observability.
     *
     * @return a mutable state containing the last known device location.
     */
    fun getLastLocation(): MutableState<LatLng?>

    /**
     * Updates the location provider's activity context and user info.
     *
     * @param context the activity context where the LocationProvider is used
     * @param userInfo a future to retrieve user info.
     */
    fun updateContext(context: ComponentActivity, userInfo: CompletableFuture<UserInfo>)
}