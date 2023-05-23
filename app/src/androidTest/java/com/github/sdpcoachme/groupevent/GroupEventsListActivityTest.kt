package com.github.sdpcoachme.groupevent

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.CoachMeTestApplication
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.GroupEventSamples
import com.github.sdpcoachme.data.UserInfoSamples
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACH_2
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACH_3
import com.github.sdpcoachme.data.UserInfoSamples.Companion.NON_COACH_1
import com.github.sdpcoachme.data.messaging.Message
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.groupevent.GroupEventsListActivity.TestTags.Tabs.Companion.MY_EVENTS
import com.github.sdpcoachme.messaging.ChatActivity
import com.github.sdpcoachme.ui.Dashboard
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class GroupEventsListActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()


    private fun getStore(): CachingStore {
        return (InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as CoachMeApplication).store
    }

    private fun getContext() = InstrumentationRegistry.getInstrumentation().targetContext

    private val allUsers = UserInfoSamples.COACHES + UserInfoSamples.NON_COACHES

    private fun waitForLoading(scenario: ActivityScenario<GroupEventsListActivity>) {
        lateinit var stateLoading: CompletableFuture<Void>
        scenario.onActivity { activity ->
            stateLoading = activity.stateLoading
        }
        stateLoading.get(3, TimeUnit.SECONDS)
    }

    @Before
    fun setup() {
        (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).clearDataStoreAndResetCachingStore()
        for (user in allUsers) {
            getStore().updateUser(user).get(1000, TimeUnit.MILLISECONDS)
        }
        for (groupEvent in GroupEventSamples.ALL) {
            getStore().updateGroupEvent(groupEvent).get(1000, TimeUnit.MILLISECONDS)
        }
        Intents.init()
    }

    @After
    fun cleanup() {
        Intents.release()
    }

    @Test
    fun allFutureEventsDisplayedInAllTab() {
        val intent = Intent(getContext(), GroupEventsListActivity::class.java)

        getStore().setCurrentEmail(NON_COACH_1.email).get(1000, TimeUnit.MILLISECONDS)
        ActivityScenario.launch<GroupEventsListActivity>(intent).use {
            waitForLoading(it)
            for (groupEvent in GroupEventSamples.ALL.filterNot { e ->
                LocalDateTime.parse(e.event.start).isBefore(LocalDateTime.now())
            }) {
                groupEventDisplayedCorrectly(groupEvent)
            }
        }
    }


    @Test
    fun clickingOnEventOpensEventDetails() {
        val intent = Intent(getContext(), GroupEventsListActivity::class.java)

        getStore().setCurrentEmail(NON_COACH_1.email).get(1000, TimeUnit.MILLISECONDS)
        ActivityScenario.launch<GroupEventsListActivity>(intent).use {
            waitForLoading(it)
            val event = GroupEventSamples.ALL.filterNot { e ->
                LocalDateTime.parse(e.event.start).isBefore(LocalDateTime.now())
            }.first()
            val tag = GroupEventsListActivity.TestTags.GroupEventItemTags(event).TITLE
            composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).performClick()
            // Assert that the correct intent has been launched
            Intents.intended(
                IntentMatchers.hasComponent(GroupEventDetailsActivity::class.java.name)
            )
        }
    }

    @Test
    fun whenOpeningChatThenGoingToContactsTheChatIsDisplayed() {
        val groupEvent = GroupEventSamples.AVAILABLE
        val organizer = getStore().getUser(groupEvent.organizer).get(1000, TimeUnit.MILLISECONDS)
        val nonParticipantUser = allUsers.filterNot { it.email in groupEvent.participants + groupEvent.organizer }.first()

        getStore().setCurrentEmail(nonParticipantUser.email).get(1000, TimeUnit.MILLISECONDS)
        getStore().sendMessage(groupEvent.groupEventId, Message(organizer.email, "Sender Name", "Message", LocalDateTime.now().toString())).get(1000, TimeUnit.MILLISECONDS)
        // place current contacts in cache
        val intent = Intent(getContext(), GroupEventsListActivity::class.java)

        ActivityScenario.launch<GroupEventsListActivity>(intent).use {
            waitForLoading(it)
            val event = GroupEventSamples.ALL.filterNot { e ->
                LocalDateTime.parse(e.event.start).isBefore(LocalDateTime.now())
                        || e.maxParticipants == e.participants.size
            }.first()
            val tag = GroupEventsListActivity.TestTags.GroupEventItemTags(event).TITLE
            composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).performClick()

            composeTestRule.onNodeWithTag(GroupEventDetailsActivity.TestTags.Buttons.JOIN_EVENT)
                .assertIsDisplayed()
                .performClick()

            // place contacts into cache
            getStore().getContactRowInfo(nonParticipantUser.email).get(1000, TimeUnit.MILLISECONDS)

            composeTestRule.onNodeWithTag(GroupEventDetailsActivity.TestTags.Buttons.CHAT)
                .assertIsDisplayed()
                .performClick()


            composeTestRule.onNodeWithTag(ChatActivity.TestTags.Buttons.BACK)
                .assertIsDisplayed()
                .performClick()

            composeTestRule.onNodeWithTag(GroupEventDetailsActivity.TestTags.Buttons.BACK)
                .assertIsDisplayed()
                .performClick()

            composeTestRule.onNodeWithTag(Dashboard.TestTags.DRAWER_HEADER).assertIsNotDisplayed()
            composeTestRule.onNodeWithTag(Dashboard.TestTags.Buttons.HAMBURGER_MENU).performClick()
            composeTestRule.onNodeWithTag(Dashboard.TestTags.DRAWER_HEADER).assertIsDisplayed()

            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.findObject(UiSelector().text("Chats"))
                .click()
            device.waitForIdle()

            composeTestRule.onNodeWithText("This event is available", useUnmergedTree = true)
                .assertIsDisplayed()
                .performClick()

            val res = device.findObject(UiSelector().text("Message"))
                .exists()

            assertTrue(res)
        }
    }


    @Test
    fun allPastEventsNotDisplayedInAllTabs() {
        val intent = Intent(getContext(), GroupEventsListActivity::class.java)

        getStore().setCurrentEmail(NON_COACH_1.email).get(1000, TimeUnit.MILLISECONDS)
        ActivityScenario.launch<GroupEventsListActivity>(intent).use {
            waitForLoading(it)
            for (groupEvent in GroupEventSamples.ALL.filterNot { e ->
                LocalDateTime.parse(e.event.start).isBefore(LocalDateTime.now())
            }) {
                composeTestRule.onNodeWithTag(GroupEventsListActivity.TestTags.GroupEventItemTags(groupEvent).TITLE)
                    .assertDoesNotExist()
            }
        }
    }

    @Test
    fun allEventsWithUserAsParticipantDisplayedInMyEventsTab() {
        val intent = Intent(getContext(), GroupEventsListActivity::class.java)

        getStore().setCurrentEmail(COACH_2.email).get(1000, TimeUnit.MILLISECONDS)
        ActivityScenario.launch<GroupEventsListActivity>(intent).use {
            waitForLoading(it)
            composeTestRule.onNodeWithTag(MY_EVENTS).performClick()
            for (groupEvent in GroupEventSamples.ALL.filter { e ->
                COACH_2.email in e.participants
            }) {
                groupEventDisplayedCorrectly(groupEvent)
            }
        }
    }

    @Test
    fun allEventsWithUserAsOrganizerDisplayedInMyEventsTab() {
        val intent = Intent(getContext(), GroupEventsListActivity::class.java)

        getStore().setCurrentEmail(COACH_2.email).get(1000, TimeUnit.MILLISECONDS)
        ActivityScenario.launch<GroupEventsListActivity>(intent).use {
            waitForLoading(it)
            composeTestRule.onNodeWithTag(MY_EVENTS).performClick()
            for (groupEvent in GroupEventSamples.ALL.filter { e ->
                e.organizer == COACH_2.email
            }) {
                groupEventDisplayedCorrectly(groupEvent)
            }
        }
    }

    @Test
    fun allEventsWithoutUserAsParticipantNotDisplayedInMyEventsTab() {
        val intent = Intent(getContext(), GroupEventsListActivity::class.java)

        getStore().setCurrentEmail(COACH_3.email).get(1000, TimeUnit.MILLISECONDS)
        ActivityScenario.launch<GroupEventsListActivity>(intent).use {
            waitForLoading(it)
            composeTestRule.onNodeWithTag(MY_EVENTS).performClick()
            for (groupEvent in GroupEventSamples.ALL.filterNot { e ->
                e.participants.contains(COACH_3.email)
            }) {
                composeTestRule.onNodeWithTag(GroupEventsListActivity.TestTags.GroupEventItemTags(groupEvent).TITLE)
                    .assertDoesNotExist()
            }
        }
    }

    private fun groupEventDisplayedCorrectly(groupEvent: GroupEvent) {
        val tags = GroupEventsListActivity.TestTags.GroupEventItemTags(groupEvent)
        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        val startDate = LocalDateTime.parse(groupEvent.event.start)
        val endDate = LocalDateTime.parse(groupEvent.event.end)

        composeTestRule.onNodeWithTag(tags.TITLE, useUnmergedTree = true).assertTextEquals(groupEvent.event.name)
        composeTestRule.onNodeWithTag(tags.SPORT, useUnmergedTree = true)
            .assertTextContains(groupEvent.event.sport.sportName, ignoreCase = true, substring = true)
        composeTestRule.onNodeWithTag(tags.LOCATION, useUnmergedTree = true)
            .assertTextEquals(groupEvent.event.address.name)
        composeTestRule.onNodeWithTag(tags.DATE, useUnmergedTree = true)
            .assertTextEquals(startDate.format(dateFormatter))
        composeTestRule.onNodeWithTag(tags.TIME, useUnmergedTree = true)
            .assertTextEquals("${startDate.format(timeFormatter)}–${endDate.format(timeFormatter)}")

        if (startDate.isBefore(LocalDateTime.now())) {
            composeTestRule.onNodeWithTag(tags.PAST_EVENT, useUnmergedTree = true).assertExists()
            composeTestRule.onNodeWithTag(tags.FULLY_BOOKED, useUnmergedTree = true).assertDoesNotExist()
        } else if (groupEvent.participants.size >= groupEvent.maxParticipants) {
            composeTestRule.onNodeWithTag(tags.FULLY_BOOKED, useUnmergedTree = true).assertExists()
            composeTestRule.onNodeWithTag(tags.PAST_EVENT, useUnmergedTree = true).assertDoesNotExist()
        } else {
            composeTestRule.onNodeWithTag(tags.FULLY_BOOKED, useUnmergedTree = true).assertDoesNotExist()
            composeTestRule.onNodeWithTag(tags.PAST_EVENT, useUnmergedTree = true).assertDoesNotExist()
        }

    }
}