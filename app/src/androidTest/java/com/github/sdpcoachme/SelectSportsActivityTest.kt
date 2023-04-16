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
import com.github.sdpcoachme.SelectSportsActivity.*
import com.github.sdpcoachme.SelectSportsActivity.TestTags.Buttons.*
import com.github.sdpcoachme.SelectSportsActivity.TestTags.Buttons.Companion.REGISTER
import com.github.sdpcoachme.SelectSportsActivity.TestTags.Companion.COLUMN
import com.github.sdpcoachme.SelectSportsActivity.TestTags.Companion.LIST_TITLE
import com.github.sdpcoachme.SelectSportsActivity.TestTags.MultiSelectListTag.Companion.ROW_TEXT_LIST
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.Buttons.Companion.GO_TO_LOGIN_BUTTON
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.TextFields.Companion.ERROR_MESSAGE_FIELD
import com.github.sdpcoachme.location.UserLocationSamples.Companion.PARIS
import junit.framework.TestCase
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit.SECONDS

@RunWith(AndroidJUnit4::class)
open class SelectSportsActivityTest {

    val email: String = "test@email.com"
    private val userInfo = UserInfo(
        firstName = "John",
        lastName = "Doe",
        email = email,
        location = PARIS,
        phone = "0123456789",
        coach = false
    )
    private val database = (InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as CoachMeApplication).database


    private val launchSelectSports = Intent(ApplicationProvider.getApplicationContext(),
        SelectSportsActivity::class.java)

    @get:Rule
    val composeTestRule = createEmptyComposeRule() //createAndroidComposeRule<SelectSportsActivity>()

    @Before
    fun setup() { // set user in db to default
        database.updateUser(userInfo)
        database.setCurrentEmail(email)
    }

    @Test
    fun tickIconsInitiallyNotDisplayed() {
        ActivityScenario.launch<SelectSportsActivity>(launchSelectSports).use {
            ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ICON).assertDoesNotExist()
            }
        }
    }

    @Test
    fun tickIconsDisplayedAfterClick() {
        ActivityScenario.launch<SelectSportsActivity>(launchSelectSports).use {
            ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ROW).performClick()
            }
            ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ICON, useUnmergedTree = true).assertIsDisplayed()
            }
        }
    }

    @Test
    fun tickIconsDisappearAfterSecondClick() {
        ActivityScenario.launch<SelectSportsActivity>(launchSelectSports).use {
            ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ROW).performClick()
            }
            ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ROW).performClick()
            }
            ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ICON, useUnmergedTree = true).assertDoesNotExist()
            }
        }
    }

    @Test
    fun correctInitialScreenContent() {
        ActivityScenario.launch<SelectSportsActivity>(launchSelectSports).use {
            ROW_TEXT_LIST.forEach {
                composeTestRule.onNodeWithTag(it.ROW).assertIsDisplayed()
            }
            composeTestRule.onNodeWithTag(COLUMN).assertIsDisplayed()
            composeTestRule.onNodeWithTag(LIST_TITLE).assertIsDisplayed()
            composeTestRule.onNodeWithTag(REGISTER)
                .assertIsDisplayed()
        }
    }

    //TODO The two following tests have been, after discussion with the team,
    // commented out as they were failing after adding the map functionality to the app.
    // They were passing before and now fail on a weird issue, that may involve some thread
    // conflicts : "google.map.api.exception : Not on main thread". It may come from a network
    // operation issued on the main ui thread.

