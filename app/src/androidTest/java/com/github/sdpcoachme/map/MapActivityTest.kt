package com.github.sdpcoachme.map

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.github.sdpcoachme.data.MapState
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.Buttons.Companion.GO_TO_LOGIN_BUTTON
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.TextFields.Companion.ERROR_MESSAGE_FIELD
import com.github.sdpcoachme.map.MapActivity.TestTags.Companion.MAP
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

    private fun mapExistsAndShowsUpOnLocation(location: LatLng?) {
        composeTestRule.setContent {
            CoachMeTheme() {
                Map(
                    modifier = Modifier.fillMaxWidth(),
                    mapState = MapState(lastKnownLocation = location)
                )
            }
        }
        composeTestRule.onRoot().onChild().assertExists().assert(hasTestTag(MAP))
        composeTestRule.onNodeWithTag(MAP).assertExists().assertIsDisplayed()
    }

    @Before
    fun setUp() {
        database.setCurrentEmail(EXISTING_EMAIL)
    }

    @Test
    fun mapExistsAndShowsUpWithoutLocation() {
        mapExistsAndShowsUpOnLocation(location = null)
    }

    @Test
    fun mapExistsAndShowsUpWithLocation() {
        mapExistsAndShowsUpOnLocation(location = random)
    }

    @Test
    fun dashboardAppContentIsMap() {
        ActivityScenario.launch<MapActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(MAP).assertIsDisplayed()
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
        }
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

}