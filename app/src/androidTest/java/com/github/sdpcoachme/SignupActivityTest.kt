package com.github.sdpcoachme

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.google.firebase.auth.FirebaseAuth
import junit.framework.TestCase
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
open class SignupActivityTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun correctNameDisplayed() {
        val launchGreeting = Intent(ApplicationProvider.getApplicationContext(), GreetingActivity::class.java)
        val name = "George"
        launchGreeting.putExtra("name", name)
        ActivityScenario.launch<GreetingActivity>(launchGreeting).use {
            composeTestRule.onNodeWithTag("text").assert(hasText(text = name, substring = true))
        }
    }

    // todo test à faire
    @Test
    fun setAndGet() {
        val phone = "0692000000"
        val email = "jc@gmail.com"
        val phoneField = "phoneTextfield"
        val emailField = "emailTextfield"
        // TODO Put testTags in seperate class
        composeTestRule.onNodeWithTag(phoneField).performTextInput(phone)
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithTag(emailField).performTextInput(email)
        Espresso.closeSoftKeyboard()

        // Set value
        composeTestRule.onNodeWithTag("setButton").performClick()

        //Erase Email in box
        composeTestRule.onNodeWithTag(emailField).performTextClearance()

        composeTestRule.onNodeWithTag("getButton").performClick()

        val retrievedEmail = composeTestRule.onNodeWithTag(emailField).assertTextContains(email)

    }

}