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
            composeTestRule.onNodeWithTag("firstName").performTextInput(user.firstName)
            Espresso.closeSoftKeyboard()
            composeTestRule.onNodeWithTag("lastName").performTextInput(user.lastName)
            Espresso.closeSoftKeyboard()
            composeTestRule.onNodeWithTag("phone").performTextInput(user.phone)
            Espresso.closeSoftKeyboard()
            composeTestRule.onNodeWithTag("location").performTextInput(user.location)
            Espresso.closeSoftKeyboard()
            composeTestRule.onNodeWithTag("registerButton").performClick()

            // Important note: this get method was used instead of onTimeout due to onTiemout not
            // being found when running tests on Cirrus CI even with java version changed in build.gradle
            val retrievedUser = database.getUser(user.email).get(10, TimeUnit.SECONDS)
            TestCase.assertEquals(user, retrievedUser)

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
            val retrievedUser = database.getUser(user.email).get(10, TimeUnit.SECONDS)
            TestCase.assertEquals(user, retrievedUser)

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
}