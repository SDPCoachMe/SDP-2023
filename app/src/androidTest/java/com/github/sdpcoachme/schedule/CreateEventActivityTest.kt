package com.github.sdpcoachme.schedule

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
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
import com.github.sdpcoachme.profile.SelectSportsActivity
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.CANCEL
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.COLOR_BOX
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.END_DATE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.END_TIME
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.LOCATION
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.SAVE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.SPORT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.START_DATE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.START_TIME
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Companion.SCAFFOLD
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Icons.Companion.CANCEL_ICON
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Icons.Companion.SAVE_ICON
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.TextFields.Companion.DESCRIPTION
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.TextFields.Companion.EVENT_NAME
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.TextFields.Companion.MAX_PARTICIPANTS
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.ACTIVITY_TITLE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.COLOR_DIALOG_TITLE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.COLOR_TEXT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.END_DATE_DIALOG_TITLE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.END_DATE_TEXT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.END_TIME_DIALOG_TITLE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.END_TIME_TEXT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.LOCATION_TEXT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.MAX_PARTICIPANTS_TEXT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.SPORT_TEXT
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
        val initiallyDisplayed = listOf(
            SCAFFOLD,
            ACTIVITY_TITLE,
            CANCEL_ICON,
            SAVE_ICON,
            EVENT_NAME,
            START_DATE_TEXT,
            START_TIME_TEXT,
            END_DATE_TEXT,
            END_TIME_TEXT,
            START_DATE,
            START_TIME,
            END_DATE,
            END_TIME,
            SPORT_TEXT,
            SPORT,
            LOCATION_TEXT,
            LOCATION,
            COLOR_TEXT,
            COLOR_BOX,
            DESCRIPTION,
        )

        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)

        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            initiallyDisplayed.forEach { tag ->
                composeTestRule.onNodeWithTag(tag, useUnmergedTree = true)
                    .assertExists()
                    .assertIsDisplayed()
            }
        }
    }

    @Test
    fun correctInitialScreenContentForGroupEvent() {
        val initiallyDisplayed = listOf(
            SCAFFOLD,
            ACTIVITY_TITLE,
            CANCEL_ICON,
            SAVE_ICON,
            EVENT_NAME,
            START_DATE_TEXT,
            START_TIME_TEXT,
            END_DATE_TEXT,
            END_TIME_TEXT,
            START_DATE,
            START_TIME,
            END_DATE,
            END_TIME,
            MAX_PARTICIPANTS_TEXT,
            MAX_PARTICIPANTS,
            SPORT_TEXT,
            SPORT,
            LOCATION_TEXT,
            LOCATION,
            COLOR_TEXT,
            COLOR_BOX,
            DESCRIPTION,
        )

        defaultIntent.putExtra("eventType", EventType.GROUP.eventTypeName)

        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            initiallyDisplayed.forEach { tag ->
                composeTestRule.onNodeWithTag(tag, useUnmergedTree = true)
                    .assertExists()
                    .assertIsDisplayed()
            }
        }
    }


    // Note: Pressing the cancel button on the date picker works, but pressing the ok button does not
    private fun openAndCancelDatePicker(
        dateTag: String,
        dialogTitleTag: String,
    ) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val testDay = 15
        val timeout = 200L

        // Open date picker
        composeTestRule.onNodeWithTag(dateTag)
            .performClick()
        composeTestRule.onNodeWithTag(dialogTitleTag, useUnmergedTree = true)
            .assertExists()

        // Choose the 15th of the month
        device.wait(Until.findObject(By.text("$testDay")), timeout)
        device.findObject(By.text("$testDay")).click()
        device.waitForIdle()

        device.findObject(By.text("Cancel")).click()
        device.wait(Until.gone(By.text("Cancel")), timeout)

        composeTestRule.onNodeWithTag(START_DATE)
            .assertTextEquals(EventOps.getDefaultEventStart().format(EventOps.getDayFormatter()))
    }

    @Test
    fun cancelAndConfirmStartDateWorks() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            openAndCancelDatePicker(START_DATE, START_DATE_DIALOG_TITLE)
        }
    }

    @Test
    fun cancelAndConfirmEndDateWorks() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            openAndCancelDatePicker(END_DATE, END_DATE_DIALOG_TITLE)
        }
    }

    // Note: Pressing the cancel button on the time picker works, but pressing the ok button does not
    private fun openAndCancelTimePicker(
        timeTag: String,
        dialogTitleTag: String,
    ) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val testHour1 = 1
        val testHour2 = 5
        val testMinute1 = 3
        val testMinute2 = 0

        // Open time picker
        composeTestRule.onNodeWithTag(timeTag)
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
            openAndCancelTimePicker(START_TIME, START_TIME_DIALOG_TITLE)
        }
    }

    @Test
    fun cancelAndConfirmEndTimeWorks() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            openAndCancelTimePicker(END_TIME, END_TIME_DIALOG_TITLE)
        }
    }

    @Test
    fun changeSportWorks() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(SPORT)
                .assertExists()
                .performClick() // launches SelectSportsActivity

            val runTag = SelectSportsActivity.TestTags.ListRowTag(Sports.RUNNING).ROW
            composeTestRule.onNodeWithTag(runTag, useUnmergedTree = true).performClick()   // unchoose running

            val swimTag = SelectSportsActivity.TestTags.ListRowTag(Sports.SWIMMING).ROW
            composeTestRule.onNodeWithTag(swimTag, useUnmergedTree = true).performClick()   // choose swimming

            composeTestRule.onNodeWithTag(SelectSportsActivity.TestTags.Buttons.DONE, useUnmergedTree = true).performClick() // go back to CreateEventActivity

            val swimIconTag = CreateEventActivity.TestTags.SportElement(Sports.SWIMMING).ICON
            composeTestRule.onNodeWithTag(swimIconTag, useUnmergedTree = true).assertExists()
            val runIconTag = CreateEventActivity.TestTags.SportElement(Sports.RUNNING).ICON
            composeTestRule.onNodeWithTag(runIconTag, useUnmergedTree = true).assertDoesNotExist()
        }
    }

    @Test
    fun changeSportThenCancelDoesNothing() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(SPORT)
                .assertExists()
                .performClick() // launches SelectSportsActivity

            val runTag = SelectSportsActivity.TestTags.ListRowTag(Sports.RUNNING).ROW
            composeTestRule.onNodeWithTag(runTag, useUnmergedTree = true).performClick()   // unchoose running

            val swimTag = SelectSportsActivity.TestTags.ListRowTag(Sports.SWIMMING).ROW
            composeTestRule.onNodeWithTag(swimTag, useUnmergedTree = true).performClick()   // choose swimming

            composeTestRule.onNodeWithTag(SelectSportsActivity.TestTags.Buttons.CANCEL, useUnmergedTree = true).performClick() // go back to CreateEventActivity

            val swimIconTag = CreateEventActivity.TestTags.SportElement(Sports.SWIMMING).ICON
            composeTestRule.onNodeWithTag(swimIconTag, useUnmergedTree = true).assertDoesNotExist()
            val runIconTag = CreateEventActivity.TestTags.SportElement(Sports.RUNNING).ICON
            composeTestRule.onNodeWithTag(runIconTag, useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun changeLocationWorks() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(LOCATION)
                .assertExists()
                .performClick() // changes address to default (Lausanne)

            composeTestRule.onNodeWithTag(LOCATION)
                .assertTextEquals(MockAddressAutocompleteHandler.DEFAULT_ADDRESS.name)
        }
    }

    private fun openAndCloseColorPicker() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Open color picker
        composeTestRule.onNodeWithTag(COLOR_BOX)
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

    private fun fillAndCheckFocus(text: String, tag: String) {
        composeTestRule.onNodeWithTag(tag)
            .assertIsNotFocused()
        composeTestRule.onNodeWithTag(tag)
            .performClick()
        composeTestRule.onNodeWithTag(tag)
            .assertIsFocused()
        composeTestRule.onNodeWithTag(tag)
            .performTextInput(text)
        composeTestRule.onNodeWithTag(tag)
            .performImeAction()
        composeTestRule.onNodeWithTag(tag)
            .assertIsNotFocused()
    }

    @Test
    fun addPrivateEventWithValidInfosRedirectsToSchedule() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)

        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            fillAndCheckFocus(defaultEvent.name, EVENT_NAME)
            fillAndCheckFocus(defaultEvent.description, DESCRIPTION)

            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.waitForIdle()
            composeTestRule.onNodeWithTag(SAVE)
                .assertExists()
                .performClick()

            device.waitForIdle()

            // Check that we are redirected to Schedule (onNodeWithText because return to schedule waits for future)
            composeTestRule.onNodeWithText("Schedule", substring = true, useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }


    @Test
    fun addGroupEventWithValidInfosRedirectsToSchedule() {
        defaultIntent.putExtra("eventType", EventType.GROUP.eventTypeName)

        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(MAX_PARTICIPANTS)
                .performTextInput("5")

            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            fillAndCheckFocus(defaultEvent.name, EVENT_NAME)
            device.waitForIdle()
            fillAndCheckFocus(defaultEvent.description, DESCRIPTION)
            device.waitForIdle()

            composeTestRule.onNodeWithTag(SAVE)
                .assertExists()
                .performClick()

            // Check that we are redirected to Schedule (onNodeWithText because return to schedule waits for future)
            composeTestRule.onNodeWithText("Schedule", substring = true, useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun cancelPrivateEventCorrectlyRedirectsToSchedule() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)

        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(CANCEL)
                .performClick()

            intended(hasComponent(ScheduleActivity::class.java.name))
        }
    }

    @Test
    fun cancelGroupEventCorrectlyRedirectsToSchedule() {
        defaultIntent.putExtra("eventType", EventType.GROUP.eventTypeName)

        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(CANCEL)
                .performClick()

            intended(hasComponent(ScheduleActivity::class.java.name))
        }
    }

    @Test
    fun addValidPrivateEventSavesToDatabase() {
        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(EVENT_NAME)
                .performTextInput(defaultEvent.name)
            composeTestRule.onNodeWithTag(DESCRIPTION)
                .performTextInput(defaultEvent.description)
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

            // since tested with private event, we directly fill in the whole form
            composeTestRule.onNodeWithTag(EVENT_NAME)
                .performTextInput(defaultEvent.name)
            composeTestRule.onNodeWithTag(DESCRIPTION)
                .performTextInput(defaultEvent.description)
            openAndCancelDatePicker(START_DATE, START_DATE_DIALOG_TITLE)
            openAndCancelDatePicker(END_DATE, END_DATE_DIALOG_TITLE)
            openAndCancelTimePicker(START_TIME, START_TIME_DIALOG_TITLE)

            // Select sport
            composeTestRule.onNodeWithTag(SPORT).performClick() // launches SelectSportsActivity
            val runTag = SelectSportsActivity.TestTags.ListRowTag(Sports.RUNNING).ROW
            composeTestRule.onNodeWithTag(runTag, useUnmergedTree = true).performClick()   // unchoose running
            val swimTag = SelectSportsActivity.TestTags.ListRowTag(Sports.SWIMMING).ROW
            composeTestRule.onNodeWithTag(swimTag, useUnmergedTree = true).performClick()   // choose swimming
            composeTestRule.onNodeWithTag(SelectSportsActivity.TestTags.Buttons.CANCEL, useUnmergedTree = true).performClick() // go back to CreateEventActivity

            // fill in remaining fields
            composeTestRule.onNodeWithTag(LOCATION)
                .performClick()
            openAndCloseColorPicker()
            openAndCancelTimePicker(END_TIME, END_TIME_DIALOG_TITLE)
            composeTestRule.onNodeWithTag(MAX_PARTICIPANTS)
                .performTextInput(maxParticipants.toString())
            composeTestRule.onNodeWithTag(SAVE)
                .performClick()

            val expectedEvent = defaultEvent
            val expectedGroupEvent = GroupEvent(
                organizer = organiser,
                maxParticipants = maxParticipants,
                participants = listOf(organiser),
                event = expectedEvent
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

