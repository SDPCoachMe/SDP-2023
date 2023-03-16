package com.github.sdpcoachme

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditProfileActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun onCreateShowsErrorIfNoEmailPassed() {
        val editProfileIntent = Intent(ApplicationProvider.getApplicationContext(), EditProfileActivity::class.java)
        ActivityScenario.launch<EditProfileActivity>(editProfileIntent).use {
            composeTestRule.onNodeWithTag("column").assertExists("Column elem does not exist")
            composeTestRule.onNodeWithTag("column").assertExists("Column elem does not exist")
            composeTestRule.onNodeWithTag("title row").assertExists("Title row elem does not exist")
            composeTestRule.onNodeWithTag("email row").assertExists("Email row elem does not exist")
            composeTestRule.onNodeWithTag("first name row")
                .assertExists("First name row elem does not exist")
            composeTestRule.onNodeWithTag("last name row")
                .assertExists("Last name row elem does not exist")
            composeTestRule.onNodeWithTag("favorite sport row")
                .assertExists("Fav sport row elem does not exist")
        }
    }

    @Test
    fun correctInitialScreenContent() {
        val editProfileIntent = Intent(ApplicationProvider.getApplicationContext(), EditProfileActivity::class.java)
        val email = "example@email.com"
        editProfileIntent.putExtra("email", email)
        ActivityScenario.launch<EditProfileActivity>(editProfileIntent).use {
            composeTestRule.onNodeWithTag("column").assertExists("Column elem does not exist")
            composeTestRule.onNodeWithTag("column").assertExists("Column elem does not exist")
            composeTestRule.onNodeWithTag("title row").assertExists("Title row elem does not exist")
            composeTestRule.onNodeWithTag("email row").assertExists("Email row elem does not exist")
            composeTestRule.onNodeWithTag("first name row")
                .assertExists("First name row elem does not exist")
            composeTestRule.onNodeWithTag("last name row")
                .assertExists("Last name row elem does not exist")
            composeTestRule.onNodeWithTag("favorite sport row")
                .assertExists("Fav sport row elem does not exist")

            //content of title row
            composeTestRule.onNodeWithText("My Profile").assertIsDisplayed()
            composeTestRule.onNodeWithTag("profile pic").assertIsDisplayed()

            //content of email row
            composeTestRule.onNodeWithText("Email: ").assertIsDisplayed()
            composeTestRule.onNodeWithTag("email address").assertIsDisplayed()

            //content of first name row
            composeTestRule.onNodeWithText("First name: ").assertIsDisplayed()
//            composeTestRule.onNodeWithTag("saved first name").assertIsDisplayed()

            //content of last name row
            composeTestRule.onNodeWithText("Last name: ").assertIsDisplayed()
//            composeTestRule.onNodeWithTag("saved last name").assertIsDisplayed()

            //content of sport row
            composeTestRule.onNodeWithText("Favorite sport: ").assertIsDisplayed()
//            composeTestRule.onNodeWithTag("saved favorite sport").assertIsDisplayed()
        }
    }

    @Test
    fun editButtonClickActivatesCorrectElements() {
        val editProfileIntent = Intent(ApplicationProvider.getApplicationContext(), EditProfileActivity::class.java)
        val email = "example@email.com"
        editProfileIntent.putExtra("email", email)
        ActivityScenario.launch<EditProfileActivity>(editProfileIntent).use {
            composeTestRule.onNodeWithTag("edit button")
                .assertIsDisplayed()
                .performClick()

            composeTestRule.onNodeWithTag("editable first name").assertIsDisplayed()
            composeTestRule.onNodeWithTag("editable last name").assertIsDisplayed()
            composeTestRule.onNodeWithTag("editable favorite sport").assertIsDisplayed()
        }
    }

    @Test
    fun editedFieldsSavedCorrectly() {
        val editProfileIntent = Intent(ApplicationProvider.getApplicationContext(), EditProfileActivity::class.java)
        val email = "example@email.com"
        editProfileIntent.putExtra("email", email)
        ActivityScenario.launch<EditProfileActivity>(editProfileIntent).use {
            //change to edit mode
            composeTestRule.onNodeWithTag("edit button")
                .assertIsDisplayed()
                .performClick()

            //edit text fields
            composeTestRule.onNodeWithTag("editable first name")
                .performTextClearance()
            composeTestRule.onNodeWithTag("editable first name")
                .performTextInput("Updated first name")
            Espresso.closeSoftKeyboard()

            composeTestRule.onNodeWithTag("editable last name")
                .performTextClearance()
            composeTestRule.onNodeWithTag("editable last name")
                .performTextInput("Updated last name")
            Espresso.closeSoftKeyboard()

            composeTestRule.onNodeWithTag("editable favorite sport")
                .performTextClearance()
            composeTestRule.onNodeWithTag("editable favorite sport")
                .performTextInput("Updated favorite sport")
            Espresso.closeSoftKeyboard()

            //save updated profile
            composeTestRule.onNodeWithTag("save button")
                .assertIsDisplayed()
                .performClick()

            //check that the updated fields are saved
            composeTestRule.onNodeWithTag("saved first name")
                .assertTextEquals("Updated first name")
            composeTestRule.onNodeWithTag("saved last name")
                .assertTextEquals("Updated last name")
            composeTestRule.onNodeWithTag("saved favorite sport")
                .assertTextEquals("Updated favorite sport")
        }
    }
}