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
class GreetingActivityTest {

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
}