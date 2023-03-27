package com.github.sdpcoachme

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.github.sdpcoachme.SignupActivity.TestTags.Buttons.Companion.BE_COACH
import com.github.sdpcoachme.SignupActivity.TestTags.Buttons.Companion.SIGN_UP
import com.github.sdpcoachme.SignupActivity.TestTags.TextFields.Companion.FIRST_NAME
import com.github.sdpcoachme.SignupActivity.TestTags.TextFields.Companion.LAST_NAME
import com.github.sdpcoachme.SignupActivity.TestTags.TextFields.Companion.LOCATION
import com.github.sdpcoachme.SignupActivity.TestTags.TextFields.Companion.PHONE
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity
import junit.framework.TestCase
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
open class SignupActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val initiallyDisplayed = listOf(
        FIRST_NAME,
        LAST_NAME,
        PHONE,
        LOCATION,
        SIGN_UP,
    )

    @Test
    fun assertThatTheNodesExist() {
        val launchSignup = Intent(ApplicationProvider.getApplicationContext(), SignupActivity::class.java)
        val email = "example@email.com"

        launchSignup.putExtra("email", email)
        ActivityScenario.launch<SignupActivity>(launchSignup).use {
            Intents.init()

            initiallyDisplayed.forEach { tag ->
                composeTestRule.onNodeWithTag(tag).assertExists("No $tag field")
            }

            Intents.release()
        }
    }

    @Test
    fun errorPageIsShownWhenSignupPageIsLaunchedWithoutEmailAsExtra() {
        ActivityScenario.launch<DashboardActivity>(Intent(ApplicationProvider.getApplicationContext(), DashboardActivity::class.java)).use {
            // not possible to use Intents.init()... to check if the correct intent
            // is launched as the intents are launched from within the onCreate function
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.Buttons.GO_TO_LOGIN_BUTTON).assertIsDisplayed()
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.TextFields.ERROR_MESSAGE_FIELD).assertIsDisplayed()
        }
    }

    @Test
    fun setAndGetUser() {
        val launchSignup = Intent(ApplicationProvider.getApplicationContext(), SignupActivity::class.java)
        val email = "jc@gmail.com"
        launchSignup.putExtra("email", email)
        ActivityScenario.launch<SignupActivity>(launchSignup).use {
            Intents.init()

            // Correct way to get application context without issues
            val database = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoachMeApplication).database
            val user = UserInfo(
                "Jean", "Dupont",
                email, "0692000000",
                "Lausanne", false, listOf())
            inputUserInfo(user)

            // Important note: this get method was used instead of onTimeout due to onTimeout not
            // being found when running tests on Cirrus CI even with java version changed in build.gradle
            val retrievedUser = database.getUser(user.email).get(10, TimeUnit.SECONDS)
            TestCase.assertEquals(user, retrievedUser)

            // Assert that we are redirected to the Dashboard with correct intent
            Intents.intended(allOf(IntentMatchers.hasComponent(SelectSportsActivity::class.java.name), hasExtra("email", user.email)))

            Intents.release()
        }
    }

    @Test
    fun setAndGetUserAsCoachWorks() {
        val launchSignup = Intent(ApplicationProvider.getApplicationContext(), SignupActivity::class.java)
        val email = "jc@gmail.com"
        launchSignup.putExtra("email", email)
        ActivityScenario.launch<SignupActivity>(launchSignup).use {
            Intents.init()

            // Correct way to get application context without issues
            val database = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoachMeApplication).database
            val user = UserInfo(
                "Jean", "Dupont",
                email, "0692000000",
                "Lausanne", true, listOf())
            inputUserInfo(user)

            // Important note: this get method was used instead of onTimeout due to onTiemout not
            // being found when running tests on Cirrus CI even with java version changed in build.gradle
            val retrievedUser = database.getUser(user.email).get(10, TimeUnit.SECONDS)
            TestCase.assertEquals(user, retrievedUser)

            // Assert that we are redirected to the Dashboard with correct intent
            Intents.intended(allOf(IntentMatchers.hasComponent(SelectSportsActivity::class.java.name), hasExtra("email", user.email)))

            Intents.release()
        }
    }

    @Test
    fun afterDbExceptionUserStaysInSignUpActivity() {
        val launchSignup = Intent(ApplicationProvider.getApplicationContext(), SignupActivity::class.java)
        val email = "throw@Exception.com"

        launchSignup.putExtra("email", email)
        ActivityScenario.launch<SignupActivity>(launchSignup).use {
            Intents.init()

            val user = UserInfo(
                "Jean", "Dupont",
                email, "0692000000",
                "Lausanne", false, listOf()
            )
            inputUserInfo(user)

            // Assert that we are still in the SignupActivity
            initiallyDisplayed.forEach { tag ->
                composeTestRule.onNodeWithTag(tag).assertExists("No $tag field")
            }

            Intents.release()
        }
    }

    private fun inputUserInfo(user: UserInfo) {
        composeTestRule.onNodeWithTag(FIRST_NAME).performTextInput(user.firstName)
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithTag(LAST_NAME).performTextInput(user.lastName)
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithTag(PHONE).performTextInput(user.phone)
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithTag(LOCATION).performTextInput(user.location)
        Espresso.closeSoftKeyboard()
        if (user.isCoach)
            composeTestRule.onNodeWithTag(BE_COACH).performClick()

        composeTestRule.onNodeWithTag(SIGN_UP).performClick()
    }
}