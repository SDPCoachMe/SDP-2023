package com.github.sdpcoachme

import android.content.Intent
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignUpActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun correctEmailDisplayed() {
        val launchGreeting = Intent(ApplicationProvider.getApplicationContext(), SignUpActivity::class.java)
        val email = "some@email.com"
        launchGreeting.putExtra("email", email)
        ActivityScenario.launch<GreetingActivity>(launchGreeting).use {
            composeTestRule.onNodeWithTag("email_text_field").assert(hasText(text = email, substring = true))
        }
    }

    @Test
    fun noEmailMessageDisplayedWhenNoEmailReceived() {
        val launchGreeting = Intent(ApplicationProvider.getApplicationContext(), SignUpActivity::class.java)

        ActivityScenario.launch<GreetingActivity>(launchGreeting).use {
            composeTestRule.onNodeWithTag("email_text_field").assert(hasText(text = "No email", substring = true))
        }
    }
}