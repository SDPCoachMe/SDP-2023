package com.github.sdpcoachme.location

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.errorhandling.ErrorHandlerLauncher
import com.github.sdpcoachme.location.MapActivity.TestTags.Companion.MAP
import com.github.sdpcoachme.location.MapActivity.TestTags.Companion.MARKER
import com.github.sdpcoachme.location.MapActivity.TestTags.Companion.MARKER_INFO_WINDOW
import com.github.sdpcoachme.location.provider.FusedLocationProvider.Companion.CAMPUS
import com.github.sdpcoachme.location.provider.LocationProvider
import com.github.sdpcoachme.profile.ProfileActivity
import com.github.sdpcoachme.ui.Dashboard
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

/**
 * Main map activity, launched after login. This activity contains the map view and makes use of
 * the application LocationProvider.
 */
class MapActivity : ComponentActivity() {
    // Allows to notice testing framework that the markers are displayed on the map
    private var markerLoading = CompletableFuture<Void>()
    private var mapLoading = CompletableFuture<Void>()

    class TestTags {
        companion object {
            const val MAP = "map"
            fun MARKER(user: UserInfo) = "marker-${user.email}"
            fun MARKER_INFO_WINDOW(user: UserInfo) = "infoWindow-${user.email}"
        }

    }

    private lateinit var database: Database
    private lateinit var email: String
    private lateinit var locationProvider: LocationProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = (application as CoachMeApplication).database
        locationProvider = (application as CoachMeApplication).locationProvider
        email = database.getCurrentEmail()

        if (email.isEmpty()) {
            val errorMsg = "The map did not receive an email address.\nPlease return to the login page and try again."
            ErrorHandlerLauncher().launchExtrasErrorHandler(this, errorMsg)
        } else {
            val user = database.getUser(email).exceptionally {
                error("MapActivity: user could not have been retrieved from the database.")
            }
            locationProvider.init(this, user)

            // Performs the whole location retrieval process
            if (locationProvider.locationIsPermitted()) {
                locationProvider.checkLocationSetting()
            } else {
                locationProvider.requestPermission()
            }
            // For now, simply retrieve all users. We can decide later whether we want to have
            // a dynamic list of markers that only displays nearby users (based on camera position
            // for example). Since we have no way to download only the users that are nearby, we
            // have to download all users anyways and filter them locally. This means that either
            // way, we already have available all users locations, so we might as well display them
            // all.
            // Note: this is absolutely not scalable, but we can change this later on.
            val futureUsers = database.getAllUsers().thenApply { users -> users.filter { it.coach } }
            setContent {
                CoachMeTheme {
                    Dashboard {
                        Map(
                            modifier = it,
                            lastUserLocation = locationProvider.getLastLocation(),
                            futureCoachesToDisplay = futureUsers,
                            markerLoading = markerLoading,
                            mapLoading = mapLoading)
                    }
                }
            }
        }
    }

}

/**
 * Displays the map with the last known user location on creation.
 * The map API displays the current location but does not explicitly give the location data.
 * Nearby coaches addresses are also marked on the map.
 */
@Composable
fun Map(
    modifier: Modifier,
    lastUserLocation: MutableState<LatLng?>,
    // Those 2 arguments have default values to avoid refactoring older tests
    futureCoachesToDisplay: CompletableFuture<List<UserInfo>> = CompletableFuture.completedFuture(listOf()),
    markerLoading: CompletableFuture<Void> = CompletableFuture<Void>(),
    mapLoading: CompletableFuture<Void> = CompletableFuture<Void>()
) {

    val context = LocalContext.current

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(CAMPUS, 15f)
    }

    var coachesToDisplay by remember { mutableStateOf(listOf<UserInfo>()) }
    LaunchedEffect(true) {
        coachesToDisplay = futureCoachesToDisplay.await()
        // For testing purposes, we need to know when the map is ready
        markerLoading.complete(null)
    }

    GoogleMap(
        // test tag contains lastUserLocation info to allow simple recomposition tracking
        modifier = modifier
            .fillMaxSize()
            .testTag(MAP + lastUserLocation.value.toString()),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = lastUserLocation.value != null,
            mapType = MapType.NORMAL
        ),
        onMapLoaded = {
            // For testing purposes, we need to know when the map is ready
            mapLoading.complete(null)
        }
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

        coachesToDisplay.map { user ->
            val state by remember {
                mutableStateOf(
                    MarkerState(LatLng(user.address.latitude, user.address.longitude))
                )
            }

            MarkerInfoWindowContent(
                state = state,
                tag = MARKER(user),
                onInfoWindowClick = {
                    // TODO: code similar to CoachesList, might be able to modularize
                    // Launches the ProfileActivity to display the coach's profile
                    val displayCoachIntent = Intent(context, ProfileActivity::class.java)
                    displayCoachIntent.putExtra("email", user.email)
                    displayCoachIntent.putExtra("isViewingCoach", true)
                    context.startActivity(displayCoachIntent)
                }
            ) {
                // TODO: code similar to CoachesList, might be able to modularize
                Column(
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)
                        .fillMaxWidth(0.8f)
                        .testTag(MARKER_INFO_WINDOW(user))
                ) {
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        color = Color.Black,
                        style = MaterialTheme.typography.h6,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            tint = Color.Gray,
                            contentDescription = "${user.firstName} ${user.lastName}'s location",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = user.address.name,
                            color = Color.Gray,
                            style = MaterialTheme.typography.body2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        user.sports.map {
                            Icon(
                                imageVector = it.sportIcon,
                                tint = Color.Gray,
                                contentDescription = it.sportName,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                    }
                }
            }
        }
    }
}