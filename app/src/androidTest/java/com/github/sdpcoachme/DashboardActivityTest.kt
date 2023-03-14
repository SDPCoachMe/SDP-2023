package com.github.sdpcoachme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
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

}