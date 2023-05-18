package com.github.sdpcoachme.groupevent

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.CoachMeTestApplication
import com.github.sdpcoachme.data.GroupEventSamples.Companion.ALL
import com.github.sdpcoachme.data.GroupEventSamples.Companion.AVAILABLE
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACHES
import com.github.sdpcoachme.data.UserInfoSamples.Companion.NON_COACHES
import com.github.sdpcoachme.database.CachingStore
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
class GroupEventDetailsActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()


    private fun getStore(): CachingStore {
        return (InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as CoachMeApplication).store
    }

    private fun getContext() = InstrumentationRegistry.getInstrumentation().targetContext

    private val allUsers = COACHES + NON_COACHES

    private fun waitForLoading(scenario: ActivityScenario<GroupEventDetailsActivity>) {
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
        for (groupEvent in ALL) {
            getStore().addGroupEvent(groupEvent).get(1000, TimeUnit.MILLISECONDS)
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
        val organizer = getStore().getUser(groupEvent.organiser).get(1000, TimeUnit.MILLISECONDS)
        val nonParticipantUser = allUsers.filterNot { it.email in groupEvent.participants + groupEvent.organiser }.first()
        val eventStart = LocalDateTime.parse(groupEvent.event.start)
        val eventEnd = LocalDateTime.parse(groupEvent.event.end)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        getStore().setCurrentEmail(nonParticipantUser.email).get(1000, TimeUnit.MILLISECONDS)
        val intent = GroupEventDetailsActivity.getIntent(
            getContext(),
            groupEvent.groupEventId
        )
        ActivityScenario.launch<GroupEventDetailsActivity>(intent).use {
            waitForLoading(it)
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
                composeTestRule.onNodeWithText("${p.firstName} ${p.lastName}").assertExists()
            }
        }
    }

    @Test
    fun nonParticipantCanJoinOnAvailableEvent() {
        val groupEvent = AVAILABLE
        val nonParticipantUser = allUsers.filterNot { it.email in groupEvent.participants + groupEvent.organiser }.first()

        getStore().setCurrentEmail(nonParticipantUser.email).get(1000, TimeUnit.MILLISECONDS)
        val intent = GroupEventDetailsActivity.getIntent(
            getContext(),
            groupEvent.groupEventId
        )
        ActivityScenario.launch<GroupEventDetailsActivity>(intent).use {
            waitForLoading(it)
            composeTestRule.onNodeWithTag(JOIN_EVENT).assertIsDisplayed().performClick()
            // TODO: assert that correct intent is sent and database is updated correctly
        }
    }
}