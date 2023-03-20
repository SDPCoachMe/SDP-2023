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
import com.github.sdpcoachme.SignupActivity.TestTags.Buttons.Companion.SIGN_UP
import com.github.sdpcoachme.SignupActivity.TestTags.TextFields.Companion.FIRST_NAME
import com.github.sdpcoachme.SignupActivity.TestTags.TextFields.Companion.LAST_NAME
import com.github.sdpcoachme.SignupActivity.TestTags.TextFields.Companion.LOCATION
import com.github.sdpcoachme.SignupActivity.TestTags.TextFields.Companion.PHONE
import com.github.sdpcoachme.data.UserInfo
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
            composeTestRule.onNodeWithTag(FIRST_NAME).performTextInput(user.firstName)
            Espresso.closeSoftKeyboard()
            composeTestRule.onNodeWithTag(LAST_NAME).performTextInput(user.lastName)
            Espresso.closeSoftKeyboard()
            composeTestRule.onNodeWithTag(PHONE).performTextInput(user.phone)
            Espresso.closeSoftKeyboard()
            composeTestRule.onNodeWithTag(LOCATION).performTextInput(user.location)
            Espresso.closeSoftKeyboard()
            composeTestRule.onNodeWithTag(SIGN_UP).performClick()

            // Important note: this get method was used instead of onTimeout due to onTiemout not
            // being found when running tests on Cirrus CI even with java version changed in build.gradle
            val retrievedUser = database.getUser(user.email).get(10, TimeUnit.SECONDS) as Map<*,*>
            TestCase.assertEquals(user.firstName, retrievedUser["firstName"])
            TestCase.assertEquals(user.lastName, retrievedUser["lastName"])
            TestCase.assertEquals(user.phone, retrievedUser["phone"])
            TestCase.assertEquals(user.location, retrievedUser["location"])
            TestCase.assertEquals(user.isCoach, retrievedUser["coach"])

            // Assert that we are redirected to the Dashboard with correct intent
            Intents.intended(allOf(IntentMatchers.hasComponent(DashboardActivity::class.java.name), hasExtra("email", user.email)))

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
            composeTestRule.onNodeWithTag("firstName").performTextInput(user.firstName)
            Espresso.closeSoftKeyboard()
            composeTestRule.onNodeWithTag("lastName").performTextInput(user.lastName)
            Espresso.closeSoftKeyboard()
            composeTestRule.onNodeWithTag("phone").performTextInput(user.phone)
            Espresso.closeSoftKeyboard()
            composeTestRule.onNodeWithTag("location").performTextInput(user.location)
            Espresso.closeSoftKeyboard()
            composeTestRule.onNodeWithTag("isCoachSwitch").performClick()
            Espresso.closeSoftKeyboard()

            composeTestRule.onNodeWithTag("registerButton").performClick()

            // Important note: this get method was used instead of onTimeout due to onTiemout not
            // being found when running tests on Cirrus CI even with java version changed in build.gradle
            val retrievedUser = database.getUser(user.email).get(10, TimeUnit.SECONDS) as Map<*,*>
            TestCase.assertEquals(user.firstName, retrievedUser["firstName"])
            TestCase.assertEquals(user.lastName, retrievedUser["lastName"])
            TestCase.assertEquals(user.phone, retrievedUser["phone"])
            TestCase.assertEquals(user.location, retrievedUser["location"])
            TestCase.assertEquals(user.isCoach, retrievedUser["coach"])

            // Assert that we are redirected to the Dashboard with correct intent
            Intents.intended(allOf(IntentMatchers.hasComponent(DashboardActivity::class.java.name), hasExtra("email", user.email)))

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
            composeTestRule.onNodeWithTag("firstName").performTextInput(user.firstName)
            Espresso.closeSoftKeyboard()
            composeTestRule.onNodeWithTag("lastName").performTextInput(user.lastName)
            Espresso.closeSoftKeyboard()
            composeTestRule.onNodeWithTag("phone").performTextInput(user.phone)
            Espresso.closeSoftKeyboard()
            composeTestRule.onNodeWithTag("location").performTextInput(user.location)
            Espresso.closeSoftKeyboard()
            composeTestRule.onNodeWithTag("registerButton").performClick()

            // Assert that we are still in the SignupActivity
            composeTestRule.onNodeWithTag("firstName").assertExists()
            composeTestRule.onNodeWithTag("lastName").assertExists()
            composeTestRule.onNodeWithTag("phone").assertExists()
            composeTestRule.onNodeWithTag("location").assertExists()
            composeTestRule.onNodeWithTag("registerButton").assertExists()
            Intents.release()
        }
    }

    @Test
    fun assertThatTheNodesExist() {
        //assert that the nodes exist
        val launchSignup = Intent(ApplicationProvider.getApplicationContext(), SignupActivity::class.java)
        val email = "example@email.com"

        launchSignup.putExtra("email", email)
        ActivityScenario.launch<SignupActivity>(launchSignup).use {
            Intents.init()
            composeTestRule.onNodeWithText("First Name").assertExists("No first name field")
            composeTestRule.onNodeWithText("Last Name").assertExists("No last name field")
            composeTestRule.onNodeWithText("Phone").assertExists("No phone field")
            composeTestRule.onNodeWithText("Location").assertExists("No location field")
            Intents.release()
        }
    }
}