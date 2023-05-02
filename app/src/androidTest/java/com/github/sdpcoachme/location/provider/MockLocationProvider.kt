package com.github.sdpcoachme.location.provider

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.location.LocationManager.GPS_PROVIDER
import androidx.activity.ComponentActivity
import androidx.activity.ComponentActivity.RESULT_OK
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.location.provider.FusedLocationProvider.Companion.DELAY
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.model.LatLng
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeoutException

class MockLocationProvider: LocationProvider {

    private lateinit var user: CompletableFuture<UserInfo>
    private lateinit var appContext: ComponentActivity
    private lateinit var mockLocation: MutableState<LatLng?>
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestSettingLauncher: ActivityResultLauncher<IntentSenderRequest>

    fun setMockMode() {
        fusedLocationProviderClient.setMockMode(true)
    }

    fun setMockLocation(latLng: LatLng) {
        val location = Location(GPS_PROVIDER).apply {
            latitude = latLng.latitude
            longitude = latLng.longitude
        }
        fusedLocationProviderClient.setMockLocation(location)
    }

    override fun init(context: ComponentActivity, userInfo: CompletableFuture<UserInfo>) {
        appContext = context
        user = userInfo
        mockLocation = mutableStateOf(null)
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
                getDeviceLocation(0)
            } else {
                setLocationToAddress()
            }
        }
    }

    override fun requestPermission() {
        requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
    }

    override fun locationIsPermitted(): Boolean {
        val locationPermission = ContextCompat.checkSelfPermission(appContext, ACCESS_FINE_LOCATION)
        return locationPermission == PERMISSION_GRANTED
    }

    private fun getDeviceLocation(delay: Long) {
        if (delay >= DELAY) {
            error("getDeviceLocation has reached its max recursive delay")
        }
        try {
            fusedLocationProviderClient.lastLocation.addOnCompleteListener {
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

    override fun checkLocationSetting() {
        val locationRequest = LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, 0)
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest.build())
        val client: SettingsClient = LocationServices.getSettingsClient(appContext)
        val locationSettingsResponse = client.checkLocationSettings(builder.build())

        locationSettingsResponse.addOnSuccessListener {
            getDeviceLocation(0)
        }
        locationSettingsResponse.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                val intentSender = exception.resolution.intentSender
                requestSettingLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            } else {
                setLocationToAddress()
            }
        }
    }

    private fun setLocationToAddress() {
        val address = user.get(DELAY, MILLISECONDS).address
        try {
            mockLocation.value = LatLng(address.latitude, address.longitude)
        } catch (e: TimeoutException) {
            error("setLocationToAddress: could not retrieve user address in time - ${e.message}")
        }
    }

    override fun getLastLocation(): MutableState<LatLng?> {
        return mockLocation
    }
}