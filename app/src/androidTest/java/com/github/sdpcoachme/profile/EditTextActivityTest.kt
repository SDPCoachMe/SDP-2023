package com.github.sdpcoachme.profile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.github.sdpcoachme.profile.EditTextActivity.Companion.DEFAULT_PLACEHOLDER
import com.github.sdpcoachme.profile.EditTextActivity.Companion.DEFAULT_TITLE
import com.github.sdpcoachme.profile.EditTextActivity.TestTags.Companion.Buttons.Companion.CANCEL
import com.github.sdpcoachme.profile.EditTextActivity.TestTags.Companion.Buttons.Companion.DONE
import com.github.sdpcoachme.profile.EditTextActivity.TestTags.Companion.TITLE
import com.github.sdpcoachme.profile.EditTextActivity.TestTags.Companion.TextFields.Companion.MAIN
import org.junit.Rule
import org.junit.Test

class EditTextActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun intentParametersAreDisplayedProperly() {
        val title = "Title"
        val placeholder = "Placeholder"
        val label = "Label"
        val initialValue = "Initial value"
        val intent = EditTextActivity.getIntent(
            context = ApplicationProvider.getApplicationContext(),
            title = title,
            placeholder = placeholder,
            label = label,
            initialValue = initialValue
        )
        ActivityScenario.launch<EditTextActivity>(intent).use {
            composeTestRule.onNodeWithTag(DONE).assertIsDisplayed()
            composeTestRule.onNodeWithTag(CANCEL).assertIsDisplayed()
            composeTestRule.onNodeWithTag(MAIN).assert(hasText(initialValue))
            composeTestRule.onNodeWithTag(TITLE).assertTextEquals(title)
            composeTestRule.onNodeWithTag(MAIN).performTextClearance()
            composeTestRule.onNodeWithTag(MAIN).performClick()
            composeTestRule.onNodeWithText(placeholder).assertIsDisplayed()
            composeTestRule.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun defaultParametersAreDisplayedProperly() {
        val intent = EditTextActivity.getIntent(
            context = ApplicationProvider.getApplicationContext(),
            title = null,
            placeholder = null,
            label = null,
            initialValue = null
        )
        ActivityScenario.launch<EditTextActivity>(intent).use {
            composeTestRule.onNodeWithTag(DONE).assertIsDisplayed()
            composeTestRule.onNodeWithTag(CANCEL).assertIsDisplayed()
            composeTestRule.onNodeWithTag(MAIN).assertTextEquals(DEFAULT_PLACEHOLDER, "")
            composeTestRule.onNodeWithTag(TITLE).assertTextEquals(DEFAULT_TITLE)
            composeTestRule.onNodeWithTag(MAIN).performClick()
            composeTestRule.onNodeWithText(DEFAULT_PLACEHOLDER).assertIsDisplayed()
        }
    }

    // Very hard to test output returned by the activity in tests. For now, we don't test it.
}