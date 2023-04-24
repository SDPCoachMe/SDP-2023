package com.github.sdpcoachme.profile

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.Dashboard.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.Dashboard.TestTags.Companion.BAR_TITLE
import com.github.sdpcoachme.Dashboard.TestTags.Companion.DRAWER_HEADER
import com.github.sdpcoachme.R
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACHES
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACH_1
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACH_2
import com.github.sdpcoachme.data.UserInfoSamples.Companion.NON_COACHES
import com.github.sdpcoachme.data.UserInfoSamples.Companion.NON_COACH_2
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.Buttons.Companion.GO_TO_LOGIN_BUTTON
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.TextFields.Companion.ERROR_MESSAGE_FIELD
import com.github.sdpcoachme.location.autocomplete.MockLocationAutocompleteHandler.Companion.DEFAULT_LOCATION
import com.github.sdpcoachme.messaging.ChatActivity
import com.github.sdpcoachme.profile.EditTextActivity.TestTags.Companion.Buttons.Companion.CANCEL
import com.github.sdpcoachme.profile.EditTextActivity.TestTags.Companion.TextFields.Companion.MAIN
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Buttons.Companion.MESSAGE_COACH
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.COACH_SWITCH
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.EMAIL
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.FIRST_NAME
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.LAST_NAME
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.LOCATION
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.PHONE
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.PROFILE_LABEL
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.SPORTS
import com.github.sdpcoachme.profile.SelectSportsActivity.TestTags.MultiSelectListTag.Companion.ROW_TEXT_LIST
import junit.framework.TestCase
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ProfileActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val defaultIntent = Intent(ApplicationProvider.getApplicationContext(), ProfileActivity::class.java)
    private fun getDatabase(): Database {
        return (InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as CoachMeApplication).database
    }

    @Before
    fun setup() {
        for (user in COACHES + NON_COACHES) {
            getDatabase().updateUser(user).join()
        }
        Intents.init()
    }

    @After
    fun cleanup() {
        Intents.release()
    }

    @Test
    fun errorPageIsShownWhenEditProfileIsLaunchedWithEmptyCurrentEmail() {
        getDatabase().setCurrentEmail("")
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            // not possible to use Intents.init()... to check if the correct intent
            // is launched as the intents are launched from within the onCreate function
            composeTestRule.onNodeWithTag(GO_TO_LOGIN_BUTTON).assertIsDisplayed()
            composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD).assertIsDisplayed()
        }
    }

    @Test
    fun correctInfoDisplayedForCoachInEditMode() {
        getDatabase().setCurrentEmail(COACH_2.email)

        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(EMAIL, useUnmergedTree = true).assertTextEquals(COACH_2.email)
            composeTestRule.onNodeWithTag(FIRST_NAME, useUnmergedTree = true).assertTextEquals(COACH_2.firstName)
            composeTestRule.onNodeWithTag(LAST_NAME, useUnmergedTree = true).assertTextEquals(COACH_2.lastName)
            composeTestRule.onNodeWithTag(LOCATION, useUnmergedTree = true).assertTextEquals(COACH_2.location.address)
            composeTestRule.onNodeWithTag(PHONE, useUnmergedTree = true).assertTextEquals(COACH_2.phone)
            composeTestRule.onNodeWithTag(SPORTS, useUnmergedTree = true).onChildren().assertCountEquals(COACH_2.sports.size)
            for (sport in COACH_2.sports) {
                composeTestRule.onNodeWithTag(SPORTS, useUnmergedTree = true).onChildren().assertAny(
                    hasContentDescription(sport.sportName))
            }
            composeTestRule.onNodeWithTag(COACH_SWITCH, useUnmergedTree = true).assertIsOn()
            composeTestRule.onNodeWithTag(PROFILE_LABEL, useUnmergedTree = true).assertTextEquals("Coach")
            composeTestRule.onNodeWithTag(MESSAGE_COACH, useUnmergedTree = true).assertDoesNotExist()
        }
    }

    @Test
    fun correctInfoDisplayedForNonCoachInEditMode() {
        getDatabase().setCurrentEmail(NON_COACH_2.email)

        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(EMAIL, useUnmergedTree = true).assertTextEquals(NON_COACH_2.email)
            composeTestRule.onNodeWithTag(FIRST_NAME, useUnmergedTree = true).assertTextEquals(NON_COACH_2.firstName)
            composeTestRule.onNodeWithTag(LAST_NAME, useUnmergedTree = true).assertTextEquals(NON_COACH_2.lastName)
            composeTestRule.onNodeWithTag(LOCATION, useUnmergedTree = true).assertTextEquals(NON_COACH_2.location.address)
            composeTestRule.onNodeWithTag(PHONE, useUnmergedTree = true).assertTextEquals(NON_COACH_2.phone)
            composeTestRule.onNodeWithTag(SPORTS, useUnmergedTree = true).onChildren().assertCountEquals(NON_COACH_2.sports.size)
            for (sport in NON_COACH_2.sports) {
                composeTestRule.onNodeWithTag(SPORTS, useUnmergedTree = true).onChildren().assertAny(
                    hasContentDescription(sport.sportName))
            }
            composeTestRule.onNodeWithTag(COACH_SWITCH, useUnmergedTree = true).assertIsOff()
            composeTestRule.onNodeWithTag(PROFILE_LABEL, useUnmergedTree = true).assertTextEquals("Client")
            composeTestRule.onNodeWithTag(MESSAGE_COACH, useUnmergedTree = true).assertDoesNotExist()
        }
    }

    @Test
    fun changingToCoachAndBackToClientWorks() {
        getDatabase().setCurrentEmail(NON_COACH_2.email)

        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(PROFILE_LABEL, useUnmergedTree = true).assertTextEquals("Client")

            composeTestRule.onNodeWithTag(COACH_SWITCH, useUnmergedTree = true).assertIsDisplayed().performClick()

            waitForUpdate(it)

            composeTestRule.onNodeWithTag(PROFILE_LABEL, useUnmergedTree = true).assertTextEquals("Coach")
        }
    }

    @Test
    fun correctInfoDisplayedForIsViewingCoach() {
        getDatabase().setCurrentEmail(NON_COACH_2.email)

        val profileIntent = defaultIntent
        val email = COACH_1.email
        profileIntent.putExtra("email", email)
        profileIntent.putExtra("isViewingCoach", true)

        ActivityScenario.launch<ProfileActivity>(profileIntent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(EMAIL, useUnmergedTree = true).assertTextEquals(COACH_1.email)
            composeTestRule.onNodeWithTag(FIRST_NAME, useUnmergedTree = true).assertDoesNotExist()
            composeTestRule.onNodeWithTag(LAST_NAME, useUnmergedTree = true).assertDoesNotExist()
            composeTestRule.onNodeWithTag(LOCATION, useUnmergedTree = true).assertTextEquals(COACH_1.location.address)
            composeTestRule.onNodeWithTag(PHONE, useUnmergedTree = true).assertTextEquals(COACH_1.phone)
            composeTestRule.onNodeWithTag(SPORTS, useUnmergedTree = true).onChildren().assertCountEquals(COACH_1.sports.size)
            for (sport in COACH_1.sports) {
                composeTestRule.onNodeWithTag(SPORTS, useUnmergedTree = true).onChildren().assertAny(
                    hasContentDescription(sport.sportName))
            }
            composeTestRule.onNodeWithTag(MESSAGE_COACH, useUnmergedTree = true).assertIsDisplayed()
            composeTestRule.onNodeWithTag(COACH_SWITCH, useUnmergedTree = true).assertDoesNotExist()
        }
    }

    @Test
    fun messageCoachButtonClickHasCorrectFunctionality() {
        getDatabase().setCurrentEmail(NON_COACH_2.email)

        val profileIntent = defaultIntent
        val email = COACH_1.email
        profileIntent.putExtra("email", email)
        profileIntent.putExtra("isViewingCoach", true)

        ActivityScenario.launch<ProfileActivity>(profileIntent).use {
            waitForUpdate(it)

            composeTestRule.onNodeWithTag(MESSAGE_COACH, useUnmergedTree = true).performClick()

            Intents.intended(
                CoreMatchers.allOf(
                    IntentMatchers.hasComponent(ChatActivity::class.java.name),
                    IntentMatchers.hasExtra("toUserEmail", COACH_1.email)
                )
            )
        }
    }

    @Test
    fun dashboardHasRightTitleInEditMode() {
        val title = (InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as CoachMeApplication).getString(R.string.my_profile)
        getDatabase().setCurrentEmail(NON_COACH_2.email)
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(BAR_TITLE).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(BAR_TITLE).assert(hasText(title))
        }
    }

    @Test
    fun dashboardHasRightTitleForIsViewingCoach() {
        val title = (InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as CoachMeApplication).getString(R.string.coach_profile)
        getDatabase().setCurrentEmail(NON_COACH_2.email)
        val profileIntent = defaultIntent
        val email = COACH_1.email
        profileIntent.putExtra("email", email)
        profileIntent.putExtra("isViewingCoach", true)
        ActivityScenario.launch<ProfileActivity>(profileIntent).use {
            composeTestRule.onNodeWithTag(BAR_TITLE).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(BAR_TITLE).assert(hasText(title))
        }
    }

    @Test
    fun dashboardIsAccessibleAndDisplayableFromProfile() {
        getDatabase().setCurrentEmail(NON_COACH_2.email)
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertExists().assertIsDisplayed()
        }
    }

    @Test
    fun editFirstName() {
        editField(FIRST_NAME, NON_COACH_2.email)
    }

    @Test
    fun editLastName() {
        editField(LAST_NAME, NON_COACH_2.email)
    }

    @Test
    fun editPhone() {
        editField(PHONE, NON_COACH_2.email)
    }

    private fun editField(tag: String, email: String) {
        getDatabase().setCurrentEmail(email)
        val newFieldValue = "new"
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).performClick()
            composeTestRule.onNodeWithTag(MAIN, useUnmergedTree = true).performTextClearance()
            composeTestRule.onNodeWithTag(MAIN, useUnmergedTree = true).performTextInput(newFieldValue)
            composeTestRule.onNodeWithTag(MAIN, useUnmergedTree = true).performImeAction()
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).assertTextEquals(newFieldValue)
        }
    }

    @Test
    fun editFirstNameThenCancelDoesNothing() {
        getDatabase().setCurrentEmail(NON_COACH_2.email)
        val newFieldValue = "new"
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(FIRST_NAME, useUnmergedTree = true).performClick()
            composeTestRule.onNodeWithTag(MAIN, useUnmergedTree = true).performTextClearance()
            composeTestRule.onNodeWithTag(MAIN, useUnmergedTree = true).performTextInput(newFieldValue)
            composeTestRule.onNodeWithTag(CANCEL, useUnmergedTree = true).performClick()
            composeTestRule.onNodeWithTag(FIRST_NAME, useUnmergedTree = true).assertTextEquals(NON_COACH_2.firstName)
        }
    }

    @Test
    fun editEmailNotPossible() {
        getDatabase().setCurrentEmail(NON_COACH_2.email)
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(EMAIL, useUnmergedTree = true).performClick()
            // Assert that no intents are sent (1 intent is sent when the activity is launched, but that's it)
            TestCase.assertEquals(1, Intents.getIntents().size)
        }
    }

    @Test
    fun editLocation() {
        getDatabase().setCurrentEmail(NON_COACH_2.email)
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(LOCATION, useUnmergedTree = true).performClick()
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(LOCATION, useUnmergedTree = true).assertTextEquals(DEFAULT_LOCATION.address)
        }
    }

    @Test
    fun editSports() {
        getDatabase().setCurrentEmail(NON_COACH_2.email)
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            waitForUpdate(it)

            composeTestRule.onNodeWithTag(SPORTS, useUnmergedTree = true).performClick()

            for (rowTag in ROW_TEXT_LIST) {
                composeTestRule.onNodeWithTag(rowTag.ROW, useUnmergedTree = true).performClick()
            }
            composeTestRule.onNodeWithTag(SelectSportsActivity.TestTags.Buttons.DONE, useUnmergedTree = true).performClick()

            // Given that we click on all sports, the list of sports is the complement
            for (sport in Sports.values().toSet() - NON_COACH_2.sports.toSet()) {
                composeTestRule.onNodeWithTag(SPORTS, useUnmergedTree = true).onChildren().assertAny(
                    hasContentDescription(sport.sportName))
            }

        }
    }

    @Test
    fun emailClickLaunchesEmailAppForIsViewingCoach() {
        checkIntentSent(EMAIL, Intent.ACTION_SENDTO)
    }

    @Test
    fun locationClickLaunchesMapsAppForIsViewingCoach() {
        checkIntentSent(LOCATION, Intent.ACTION_VIEW)
    }

    @Test
    fun phoneClickLaunchesPhoneAppForIsViewingCoach() {
        checkIntentSent(PHONE, Intent.ACTION_DIAL)
    }

    private fun checkIntentSent(tag: String, action: String) {
        getDatabase().setCurrentEmail(NON_COACH_2.email)
        val profileIntent = defaultIntent
        val email = COACH_1.email
        profileIntent.putExtra("email", email)
        profileIntent.putExtra("isViewingCoach", true)
        ActivityScenario.launch<ProfileActivity>(profileIntent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).performClick()
            Intents.intended(IntentMatchers.hasAction(action))
        }
    }

    /**
     * This waits for the state to be updated in the UI (which is asynchronous).
     */
    private fun waitForUpdate(scenario: ActivityScenario<ProfileActivity>) {
        lateinit var stateUpdated: CompletableFuture<Void>
        scenario.onActivity { activity ->
            stateUpdated = activity.stateUpdated
        }
        stateUpdated.get(3, TimeUnit.SECONDS)
        scenario.onActivity { activity ->
            // Reset the future so that we can wait for the next update
            activity.stateUpdated = CompletableFuture()
        }
    }
}