package com.github.sdpcoachme

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sdpcoachme.ProfileActivity.TestTags.Buttons.Companion.EDIT
import com.github.sdpcoachme.ProfileActivity.TestTags.Buttons.Companion.MESSAGE_COACH
import com.github.sdpcoachme.ProfileActivity.TestTags.Buttons.Companion.SAVE
import com.github.sdpcoachme.ProfileActivity.TestTags.Buttons.Companion.SELECT_SPORTS
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.CLIENT_COACH
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.COACH_CLIENT_INFO
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.EMAIL
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.FIRST_NAME
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.LAST_NAME
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.LOCATION
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.PROFILE_LABEL
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.PROFILE_PICTURE
import com.github.sdpcoachme.ProfileActivity.TestTags.Companion.SELECTED_SPORTS
import com.github.sdpcoachme.data.UserInfo
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val displayedAfterEditButtonClicked = listOf(
        FIRST_NAME.FIELD,
        LAST_NAME.FIELD,
        CLIENT_COACH.SWITCH,

        SAVE
    )

    private val defaultEmail = "example@email.com"
    private val defaultIntent = Intent(ApplicationProvider.getApplicationContext(), ProfileActivity::class.java)
        .putExtra("email", defaultEmail)

    private val initiallyDisplayed = listOf(
        PROFILE_LABEL,
        PROFILE_PICTURE,

        EMAIL.LABEL,
        EMAIL.TEXT,

        FIRST_NAME.LABEL,
        FIRST_NAME.TEXT,

        LAST_NAME.LABEL,
        LAST_NAME.TEXT,

        LOCATION.LABEL,
        LOCATION.TEXT,

        SELECTED_SPORTS.LABEL,
        SELECTED_SPORTS.ROW,
    )

    @Test
    fun correctInitialScreenContent() {
        val initiallyDisplayedForUser = initiallyDisplayed.plus(EDIT)

        val initiallyNotDisplayed = listOf(
            FIRST_NAME.FIELD,
            LAST_NAME.FIELD,
            CLIENT_COACH.SWITCH,

            SAVE
        )

        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            initiallyDisplayedForUser.forEach { tag ->
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
    fun errorPageIsShownWhenProfileActivityIsLaunchedWithoutEmailAsExtra() {
        ActivityScenario.launch<DashboardActivity>(Intent(ApplicationProvider.getApplicationContext(), ProfileActivity::class.java)).use {
            // not possible to use Intents.init()... to check if the correct intent
            // is launched as the intents are launched from within the onCreate function
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.Buttons.GO_TO_LOGIN_BUTTON).assertIsDisplayed()
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.TextFields.ERROR_MESSAGE_FIELD).assertIsDisplayed()
        }
    }

    @Test
    fun editButtonClickActivatesCorrectElements() {
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
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
        )
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
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
        val user = UserInfo(
            "first",
            "last",
            defaultEmail,
            "012345",
            "Some Place",
            false,
            listOf()
        )
        val db = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoachMeApplication).database
        db.addUser(user)

        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {

            composeTestRule.onNodeWithTag(EMAIL.TEXT).assertTextEquals(defaultEmail)
            composeTestRule.onNodeWithTag(FIRST_NAME.TEXT).assertTextEquals(user.firstName)
            composeTestRule.onNodeWithTag(LAST_NAME.TEXT).assertTextEquals(user.lastName)
            composeTestRule.onNodeWithTag(LOCATION.TEXT).assertTextEquals(user.location)
            // TODO: add the other fields once they are implemented:

            composeTestRule.onNodeWithTag(EDIT)
                .assertIsDisplayed()
                .performClick()

            displayedAfterEditButtonClicked.forEach { tag ->
                composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
            }

            composeTestRule.onNodeWithTag(EMAIL.TEXT).assertTextEquals(defaultEmail)
            composeTestRule.onNodeWithTag(FIRST_NAME.FIELD).assertTextEquals(user.firstName)
            composeTestRule.onNodeWithTag(LAST_NAME.FIELD).assertTextEquals(user.lastName)
            composeTestRule.onNodeWithTag(LOCATION.FIELD).assertTextEquals(user.location)
            // TODO: add the other fields once they are implemented:
        }
    }

    @Test
    fun requestForNonExistentEmailDisplaysEmptyUserFields() {
        val profileIntent = Intent(ApplicationProvider.getApplicationContext(), ProfileActivity::class.java)
        val email = "non-existant@email.com"
        profileIntent.putExtra("email", email)
        ActivityScenario.launch<ProfileActivity>(profileIntent).use {

            composeTestRule.onNodeWithTag(EMAIL.TEXT).assertTextEquals(email)
            composeTestRule.onNodeWithTag(FIRST_NAME.TEXT).assertTextEquals("")
            composeTestRule.onNodeWithTag(LAST_NAME.TEXT).assertTextEquals("")
            composeTestRule.onNodeWithTag(LOCATION.TEXT).assertTextEquals("")
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
            composeTestRule.onNodeWithTag(LOCATION.FIELD).assertTextEquals("")
            // TODO: add the other fields once they are implemented:
        }
    }

    @Test
    fun changingToCoachAndBackToClientWorks() {
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {

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

    @Test
    fun coachProfileShownWhenIsViewingCoachProfileIsTrue() {
        val displayedForUserLookingAtCoach = initiallyDisplayed.plus(listOf(MESSAGE_COACH, SELECTED_SPORTS.ROW, SELECTED_SPORTS.LABEL))

        val profileIntent = Intent(ApplicationProvider.getApplicationContext(), ProfileActivity::class.java)
        val email = "example@email.com"
        profileIntent.putExtra("email", email)
        profileIntent.putExtra("isViewingCoach", true)
        ActivityScenario.launch<ProfileActivity>(profileIntent).use {
            displayedForUserLookingAtCoach.forEach { tag ->
                composeTestRule.onNodeWithTag(tag).assertExists()
            }
        }
    }

    @Test
    fun messageCoachButtonClickHasCorrectFunctionality() {
        val displayedForUserLookingAtCoach = initiallyDisplayed.plus(MESSAGE_COACH)

        val profileIntent = Intent(ApplicationProvider.getApplicationContext(), ProfileActivity::class.java)
        val email = "example@email.com"
        profileIntent.putExtra("email", email)
        profileIntent.putExtra("isViewingCoach", true)
        ActivityScenario.launch<ProfileActivity>(profileIntent).use {
            Intents.init()
            displayedForUserLookingAtCoach.forEach { tag ->
                composeTestRule.onNodeWithTag(tag).assertExists()
            }

            composeTestRule.onNodeWithTag(MESSAGE_COACH)
                .assertIsDisplayed()
                .performClick()

            // TODO: add check for the messaging activity once it is implemented
            Intents.release()
        }
    }

    @Test
    fun selectSportsButtonRedirectsToSelectSportsActivity() {
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            Intents.init()

            composeTestRule.onNodeWithTag(SELECT_SPORTS)
                .assertIsDisplayed()
                .performClick()

            Intents.intended(IntentMatchers.hasComponent(SelectSportsActivity::class.java.name))
            Intents.release()
        }
    }

    @Test
    fun selectSportsButtonNotPresentInEditMode() {
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            Intents.init()
            composeTestRule.onNodeWithTag(SELECT_SPORTS)
                .assertIsDisplayed()

            composeTestRule.onNodeWithTag(EDIT)
                .assertIsDisplayed()
                .performClick()

            composeTestRule.onNodeWithTag(SELECT_SPORTS)
                .assertDoesNotExist()

            composeTestRule.onNodeWithTag(SAVE)
                .assertIsDisplayed()
                .performClick()

            composeTestRule.onNodeWithTag(SELECT_SPORTS)
                .assertIsDisplayed()
                .performClick()

            Intents.intended(IntentMatchers.hasComponent(SelectSportsActivity::class.java.name))
            Intents.release()
        }
    }
}