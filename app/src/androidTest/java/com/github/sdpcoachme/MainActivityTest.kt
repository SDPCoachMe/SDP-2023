package com.github.sdpcoachme

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiSelector
import com.github.sdpcoachme.firebase.auth.FirebaseAuthActivity
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun correctIntentFiredOnDisplayMessageClick() {
        Intents.init()

        val name = "John Lennon"
        composeTestRule.onNodeWithTag("textfield").performTextInput(name)
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithTag("button").performClick()

        intended(allOf(hasComponent(GreetingActivity::class.java.name), hasExtra("name", name)))
        Intents.release()
    }

    @Test
    fun correctIntentFiredOnSignInPageButtonClick() {
        //correctIntent fired on sign in page button click
        Intents.init()

        composeTestRule.onNodeWithTag("signInPageButton").performClick()

        intended(hasComponent(FirebaseAuthActivity::class.java.name))
        Intents.release()
    }
}