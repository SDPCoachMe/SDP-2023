package com.github.sdpcoachme.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.Dashboard
import com.github.sdpcoachme.data.MapState
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.firebase.database.Database
import com.github.sdpcoachme.map.MapActivity.TestTags.Companion.MAP
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

class MapActivity : ComponentActivity() {

    class TestTags {
        companion object {
            const val MAP = "map"
        }
    }

    private lateinit var database: Database
    private lateinit var email: String

    /**
     * Create an activity for result : display window to request asked permission.
     * If granted, launches the callback (here getDeviceLocation(...) which retrieves the user's
     * location). The contract is a predefined "function" which takes a permission as input and
     * outputs if the user has granted it or not.
     */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                isGranted: Boolean -> if (isGranted) {
            mapViewModel.getDeviceLocation(fusedLocationProviderClient)
        } else {
            // TODO Permission denied or only COARSE given
        }
        }

    /**
     * This function updates the location state of the MapViewModel if the permission is granted.
     * If the permission is denied, it requests it.
     */
    private fun getLocation() =
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            PackageManager.PERMISSION_GRANTED -> {
                mapViewModel.getDeviceLocation(fusedLocationProviderClient)
            }
            else -> requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val mapViewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = (application as CoachMeApplication).database
        email = database.getCurrentEmail()

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
                        Map(modifier = modifier, mapState = mapViewModel.mapState.value)
                    }
                    Dashboard(dashboardContent, email)
                }
            }
        }
    }
}


/**
 * Displays the map with the last known user location on creation.
 * The map API displays the current location but does not explicitly give the location data.
 * This is done by given a MapState via its lastKnownLocation attribute that retrieves the
 * location data. The MapState is updated by the MapViewModel in the DashboardActivity.
 */
@Composable
fun Map(modifier: Modifier, mapState: MapState) {

    val campus = LatLng(46.520536,6.568318)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(campus, 15f)
    }

    GoogleMap(
        modifier = modifier
            .fillMaxSize()
            .testTag(MAP),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = mapState.lastKnownLocation != null,
            mapType = MapType.NORMAL
        )
    ) {
        // moves camera to last known location
        if (mapState.lastKnownLocation != null) {
            // relaunches the effect when the key changes. So here only once
            LaunchedEffect(true) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(mapState.lastKnownLocation, 17f)
                )
            }
        }

        // places a marker at the last known position, if location is null, places at campus
        Marker(state = MarkerState(mapState.lastKnownLocation?:campus))
    }
}