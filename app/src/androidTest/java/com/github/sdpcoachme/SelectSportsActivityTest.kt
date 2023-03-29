package com.github.sdpcoachme

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.UserInfo
import junit.framework.TestCase
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
open class SelectSportsActivityTest {

    val email: String = "test@email.com"
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
    val launchSignup = Intent(ApplicationProvider.getApplicationContext(),
        SelectSportsActivity::class.java)
        .putExtra("email", email)

    @get:Rule
    val composeTestRule = createEmptyComposeRule() //createAndroidComposeRule<SelectSportsActivity>()

    @Before
    fun setup() { // set user in db to default
        database.addUser(userInfo)
    }

    @Test
    fun tickIconsInitiallyNotDisplayed() {
        ActivityScenario.launch<SignupActivity>(launchSignup).use {
            SelectSportsActivity.TestTags.MultiSelectListTag.ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ICON).assertDoesNotExist()
            }
        }
    }

    @Test
    fun tickIconsDisplayedAfterClick() {
        ActivityScenario.launch<SignupActivity>(launchSignup).use {
            SelectSportsActivity.TestTags.MultiSelectListTag.ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ROW).performClick()
            }
            SelectSportsActivity.TestTags.MultiSelectListTag.ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ICON, useUnmergedTree = true).assertIsDisplayed()
            }
        }
    }

    @Test
    fun tickIconsDisappearAfterSecondClick() {
        ActivityScenario.launch<SignupActivity>(launchSignup).use {
            SelectSportsActivity.TestTags.MultiSelectListTag.ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ROW).performClick()
            }
            SelectSportsActivity.TestTags.MultiSelectListTag.ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ROW).performClick()
            }
            SelectSportsActivity.TestTags.MultiSelectListTag.ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ICON, useUnmergedTree = true).assertDoesNotExist()
            }
        }
    }

    @Test
    fun correctInitialScreenContent() {
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
    fun userInfoSelectedSportCorrectlyReplaced() {
        ActivityScenario.launch<SignupActivity>(launchSignup).use {
            // Note works only if there are at least 2 sports
            val userInfo =
                userInfo.copy(sports = listOf(Sports.values()[1])) // select favorite sport
            val updatedUser =
                database.addUser(userInfo)
                    .thenApply {
                        val launchSignup = Intent(
                            ApplicationProvider.getApplicationContext(),
                            SelectSportsActivity::class.java
                        )
                        launchSignup.putExtra("email", email)
                        ActivityScenario.launch<SignupActivity>(launchSignup).use {
                            composeTestRule.onNodeWithTag(SelectSportsActivity.TestTags.MultiSelectListTag.ROW_TEXT_LIST[0].ROW)
                                .performClick() // select new sport
                            composeTestRule.onNodeWithTag(SelectSportsActivity.TestTags.MultiSelectListTag.ROW_TEXT_LIST[1].ROW)
                                .performClick() // deselect previous sport
                            composeTestRule.onNodeWithTag(SelectSportsActivity.TestTags.Buttons.REGISTER)
                                .performClick()
                        }
                    }.thenCompose {
                        database.getUser(email)
                    }.get(10, TimeUnit.SECONDS)

            // Check that the user has the first sport
            assertThat(updatedUser.sports, hasItem(Sports.values()[0]))
            // Check that the user does not have the other sports
            Sports.values().toList().subList(1, Sports.values().size).forEach {
                assertThat(updatedUser.sports, not(hasItem(it)))
            }
        }
    }

    @Test
    fun userInfoUpdatedWithAllSelectedSportsAndRedirectedToDashboardActivity() {
        checkRedirectionAfterRegister(launchSignup, DashboardActivity::class.java.name)
    }

    @Test
    fun redirectsToProfileActivityWhenIsEditingProfileIsTrue() {
        checkRedirectionAfterRegister(launchSignup.putExtra("isEditingProfile", true), EditProfileActivity::class.java.name)
    }

    private fun checkRedirectionAfterRegister(launcher: Intent, intendedClass: String) {
        ActivityScenario.launch<SignupActivity>(launchSignup).use {
            Intents.init()
            val updatedUser =
                database.addUser(userInfo)
                    .thenApply {
                        ActivityScenario.launch<SignupActivity>(launcher).use {
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

            Intents.intended(
                allOf(
                    IntentMatchers.hasComponent(intendedClass),
                    hasExtra("email", email)
                )
            )
            Intents.release()
        }
    }
}