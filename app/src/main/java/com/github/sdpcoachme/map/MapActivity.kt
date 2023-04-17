package com.github.sdpcoachme.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.Dashboard
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.map.MapActivity.Companion.CAMPUS
import com.github.sdpcoachme.map.MapActivity.TestTags.Companion.MAP
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

/**
 * Main map activity, launched after login. This activity contains the map view and holds
 * the current last known user location.
 */
class MapActivity : ComponentActivity() {

    class TestTags {
        companion object {
            const val MAP = "map"
        }
    }

    companion object {
        val CAMPUS = LatLng(46.520536,6.568318)
    }

    private lateinit var database: Database
    private lateinit var email: String

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    // the user location is communicated via CoachMeApplication to avoid storing it in the database
    private lateinit var lastUserLocation: MutableState<LatLng?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = (application as CoachMeApplication).database
        email = database.getCurrentEmail()
        lastUserLocation = (application as CoachMeApplication).userLocation

        // gets user location at map creation
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        getLocation()

        if (email.isEmpty()) {
            val errorMsg = "The map did not receive an email address.\nPlease return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        } else {
            setContent {
                CoachMeTheme {
                    val dashboardContent: @Composable (Modifier) -> Unit = { modifier ->
                        Map(modifier = modifier, lastUserLocation = lastUserLocation)
                    }
                    Dashboard(dashboardContent, email)
                }
            }
        }
    }

    /**
     * Create an activity for result : display window to request asked permission.
     * If granted, launches the callback (here getDeviceLocation(...) which retrieves the user's
     * location). The contract is a predefined "function" which takes a permission as input and
     * outputs if the user has granted it or not.
     */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                getDeviceLocation(fusedLocationProviderClient)
            } else {
            // TODO Permission denied or only COARSE given
            }
        }

    /**
     * This function updates the state of lastUserLocation if the permission is granted.
     * If the permission is denied, it requests it.
     */
    private fun getLocation() =
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            PackageManager.PERMISSION_GRANTED -> {
                getDeviceLocation(fusedLocationProviderClient)
            }
            else -> requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    /**
     * Performs the location retrieval. Permission are checked before this function call
     */
    @SuppressLint("MissingPermission") //permission is checked before the call
    private fun getDeviceLocation(fusedLocationProviderClient: FusedLocationProviderClient) {
        try {
            fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.result != null) {
                        println("Last location has been successfully retrieved")
                        lastUserLocation.value = LatLng(
                            task.result.latitude,
                            task.result.longitude
                        )
                        updateUserLocation()
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

    /**
     * Updates the user location of the app. Needs to be called everytime we get a new device
     * location : already called in getDeviceLocation for this purpose
     */
    private fun updateUserLocation() {
        (application as CoachMeApplication).userLocation = lastUserLocation
    }
}

/**
 * Displays the map with the last known user location on creation.
 * The map API displays the current location but does not explicitly give the location data.
 * This is done by given a MapState via its lastKnownLocation attribute that retrieves the
 * location data. The MapState is updated by the MapViewModel in the DashboardActivity.
 */
@Composable
fun Map(modifier: Modifier, lastUserLocation: MutableState<LatLng?>) {

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(CAMPUS, 15f)
    }

    println("MAP WAS LAUNCHED WITH"+ lastUserLocation.value.toString())

    GoogleMap(
        // test tag contains lastUserLocation info to allow simple recomposition tracking
        modifier = modifier
            .fillMaxSize()
            .testTag(MAP + lastUserLocation.value.toString()),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = lastUserLocation.value != null,
            mapType = MapType.NORMAL
        )
    ) {
        // moves camera to last known location
        if (lastUserLocation.value != null) {
            // relaunches the effect when the key changes. So here only once
            LaunchedEffect(true) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(lastUserLocation.value!!, 17f)
                )
            }
        }
    }
}