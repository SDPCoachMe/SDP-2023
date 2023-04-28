package com.github.sdpcoachme.auth

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.*
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.auth.SignupActivity.TestTags.Buttons.Companion.BE_COACH
import com.github.sdpcoachme.auth.SignupActivity.TestTags.Buttons.Companion.SIGN_UP
import com.github.sdpcoachme.auth.SignupActivity.TestTags.TextFields.Companion.FIRST_NAME
import com.github.sdpcoachme.auth.SignupActivity.TestTags.TextFields.Companion.LAST_NAME
import com.github.sdpcoachme.auth.SignupActivity.TestTags.TextFields.Companion.PHONE
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.Buttons.Companion.GO_TO_LOGIN_BUTTON
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.TextFields.Companion.ERROR_MESSAGE_FIELD
import com.github.sdpcoachme.location.autocomplete.MockLocationAutocompleteHandler
import com.github.sdpcoachme.profile.SelectSportsActivity
import junit.framework.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit.SECONDS


@RunWith(AndroidJUnit4::class)
open class SignupActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<SignupActivity>

    private val database = (getInstrumentation().targetContext.applicationContext as CoachMeApplication).database
    private val defaultUser = UserInfo(
        firstName = "Jean",
        lastName = "Dupont",
        email= "example@email.com",
        phone = "0692000000",
        location = MockLocationAutocompleteHandler.DEFAULT_LOCATION, // Make sure to use this here, so that
        // the test does not fail if the default location returned by the mock autocomplete handler
        // changes
        coach = false,
        sports = emptyList() // Given that we don't input sports, we need to have this empty here
    )
    private val defaultCoach = defaultUser.copy(coach = true)
    private val exceptionUser = defaultUser.copy(email = "throw@Exception.com")

    private val initiallyDisplayed = listOf(
        FIRST_NAME,
        LAST_NAME,
        PHONE,
        SIGN_UP,
    )

    @Before
    fun setup() {
        Intents.init()
    }

    // Necessary to always release the Intents and close the scenario
    @After
    fun cleanup() {
        Intents.release()
        scenario.close()
    }

    private fun launchSignupActivity(email: String) {
        database.setCurrentEmail(email)
        val launchSignup = Intent(ApplicationProvider.getApplicationContext(), SignupActivity::class.java)
        scenario = ActivityScenario.launch(launchSignup)
    }

    @Test
    fun assertThatTheNodesExist() {
        launchSignupActivity(defaultUser.email)
        initiallyDisplayed.forEach { tag ->
            composeTestRule.onNodeWithTag(tag).assertExists("No $tag field")
        }
    }

    @Test
    fun errorPageIsShownWhenSignupIsLaunchedWithEmptyCurrentEmail() {
        launchSignupActivity("")
        // not possible to use Intents.init()... to check if the correct intent
        // is launched as the intents are launched from within the onCreate function
        composeTestRule.onNodeWithTag(GO_TO_LOGIN_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD).assertIsDisplayed()
    }

    @Test
    fun setAndGetUserAsNonCoachWorks() {
        setAndGetUser(defaultUser)
    }

    @Test
    fun setAndGetUserAsCoachWorks() {
        setAndGetUser(defaultCoach)
    }

    @Test
    fun errorPageIsShownWhenDBThrowsException() {
        launchSignupActivity(exceptionUser.email)
        inputUserInfo(exceptionUser)

        // Wait for activity to send to database
        lateinit var databaseStateSending: CompletableFuture<Void>
        scenario.onActivity {
            databaseStateSending = it.databaseStateSending
        }
        var exceptionThrown = false
        databaseStateSending.handle { _, exception ->
            exceptionThrown = exception != null
            // Recover from exception
            null
        }.get(10, SECONDS)
        // Make sure exception was thrown
        TestCase.assertTrue(exceptionThrown)

        composeTestRule.onNodeWithTag(GO_TO_LOGIN_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD).assertIsDisplayed()
    }

    private fun setAndGetUser(user: UserInfo) {
        launchSignupActivity(user.email)
        inputUserInfo(user)

        // Wait for activity to send to database
        lateinit var databaseStateSending: CompletableFuture<Void>
        scenario.onActivity {
            databaseStateSending = it.databaseStateSending
        }
        databaseStateSending.get(10, SECONDS)

        // Important note: this get method was used instead of onTimeout due to onTimeout not
        // being found when running tests on Cirrus CI even with java version changed in build.gradle
        val retrievedUser = database.getUser(user.email).get(10, SECONDS)
        TestCase.assertEquals(user, retrievedUser)

        // Assert that we are redirected to the SelectSportsActivity with correct intent
        Intents.intended(IntentMatchers.hasComponent(SelectSportsActivity::class.java.name))
    }

    private fun inputUserInfo(user: UserInfo) {
        // Put focus on first name field
        composeTestRule.onNodeWithTag(FIRST_NAME)
            .performClick()

        fillAndCheckFocus(user.firstName, FIRST_NAME)
        fillAndCheckFocus(user.lastName, LAST_NAME)
        fillAndCheckFocus(user.phone, PHONE)

        if (user.coach)
            composeTestRule.onNodeWithTag(BE_COACH).performClick()

        composeTestRule.onNodeWithTag(SIGN_UP).performClick()

        // Testing Google Places Autocomplete Activity is too complex, instead, we've mocked it
        // so that it directly returns a fixed location MockLocationAutocompleteHandler.DEFAULT_LOCATION

        // We also don't input sports, given that an obscure bug occurs in testing once the app is redirected to
        // the MapActivity. If we don't input sports, we never get redirected to the MapActivity, and no
        // obscure crash occurs.
    }

    private fun fillAndCheckFocus(text: String, tag: String) {
        composeTestRule.onNodeWithTag(tag)
            .assertIsFocused()
        composeTestRule.onNodeWithTag(tag)
            .performTextInput(text)
        composeTestRule.onNodeWithTag(tag)
            .performImeAction()
        composeTestRule.onNodeWithTag(tag)
            .assertIsNotFocused()
    }
}