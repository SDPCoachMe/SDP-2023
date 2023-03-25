package com.github.sdpcoachme

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.UserInfo
import junit.framework.TestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
open class SelectSportsActivityTest {

    val email: String = "test@email.com"

    @get:Rule
    val composeTestRule = createAndroidComposeRule<SelectSportsActivity>()

    @Test
    fun iconsIniallyNotDisplayed() {
        val launchSignup = Intent(ApplicationProvider.getApplicationContext(), SelectSportsActivity::class.java)
        launchSignup.putExtra("email", email)
        ActivityScenario.launch<SignupActivity>(launchSignup).use {
            SelectSportsActivity.TestTags.MultiSelectListTag.ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ICON).assertDoesNotExist()
            }
        }
    }

    // todo comprendre pourquoi le test ne passe pas
    @Test
    fun iconsDisplayedAfterClick() {
        val launchSignup = Intent(ApplicationProvider.getApplicationContext(), SelectSportsActivity::class.java)
        launchSignup.putExtra("email", email)
        ActivityScenario.launch<SignupActivity>(launchSignup).use {
            SelectSportsActivity.TestTags.MultiSelectListTag.ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ROW).performClick()
                composeTestRule.onNodeWithTag(it.ICON).assertExists()
            }
        }
    }

    @Test
    fun correctInitialScreenContent() {
        val launchSignup = Intent(ApplicationProvider.getApplicationContext(),
            SelectSportsActivity::class.java)
        launchSignup.putExtra("email", email)
        ActivityScenario.launch<SignupActivity>(launchSignup).use {
            SelectSportsActivity.TestTags.MultiSelectListTag.ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ROW).assertIsDisplayed()
            }
            composeTestRule.onNodeWithTag(SelectSportsActivity.TestTags.COLUMN).assertIsDisplayed()
            composeTestRule.onNodeWithTag(SelectSportsActivity.TestTags.LIST_TITLE).assertIsDisplayed()
            composeTestRule.onNodeWithTag(SelectSportsActivity.TestTags.Buttons.REGISTER)
                .assertIsDisplayed()
        }
    }

    @Test
    fun userInfoUpdatedWithSelectedSports() {
        val userInfo = UserInfo(
            firstName = "John",
            lastName = "Doe",
            email = email,
            location = "Paris",
            phone = "0123456789",
            sports = listOf()
        )
        val database = (InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as CoachMeApplication).database

        val updatedUser =
            database.addUser(userInfo)
                .thenApply {
                    val launchSignup = Intent(ApplicationProvider.getApplicationContext(),
                        SelectSportsActivity::class.java)
                    launchSignup.putExtra("email", email)
                    ActivityScenario.launch<SignupActivity>(launchSignup).use {
                        SelectSportsActivity.TestTags.MultiSelectListTag.ROW_TEXT_LIST.forEach {
                            composeTestRule.onNodeWithTag(it.ROW).performClick()
                        }
                        composeTestRule.onNodeWithTag(SelectSportsActivity.TestTags.Buttons.REGISTER)
                            .performClick()
                    }
                }.thenCompose {
                    database.getUser(email)
                }.get(10, TimeUnit.SECONDS)

        TestCase.assertEquals(updatedUser.sports, Sports.values().toList())
    }



}