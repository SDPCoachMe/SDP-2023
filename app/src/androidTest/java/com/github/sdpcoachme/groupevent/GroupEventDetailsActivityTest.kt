package com.github.sdpcoachme.groupevent

import android.content.Intent.ACTION_VIEW
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.CoachMeTestApplication
import com.github.sdpcoachme.data.GroupEventSamples.Companion.ALL
import com.github.sdpcoachme.data.GroupEventSamples.Companion.AVAILABLE
import com.github.sdpcoachme.data.GroupEventSamples.Companion.FULLY_BOOKED
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACHES
import com.github.sdpcoachme.data.UserInfoSamples.Companion.NON_COACHES
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Buttons.Companion.CHAT
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Buttons.Companion.JOIN_EVENT
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_DAY
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_DESCRIPTION
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_LOCATION
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_MONTH
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_NAME
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_SPORT
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.EVENT_TIME
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Companion.ORGANIZER_NAME
import com.github.sdpcoachme.groupevent.GroupEventDetailsActivity.TestTags.Tabs.Companion.PARTICIPANTS
import com.github.sdpcoachme.messaging.ChatActivity
import com.github.sdpcoachme.profile.ProfileActivity
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class GroupEventDetailsActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()


    private fun getStore(): CachingStore {
        return (InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as CoachMeApplication).store
    }

    private fun getContext() = InstrumentationRegistry.getInstrumentation().targetContext

    private val allUsers = COACHES + NON_COACHES

    private fun waitForUpdate(scenario: ActivityScenario<GroupEventDetailsActivity>) {
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

    @Before
    fun setup() {
        (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).clearDataStoreAndResetCachingStore()
        for (user in allUsers) {
            getStore().updateUser(user).get(1000, TimeUnit.MILLISECONDS)
        }
        for (groupEvent in ALL) {
            getStore().updateGroupEvent(groupEvent).get(1000, TimeUnit.MILLISECONDS)
        }
        Intents.init()
    }

    @After
    fun cleanup() {
        Intents.release()
    }

    @Test
    fun allFieldsAreDisplayedCorrectly() {
        val groupEvent = AVAILABLE
        val participants = groupEvent.participants.map { getStore().getUser(it).get(1000, TimeUnit.MILLISECONDS) }
        val organizer = getStore().getUser(groupEvent.organizer).get(1000, TimeUnit.MILLISECONDS)
        val nonParticipantUser = allUsers.filterNot { it.email in groupEvent.participants + groupEvent.organizer }.first()
        val eventStart = LocalDateTime.parse(groupEvent.event.start)
        val eventEnd = LocalDateTime.parse(groupEvent.event.end)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        getStore().setCurrentEmail(nonParticipantUser.email).get(1000, TimeUnit.MILLISECONDS)
        val intent = GroupEventDetailsActivity.getIntent(
            getContext(),
            groupEvent.groupEventId
        )
        ActivityScenario.launch<GroupEventDetailsActivity>(intent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(EVENT_NAME).assertIsDisplayed().assertTextEquals(
                groupEvent.event.name
            )
            composeTestRule.onNodeWithTag(EVENT_SPORT).assertIsDisplayed().assertTextContains(
                groupEvent.event.sport.sportName, substring = false, ignoreCase = true
            )
            composeTestRule.onNodeWithTag(EVENT_MONTH).assertIsDisplayed().assertTextEquals(
                eventStart.month.getDisplayName(
                java.time.format.TextStyle.SHORT,
                java.util.Locale.US).uppercase()
            )
            composeTestRule.onNodeWithTag(EVENT_DAY).assertIsDisplayed().assertTextEquals(
                eventStart.dayOfMonth.toString()
            )
            composeTestRule.onNodeWithTag(ORGANIZER_NAME).assertIsDisplayed().assertTextContains(
                "${organizer.firstName} ${organizer.lastName}"
            )
            composeTestRule.onNodeWithTag(EVENT_LOCATION).assertIsDisplayed().assertTextEquals(
                groupEvent.event.address.name
            )
            composeTestRule.onNodeWithTag(EVENT_TIME).assertTextEquals(
                "${eventStart.format(timeFormatter)}–${eventEnd.format(timeFormatter)}"
            )
            composeTestRule.onNodeWithTag(EVENT_DESCRIPTION).assertExists().assertTextEquals(
                groupEvent.event.description
            )

            composeTestRule.onNodeWithTag(PARTICIPANTS).assertIsDisplayed().performClick()

            participants.forEach { p ->
                composeTestRule.onNodeWithText("${p.firstName} ${p.lastName}", substring = true).assertExists()
            }

            composeTestRule
                .onNodeWithText(
                    "${groupEvent.participants.size}/${groupEvent.maxParticipants} participants",
                    substring = true,
                    ignoreCase = true
                )
                .assertExists()
        }
    }

    @Test
    fun nonParticipantCanJoinOnAvailableEvent() {
        val groupEvent = AVAILABLE
        val nonParticipantUser = allUsers.filterNot { it.email in groupEvent.participants + groupEvent.organizer }.first()

        getStore().setCurrentEmail(nonParticipantUser.email).get(1000, TimeUnit.MILLISECONDS)
        val intent = GroupEventDetailsActivity.getIntent(
            getContext(),
            groupEvent.groupEventId
        )
        ActivityScenario.launch<GroupEventDetailsActivity>(intent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(CHAT).assertDoesNotExist()
            composeTestRule.onNodeWithTag(JOIN_EVENT)
                .assertTextContains("JOIN EVENT", ignoreCase = true, substring = true)
                .assertIsDisplayed().performClick()
            // First wait for UI to update, meaning database has received the update
            waitForUpdate(it)
            // Then check the database
            val updatedGroupEvent = getStore().getGroupEvent(groupEvent.groupEventId).get(1000, TimeUnit.MILLISECONDS)
            assertThat(updatedGroupEvent.participants, hasItem(nonParticipantUser.email))
            val updatedSchedule = getStore().getSchedule(
                LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            ).get(1000, TimeUnit.MILLISECONDS)
            assertThat(updatedSchedule.groupEvents, hasItem(updatedGroupEvent.groupEventId))
            val updatedChat = getStore().getChat(groupEvent.groupEventId).get(1000, TimeUnit.MILLISECONDS)
            assertThat(updatedChat.participants, hasItem(nonParticipantUser.email))
            // Do not check that the group is added to the contacts, as this is an implementation detail
        }
    }

    @Test
    fun nonParticipantCannotJoinOnFullEvent() {
        val groupEvent = FULLY_BOOKED
        val nonParticipantUser = allUsers.filterNot { it.email in groupEvent.participants + groupEvent.organizer }.first()

        getStore().setCurrentEmail(nonParticipantUser.email).get(1000, TimeUnit.MILLISECONDS)
        val intent = GroupEventDetailsActivity.getIntent(
            getContext(),
            groupEvent.groupEventId
        )
        ActivityScenario.launch<GroupEventDetailsActivity>(intent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(CHAT).assertDoesNotExist()
            composeTestRule.onNodeWithTag(JOIN_EVENT)
                .assertTextContains("FULLY BOOKED", ignoreCase = true, substring = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun participantCanOpenChat() {
        val groupEvent = FULLY_BOOKED
        val participantUser = allUsers.first { it.email in groupEvent.participants }

        getStore().setCurrentEmail(participantUser.email).get(1000, TimeUnit.MILLISECONDS)
        val intent = GroupEventDetailsActivity.getIntent(
            getContext(),
            groupEvent.groupEventId
        )
        ActivityScenario.launch<GroupEventDetailsActivity>(intent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(JOIN_EVENT).assertDoesNotExist()
            composeTestRule.onNodeWithTag(CHAT).assertIsDisplayed().performClick()
            // Check that correct intent is sent
            Intents.intended(
                allOf(
                    hasComponent(ChatActivity::class.java.name),
                    hasExtra("chatId", groupEvent.groupEventId)
                )
            )
        }
    }

    @Test
    fun openLocationInGoogleMaps() {
        val groupEvent = FULLY_BOOKED
        val participantUser = allUsers.first { it.email in groupEvent.participants }

        getStore().setCurrentEmail(participantUser.email).get(1000, TimeUnit.MILLISECONDS)
        val intent = GroupEventDetailsActivity.getIntent(
            getContext(),
            groupEvent.groupEventId
        )
        ActivityScenario.launch<GroupEventDetailsActivity>(intent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(EVENT_LOCATION).performClick()
            Intents.intended(IntentMatchers.hasAction(ACTION_VIEW))
        }
    }

    @Test
    fun openOrganizerProfile() {
        val groupEvent = FULLY_BOOKED
        val nonParticipantUser = allUsers.filterNot { it.email in groupEvent.participants + groupEvent.organizer }.first()
        val organizer = getStore().getUser(groupEvent.organizer).get(1000, TimeUnit.MILLISECONDS)

        getStore().setCurrentEmail(nonParticipantUser.email).get(1000, TimeUnit.MILLISECONDS)
        val intent = GroupEventDetailsActivity.getIntent(
            getContext(),
            groupEvent.groupEventId
        )
        ActivityScenario.launch<GroupEventDetailsActivity>(intent).use {
            waitForUpdate(it)
            composeTestRule.onNodeWithTag(ORGANIZER_NAME).performClick()

            // Check that the ProfileActivity is launched with the correct extras
            Intents.intended(
                CoreMatchers.allOf(
                    hasComponent(ProfileActivity::class.java.name),
                    hasExtra("email", organizer.email),
                    hasExtra("isViewingCoach", true)
                )
            )
        }
    }

    @Test
    fun openParticipantProfile() {
        val groupEvent = FULLY_BOOKED
        val nonParticipantUser = allUsers.filterNot { it.email in groupEvent.participants + groupEvent.organizer }.first()
        val participantUser = allUsers.first { it.email in groupEvent.participants }

        getStore().setCurrentEmail(nonParticipantUser.email).get(1000, TimeUnit.MILLISECONDS)
        val intent = GroupEventDetailsActivity.getIntent(
            getContext(),
            groupEvent.groupEventId
        )
        ActivityScenario.launch<GroupEventDetailsActivity>(intent).use {
            waitForUpdate(it)

            composeTestRule.onNodeWithTag(PARTICIPANTS).performClick()

            composeTestRule
                .onNodeWithText("${participantUser.firstName} ${participantUser.lastName}", substring = true)
                .performClick()

            // Check that the ProfileActivity is launched with the correct extras
            Intents.intended(
                CoreMatchers.allOf(
                    hasComponent(ProfileActivity::class.java.name),
                    hasExtra("email", participantUser.email),
                    hasExtra("isViewingCoach", true)
                )
            )
        }
    }
}