package com.github.sdpcoachme

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sdpcoachme.EditProfileActivity.TestTags.Buttons.Companion.EDIT
import com.github.sdpcoachme.EditProfileActivity.TestTags.Buttons.Companion.SAVE
import com.github.sdpcoachme.EditProfileActivity.TestTags.Companion.EMAIL
import com.github.sdpcoachme.EditProfileActivity.TestTags.Companion.FIRST_NAME
import com.github.sdpcoachme.EditProfileActivity.TestTags.Companion.LAST_NAME
import com.github.sdpcoachme.EditProfileActivity.TestTags.Companion.PROFILE_LABEL
import com.github.sdpcoachme.EditProfileActivity.TestTags.Companion.PROFILE_PICTURE
import com.github.sdpcoachme.EditProfileActivity.TestTags.Companion.SPORT
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditProfileActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun correctInitialScreenContent() {
        val initiallyDisplayed = listOf(
            PROFILE_LABEL,
            PROFILE_PICTURE,

            EMAIL.LABEL,
            EMAIL.TEXT,

            FIRST_NAME.LABEL,
            FIRST_NAME.TEXT,

            LAST_NAME.LABEL,
            LAST_NAME.TEXT,

            SPORT.LABEL,
            SPORT.TEXT,

            EDIT
        )

        val initiallyNotDisplayed = listOf(
            FIRST_NAME.FIELD,
            LAST_NAME.FIELD,
            SPORT.FIELD,

            SAVE
        )

        val editProfileIntent = Intent(ApplicationProvider.getApplicationContext(), EditProfileActivity::class.java)
        val email = "example@email.com"
        editProfileIntent.putExtra("email", email)
        ActivityScenario.launch<EditProfileActivity>(editProfileIntent).use {
            initiallyDisplayed.forEach { tag ->
                // assertIsDisplayed() behaves strangely with components that are empty (empty Text()
                // components for example, or Text() components whose text is loaded asynchronously)
                composeTestRule.onNodeWithTag(tag).assertExists()
            }

            initiallyNotDisplayed.forEach { tag ->
                composeTestRule.onNodeWithTag(tag).assertDoesNotExist()
            }

        }
    }

    @Test
    fun editButtonClickActivatesCorrectElements() {
        val displayedAfterEditButtonClicked = listOf(
            FIRST_NAME.FIELD,
            LAST_NAME.FIELD,
            SPORT.FIELD,

            SAVE
        )

        val editProfileIntent = Intent(ApplicationProvider.getApplicationContext(), EditProfileActivity::class.java)
        val email = "example@email.com"
        editProfileIntent.putExtra("email", email)
        ActivityScenario.launch<EditProfileActivity>(editProfileIntent).use {
            composeTestRule.onNodeWithTag(EDIT)
                .assertIsDisplayed()
                .performClick()

            displayedAfterEditButtonClicked.forEach { tag ->
                composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
            }
        }
    }

    @Test
    fun editedFieldsSavedCorrectly() {
        val newValues = mapOf(
            FIRST_NAME to "Updated first name",
            LAST_NAME to "Updated last name",
            SPORT to "Updated favorite sport"
        )

        val editProfileIntent = Intent(ApplicationProvider.getApplicationContext(), EditProfileActivity::class.java)
        val email = "example@email.com"
        editProfileIntent.putExtra("email", email)
        ActivityScenario.launch<EditProfileActivity>(editProfileIntent).use {
            //change to edit mode
            composeTestRule.onNodeWithTag(EDIT)
                .assertIsDisplayed()
                .performClick()

            //edit text fields
            newValues.forEach { (field, newValue) ->
                composeTestRule.onNodeWithTag(field.FIELD)
                    .performTextClearance()
                composeTestRule.onNodeWithTag(field.FIELD)
                    .performTextInput(newValue)
                Espresso.closeSoftKeyboard()
            }

            //save updated profile
            composeTestRule.onNodeWithTag(SAVE)
                .assertIsDisplayed()
                .performClick()

            //check that the updated fields are saved
            newValues.forEach { (field, newValue) ->
                composeTestRule.onNodeWithTag(field.TEXT)
                    .assertTextEquals(newValue)
            }
        }
    }
}