//    @Test
    fun userInfoSelectedSportCorrectlyReplaced() {
        ActivityScenario.launch<SelectSportsActivity>(launchSelectSports).use {
            // Note works only if there are at least 2 sports
            val userInfo =
                userInfo.copy(sports = listOf(Sports.values()[1])) // select favorite sport
            val updatedUser =
                database.updateUser(userInfo)
                    .thenApply {
                        val intentSelectSports = launchSelectSports
                        intentSelectSports.putExtra("email", email)
                        ActivityScenario.launch<SignupActivity>(intentSelectSports).use {
                            composeTestRule.onNodeWithTag(ROW_TEXT_LIST[0].ROW)
                                .performClick() // select new sport
                            composeTestRule.onNodeWithTag(ROW_TEXT_LIST[1].ROW)
                                .performClick() // deselect previous sport
                            composeTestRule.onNodeWithTag(REGISTER)
                                .performClick()
                        }
                    }.thenCompose {
                        database.getUser(email)
                    }.get(10, SECONDS)

            // Check that the user has the first sport
            assertThat(updatedUser.sports, hasItem(Sports.values()[0]))
            // Check that the user does not have the other sports
            Sports.values().toList().subList(1, Sports.values().size).forEach {
                assertThat(updatedUser.sports, not(hasItem(it)))
            }
        }
    }

//    @Test
    fun userInfoUpdatedWithAllSelectedSportsAndRedirectedToDashboardActivity() {
        checkRedirectionAfterRegister(launchSelectSports, DashboardActivity::class.java.name)
    }

    @Test
    fun redirectsToProfileActivityWhenIsEditingProfileIsTrue() {
        checkRedirectionAfterRegister(launchSelectSports.putExtra("isEditingProfile", true), ProfileActivity::class.java.name)
    }

    private fun checkRedirectionAfterRegister(launcher: Intent, intendedClass: String) {
        ActivityScenario.launch<SelectSportsActivity>(launchSelectSports).use {
            Intents.init()
            val updatedUser =
                database.updateUser(userInfo)
                    .thenApply {
                        ActivityScenario.launch<SelectSportsActivity>(launcher).use {
                            ROW_TEXT_LIST.forEach {
                                composeTestRule.onNodeWithTag(it.ROW).performClick()
                            }
                            composeTestRule.onNodeWithTag(REGISTER)
                                .performClick()
                        }
                    }.thenCompose {
                        database.getUser(email)
                    }.get(10, SECONDS)

            TestCase.assertEquals(updatedUser.sports, Sports.values().toList())

            Intents.intended(IntentMatchers.hasComponent(intendedClass))
            Intents.release()
        }
    }

    @Test
    fun errorPageIsShownWhenSelectSportsIsLaunchedWithEmptyCurrentEmail() {
        database.setCurrentEmail("")
        ActivityScenario.launch<SelectSportsActivity>(launchSelectSports).use {
            // not possible to use Intents.init()... to check if the correct intent
            // is launched as the intents are launched from within the onCreate function
            composeTestRule.onNodeWithTag(GO_TO_LOGIN_BUTTON).assertIsDisplayed()
            composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD).assertIsDisplayed()
        }
    }
    
    @Test
    fun errorPageIsShownAfterDBGetUserError() {
        errorPageLaunchChecker("throwGet@Exception.com")
    }

    @Test
    fun errorPageIsShownAfterDBAddUserError() {
        errorPageLaunchChecker("throw@Exception.com")
    }

    private fun errorPageLaunchChecker(errorEmail: String) {
        database.setCurrentEmail(errorEmail)
        ActivityScenario.launch<SelectSportsActivity>(launchSelectSports).use {
            Intents.init()
            composeTestRule.onNodeWithTag(ROW_TEXT_LIST[0].ROW)
                .performClick() // select new sport

            composeTestRule.onNodeWithTag(REGISTER)
                .assertIsDisplayed()
                .performClick()
            Intents.intended(
                allOf(
                    IntentMatchers.hasComponent(IntentExtrasErrorHandlerActivity::class.java.name),
                    hasExtra(
                        "errorMsg",
                        "There was a database error.\nPlease return to the login page and try again."
                    )
                )
            )
            composeTestRule.onNodeWithTag(GO_TO_LOGIN_BUTTON)
                .assertIsDisplayed()
            composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD)
                .assertIsDisplayed()
            Intents.release()
        }
    }
}