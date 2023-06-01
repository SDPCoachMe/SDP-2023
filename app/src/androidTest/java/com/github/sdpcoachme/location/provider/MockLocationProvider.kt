package com.github.sdpcoachme.location.provider

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.github.sdpcoachme.data.AddressSamples.Companion.LAUSANNE
import com.github.sdpcoachme.data.UserInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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

    private fun getDeviceLocation(numberOfTries: Int = 60) {
        // Stop the recursion if number of tries is exceeded
        if (numberOfTries <= 0) {
            Log.e(
                "FusedLocationProvider",
                "getDeviceLocation exceeded max number of tries without retrieving location"
            )
            return
        }
        // The fusedLocationProviderClient should be correctly instantiated before calling this function.
        assert(fusedLocationProviderClient != null)
        try {
            fusedLocationProviderClient!!.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnCompleteListener {
                if (it.isSuccessful && it.result != null) {
                    mockLocation.value = LatLng(it.result.latitude, it.result.longitude)
                } else {
                    // The location service is enabled but the location is not available
                    // (maybe because the device location service is not yet deployed)
                    // We try again after 1s delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        getDeviceLocation(numberOfTries - 1)
                    }, 500)
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