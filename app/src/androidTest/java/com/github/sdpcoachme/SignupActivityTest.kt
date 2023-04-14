package com.github.sdpcoachme

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.*
import com.github.sdpcoachme.SignupActivity.TestTags.Buttons.Companion.BE_COACH
import com.github.sdpcoachme.SignupActivity.TestTags.Buttons.Companion.SIGN_UP
import com.github.sdpcoachme.SignupActivity.TestTags.TextFields.Companion.FIRST_NAME
import com.github.sdpcoachme.SignupActivity.TestTags.TextFields.Companion.LAST_NAME
import com.github.sdpcoachme.SignupActivity.TestTags.TextFields.Companion.PHONE
import com.github.sdpcoachme.UserLocationSamples.Companion.LAUSANNE
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.data.UserLocation
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity
import junit.framework.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
open class SignupActivityTest {

    @get:Rule
    var testName = TestName()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<SignupActivity>

    private val database = (getInstrumentation().targetContext.applicationContext as CoachMeApplication).database
    private val defaultEmail = "example@email.com"
    private val exceptionEmail = "throw@Exception.com"

    private val initiallyDisplayed = listOf(
        FIRST_NAME,
        LAST_NAME,
        PHONE,
        SIGN_UP,
    )

    @Before
    fun setup() {
        if (testName.methodName.equals("errorPageIsShownWhenSignupIsLaunchedWithEmptyCurrentEmail")) {
            database.setCurrentEmail("")
        } else if (testName.methodName.equals("errorPageIsShownWhenDBThrowsException")) {
            database.setCurrentEmail(exceptionEmail)
        } else {
            database.setCurrentEmail(defaultEmail)
        }
        val launchSignup = Intent(ApplicationProvider.getApplicationContext(), SignupActivity::class.java)
        scenario = ActivityScenario.launch(launchSignup)
        Intents.init()
    }

    // Necessary to always release the Intents and close the scenario
    @After
    fun cleanup() {
        Intents.release()
        scenario.close()
    }

    @Test
    fun assertThatTheNodesExist() {
        initiallyDisplayed.forEach { tag ->
            composeTestRule.onNodeWithTag(tag).assertExists("No $tag field")
        }
    }

    @Test
    fun errorPageIsShownWhenSignupIsLaunchedWithEmptyCurrentEmail() {
        // not possible to use Intents.init()... to check if the correct intent
        // is launched as the intents are launched from within the onCreate function
        composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.Buttons.GO_TO_LOGIN_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.TextFields.ERROR_MESSAGE_FIELD).assertIsDisplayed()
    }

    @Test
    fun setAndGetUser() {
        val user = UserInfo(
            "Jean", "Dupont",
            defaultEmail, "0692000000",
            LAUSANNE, false, emptyList()
        )
        inputUserInfo(user)

        // Wait for activity to send to database
        scenario.onActivity { activity ->
            activity.databaseStateSending.get(10, TimeUnit.SECONDS)
        }

        // Important note: this get method was used instead of onTimeout due to onTimeout not
        // being found when running tests on Cirrus CI even with java version changed in build.gradle
        val retrievedUser = database.getUser(user.email).get(10, TimeUnit.SECONDS)
        assertEqualsExceptLocation(user, retrievedUser)

        // Assert that we are redirected to the SelectSportsActivity with correct intent
        Intents.intended(IntentMatchers.hasComponent(SelectSportsActivity::class.java.name))
    }

    @Test
    fun setAndGetUserAsCoachWorks() {
        val user = UserInfo(
            "Jean", "Dupont",
            defaultEmail, "0692000000",
            LAUSANNE, true,
            emptyList()
        )
        inputUserInfo(user)

        // Wait for activity to send to database
        scenario.onActivity { activity ->
            activity.databaseStateSending.get(10, TimeUnit.SECONDS)
        }

        // Important note: this get method was used instead of onTimeout due to onTiemout not
        // being found when running tests on Cirrus CI even with java version changed in build.gradle
        val retrievedUser = database.getUser(user.email).get(10, TimeUnit.SECONDS)
        assertEqualsExceptLocation(user, retrievedUser)

        // Assert that we are redirected to the Dashboard with correct intent
        Intents.intended(IntentMatchers.hasComponent(SelectSportsActivity::class.java.name))
    }

    @Test
    fun errorPageIsShownWhenDBThrowsException() {
        val user = UserInfo(
            "Jean", "Dupont",
            exceptionEmail, "0692000000",
            LAUSANNE, false,
            emptyList()
        )
        inputUserInfo(user)

        // Wait for activity to send to database
        scenario.onActivity { activity ->
            var exceptionThrown = false
            activity.databaseStateSending.handle { _, _ ->
                exceptionThrown = true
                // Recover from exception
                null
            }.get(10, TimeUnit.SECONDS)
            // Make sure exception was thrown
            TestCase.assertTrue(exceptionThrown)
        }

        composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.Buttons.GO_TO_LOGIN_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.TextFields.ERROR_MESSAGE_FIELD).assertIsDisplayed()
    }

    private val autocompleteLocationInput = "Times Square"
    private fun inputUserInfo(user: UserInfo) {
        composeTestRule.onNodeWithTag(FIRST_NAME).performTextInput(user.firstName)
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithTag(LAST_NAME).performTextInput(user.lastName)
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithTag(PHONE).performTextInput(user.phone)
        Espresso.closeSoftKeyboard()
        if (user.coach)
            composeTestRule.onNodeWithTag(BE_COACH).performClick()

        composeTestRule.onNodeWithTag(SIGN_UP).performClick()

        // Testing Google Places Autocomplete Activity requires UI automator
        // Note: this is very dirty, but I could not find a better way to do it
        val device = UiDevice.getInstance(getInstrumentation())
        device.findObject(By.text("Search")).setText(autocompleteLocationInput)
        Espresso.closeSoftKeyboard()
        device.waitForIdle(3000)
        device.pressEnter()
        device.pressEnter()
        device.pressEnter()
    }

    // Do not compare location, as using the autocomplete activity might give unexpected location results
    private fun assertEqualsExceptLocation(user1: UserInfo, user2: UserInfo) {
        TestCase.assertEquals(user1.copy(location = UserLocation()), user2.copy(location = UserLocation()))
    }
}