package com.github.sdpcoachme.location.provider

import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.github.sdpcoachme.data.UserAddressSamples.Companion.LAUSANNE
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.location.provider.FusedLocationProvider.Companion.DELAY
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException

/**
 * Mock a LocationProvider that behaves exactly as a FusedLocationProvider but with controllable
 * location permission and setting. See FusedLocationProvider and LocationProvider for further
 * documentation.
 */
class MockLocationProvider: LocationProvider {

    private var user: CompletableFuture<UserInfo> = CompletableFuture.completedFuture(null)
    private var appContext: ComponentActivity = ComponentActivity()
    private var mockLocation: MutableState<LatLng?> = mutableStateOf(null)
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    /**
     * The two var that allow us to control the stateflow of the location provider.
     * Should not be set in init(...) as this would override potential withPermission/withSetting
     * calls in tests.
     */
    private var withSetting: Boolean = true
    private var withPermission: Boolean = true
    fun withoutSetting() {
        withSetting = false
    }
    fun withoutPermission() {
        withPermission = false
    }

    override fun updateContext(context: ComponentActivity, userInfo: CompletableFuture<UserInfo>) {
        appContext = context
        user = userInfo
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(appContext)
    }

    override fun requestPermission() {
        if (withPermission) {
            checkLocationSetting()
        } else {
            setLocationToAddress()
        }
    }

    override fun locationIsPermitted(): Boolean {
        return withPermission
    }

    override fun checkLocationSetting() {
        if (withSetting) {
            getDeviceLocation(0)
        } else {
            setLocationToAddress()
        }
    }

    override fun getLastLocation(): MutableState<LatLng?> {
        return mockLocation
    }

    private fun getDeviceLocation(delay: Long) {
        assert(fusedLocationProviderClient != null)
        if (delay >= DELAY) {
            error("getDeviceLocation has reached its max recursive delay")
        }
        try {
            fusedLocationProviderClient?.lastLocation?.addOnCompleteListener {
                if (it.isSuccessful) {
                    if (it.result != null) {
                        mockLocation.value = LatLng(it.result.latitude, it.result.longitude)
                    } else {
                        getDeviceLocation(delay + 1)
                    }
                }
            }
        } catch (e: SecurityException) {
            error("getDeviceLocation was called without correct permissions : ${e.message}")
        }
    }

    private fun setLocationToAddress() {
        val address = if (user.isCompletedExceptionally) LAUSANNE else user.get().address
        try {
            mockLocation.value = LatLng(address.latitude, address.longitude)
        } catch (e: TimeoutException) {
            error("setLocationToAddress: could not retrieve user address in time - ${e.message}")
        }
    }
}