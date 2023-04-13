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
import com.github.sdpcoachme.DashboardActivity.TestTags.Buttons.Companion.COACHES_LIST
import com.github.sdpcoachme.DashboardActivity.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.DashboardActivity.TestTags.Buttons.Companion.LOGOUT
import com.github.sdpcoachme.DashboardActivity.TestTags.Buttons.Companion.MESSAGING
import com.github.sdpcoachme.DashboardActivity.TestTags.Buttons.Companion.PROFILE
import com.github.sdpcoachme.DashboardActivity.TestTags.Companion.DASHBOARD_EMAIL
import com.github.sdpcoachme.DashboardActivity.TestTags.Companion.DRAWER_HEADER
import com.github.sdpcoachme.DashboardActivity.TestTags.Companion.MAP
import com.github.sdpcoachme.DashboardActivity.TestTags.Companion.MENU_LIST
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardActivityTest {

    private val EXISTING_EMAIL = "example@email.com"

    private val database = (InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as CoachMeApplication).database

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
    fun errorPageIsShownWhenEditProfileIsLaunchedWithEmptyCurrentEmail() {
        database.setCurrentEmail("")
        ActivityScenario.launch<DashboardActivity>(Intent(ApplicationProvider.getApplicationContext(), ScheduleActivity::class.java)).use {
            // not possible to use Intents.init()... to check if the correct intent
            // is launched as the intents are launched from within the onCreate function
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.Buttons.GO_TO_LOGIN_BUTTON).assertIsDisplayed()
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.TextFields.ERROR_MESSAGE_FIELD).assertIsDisplayed()
        }
    }

    @Test
    fun drawerOpensOnMenuClick() {
        val launchDashboard = Intent(
            ApplicationProvider.getApplicationContext(),
            DashboardActivity::class.java
        )
        ActivityScenario.launch<DashboardActivity>(launchDashboard).use {
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsDisplayed()
        }
    }

    @Test
    fun drawerClosesOnLeftSwipe() {
        val launchDashboard = Intent(
            ApplicationProvider.getApplicationContext(),
            DashboardActivity::class.java
        )
        ActivityScenario.launch<DashboardActivity>(launchDashboard).use {
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
            // closes on left swipe
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsDisplayed()
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
        }
    }

    @Test
    fun drawerIgnoresInvalidSwipe() {
        val launchDashboard = Intent(
            ApplicationProvider.getApplicationContext(),
            DashboardActivity::class.java
        )
        ActivityScenario.launch<DashboardActivity>(launchDashboard).use {
            // ignores right swipe if opened
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
            composeTestRule.onRoot().performTouchInput { swipeRight() }
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsDisplayed()
        }
    }

    @Test
    fun drawerClosesOnOutsideTouch() {
        val launchDashboard = Intent(
            ApplicationProvider.getApplicationContext(),
            DashboardActivity::class.java
        )
        ActivityScenario.launch<DashboardActivity>(launchDashboard).use {
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
        val launchDashboard = Intent(
            ApplicationProvider.getApplicationContext(),
            DashboardActivity::class.java
        )
        ActivityScenario.launch<DashboardActivity>(launchDashboard).use {
            composeTestRule.onNodeWithTag(MENU_LIST).onChildren().assertAll(hasClickAction())
        }
    }

    @Test
    fun dashboardDisplaysCorrectEmailFromReceivedIntent() {
        val email = EXISTING_EMAIL
        val launchDashboard = Intent(
            ApplicationProvider.getApplicationContext(),
            DashboardActivity::class.java
        )
        ActivityScenario.launch<DashboardActivity>(launchDashboard).use {
            composeTestRule.onNodeWithTag(DASHBOARD_EMAIL).assert(hasText(text = email))
        }
    }
    private fun dashboardCorrectlyRedirectsOnMenuItemClick(
        tag: String,
        intentMatcher: Matcher<Intent>
    ) {
        val launchDashboard = Intent(
            ApplicationProvider.getApplicationContext(),
            DashboardActivity::class.java
        )
        ActivityScenario.launch<DashboardActivity>(launchDashboard).use {
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

    // TODO add more tests for the other menu items

    @Test
    fun mapHasValidApiKey() {
        check(BuildConfig.MAPS_API_KEY.isNotBlank()
                && BuildConfig.MAPS_API_KEY != "YOUR_API_KEY") {
            "Maps API key not specified"
        }
    }

    @Test
    fun onlyMapIsDisplayedOnCreation() {
        val launchDashboard = Intent(
            ApplicationProvider.getApplicationContext(),
            DashboardActivity::class.java
        )
        ActivityScenario.launch<DashboardActivity>(launchDashboard).use {
            composeTestRule.onNodeWithTag(MAP).assertIsDisplayed()
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
        }
    }

}