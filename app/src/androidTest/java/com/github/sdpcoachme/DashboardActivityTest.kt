package com.github.sdpcoachme

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
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sdpcoachme.DashboardActivity.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.DashboardActivity.TestTags.Buttons.Companion.LOGOUT
import com.github.sdpcoachme.DashboardActivity.TestTags.Buttons.Companion.PROFILE
import com.github.sdpcoachme.DashboardActivity.TestTags.Companion.DASHBOARD_EMAIL
import com.github.sdpcoachme.DashboardActivity.TestTags.Companion.DRAWER_HEADER
import com.github.sdpcoachme.DashboardActivity.TestTags.Companion.MENU_LIST
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardActivityTest {
    private val EXISTING_EMAIL = "example@email.com"

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun errorPageIsShownWhenDashboardIsLaunchedWithoutEmailAsExtra() {
        ActivityScenario.launch<DashboardActivity>(Intent(ApplicationProvider.getApplicationContext(), DashboardActivity::class.java)).use {
            // not possible to use Intents.init()... to check if the correct intent
            // is launched as the intents are launched from within the onCreate function
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.Buttons.GO_TO_LOGIN_BUTTON).assertIsDisplayed()
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.TextFields.ERROR_MESSAGE_FIELD).assertIsDisplayed()
        }
    }

    @Test
    fun drawerOpensOnMenuClick() {
        val email = EXISTING_EMAIL
        val launchDashboard = Intent(
            ApplicationProvider.getApplicationContext(),
            DashboardActivity::class.java
        )
        launchDashboard.putExtra("email", email)
        ActivityScenario.launch<DashboardActivity>(launchDashboard).use {
            Intents.init()
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsDisplayed()
            Intents.release()
        }
    }

    @Test
    fun drawerReactsOnCorrectSwipe() {
        val email = EXISTING_EMAIL
        val launchDashboard = Intent(
            ApplicationProvider.getApplicationContext(),
            DashboardActivity::class.java
        )
        launchDashboard.putExtra("email", email)
        ActivityScenario.launch<DashboardActivity>(launchDashboard).use {
            Intents.init()
            // opens on right swipe
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
            composeTestRule.onRoot().performTouchInput { swipeRight() }
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsDisplayed()
            // closes on left swipe
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsDisplayed()
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
            Intents.release()
        }
    }

    @Test
    fun drawerIgnoresInvalidSwipe() {
        val email = EXISTING_EMAIL
        val launchDashboard = Intent(
            ApplicationProvider.getApplicationContext(),
            DashboardActivity::class.java
        )
        launchDashboard.putExtra("email", email)
        ActivityScenario.launch<DashboardActivity>(launchDashboard).use {
            Intents.init()
            // ignores left swipe if closed
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
            composeTestRule.onRoot().performTouchInput { swipeLeft() }
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
            // ignores right swipe if opened
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
            composeTestRule.onRoot().performTouchInput { swipeRight() }
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsDisplayed()
            Intents.release()
        }
    }

    @Test
    fun drawerClosesOnOutsideTouch() {
        val email = EXISTING_EMAIL
        val launchDashboard = Intent(
            ApplicationProvider.getApplicationContext(),
            DashboardActivity::class.java
        )
        launchDashboard.putExtra("email", email)
        ActivityScenario.launch<DashboardActivity>(launchDashboard).use {
            Intents.init()
            val width = composeTestRule.onRoot().getBoundsInRoot().width
            val height = composeTestRule.onRoot().getBoundsInRoot().height
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsDisplayed()
            composeTestRule.onRoot().performTouchInput {
                click(position = Offset(width.toPx() - 10, height.toPx() / 2))
            }
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertIsNotDisplayed()
            Intents.release()
        }
    }

    @Test
    fun drawerBodyContainsClickableMenu() {
        val email = EXISTING_EMAIL
        val launchDashboard = Intent(
            ApplicationProvider.getApplicationContext(),
            DashboardActivity::class.java
        )
        launchDashboard.putExtra("email", email)
        ActivityScenario.launch<DashboardActivity>(launchDashboard).use {
            Intents.init()
            composeTestRule.onNodeWithTag(MENU_LIST).onChildren().assertAll(hasClickAction())
            Intents.release()
        }
    }

    @Test
    fun dashboardDisplaysCorrectEmailFromReceivedIntent() {
        val email = EXISTING_EMAIL
        val launchDashboard = Intent(
            ApplicationProvider.getApplicationContext(),
            DashboardActivity::class.java
        )
        launchDashboard.putExtra("email", email)
        ActivityScenario.launch<DashboardActivity>(launchDashboard).use {
            Intents.init()
            composeTestRule.onNodeWithTag(DASHBOARD_EMAIL).assert(hasText(text = email))
            Intents.release()
        }
    }
    private fun dashboardCorrectlyRedirectsOnMenuItemClick(
        userEmail: String,
        tag: String,
        intentMatcher: Matcher<Intent>
    ) {
        val launchDashboard = Intent(
            ApplicationProvider.getApplicationContext(),
            DashboardActivity::class.java
        )
        launchDashboard.putExtra("email", userEmail)
        ActivityScenario.launch<DashboardActivity>(launchDashboard).use {
            Intents.init()
            composeTestRule.onNodeWithTag(tag).performClick()
            intended(intentMatcher)
            Intents.release()
        }
    }

    @Test
    fun dashboardCorrectlyRedirectsOnProfileClick() {
        val email = "john.lennon@gmail.com"
        dashboardCorrectlyRedirectsOnMenuItemClick(
            email,
            PROFILE,
            allOf(
                hasComponent(ProfileActivity::class.java.name),
                hasExtra("email", email)
            )
        )
    }
    @Test
    fun dashboardCorrectlyRedirectsOnLogOutClick() {
        dashboardCorrectlyRedirectsOnMenuItemClick(
            EXISTING_EMAIL,
            LOGOUT,
            hasComponent(LoginActivity::class.java.name)
        )
    }

    // TODO add more tests for the other menu items

}