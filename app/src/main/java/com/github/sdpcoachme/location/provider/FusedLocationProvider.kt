package com.github.sdpcoachme.location.provider

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.activity.ComponentActivity
import androidx.activity.ComponentActivity.RESULT_OK
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.location.MapActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.model.LatLng
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeoutException

/**
 * Implementation of a LocationProvider using a FusedLocationProviderClient, ie using the location
 * service of the Google API. See LocationProvider for further documentation.
 */
class FusedLocationProvider : LocationProvider {

    companion object {
        val CAMPUS = LatLng(46.520536,6.568318)
        const val DELAY = 5000L
    }

    private lateinit var user: CompletableFuture<UserInfo>
    private lateinit var appContext: ComponentActivity
    private lateinit var lastUserLocation: MutableState<LatLng?>
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    /**
     * Create an activity for result to display window to request location permission.
     * The contract is a predefined "function" which takes a permission as input and
     * outputs if the user has granted it or not. See init(...).
     */
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    /**
     * Create an activity for result to display a window to request location setting.
     * The contract is a predefined "function" which takes an IntentSenderRequest as input and
     * outputs if the user has enabled the setting or not. See init(...).
     */
    private lateinit var requestSettingLauncher:  ActivityResultLauncher<IntentSenderRequest>

    override fun init(context: ComponentActivity, userInfo: CompletableFuture<UserInfo>) {
        appContext = context
        user = userInfo
        // Only the MapActivity initializes the location as it is guaranteed to be set in its
        // context. For other activities, we launch init on the LocationProvider of the app while
        // maintaining the lastUserLocation state.
        // This allows single activity launch tests, ie without launching the MapActivity first.
        if (context is MapActivity) {
            lastUserLocation = mutableStateOf(null)
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(appContext)

        requestPermissionLauncher = appContext.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                checkLocationSetting()
            } else {
                setLocationToAddress()
            }
        }

        requestSettingLauncher = appContext.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) {
            if (it.resultCode == RESULT_OK) {
                // User enabled device location, we can now retrieve it
                getDeviceLocation(0)
            } else {
                // User did not enable device location
                setLocationToAddress()
            }
        }
    }

    override fun requestPermission() {
        requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
    }

    /**
     * Especially useful after application installation.
     */
    override fun locationIsPermitted(): Boolean {
        val locationPermission = ContextCompat.checkSelfPermission(appContext, ACCESS_FINE_LOCATION)
        return locationPermission == PERMISSION_GRANTED
    }

    /**
     * This function is always called with granted permissions
     * and the device location settings enabled. This safely allows the recursive call needed in the
     * case the device location service takes time to be deployed. A max iteration "timeout" has
     * been set to ensure termination.
     *
     * Notice that the fusedLocationProviderClient.lastLocation task is null if the location is
     * disabled on the device (also even if the last location was previously retrieved because
     * disabling the device location clears the cache).
     */
    @SuppressLint("MissingPermission") //permission is checked before the call
    private fun getDeviceLocation(delay: Long) {
        // getDeviceLocation should not be called more than MAX_LOCATION_DELAY time
        if (delay >= DELAY) {
            error("getDeviceLocation has reached its max recursive delay")
        }
        try {
            fusedLocationProviderClient.lastLocation.addOnCompleteListener {
                if (it.isSuccessful) {
                    if (it.result != null) {
                        lastUserLocation.value = LatLng(it.result.latitude, it.result.longitude)
                    } else {
                        // Yann: I haven't found any better way to handle this so I'm open to
                        // suggestions ! Futures and proper TimeOuts were tested but the
                        // lastLocation task would not complete successfully nor could I find any
                        // suitable implementation.
                        getDeviceLocation(delay + 1)
                    }
                }
            }
        } catch (e: SecurityException) {
            error("getDeviceLocation was called without correct permissions : ${e.message}")
        }
    }

    /**
     * If location service is enabled on the device, launches the location retrieval.
     * Else, requests the user to enable the location service of the device.
     */
    override fun checkLocationSetting() {
        // A location request is needed to check the location settings although we don't need
        // this request to perform a one-time location retrieval.
        // Interval is set to 0 but it doesn't matter since we don't really use this request.
        val locationRequest = LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, 0)
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest.build())
        val client: SettingsClient = LocationServices.getSettingsClient(appContext)
        val locationSettingsResponse = client.checkLocationSettings(builder.build())

        locationSettingsResponse.addOnSuccessListener {
            // Location settings are satisfied, get the last known location
            getDeviceLocation(0)
        }
        locationSettingsResponse.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                val intentSender = exception.resolution.intentSender
                requestSettingLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            } else {
                // else, there is no way to fix the location settings
                setLocationToAddress()
            }
        }
    }

    /**
     * Called when user refuses location permission or service requests. The address of the user
     * is then set to be the default location.
     */
    private fun setLocationToAddress() {
        val msg = "Current location was set to your address."
        val popup = Toast.makeText(appContext, msg, LENGTH_LONG)
        popup.show()
        val address = user.get(DELAY, MILLISECONDS).address

        Handler(Looper.getMainLooper()).postDelayed({ popup.cancel() }, DELAY)
        try {
            lastUserLocation.value = LatLng(address.latitude, address.longitude)
        } catch (e: TimeoutException) {
            error("setLocationToAddress: could not retrieve user address in time - ${e.message}")
        }
    }

    override fun getLastLocation(): MutableState<LatLng?> {
        return lastUserLocation
    }

}