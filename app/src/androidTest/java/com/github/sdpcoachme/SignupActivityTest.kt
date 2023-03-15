package com.github.sdpcoachme

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.*
import junit.framework.TestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
open class SignupActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<SignupActivity>()

    @Test
    fun setAndGetUser() {
        val database = (composeTestRule.activity.application as CoachMeApplication).database
        val user = UserInfo(
            "Jean", "Dupont",
            "jc@gmail.com", "0692000000",
            "Lausanne", listOf())
        composeTestRule.onNodeWithTag("firstName").performTextInput(user.firstName)
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithTag("lastName").performTextInput(user.lastName)
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithTag("email").performTextInput(user.email)
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithTag("phone").performTextInput(user.phone)
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithTag("location").performTextInput(user.location)
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithTag("registerButton").performClick()

        val retrievedUser = database.getUser(user).orTimeout(10, TimeUnit.SECONDS).join()
        TestCase.assertEquals(user, retrievedUser)
    }

}