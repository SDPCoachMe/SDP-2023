package com.github.sdpcoachme.location

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.R
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACHES
import com.github.sdpcoachme.data.UserInfoSamples.Companion.NON_COACHES
import com.github.sdpcoachme.location.MapActivity.TestTags.Companion.MAP
import com.github.sdpcoachme.location.provider.FusedLocationProvider.Companion.DELAY
import com.github.sdpcoachme.location.provider.MockLocationProvider
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.BAR_TITLE
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.DRAWER_HEADER
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.google.android.gms.maps.model.LatLng
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Test class for the Composable MapView. Unfortunately, the GoogleMap api for Jetpack Compose
 * does not provide testTags for Marker.
 */
@RunWith(AndroidJUnit4::class)
class MapActivityTest {
    private val random = LatLng(42.0,42.0)

    @get: Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        ACCESS_FINE_LOCATION
    )

    private val store = (InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as CoachMeApplication).store
    private val defaultIntent =
        Intent(ApplicationProvider.getApplicationContext(), MapActivity::class.java)
    private val EXISTING_EMAIL = "example@email.com"
    private val context = (InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as CoachMeApplication)
    private val mockLocationProvider = (context.locationProvider as MockLocationProvider)

    @Before
    fun setUp() {
        store.retrieveData.get(1, SECONDS)
        store.setCurrentEmail(EXISTING_EMAIL)
        for (coach in COACHES) {
            store.updateUser(coach).join()
        }
        for (nonCoach in NON_COACHES) {
            store.updateUser(nonCoach).join()
        }
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @SuppressLint("UnrememberedMutableState")
    private fun mapViewWorksOnLocation(location: LatLng?) {
        val lastUserLocation = mutableStateOf(location)
        val mapTag = MAP + location.toString()

        composeTestRule.setContent {
            CoachMeTheme() {
                Map(
                    email = "",
                    modifier = Modifier.fillMaxWidth(),
                    lastUserLocation = lastUserLocation,
                    store = store
                )
            }
        }
        composeTestRule.onRoot().onChild().assertExists().assert(hasTestTag(mapTag))
        composeTestRule.onNodeWithTag(mapTag).assertExists().assertIsDisplayed()
    }

    @SuppressLint("UnrememberedMutableState")
    @Test
    fun mapRecomposesOnLocationChange() {
        val firstLocation = random
        val secondLocation = LatLng(random.latitude + 1, random.longitude + 1)
        val firstTag = MAP + firstLocation.toString()
        val secondTag = MAP + secondLocation.toString()

        val lastUserLocation: MutableState<LatLng?> = mutableStateOf(firstLocation)
        composeTestRule.setContent {
            CoachMeTheme() {
                Map(email = "", modifier = Modifier.fillMaxWidth(),lastUserLocation = lastUserLocation, store = store)
            }
        }
        composeTestRule.onNodeWithTag(firstTag).assertExists()
        composeTestRule.onNodeWithTag(secondTag).assertDoesNotExist()
        lastUserLocation.value = secondLocation
        composeTestRule.onNodeWithTag(secondTag).assertExists()
        composeTestRule.onNodeWithTag(firstTag).assertDoesNotExist()
    }

    @Test
    fun mapViewWorksWithLocation() {
        mapViewWorksOnLocation(location = null)
    }

    @Test
    fun mapViewWorksWithoutLocation() {
        mapViewWorksOnLocation(location = random)
    }

    @Test
    fun dashboardHasRightTitleOnMap() {
        val title = (InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as CoachMeApplication).getString(R.string.app_name)
        ActivityScenario.launch<MapActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(BAR_TITLE).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(BAR_TITLE).assert(hasText(title))
        }
    }
    @Test
    fun dashboardIsAccessibleDrawableDisplayableFromMap() {
        ActivityScenario.launch<MapActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertExists().assertIsDisplayed()
        }
    }

    @Test
    fun mapActivityHasADisplayedMapView() {
        ActivityScenario.launch<MapActivity>(defaultIntent).use {
            val lastLocation = mockLocationProvider.getLastLocation()
            val mapTag = MAP + lastLocation.value.toString()
            composeTestRule.onNodeWithTag(mapTag).assertExists().assertIsDisplayed()
        }
    }

    @Test
    fun mapActivityWorksWithoutLocationSetting() {
        mockLocationProvider.withoutSetting()
        ActivityScenario.launch<MapActivity>(defaultIntent).use {
            val lastLocation = mockLocationProvider.getLastLocation()
            val mapTag = MAP + lastLocation.value.toString()
            composeTestRule.onNodeWithTag(mapTag).assertExists().assertIsDisplayed()
        }
    }

    @Test
    fun mapActivityWorksWithoutPermission() {
        mockLocationProvider.withoutPermission()
        ActivityScenario.launch<MapActivity>(defaultIntent).use {
            val lastLocation = mockLocationProvider.getLastLocation()
            val mapTag = MAP + lastLocation.value.toString()
            composeTestRule.onNodeWithTag(mapTag).assertExists().assertIsDisplayed()
        }
    }

    private fun mapLocationIsAddress() {
        ActivityScenario.launch<MapActivity>(defaultIntent).use {
            val lastLocation = mockLocationProvider.getLastLocation().value
            val userAddress = store.getUser(EXISTING_EMAIL).get(DELAY, MILLISECONDS).address
            val userLatLng = LatLng(userAddress.latitude, userAddress.longitude)
            assertThat(lastLocation, `is`(userLatLng))
        }
    }

    @Test
    fun mapLocationWithoutPermissionIsAddress() {
        mockLocationProvider.withoutPermission()
        mapLocationIsAddress()
    }

    @Test
    fun mapLocationWithoutSettingIsAddress() {
        mockLocationProvider.withoutSetting()
        mapLocationIsAddress()
    }


    // TODO: for now, no tests are done on the markers in the map, since I could not find a way to
    //  detect them in the composeTestRule. The testing code is left here for future reference, if
    //  we ever decide to test the markers at some point.
