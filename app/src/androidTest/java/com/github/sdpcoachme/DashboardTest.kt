package com.github.sdpcoachme

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.COACHES_LIST
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.LOGOUT
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.MESSAGING
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.PROFILE
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.SCHEDULE
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.DASHBOARD_EMAIL
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.DRAWER_HEADER
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.MENU_LIST
import com.github.sdpcoachme.auth.LoginActivity
import com.github.sdpcoachme.location.MapActivity
import com.github.sdpcoachme.profile.CoachesListActivity
import com.github.sdpcoachme.profile.ProfileActivity
import com.github.sdpcoachme.schedule.ScheduleActivity
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for the Dashboard. As the Dashboard is built over all other activities, we set the
 * main testing activity here to be the MapActivity (see mockActivity val). But the tests of this class should only
 * be specific to the dashboard functionality and stay generic to other activities.
 */
@RunWith(AndroidJUnit4::class)
class DashboardTest {

    private val mockActivity = MapActivity::class.java

    private val EXISTING_EMAIL = "example@email.com"

    private val database = (InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as CoachMeApplication).database

    private val defaultIntent = Intent(ApplicationProvider.getApplicationContext(), mockActivity)

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    // WARNING : this rule will try to grant permissions on the device.
    // Make sure to "Allow granting permissions and simulating input via USB debugging"
    // in the device settings (sometimes called "Permission surveillance").
    // If the device does not disable permission surveillance, the test will crash.
    @get:Rule
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(ACCESS_FINE_LOCATION)

    @Before
    fun setUp() {
        database.setCurrentEmail(EXISTING_EMAIL)
    }

    @Before
    fun initIntents() {
        Intents.init()
    }

    @After
    fun releaseIntents() {
        Intents.release()
    }

    @Test
    fun drawerOpensOnMenuClick() {
        ActivityScenario.launch<MapActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsDisplayed()
        }
    }

    @Test
    fun drawerClosesOnLeftSwipe() {
        ActivityScenario.launch<MapActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
            // closes on left swipe
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsDisplayed()
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
        }
    }

    @Test
    fun drawerIgnoresInvalidSwipe() {
        ActivityScenario.launch<MapActivity>(defaultIntent).use {
            // ignores right swipe if opened
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
            composeTestRule.onRoot().performTouchInput { swipeRight() }
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsDisplayed()
        }
    }

    @Test
    fun drawerClosesOnOutsideTouch() {
        ActivityScenario.launch<MapActivity>(defaultIntent).use {
            val width = composeTestRule.onRoot().getBoundsInRoot().width
            val height = composeTestRule.onRoot().getBoundsInRoot().height
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsDisplayed()
            composeTestRule.onRoot().performTouchInput {
                click(position = Offset(width.toPx() - 10, height.toPx() / 2))
            }
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
        }
    }

    @Test
    fun drawerBodyContainsClickableMenu() {
        ActivityScenario.launch<MapActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(MENU_LIST).onChildren().assertAll(hasClickAction())
        }
    }

    @Test
    fun dashboardDisplaysCorrectEmailFromReceivedIntent() {
        ActivityScenario.launch<MapActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(DASHBOARD_EMAIL).assert(hasText(text = EXISTING_EMAIL))
        }
    }
    private fun dashboardCorrectlyRedirectsOnMenuItemClick(
        tag: String,
        intentMatcher: Matcher<Intent>
    ) {
        ActivityScenario.launch<MapActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(tag).performClick()
            intended(intentMatcher)
        }
    }

    @Test
    fun dashboardCorrectlyRedirectsOnProfileClick() {
        dashboardCorrectlyRedirectsOnMenuItemClick(
            PROFILE,
            hasComponent(ProfileActivity::class.java.name)
        )
    }
    @Test
    fun dashboardCorrectlyRedirectsOnLogOutClick() {
        dashboardCorrectlyRedirectsOnMenuItemClick(
            LOGOUT,
            hasComponent(LoginActivity::class.java.name)
        )
    }

    @Test
    fun dashboardCorrectlyRedirectsOnCoachesListClick() {
        dashboardCorrectlyRedirectsOnMenuItemClick(
            COACHES_LIST,
            hasComponent(CoachesListActivity::class.java.name)
        )
    }

    @Test
    fun dashboardCorrectlyRedirectsOnMessagingClick() {
        dashboardCorrectlyRedirectsOnMenuItemClick(
            MESSAGING,
            hasComponent(CoachesListActivity::class.java.name)
        )
    }

    @Test
    fun dashboardCorrectlyRedirectsOnScheduleClick() {
        dashboardCorrectlyRedirectsOnMenuItemClick(
            SCHEDULE,
            hasComponent(ScheduleActivity::class.java.name)
        )
    }

    @Test
    fun mapHasValidApiKey() {
        check(BuildConfig.MAPS_API_KEY.isNotBlank()
                && BuildConfig.MAPS_API_KEY != "YOUR_API_KEY") {
            "Maps API key not specified"
        }
    }

    @Test
    fun currentAppActivityIsDashboardContent() {
        ActivityScenario.launch<MapActivity>(defaultIntent).use {
            val context = (InstrumentationRegistry.getInstrumentation()
                .targetContext.applicationContext as CoachMeApplication)
            val mapTag = MapActivity.TestTags.MAP + context.userLocation.value.toString()
            composeTestRule.onNodeWithTag(mapTag).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
        }
    }

}