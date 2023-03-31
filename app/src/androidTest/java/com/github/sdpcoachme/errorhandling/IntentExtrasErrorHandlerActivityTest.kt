package com.github.sdpcoachme.errorhandling

import android.content.Intent
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.github.sdpcoachme.ProfileActivity
import com.github.sdpcoachme.LoginActivity
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.Buttons.Companion.GO_TO_LOGIN_BUTTON
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.TextFields.Companion.ERROR_MESSAGE_FIELD
import org.junit.Rule
import org.junit.Test

class IntentExtrasErrorHandlerActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun whenLaunchedWithoutExtraItDisplaysGenericErrorMessage() {
        val genericErrorMsg = "An error occurred. Please return to the login page and retry."

        val emptyErrorIntent = Intent(ApplicationProvider.getApplicationContext(), IntentExtrasErrorHandlerActivity::class.java)

        ActivityScenario.launch<ProfileActivity>(emptyErrorIntent).use {

            composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD).assertExists()
            composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD).assertTextEquals(genericErrorMsg)

            composeTestRule.onNodeWithTag(GO_TO_LOGIN_BUTTON).assertExists()
        }
    }

    @Test
    fun whenLaunchedWithExtraItDisplaysThePassedErrorMessage() {
        val errorMsg = "This is a test error message."
        val emptyErrorIntent = Intent(ApplicationProvider.getApplicationContext(), IntentExtrasErrorHandlerActivity::class.java)
        emptyErrorIntent.putExtra("errorMsg", errorMsg)

        ActivityScenario.launch<ProfileActivity>(emptyErrorIntent).use {

            composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD).assertExists()
            composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD).assertTextEquals(errorMsg)

            composeTestRule.onNodeWithTag(GO_TO_LOGIN_BUTTON).assertExists()
        }
    }

    @Test
    fun goToLoginButtonClickAfterEmptyIntentRedirectsToLoginPage() {
        val emptyErrorIntent = Intent(ApplicationProvider.getApplicationContext(), IntentExtrasErrorHandlerActivity::class.java)

        ActivityScenario.launch<ProfileActivity>(emptyErrorIntent).use {
            Intents.init()
            composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD).assertExists()

            composeTestRule.onNodeWithTag(GO_TO_LOGIN_BUTTON).assertExists()
                .performClick()

            Intents.intended(hasComponent(LoginActivity::class.java.name))
            Intents.release()
        }
    }

    @Test
    fun goToLoginButtonClickAfterIntentWithExtraRedirectsToLoginPage() {
        val emptyErrorIntent = Intent(ApplicationProvider.getApplicationContext(), IntentExtrasErrorHandlerActivity::class.java)
        val errorMsg = "This is a test error message."
        emptyErrorIntent.putExtra("errorMsg", errorMsg)

        ActivityScenario.launch<ProfileActivity>(emptyErrorIntent).use {
            Intents.init()
            composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD).assertExists()
            composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD).assertTextEquals(errorMsg)

            composeTestRule.onNodeWithTag(GO_TO_LOGIN_BUTTON).assertExists()
                .performClick()

            Intents.intended(hasComponent(LoginActivity::class.java.name))
            Intents.release()
        }
    }
}