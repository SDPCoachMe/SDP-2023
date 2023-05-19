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
import com.github.sdpcoachme.CoachMeTestApplication
import com.github.sdpcoachme.R
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACHES
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACH_1
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACH_2
import com.github.sdpcoachme.data.UserInfoSamples.Companion.NON_COACHES
import com.github.sdpcoachme.data.UserInfoSamples.Companion.NON_COACH_2
import com.github.sdpcoachme.data.messaging.Chat
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.location.autocomplete.MockAddressAutocompleteHandler.Companion.DEFAULT_ADDRESS
import com.github.sdpcoachme.messaging.ChatActivity
import com.github.sdpcoachme.profile.EditTextActivity.TestTags.Companion.Buttons.Companion.CANCEL
import com.github.sdpcoachme.profile.EditTextActivity.TestTags.Companion.TextFields.Companion.MAIN
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Buttons.Companion.MESSAGE_COACH
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.ADDRESS
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.COACH_SWITCH
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.EMAIL
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.FIRST_NAME
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.LAST_NAME
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.PHONE
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.PROFILE_LABEL
import com.github.sdpcoachme.profile.ProfileActivity.TestTags.Companion.SPORTS
import com.github.sdpcoachme.profile.SelectSportsActivity.TestTags.MultiSelectListTag.Companion.ROW_TEXT_LIST
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.BAR_TITLE
import com.github.sdpcoachme.ui.Dashboard.TestTags.Companion.DRAWER_HEADER
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
    private fun getStore(): CachingStore {
        return (InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as CoachMeApplication).store
    }

    @Before
    fun setup() {
        (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).clearDataStoreAndResetCachingStore()
        getStore().retrieveData.get(1, TimeUnit.SECONDS)
        for (user in COACHES + NON_COACHES) {
            getStore().updateUser(user).join()
        }
        Intents.init()
    }

    @After
    fun cleanup() {
        Intents.release()
    }

    @Test
    fun correctInfoDisplayedForCoachInEditMode() {
        getStore().setCurrentEmail(COACH_2.email).get(1000, TimeUnit.MILLISECONDS)

        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(EMAIL, useUnmergedTree = true).assertTextEquals(COACH_2.email)
            composeTestRule.onNodeWithTag(FIRST_NAME, useUnmergedTree = true).assertTextEquals(COACH_2.firstName)
            composeTestRule.onNodeWithTag(LAST_NAME, useUnmergedTree = true).assertTextEquals(COACH_2.lastName)
            composeTestRule.onNodeWithTag(ADDRESS, useUnmergedTree = true).assertTextEquals(COACH_2.address.name)
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
        getStore().setCurrentEmail(NON_COACH_2.email).get(1000, TimeUnit.MILLISECONDS)

        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(EMAIL, useUnmergedTree = true).assertTextEquals(NON_COACH_2.email)
            composeTestRule.onNodeWithTag(FIRST_NAME, useUnmergedTree = true).assertTextEquals(NON_COACH_2.firstName)
            composeTestRule.onNodeWithTag(LAST_NAME, useUnmergedTree = true).assertTextEquals(NON_COACH_2.lastName)
            composeTestRule.onNodeWithTag(ADDRESS, useUnmergedTree = true).assertTextEquals(NON_COACH_2.address.name)
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
        getStore().setCurrentEmail(NON_COACH_2.email).get(1000, TimeUnit.MILLISECONDS)

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
        getStore().setCurrentEmail(NON_COACH_2.email).get(1000, TimeUnit.MILLISECONDS)

        val profileIntent = defaultIntent
        val email = COACH_1.email
        profileIntent.putExtra("email", email)
        profileIntent.putExtra("isViewingCoach", true)

        ActivityScenario.launch<ProfileActivity>(profileIntent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(EMAIL, useUnmergedTree = true).assertTextEquals(COACH_1.email)
            composeTestRule.onNodeWithTag(FIRST_NAME, useUnmergedTree = true).assertDoesNotExist()
            composeTestRule.onNodeWithTag(LAST_NAME, useUnmergedTree = true).assertDoesNotExist()
            composeTestRule.onNodeWithTag(ADDRESS, useUnmergedTree = true).assertTextEquals(COACH_1.address.name)
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
        getStore().setCurrentEmail(NON_COACH_2.email).get(1000, TimeUnit.MILLISECONDS)

        val profileIntent = defaultIntent
        val email = COACH_1.email
        profileIntent.putExtra("email", email)
        profileIntent.putExtra("isViewingCoach", true)

        val expectedChatId = Chat.chatIdForPersonalChats(email, NON_COACH_2.email)

        ActivityScenario.launch<ProfileActivity>(profileIntent).use {
            waitForUpdate(it)

            composeTestRule.onNodeWithTag(MESSAGE_COACH, useUnmergedTree = true).performClick()

            Intents.intended(
                CoreMatchers.allOf(
                    IntentMatchers.hasComponent(ChatActivity::class.java.name),
                    IntentMatchers.hasExtra("chatId", expectedChatId)
                )
            )
        }
    }

    @Test
    fun dashboardHasRightTitleForMyProfile() {
        val title = (InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as CoachMeApplication).getString(R.string.my_profile)
        getStore().setCurrentEmail(NON_COACH_2.email).get(1000, TimeUnit.MILLISECONDS)
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(BAR_TITLE).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(BAR_TITLE).assert(hasText(title))
        }
    }

    @Test
    fun dashboardHasRightTitleForIsViewingCoach() {
        val title = (InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as CoachMeApplication).getString(R.string.profile_details)
        getStore().setCurrentEmail(NON_COACH_2.email)
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
        getStore().setCurrentEmail(NON_COACH_2.email)
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
        getStore().setCurrentEmail(email).get(1000, TimeUnit.MILLISECONDS)
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
        getStore().setCurrentEmail(NON_COACH_2.email).get(1000, TimeUnit.MILLISECONDS)
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
        getStore().setCurrentEmail(NON_COACH_2.email).get(1000, TimeUnit.MILLISECONDS)
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(EMAIL, useUnmergedTree = true).performClick()
            // Assert that no intents are sent (1 intent is sent when the activity is launched, but that's it)
            TestCase.assertEquals(1, Intents.getIntents().size)
        }
    }

    @Test
    fun editAddress() {
        getStore().setCurrentEmail(NON_COACH_2.email)
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(ADDRESS, useUnmergedTree = true).performClick()
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(ADDRESS, useUnmergedTree = true).assertTextEquals(DEFAULT_ADDRESS.name)
        }
    }

    @Test
    fun editSportsClickOnAllSportsResultsInComplementChosen() {
        getStore().setCurrentEmail(NON_COACH_2.email).get(1000, TimeUnit.MILLISECONDS)
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            waitForUpdate(it)

            composeTestRule.onNodeWithTag(SPORTS, useUnmergedTree = true).performClick()

            for (rowTag in ROW_TEXT_LIST) {
                composeTestRule.onNodeWithTag(rowTag.ROW, useUnmergedTree = true).performClick()
            }
            composeTestRule.onNodeWithTag(SelectSportsActivity.TestTags.Buttons.DONE, useUnmergedTree = true).performClick()

            waitForUpdate(it)
            // Given that we click on all sports, the list of sports is the complement
            for (sport in Sports.values().toSet() - NON_COACH_2.sports.toSet()) {
                composeTestRule.onNodeWithTag(SPORTS, useUnmergedTree = true).onChildren().assertAny(
                    hasContentDescription(sport.sportName))
            }

        }
    }

    @Test
    fun editSportsThenCancelDoesNothing() {
        getStore().setCurrentEmail(NON_COACH_2.email).get(1000, TimeUnit.MILLISECONDS)
        ActivityScenario.launch<ProfileActivity>(defaultIntent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(SPORTS, useUnmergedTree = true).performClick()

            for (rowTag in ROW_TEXT_LIST) {
                composeTestRule.onNodeWithTag(rowTag.ROW, useUnmergedTree = true).performClick()
            }
            composeTestRule.onNodeWithTag(SelectSportsActivity.TestTags.Buttons.CANCEL, useUnmergedTree = true).performClick()

            // List of sports should be as it was before
            for (sport in NON_COACH_2.sports.toSet()) {
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
    fun addressClickLaunchesMapsAppForIsViewingCoach() {
        checkIntentSent(ADDRESS, Intent.ACTION_VIEW)
    }

    @Test
    fun phoneClickLaunchesPhoneAppForIsViewingCoach() {
        checkIntentSent(PHONE, Intent.ACTION_DIAL)
    }

    private fun checkIntentSent(tag: String, action: String) {
        getStore().setCurrentEmail(NON_COACH_2.email)
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