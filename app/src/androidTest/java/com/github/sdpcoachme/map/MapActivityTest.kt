package com.github.sdpcoachme.map

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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.Dashboard.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.Dashboard.TestTags.Companion.BAR_TITLE
import com.github.sdpcoachme.Dashboard.TestTags.Companion.DRAWER_HEADER
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.Buttons.Companion.GO_TO_LOGIN_BUTTON
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.TextFields.Companion.ERROR_MESSAGE_FIELD
import com.github.sdpcoachme.map.MapActivity.TestTags.Companion.MAP
import com.github.sdpcoachme.map.MapActivity.TestTags.Companion.MAP_WITHOUT_LOCATION
import com.github.sdpcoachme.ui.theme.CoachMeTheme
import com.google.android.gms.maps.model.LatLng
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
            composeTestRule.onNodeWithTag(MAP_WITHOUT_LOCATION).assertExists().assertIsDisplayed()
        }
    }

}