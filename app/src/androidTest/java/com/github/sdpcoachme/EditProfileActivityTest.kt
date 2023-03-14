package com.github.sdpcoachme

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditProfileActivityTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<EditProfileActivity>()

    @Test
    fun correctInitialScreenContent() {
        composeTestRule.onNodeWithTag("column").assertExists("Column elem does not exist")
        composeTestRule.onNodeWithTag("title row").assertExists("Title row elem does not exist")
        composeTestRule.onNodeWithTag("email row").assertExists("Email row elem does not exist")
        composeTestRule.onNodeWithTag("first name row").assertExists("First name row elem does not exist")
        composeTestRule.onNodeWithTag("last name row").assertExists("Last name row elem does not exist")
        composeTestRule.onNodeWithTag("fav sport row").assertExists("Fav sport row elem does not exist")

        //content of title row
        composeTestRule.onNodeWithText("My Profile").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile pic").assertIsDisplayed()

        //content of email row
        composeTestRule.onNodeWithText("Email: ").assertIsDisplayed()
        composeTestRule.onNodeWithTag("email address").assertIsDisplayed()

        //content of first name row
        composeTestRule.onNodeWithText("First name: ").assertIsDisplayed()
        composeTestRule.onNodeWithTag("saved first name").assertIsDisplayed()

        //content of last name row
        composeTestRule.onNodeWithText("Last name: ").assertIsDisplayed()
        composeTestRule.onNodeWithTag("saved last name").assertIsDisplayed()

        //content of sport row
        composeTestRule.onNodeWithText("Favorite sport: ").assertIsDisplayed()
        composeTestRule.onNodeWithTag("saved fav sport").assertIsDisplayed()
    }

    @Test
    fun editButtonClickActivatesCorrectElements() {
        composeTestRule.onNodeWithTag("edit button")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithTag("editable first name").assertIsDisplayed()
        composeTestRule.onNodeWithTag("editable last name").assertIsDisplayed()
        composeTestRule.onNodeWithTag("editable fav sport").assertIsDisplayed()
    }

    @Test
    fun saveButtonReturnsToCorrectElements() {
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

        composeTestRule.onNodeWithTag("editable fav sport")
            .performTextClearance()
        composeTestRule.onNodeWithTag("editable fav sport")
            .performTextInput("Updated fav sport")
        Espresso.closeSoftKeyboard()

        //save updated profile
        composeTestRule.onNodeWithTag("save button")
            .assertIsDisplayed()
            .performClick()

        correctInitialScreenContent()
    }
}