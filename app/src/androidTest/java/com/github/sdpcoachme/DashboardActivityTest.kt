package com.github.sdpcoachme

import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<DashboardActivity>()

    @Test
    fun drawerOpensOnMenuClick() {
        composeTestRule.onNodeWithTag("drawerHeader").assertIsNotDisplayed()
        composeTestRule.onNodeWithTag("appBarMenuIcon").performClick()
        composeTestRule.onNodeWithTag("drawerHeader").assertIsDisplayed()
    }

    @Test
    fun drawerReactsOnCorrectSwipe() {
        // opens on right swipe
        composeTestRule.onNodeWithTag("drawerHeader").assertIsNotDisplayed()
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.onNodeWithTag("drawerHeader").assertIsDisplayed()
        // closes on left swipe
        composeTestRule.onNodeWithTag("drawerHeader").assertIsDisplayed()
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.onNodeWithTag("drawerHeader").assertIsNotDisplayed()
    }

    @Test
    fun drawerIgnoresInvalidSwipe() {
        // ignores left swipe if closed
        composeTestRule.onNodeWithTag("drawerHeader").assertIsNotDisplayed()
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.onNodeWithTag("drawerHeader").assertIsNotDisplayed()
        // ignores right swipe if opened
        composeTestRule.onNodeWithTag("appBarMenuIcon").performClick()
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.onNodeWithTag("drawerHeader").assertIsDisplayed()
    }

    @Test
    fun drawerClosesOnOutsideTouch() {
        val width = composeTestRule.onRoot().getBoundsInRoot().width
        val height = composeTestRule.onRoot().getBoundsInRoot().height
        composeTestRule.onNodeWithTag("appBarMenuIcon").performClick()
        composeTestRule.onNodeWithTag("drawerHeader").assertIsDisplayed()
        composeTestRule.onRoot().performTouchInput {
            click(position = Offset(width.toPx() - 10, height.toPx() / 2))
        }
        composeTestRule.onNodeWithTag("drawerHeader").assertIsNotDisplayed()
    }

    @Test
    fun drawerBodyContainsClickableMenu() {
        composeTestRule.onNodeWithTag("menuList").onChildren().assertAll(hasClickAction())
    }

    @Test
    fun dashboardDisplaysCorrectEmailFromReceivedIntent() {
        val email = "john.lennon@gmail.com"
        val launchDashboard = Intent(
            ApplicationProvider.getApplicationContext(),
            DashboardActivity::class.java
        )
        launchDashboard.putExtra("email", email)
        ActivityScenario.launch<DashboardActivity>(launchDashboard).use {
            composeTestRule.onNodeWithTag("dashboardEmail").assert(hasText(text = email))
        }
    }

    // TODO refactor this in some way to allow for test tags in menu items
    // TODO those tests are very similar, could be merged into one function
    @Test
    fun dashboardCorrectlyRedirectsOnProfileClick() {
        val email = "john.lennon@gmail.com"
        val launchDashboard = Intent(
            ApplicationProvider.getApplicationContext(),
            DashboardActivity::class.java
        )
        launchDashboard.putExtra("email", email)
        ActivityScenario.launch<DashboardActivity>(launchDashboard).use {
            Intents.init()

            val rootNode = composeTestRule.onNodeWithTag("menuList")
            // TODO refactor this in some way to allow for test tags in menu items
            rootNode.onChildAt(1).performClick()
            intended(allOf(hasComponent(EditProfileActivity::class.java.name), hasExtra("email", email)))
            Intents.release()
        }
    }

    @Test
    fun dashboardCorrectlyRedirectsOnLogOutClick() {
        Intents.init()

        val rootNode = composeTestRule.onNodeWithTag("menuList")
        // TODO refactor this in some way to allow for test tags in menu items
        rootNode.onChildAt(5).performClick()
        intended(hasComponent(LoginActivity::class.java.name))

        Intents.release()
    }
    // TODO add more tests for the other menu items

}