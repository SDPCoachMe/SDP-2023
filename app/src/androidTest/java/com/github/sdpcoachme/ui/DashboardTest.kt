package com.github.sdpcoachme.ui

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.github.sdpcoachme.BuildConfig
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.CoachMeTestApplication
import com.github.sdpcoachme.auth.LoginActivity
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.database.MockDatabase
import com.github.sdpcoachme.location.MapActivity
import com.github.sdpcoachme.profile.CoachesListActivity
import com.github.sdpcoachme.profile.ProfileActivity
import com.github.sdpcoachme.schedule.ScheduleActivity
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.COACHES_LIST
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.LOGOUT
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.MESSAGING
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.PLAN
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.PROFILE
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.SCHEDULE
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.DASHBOARD_EMAIL
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.DASHBOARD_NAME
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.DRAWER_HEADER
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.MENU_LIST
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Test class for the Dashboard. But the tests of this class should only
 * be specific to the dashboard functionality and stay generic to other activities.
 */
@RunWith(AndroidJUnit4::class)
class DashboardTest {

    private val EXISTING_EMAIL = MockDatabase.getDefaultEmail()
    private val EXISTING_NAME = "${MockDatabase.getDefaultUser().firstName} ${MockDatabase.getDefaultUser().lastName}"

    private lateinit var store: CachingStore
    /*private val store = (InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as CoachMeApplication).store*/

    @get:Rule
    val composeTestRule = createComposeRule()

    // WARNING : this rule will try to grant permissions on the device.
    // Make sure to "Allow granting permissions and simulating input via USB debugging"
    // in the device settings (sometimes called "Permission surveillance").
    // If the device does not disable permission surveillance, the test will crash.
    @get:Rule
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(ACCESS_FINE_LOCATION)

    @Before
    fun initIntents() {
        (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).clearDataStoreAndResetCachingStore()
        store = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoachMeApplication).store
        store.retrieveData.get(1, TimeUnit.SECONDS)
        store.setCurrentEmail(EXISTING_EMAIL).get(100, TimeUnit.MILLISECONDS)
        Intents.init()
    }

    @After
    fun releaseIntents() {
        store.setCurrentEmail("")
        ApplicationProvider.getApplicationContext<CoachMeTestApplication>().clearDataStoreAndResetCachingStore()
        Intents.release()
    }

    /**
     * Helper method to set the Dashboard to our composeTestRule
     */
    private fun setUpDashboard(withoutEmail: Boolean = false) {
        if (withoutEmail) {
            store.setCurrentEmail("").get(1, TimeUnit.SECONDS)
        }
        val UIDisplayed = CompletableFuture<Void>()
        composeTestRule.setContent {
            Dashboard(UIDisplayed = UIDisplayed) {}
        }
        UIDisplayed.get(1000, TimeUnit.MILLISECONDS)
    }

    /**
     * Helper method to set the Dashboard with an arbitrarily chosen activity, to provide an
     * application context
     */
    private fun setUpDashboardWithActivityContext(): ActivityScenario<MapActivity>? {
        val mockActivity = MapActivity::class.java
        val mockIntent = Intent(ApplicationProvider.getApplicationContext(), mockActivity)
        return ActivityScenario.launch(mockIntent)
    }

    @Test
    fun drawerOpensOnMenuClick() {
        setUpDashboard()
        composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
        composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsDisplayed()
    }

    @Test
    fun drawerClosesOnLeftSwipe() {
        setUpDashboard()
        composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
        // closes on left swipe
        composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsDisplayed()
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
    }

    @Test
    fun drawerIgnoresInvalidSwipe() {
        setUpDashboard()
        // ignores right swipe if opened
        composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsDisplayed()
    }

    @Test
    fun drawerClosesOnOutsideTouch() {
        setUpDashboard()
        val width = composeTestRule.onRoot().getBoundsInRoot().width
        val height = composeTestRule.onRoot().getBoundsInRoot().height
        composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
        composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsDisplayed()
        composeTestRule.onRoot().performTouchInput {
            click(position = Offset(width.toPx() - 10, height.toPx() / 2))
        }
        composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
    }

    @Test
    fun drawerBodyContainsClickableMenu() {
        setUpDashboard()
        composeTestRule.onNodeWithTag(MENU_LIST).onChildren().assertAll(hasClickAction())
    }

    @Test
    fun dashboardDisplaysCorrectEmailFromReceivedIntent() {
        setUpDashboard()
        composeTestRule.onNodeWithTag(DASHBOARD_EMAIL).assert(hasText(text = EXISTING_EMAIL))
    }

    @Test
    fun dashboardDisplaysCorrectNameOfUser() {
        setUpDashboard()
        composeTestRule.onNodeWithTag(DASHBOARD_NAME).assert(hasText(text = EXISTING_NAME))
    }

    private fun dashboardCorrectlyRedirectsOnMenuItemClick(
        tag: String,
        intentMatcher: Matcher<Intent>
    ) {
        composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
        composeTestRule.onNodeWithTag(tag).performClick()
        intended(intentMatcher)
    }

    @Test
    fun dashboardCorrectlyRedirectsOnProfileClick() {
        setUpDashboard()
        dashboardCorrectlyRedirectsOnMenuItemClick(
            PROFILE,
            hasComponent(ProfileActivity::class.java.name)
        )
    }

    @Test
    fun dashboardCorrectlyRedirectsOnPlanClick() {
        setUpDashboard()
        dashboardCorrectlyRedirectsOnMenuItemClick(
            PLAN,
            hasComponent(MapActivity::class.java.name)
        )
        pressBackUnconditionally()
    }

    @Test
    fun dashboardCorrectlyRedirectsOnLogOutClick() {
        // needs an application context here to get the authenticator
        setUpDashboardWithActivityContext().use {
            dashboardCorrectlyRedirectsOnMenuItemClick(
                LOGOUT,
                hasComponent(LoginActivity::class.java.name)
            )
        }
        pressBackUnconditionally()
    }

    @Test
    fun dashboardCorrectlyRedirectsOnCoachesListClick() {
        setUpDashboard()
        dashboardCorrectlyRedirectsOnMenuItemClick(
            COACHES_LIST,
            hasComponent(CoachesListActivity::class.java.name)
        )
    }

    @Test
    fun dashboardCorrectlyRedirectsOnMessagingClick() {
        setUpDashboard()
        dashboardCorrectlyRedirectsOnMenuItemClick(
            MESSAGING,
            hasComponent(CoachesListActivity::class.java.name)
        )
    }

    @Test
    fun dashboardCorrectlyRedirectsOnScheduleClick() {
        setUpDashboard()
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

    // This test is not very crucial here, just verifies that for some activity, the dashboard
    // behaves correctly.

    // TODO In accordance with the team, this test is failing for some reason because of a timeout on the CI
    // It is passing locally though, so we are commenting it out for now

    /*

    @Test
    fun currentAppActivityIsDashboardContent() {
        setUpDashboardWithActivityContext().use {
            val context = (InstrumentationRegistry.getInstrumentation()
                .targetContext.applicationContext as CoachMeApplication)
            val lastLocation = context.locationProvider.getLastLocation()
            val mapTag = MAP + lastLocation.value.toString()
            composeTestRule.waitUntil(5000) { lastLocation.value != null }
            composeTestRule.onNodeWithTag(mapTag).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
        }
    }

     */



}