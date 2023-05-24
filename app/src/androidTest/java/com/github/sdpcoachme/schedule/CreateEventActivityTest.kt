package com.github.sdpcoachme.schedule

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.CoachMeTestApplication
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.UserInfoSamples
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.EventColors
import com.github.sdpcoachme.data.schedule.EventType
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.location.autocomplete.MockAddressAutocompleteHandler
import com.github.sdpcoachme.profile.EditTextActivity
import com.github.sdpcoachme.profile.SelectSportsActivity
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.COLOR_BOX
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.SAVE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Companion.SCAFFOLD
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Icons.Companion.CANCEL_ICON
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Icons.Companion.SAVE_ICON
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Label.Companion.COLOR_LABEL
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Label.Companion.DESCRIPTION_LABEL
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Label.Companion.END_DATE_LABEL
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Label.Companion.END_TIME_LABEL
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Label.Companion.EVENT_TITLE_LABEL
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Label.Companion.LOCATION_LABEL
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Label.Companion.MAX_PARTICIPANTS_LABEL
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Label.Companion.SPORT_LABEL
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Label.Companion.START_DATE_LABEL
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Label.Companion.START_TIME_LABEL
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.ACTIVITY_TITLE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.COLOR_DIALOG_TITLE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.DESCRIPTION_TEXT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.END_DATE_DIALOG_TITLE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.END_DATE_TEXT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.END_TIME_DIALOG_TITLE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.END_TIME_TEXT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.EVENT_TITLE_TEXT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.LOCATION_TEXT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.MAX_PARTICIPANTS_TEXT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.START_DATE_DIALOG_TITLE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.START_DATE_TEXT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.START_TIME_DIALOG_TITLE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.START_TIME_TEXT
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CreateEventActivityTest {
    private lateinit var store: CachingStore
    private val coachEmail = UserInfoSamples.COACH_1.email
    private val defaultIntent = Intent(ApplicationProvider.getApplicationContext(), CreateEventActivity::class.java)

    private val eventDateFormatter = EventOps.getEventDateFormatter()
    private val currentWeekMonday = EventOps.getStartMonday()

    private val defaultEvent = Event(
        name = "Test Event",
        color = EventColors.DEFAULT.color.value.toString(),
        start = EventOps.getDefaultEventStart().format(eventDateFormatter),
        end = EventOps.getDefaultEventEnd().format(eventDateFormatter),
        sport = Sports.SWIMMING,
        address = MockAddressAutocompleteHandler.DEFAULT_ADDRESS,
        description = "Test Description",
    )

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).clearDataStoreAndResetCachingStore()
        store = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoachMeApplication).store
        store.retrieveData.get(1, TimeUnit.SECONDS)
        store.setCurrentEmail(coachEmail)
        Intents.init()
    }

    @After
    fun teardown() {
        EventOps.clearMultiDayEventMap()
        store.setCurrentEmail("")
        ApplicationProvider.getApplicationContext<CoachMeTestApplication>().clearDataStoreAndResetCachingStore()
        Intents.release()
    }

    @Test
    fun correctInitialScreenContentForPrivateEvent() {
        val initiallyDisplayedTags = listOf(
            SCAFFOLD,
            ACTIVITY_TITLE,
            CANCEL_ICON,
            SAVE_ICON,
            START_DATE_TEXT,
            START_TIME_TEXT,
            END_DATE_TEXT,
            END_TIME_TEXT,
            COLOR_BOX,
        )
        val initiallyDisplayedLabels = listOf(
            EVENT_TITLE_LABEL,
            START_DATE_LABEL,
            START_TIME_LABEL,
            END_DATE_LABEL,
            END_TIME_LABEL,
            SPORT_LABEL,
            LOCATION_LABEL,
            COLOR_LABEL,
            DESCRIPTION_LABEL,
        )

        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)

        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            initiallyDisplayedTags.forEach { tag ->
                println("Checking for tag: $tag")
                composeTestRule.onNodeWithTag(tag, useUnmergedTree = true)
                    .assertExists()
                    .assertIsDisplayed()
            }
            initiallyDisplayedLabels.forEach { label ->
                composeTestRule.onNodeWithText(label, useUnmergedTree = true)
                    .assertExists()
                    .assertIsDisplayed()
            }
        }
    }

    @Test
    fun correctInitialScreenContentForGroupEvent() {
        val initiallyDisplayedTags = listOf(
            SCAFFOLD,
            ACTIVITY_TITLE,
            CANCEL_ICON,
            SAVE_ICON,
            START_DATE_TEXT,
            START_TIME_TEXT,
            END_DATE_TEXT,
            END_TIME_TEXT,
            MAX_PARTICIPANTS_TEXT,
            COLOR_BOX,
        )
        val initiallyDisplayedLabels = listOf(
            EVENT_TITLE_LABEL,
            START_DATE_LABEL,
            START_TIME_LABEL,
            END_DATE_LABEL,
            END_TIME_LABEL,
            MAX_PARTICIPANTS_LABEL,
            SPORT_LABEL,
            LOCATION_LABEL,
            COLOR_LABEL,
            DESCRIPTION_LABEL,
        )

        defaultIntent.putExtra("eventType", EventType.GROUP.eventTypeName)

        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            initiallyDisplayedTags.forEach { tag ->
                composeTestRule.onNodeWithTag(tag, useUnmergedTree = true)
                    .assertExists()
                    .assertIsDisplayed()
            }
            initiallyDisplayedLabels.forEach { label ->
                composeTestRule.onNodeWithText(label, useUnmergedTree = true)
                    .assertExists()
                    .assertIsDisplayed()
            }
        }
    }


    // Note: Pressing the cancel button on the date picker works, but pressing the ok button does not
    private fun openAndCancelDatePicker(
        dateLabel: String,
        dialogTitleTag: String,
    ) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val testDay = 15
        val timeout = 200L

        // Open date picker
        composeTestRule.onNodeWithText(dateLabel, substring = true, useUnmergedTree = true)
            .performClick()
        composeTestRule.onNodeWithTag(dialogTitleTag, useUnmergedTree = true)
            .assertExists()

        // Choose the 15th of the month
        device.wait(Until.findObject(By.text("$testDay")), timeout)
        device.findObject(By.text("$testDay")).click()
        device.waitForIdle()

        device.findObject(By.text("Cancel")).click()
        device.wait(Until.gone(By.text("Cancel")), timeout)

        composeTestRule.onNodeWithTag(dialogTitleTag, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun cancelAndConfirmStartDateWorks() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            openAndCancelDatePicker(START_DATE_LABEL, START_DATE_DIALOG_TITLE)
        }
    }

    @Test
    fun cancelAndConfirmEndDateWorks() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            openAndCancelDatePicker(END_DATE_LABEL, END_DATE_DIALOG_TITLE)
        }
    }

    // Note: Pressing the cancel button on the time picker works, but pressing the ok button does not
    private fun openAndCancelTimePicker(
        timeLabel: String,
        dialogTitleTag: String,
    ) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val testHour1 = 1
        val testHour2 = 5
        val testMinute1 = 3
        val testMinute2 = 0

        // Open time picker
        composeTestRule.onNodeWithText(timeLabel, substring = true, useUnmergedTree = true)
            .performClick()
        composeTestRule.onNodeWithTag(dialogTitleTag, useUnmergedTree = true)
            .assertExists()

        // Choose the 15:30
        val timeout = 600L
        device.wait(Until.findObject(By.text("$testHour1")), timeout)
        device.findObject(By.text("$testHour1")).click()
        device.waitForIdle()
        device.wait(Until.findObject(By.text("$testHour2")), timeout)
        device.findObject(By.text("$testHour2")).click()
        device.waitForIdle()
        device.wait(Until.findObject(By.text("$testMinute1")), timeout)
        device.findObject(By.text("$testMinute1")).click()
        device.waitForIdle()
        device.wait(Until.findObject(By.text("$testMinute2")), timeout)
        device.findObject(By.text("$testMinute2")).click()
        device.waitForIdle()

        // Press cancel
        device.findObject(By.text("Cancel")).click()
        device.wait(Until.gone(By.text("Cancel")), timeout)
    }

    @Test
    fun cancelAndConfirmStartTimeWorks() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            openAndCancelTimePicker(START_TIME_LABEL, START_TIME_DIALOG_TITLE)
        }
    }

    @Test
    fun cancelAndConfirmEndTimeWorks() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            openAndCancelTimePicker(END_TIME_LABEL, END_TIME_DIALOG_TITLE)
        }
    }

    @Test
    fun changeSportWorks() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            composeTestRule.onNodeWithText(SPORT_LABEL)
                .assertExists()
                .performClick() // launches SelectSportsActivity

            val runTag = SelectSportsActivity.TestTags.ListRowTag(Sports.RUNNING).ROW
            composeTestRule.onNodeWithTag(runTag, useUnmergedTree = true).performScrollTo().performClick()   // unchoose running

            val swimTag = SelectSportsActivity.TestTags.ListRowTag(Sports.SWIMMING).ROW
            composeTestRule.onNodeWithTag(swimTag, useUnmergedTree = true).performScrollTo().performClick()   // choose swimming

            composeTestRule.onNodeWithTag(SelectSportsActivity.TestTags.Buttons.DONE, useUnmergedTree = true).performClick() // go back to CreateEventActivity
            composeTestRule.onNodeWithTag(CreateEventActivity.TestTags.SPORTS, useUnmergedTree = true).onChildren().assertAny(
                hasContentDescription(Sports.SWIMMING.sportName)
            )
        }
    }

    @Test
    fun changeSportThenCancelDoesNothing() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            composeTestRule.onNodeWithText(SPORT_LABEL, useUnmergedTree = true)
                .assertExists()
                .performClick() // launches SelectSportsActivity

            val runTag = SelectSportsActivity.TestTags.ListRowTag(Sports.RUNNING).ROW
            composeTestRule.onNodeWithTag(runTag, useUnmergedTree = true).performScrollTo().performClick()   // unchoose running

            val swimTag = SelectSportsActivity.TestTags.ListRowTag(Sports.SWIMMING).ROW
            composeTestRule.onNodeWithTag(swimTag, useUnmergedTree = true).performScrollTo().performClick()   // choose swimming

            composeTestRule.onNodeWithTag(SelectSportsActivity.TestTags.Buttons.CANCEL, useUnmergedTree = true).performClick() // go back to CreateEventActivity
            composeTestRule.onNodeWithTag(CreateEventActivity.TestTags.SPORTS, useUnmergedTree = true).onChildren().assertAny(
                hasContentDescription(Sports.RUNNING.sportName)
            )
        }
    }

    @Test
    fun changeLocationWorks() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            composeTestRule.onNodeWithText(LOCATION_LABEL, substring = true, useUnmergedTree = true)
                .assertExists()
                .performClick() // changes address to default (Lausanne)

            composeTestRule.onNodeWithTag(LOCATION_TEXT, useUnmergedTree = true)
                .assertTextEquals(MockAddressAutocompleteHandler.DEFAULT_ADDRESS.name)
        }
    }

    private fun openAndCloseColorPicker() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Open color picker
        composeTestRule.onNodeWithText(COLOR_LABEL, substring = true, useUnmergedTree = true)
            .performClick()
        composeTestRule.onNodeWithTag(COLOR_DIALOG_TITLE, useUnmergedTree = true)
            .assertExists()

        // Press cancel
        device.findObject(By.text("Cancel")).click()
        device.wait(Until.gone(By.text("Cancel")), 100)

        composeTestRule.onNodeWithTag(COLOR_DIALOG_TITLE)
            .assertDoesNotExist()
    }

    @Test
    fun chooseAndCancelColorPickerWorks() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            openAndCloseColorPicker()
        }
    }

    private fun editFieldTo(text: String, tag: String) {
        composeTestRule.onNodeWithText(tag, substring = true, useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag(EditTextActivity.TestTags.Companion.TextFields.MAIN, useUnmergedTree = true).performTextClearance()
        composeTestRule.onNodeWithTag(EditTextActivity.TestTags.Companion.TextFields.MAIN, useUnmergedTree = true).performTextInput(text)
        composeTestRule.onNodeWithTag(EditTextActivity.TestTags.Companion.TextFields.MAIN, useUnmergedTree = true).performImeAction()
    }

    @Test
    fun fillInPrivateEventWithValidInfosWorks() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)

        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

            editFieldTo(defaultEvent.name, EVENT_TITLE_LABEL)
            device.waitForIdle()
            composeTestRule.onNodeWithTag(EVENT_TITLE_TEXT, useUnmergedTree = true).assertTextEquals(defaultEvent.name)
            editFieldTo(defaultEvent.description, DESCRIPTION_LABEL)
            device.waitForIdle()
            composeTestRule.onNodeWithTag(DESCRIPTION_TEXT, useUnmergedTree = true).assertTextEquals(defaultEvent.description)
        }
    }

    @Test
    fun fillInGroupEventWithValidInfosWorks() {
        defaultIntent.putExtra("eventType", EventType.GROUP.eventTypeName)

        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

            editFieldTo(defaultEvent.name, EVENT_TITLE_LABEL)
            device.waitForIdle()
            composeTestRule.onNodeWithTag(EVENT_TITLE_TEXT, useUnmergedTree = true).assertTextEquals(defaultEvent.name)
            editFieldTo("5", MAX_PARTICIPANTS_LABEL)
            device.waitForIdle()
            composeTestRule.onNodeWithTag(MAX_PARTICIPANTS_TEXT, useUnmergedTree = true).assertTextEquals("5")
            editFieldTo(defaultEvent.description, DESCRIPTION_LABEL)
            device.waitForIdle()
            composeTestRule.onNodeWithTag(DESCRIPTION_TEXT, useUnmergedTree = true).assertTextEquals(defaultEvent.description)
        }
    }

    @Test
    fun addValidPrivateEventSavesToDatabase() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

            editFieldTo(defaultEvent.name, EVENT_TITLE_LABEL)
            device.waitForIdle()
            editFieldTo(defaultEvent.description, DESCRIPTION_LABEL)
            device.waitForIdle()

            composeTestRule.onNodeWithTag(SAVE)
                .performClick()

            val expectedEvent = defaultEvent.copy(
                sport = Sports.RUNNING,
                address = MockAddressAutocompleteHandler.DEFAULT_ADDRESS,
            )

            store.getSchedule(currentWeekMonday).thenAccept {
                val actualEvents = it.events

                assertThat(actualEvents.size, `is`(1))
                assertThat(actualEvents[0], `is`(expectedEvent))
            }
        }
    }

    @Test
    fun addValidGroupEventSavesToDatabase() {
        defaultIntent.putExtra("eventType", EventType.GROUP.eventTypeName)
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            val maxParticipants = 5
            val organiser = store.getCurrentEmail().get().replace(".", ",")
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

            // since tested with private event, we directly fill in the whole form
            editFieldTo(defaultEvent.name, EVENT_TITLE_LABEL)
            device.waitForIdle()
            editFieldTo(defaultEvent.description, DESCRIPTION_LABEL)
            device.waitForIdle()
            openAndCancelDatePicker(START_DATE_LABEL, START_DATE_DIALOG_TITLE)
            openAndCancelDatePicker(END_DATE_LABEL, END_DATE_DIALOG_TITLE)
            openAndCancelTimePicker(START_TIME_LABEL, START_TIME_DIALOG_TITLE)

            // Select sport
            composeTestRule.onNodeWithText(SPORT_LABEL).performClick() // launches SelectSportsActivity
            val runTag = SelectSportsActivity.TestTags.ListRowTag(Sports.RUNNING).ROW
            composeTestRule.onNodeWithTag(runTag, useUnmergedTree = true).performClick()   // unchoose running
            val swimTag = SelectSportsActivity.TestTags.ListRowTag(Sports.SWIMMING).ROW
            composeTestRule.onNodeWithTag(swimTag, useUnmergedTree = true).performClick()   // choose swimming
            composeTestRule.onNodeWithTag(SelectSportsActivity.TestTags.Buttons.DONE, useUnmergedTree = true).performClick() // go back to CreateEventActivity

            // fill in remaining fields
            composeTestRule.onNodeWithText(LOCATION_LABEL, substring = true, useUnmergedTree = true)
                .performClick()
            openAndCloseColorPicker()
            openAndCancelTimePicker(END_TIME_LABEL, END_TIME_DIALOG_TITLE)
            editFieldTo(maxParticipants.toString(), MAX_PARTICIPANTS_LABEL)
            device.waitForIdle()
            composeTestRule.onNodeWithTag(SAVE)
                .performClick()

            val expectedEvent = defaultEvent.copy(sport = Sports.SWIMMING)
            val expectedGroupEvent = GroupEvent(
                organizer = organiser,
                maxParticipants = maxParticipants,
                participants = listOf(organiser),
                event = expectedEvent.copy(sport = Sports.SWIMMING)
            )

            val eventId = "@@event" + organiser + defaultEvent.start.format(eventDateFormatter)

            store.getSchedule(currentWeekMonday).thenCompose {
                val actualEvents = it.events
                assertThat(actualEvents.size, `is`(1))
                assertThat(actualEvents[0], `is`(expectedEvent))    // check if extracted event is saved in schedule correctly
                store.getGroupEvent(eventId)
            }.thenAccept { groupEvent ->
                assertThat(groupEvent, `is`(expectedGroupEvent))    // check if group event itself is saved in database correctly
            }
        }
    }
}

