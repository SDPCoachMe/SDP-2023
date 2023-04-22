package com.github.sdpcoachme.profile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.github.sdpcoachme.EditTextActivity.TestTags.Companion.TITLE
import com.github.sdpcoachme.EditTextActivity.TestTags.Companion.TextFields.Companion.MAIN
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
            composeTestRule.onNodeWithTag(MAIN).assert(hasText(initialValue))
            composeTestRule.onNodeWithTag(TITLE).assertTextEquals(title)
            composeTestRule.onNodeWithTag(MAIN).performTextClearance()
            composeTestRule.onNodeWithTag(MAIN).performClick()
            composeTestRule.onNodeWithText(placeholder).assertIsDisplayed()
            composeTestRule.onNodeWithText(label).assertIsDisplayed()
        }
    }

    // Very hard to test output returned by the activity in tests. For now, we don't test it.
}