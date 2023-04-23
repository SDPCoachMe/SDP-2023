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
import com.github.sdpcoachme.Dashboard.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.Dashboard.TestTags.Companion.BAR_TITLE
import com.github.sdpcoachme.Dashboard.TestTags.Companion.DRAWER_HEADER
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACHES
import com.github.sdpcoachme.data.UserInfoSamples.Companion.NON_COACHES
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.Buttons.Companion.GO_TO_LOGIN_BUTTON
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.TextFields.Companion.ERROR_MESSAGE_FIELD
import com.github.sdpcoachme.location.MapActivity.TestTags.Companion.MAP
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.google.android.gms.maps.model.LatLng
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for the Composable MapView. Unfortunately, the GoogleMap api for Jetpack Compose
 * does not provide testTags for Marker.
 */
@RunWith(AndroidJUnit4::class)
class MapActivityTest {

    // TODO add tests for markers

    private val random = LatLng(42.0,42.0)

    @get: Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        ACCESS_FINE_LOCATION
    )

    private val database = (InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as CoachMeApplication).database
    private val defaultIntent =
        Intent(ApplicationProvider.getApplicationContext(), MapActivity::class.java)
    private val EXISTING_EMAIL = "example@email.com"

    @Before
    fun setUp() {
        database.setCurrentEmail(EXISTING_EMAIL)
        for (coach in COACHES) {
            database.updateUser(coach).join()
        }
        for (nonCoach in NON_COACHES) {
            database.updateUser(nonCoach).join()
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
                    modifier = Modifier.fillMaxWidth(),
                    lastUserLocation = lastUserLocation
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
                Map(modifier = Modifier.fillMaxWidth(),lastUserLocation = lastUserLocation)
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
    fun errorPageIsShownWhenMapIsLaunchedWithEmptyCurrentEmail() {
        database.setCurrentEmail("")
        ActivityScenario.launch<MapActivity>(defaultIntent).use {
            // not possible to use Intents.init()... to check if the correct intent
            // is launched as the intents are launched from within the onCreate function
            composeTestRule.onNodeWithTag(GO_TO_LOGIN_BUTTON).assertIsDisplayed()
            composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD).assertIsDisplayed()
        }
    }

    @Test
    fun dashboardHasRightTitleOnMap() {
        ActivityScenario.launch<MapActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(BAR_TITLE).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(BAR_TITLE).assert(hasText("Coach Me"))
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
            val context = (InstrumentationRegistry.getInstrumentation()
                .targetContext.applicationContext as CoachMeApplication)
            val mapTag = MAP + context.userLocation.value.toString()
            composeTestRule.onNodeWithTag(mapTag).assertExists().assertIsDisplayed()
        }
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