package com.github.sdpcoachme

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sdpcoachme.EditProfileActivity.TestTags.Buttons.Companion.EDIT
import com.github.sdpcoachme.EditProfileActivity.TestTags.Buttons.Companion.SAVE
import com.github.sdpcoachme.EditProfileActivity.TestTags.Companion.CLIENT_COACH
import com.github.sdpcoachme.EditProfileActivity.TestTags.Companion.COACH_CLIENT_INFO
import com.github.sdpcoachme.EditProfileActivity.TestTags.Companion.EMAIL
import com.github.sdpcoachme.EditProfileActivity.TestTags.Companion.FIRST_NAME
import com.github.sdpcoachme.EditProfileActivity.TestTags.Companion.LAST_NAME
import com.github.sdpcoachme.EditProfileActivity.TestTags.Companion.PROFILE_LABEL
import com.github.sdpcoachme.EditProfileActivity.TestTags.Companion.PROFILE_PICTURE
import com.github.sdpcoachme.EditProfileActivity.TestTags.Companion.SPORT
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditProfileActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val displayedAfterEditButtonClicked = listOf(
        FIRST_NAME.FIELD,
        LAST_NAME.FIELD,
        SPORT.FIELD,
        CLIENT_COACH.SWITCH,

        SAVE
    )

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
            CLIENT_COACH.SWITCH,

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
    fun errorPageIsShownWhenEditProfileIsLaunchedWithoutEmailAsExtra() {
        ActivityScenario.launch<DashboardActivity>(Intent(ApplicationProvider.getApplicationContext(), EditProfileActivity::class.java)).use {
            // not possible to use Intents.init()... to check if the correct intent
            // is launched as the intents are launched from within the onCreate function
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.Buttons.GO_TO_LOGIN_BUTTON).assertIsDisplayed()
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.TextFields.ERROR_MESSAGE_FIELD).assertIsDisplayed()
        }
    }

    @Test
    fun editButtonClickActivatesCorrectElements() {

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

    @Test
    fun requestForExistingEmailDisplaysCorrectInfoInUserFields() {
        val editProfileIntent = Intent(ApplicationProvider.getApplicationContext(), EditProfileActivity::class.java)
        val email = "some@mail.com"
        val db = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoachMeApplication).database
        db.addUser(
                UserInfo(
                    "first",
                    "last",
                    email,
                    "012345",
                    "Some Place",
                    false,
                    listOf()
                )
            )

        editProfileIntent.putExtra("email", email)
        ActivityScenario.launch<EditProfileActivity>(editProfileIntent).use {

            composeTestRule.onNodeWithTag(EMAIL.TEXT).assertTextEquals(email)
            composeTestRule.onNodeWithTag(FIRST_NAME.TEXT).assertTextEquals("first")
            composeTestRule.onNodeWithTag(LAST_NAME.TEXT).assertTextEquals("last")
            // TODO: add the other fields once they are implemented:

            composeTestRule.onNodeWithTag(EDIT)
                .assertIsDisplayed()
                .performClick()

            displayedAfterEditButtonClicked.forEach { tag ->
                composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
            }

            composeTestRule.onNodeWithTag(EMAIL.TEXT).assertTextEquals(email)
            composeTestRule.onNodeWithTag(FIRST_NAME.FIELD).assertTextEquals("first")
            composeTestRule.onNodeWithTag(LAST_NAME.FIELD).assertTextEquals("last")
            // TODO: add the other fields once they are implemented:
        }
    }

    @Test
    fun requestForNonExistentEmailDisplaysEmptyUserFields() {
        val editProfileIntent = Intent(ApplicationProvider.getApplicationContext(), EditProfileActivity::class.java)
        val email = "non-existant@email.com"
        editProfileIntent.putExtra("email", email)
        ActivityScenario.launch<EditProfileActivity>(editProfileIntent).use {

            composeTestRule.onNodeWithTag(EMAIL.TEXT).assertTextEquals(email)
            composeTestRule.onNodeWithTag(FIRST_NAME.TEXT).assertTextEquals("")
            composeTestRule.onNodeWithTag(LAST_NAME.TEXT).assertTextEquals("")
            // TODO: add the other fields once they are implemented:

            composeTestRule.onNodeWithTag(EDIT)
                .assertIsDisplayed()
                .performClick()

            displayedAfterEditButtonClicked.forEach { tag ->
                composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
            }

            composeTestRule.onNodeWithTag(EMAIL.TEXT).assertTextEquals("non-existant@email.com")
            composeTestRule.onNodeWithTag(FIRST_NAME.FIELD).assertTextEquals("")
            composeTestRule.onNodeWithTag(LAST_NAME.FIELD).assertTextEquals("")
            // TODO: add the other fields once they are implemented:
        }
    }

    @Test
    fun changingToCoachAndBackToClientWorks() {
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

            composeTestRule.onNodeWithTag(COACH_CLIENT_INFO).assertTextEquals("Client")
            composeTestRule.onNodeWithTag(CLIENT_COACH.TEXT).assertTextEquals("I would like to become a coach")

            composeTestRule.onNodeWithTag(CLIENT_COACH.SWITCH)
                .assertIsDisplayed()
                .performClick()

            composeTestRule.onNodeWithTag(SAVE)
                .assertIsDisplayed()
                .performClick()

            composeTestRule.onNodeWithTag(EDIT)
                .assertIsDisplayed()
                .performClick()

            composeTestRule.onNodeWithTag(COACH_CLIENT_INFO).assertTextEquals("Coach")
            composeTestRule.onNodeWithTag(CLIENT_COACH.TEXT).assertTextEquals("I would like to become a client")
        }
    }
}