//    @Test
//    fun mapCreatesCorrectMarkers() {
//        ActivityScenario.launch<MapActivity>(defaultIntent).use {
//            // Wait for activity to finish loading UI
//            lateinit var markerLoading: CompletableFuture<Void>
//            lateinit var mapLoading: CompletableFuture<Void>
//            it.onActivity { activity ->
//                markerLoading = activity.markerLoading
//                mapLoading = activity.mapLoading
//            }
//            CompletableFuture.allOf(
//                markerLoading,
//                mapLoading
//            ).get(3000, TimeUnit.MILLISECONDS)
//
//            // Check markers
//            for (coach in COACHES) {
//                val markerTag = MARKER(coach)
//                composeTestRule.onNodeWithTag(markerTag).assertExists().assertIsDisplayed()
//            }
//            for (nonCoach in NON_COACHES) {
//                val markerTag = MARKER(nonCoach)
//                composeTestRule.onNodeWithTag(markerTag).assertDoesNotExist()
//            }
//        }
//    }
//
//    @Test
//    fun clickingOnMarkerOpensCoachProfile() {
//        ActivityScenario.launch<MapActivity>(defaultIntent).use {
//            // Wait for activity to finish loading UI
//            lateinit var markerLoading: CompletableFuture<Void>
//            lateinit var mapLoading: CompletableFuture<Void>
//            it.onActivity { activity ->
//                markerLoading = activity.markerLoading
//                mapLoading = activity.mapLoading
//            }
//            CompletableFuture.allOf(
//                markerLoading,
//                mapLoading
//            ).get(3000, TimeUnit.MILLISECONDS)
//            val coach = COACH_1
//            val markerTag = MARKER(coach)
//            val windowTag = MARKER_INFO_WINDOW(coach)
//            composeTestRule.onNodeWithTag(markerTag).performClick()
//            composeTestRule.onNodeWithTag(windowTag).assertExists().performClick()
//            // Check that the ProfileActivity is launched with the correct extras
//            Intents.intended(
//                allOf(
//                    hasComponent(ProfileActivity::class.java.name),
//                    hasExtra("email", coach.email),
//                    hasExtra("isViewingCoach", true)
//                )
//            )
//        }
//    }

}