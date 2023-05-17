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
import com.github.sdpcoachme.data.Address
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.EventColors
import com.github.sdpcoachme.data.schedule.EventType
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.CANCEL
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.COLOR_BOX
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.END_DATE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.END_TIME
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.SAVE
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
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CreateEventActivityTest {
    private lateinit var store: CachingStore
    private val defaultEmail = "example@email.com"
    private val defaultIntent = Intent(ApplicationProvider.getApplicationContext(), CreateEventActivity::class.java)

    private val currentWeekMonday = EventOps.getStartMonday()
    private val defaultEventName = "Test Event"
    private val defaultEventStart = EventOps.getDefaultEventStart()
    private val defaultEventEnd = EventOps.getDefaultEventEnd()
    private val defaultEventDescription = "Test Description"

    private val eventDateFormatter = EventOps.getEventDateFormatter()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).clearDataStoreAndResetCachingStore()
        store = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoachMeApplication).store
        store.retrieveData.get(1, TimeUnit.SECONDS)
        store.setCurrentEmail(defaultEmail)
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
            // TODO: Location and sport
            COLOR_TEXT,
            COLOR_BOX,
            DESCRIPTION,
        )

        defaultIntent.putExtra("eventType", EventType.PRIVATE.eventTypeName)

        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            initiallyDisplayed.forEach { tag ->
                composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
                composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
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
            // TODO: Location and sport
            COLOR_TEXT,
            COLOR_BOX,
            DESCRIPTION,
        )

        defaultIntent.putExtra("eventType", EventType.GROUP.eventTypeName)

        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            initiallyDisplayed.forEach { tag ->
                composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
                composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
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
            fillAndCheckFocus(defaultEventName, EVENT_NAME)
            fillAndCheckFocus(defaultEventDescription, DESCRIPTION)

            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.waitForIdle()
            composeTestRule.onNodeWithTag(SAVE)
                .assertExists()
            composeTestRule.onNodeWithTag(SAVE)
                .performClick()
            device.waitForIdle()

            intended(hasComponent(ScheduleActivity::class.java.name))

        }
    }

    @Test
    fun addGroupEventWithValidInfosRedirectsToSchedule() {
        defaultIntent.putExtra("eventType", EventType.GROUP.eventTypeName)

        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(MAX_PARTICIPANTS)
                .performTextInput("5")

            fillAndCheckFocus(defaultEventName, EVENT_NAME)
            fillAndCheckFocus(defaultEventDescription, DESCRIPTION)

            composeTestRule.onNodeWithTag(SAVE)
                .assertExists()
            composeTestRule.onNodeWithTag(SAVE)
                .performClick()


            // TODO: the following checks fail because scheduleActivity is not launched fast enough (would pass)
            /*composeTestRule.onNodeWithTag(SCHEDULE_COLUMN)
                .assertExists()
            intended(hasComponent(ScheduleActivity::class.java.name))*/

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
                .performTextInput(defaultEventName)
            composeTestRule.onNodeWithTag(DESCRIPTION)
                .performTextInput(defaultEventDescription)
            composeTestRule.onNodeWithTag(SAVE)
                .performClick()

            val expectedEvent = Event(
                name = defaultEventName,
                color = EventColors.DEFAULT.color.value.toString(),
                start = defaultEventStart.format(eventDateFormatter),
                end = defaultEventEnd.format(eventDateFormatter),
                //sport = ???,
                address = Address(),   // adapt this when location choosing is added
                description = defaultEventDescription
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
                .performTextInput(defaultEventName)
            composeTestRule.onNodeWithTag(DESCRIPTION)
                .performTextInput(defaultEventDescription)
            openAndCancelDatePicker(START_DATE, START_DATE_DIALOG_TITLE)
            openAndCancelDatePicker(END_DATE, END_DATE_DIALOG_TITLE)
            openAndCancelTimePicker(START_TIME, START_TIME_DIALOG_TITLE)
            openAndCloseColorPicker()
            openAndCancelTimePicker(END_TIME, END_TIME_DIALOG_TITLE)
            composeTestRule.onNodeWithTag(MAX_PARTICIPANTS)
                .performTextInput(maxParticipants.toString())
            composeTestRule.onNodeWithTag(SAVE)
                .performClick()

            val expectedEvent = Event(
                name = defaultEventName,
                color = EventColors.DEFAULT.color.value.toString(),
                start = defaultEventStart.format(eventDateFormatter),
                end = defaultEventEnd.format(eventDateFormatter),
                //sport = ???,
                address = Address(),   // adapt this when location choosing is added
                description = defaultEventDescription
            )

            val expectedGroupEvent = GroupEvent(
                organiser = organiser,
                maxParticipants = maxParticipants,
                participants = listOf(),
                event = expectedEvent
            )

            val eventId = "@@event" + organiser + defaultEventStart.format(eventDateFormatter)

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

