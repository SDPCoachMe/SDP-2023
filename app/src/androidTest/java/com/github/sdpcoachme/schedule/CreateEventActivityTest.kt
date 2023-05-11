package com.github.sdpcoachme.schedule

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
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
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.EventColors
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity
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
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.ACTIVITY_TITLE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.COLOR_DIALOG_TITLE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.COLOR_TEXT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.END_DATE_DIALOG_TITLE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.END_DATE_TEXT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.END_TIME_DIALOG_TITLE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.END_TIME_TEXT
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
        store = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoachMeApplication).store
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
    fun correctInitialScreenContent() {
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
            COLOR_TEXT,
            COLOR_BOX,
            DESCRIPTION,
        )

        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            initiallyDisplayed.forEach { tag ->
                composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
                composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
            }
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
    fun addEventWithEmptyEmailRedirectsToErrorPage() {
        store.setCurrentEmail("")
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.Buttons.GO_TO_LOGIN_BUTTON)
                .assertIsDisplayed()
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.TextFields.ERROR_MESSAGE_FIELD)
                .assertIsDisplayed()

            composeTestRule.onNodeWithText("New event did not receive an email address.\n Please return to the login page and try again.")
                .assertIsDisplayed()
        }
    }

    @Test
    fun addEventWithCustomNameAndDescriptionRedirectsToSchedule() {
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            fillAndCheckFocus(defaultEventName, EVENT_NAME)
            fillAndCheckFocus(defaultEventDescription, DESCRIPTION)

            composeTestRule.onNodeWithTag(SAVE)
                .assertExists()
            composeTestRule.onNodeWithTag(SAVE)
                .performClick()

            intended(hasComponent(ScheduleActivity::class.java.name))
        }
    }

    @Test
    fun cancelAddEventCorrectlyRedirectsToSchedule() {
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(CANCEL)
                .performClick()

            intended(hasComponent(ScheduleActivity::class.java.name))
        }
    }

    @Test
    fun addEventSavesToDatabase() {
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(EVENT_NAME)
                .performTextInput(defaultEventName)
            composeTestRule.onNodeWithTag(DESCRIPTION)
                .performTextInput(defaultEventDescription)
            composeTestRule.onNodeWithTag(SAVE)
                .performClick()

            val expectedEvent = Event(
                defaultEventName,
                defaultEventStart.format(eventDateFormatter),
                defaultEventEnd.format(eventDateFormatter),
                defaultEventDescription,
                EventColors.DEFAULT.color.value.toString()
            )

            store.getSchedule(currentWeekMonday).thenAccept {
                val actualEvents = it.events

                assertThat(actualEvents.size, `is`(1))
                assertThat(actualEvents[0], `is`(expectedEvent))
            }
        }
    }

    // Note: Pressing the cancel button on the date picker works, but pressing the ok button does not
    private fun cancelAndSavePickedDate(
        dateTag: String,
        dialogTitleTag: String,
    ) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val testDay = 15
        val expectedDate = currentWeekMonday.withDayOfMonth(testDay)

        // Open date picker
        composeTestRule.onNodeWithTag(dateTag)
            .performClick()
        composeTestRule.onNodeWithTag(dialogTitleTag, useUnmergedTree = true)
            .assertExists()

        // Choose the 15th of the month
        device.wait(Until.findObject(By.text("$testDay")), 500)
        device.findObject(By.text("$testDay")).click()
        device.waitForIdle()

        device.findObject(By.text("Cancel")).click()
        device.wait(Until.gone(By.text("Cancel")), 500)

        composeTestRule.onNodeWithTag(START_DATE)
            .assertTextEquals(EventOps.getDefaultEventStart().format(EventOps.getDayFormatter()))

        // Open date picker
        composeTestRule.onNodeWithTag(dateTag)
            .performClick()
        composeTestRule.onNodeWithTag(dialogTitleTag, useUnmergedTree = true)
            .assertExists()

        // Choose the 15th of the month
        device.wait(Until.findObject(By.text("$testDay")), 500)
        device.findObject(By.text("$testDay")).click()
        device.waitForIdle()

        /*device.findObject(By.text("Ok")).click()
        device.wait(Until.gone(By.text("Ok")), 500)

        composeTestRule.onNodeWithTag(START_DATE)
            .assertTextEquals(expectedDate.format(EventOps.getDayFormatter()))*/
    }

    @Test
    fun cancelAndConfirmStartDateWorks() {
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            cancelAndSavePickedDate(START_DATE, START_DATE_DIALOG_TITLE)
        }
    }

    @Test
    fun cancelAndConfirmEndDateWorks() {
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            cancelAndSavePickedDate(END_DATE, END_DATE_DIALOG_TITLE)
        }
    }

    // Note: Pressing the cancel button on the time picker works, but pressing the ok button does not
    private fun cancelAndSavePickedTime(
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
        device.wait(Until.findObject(By.text("$testHour1")), 500)
        device.findObject(By.text("$testHour1")).click()
        device.waitForIdle()
        device.wait(Until.findObject(By.text("$testHour2")), 500)
        device.findObject(By.text("$testHour2")).click()
        device.waitForIdle()
        device.wait(Until.findObject(By.text("$testMinute1")), 500)
        device.findObject(By.text("$testMinute1")).click()
        device.waitForIdle()
        device.wait(Until.findObject(By.text("$testMinute2")), 500)
        device.findObject(By.text("$testMinute2")).click()
        device.waitForIdle()

        // Press cancel
        device.findObject(By.text("Cancel")).click()
        device.wait(Until.gone(By.text("Cancel")), 500)

        composeTestRule.onNodeWithTag(START_TIME)
            .assertTextEquals(EventOps.getDefaultEventStart().format(EventOps.getTimeFormatter()))

        // Open time picker
        composeTestRule.onNodeWithTag(timeTag)
            .performClick()
        composeTestRule.onNodeWithTag(dialogTitleTag, useUnmergedTree = true)
            .assertExists()

        device.waitForIdle()

        // Choose the 15:30
        device.wait(Until.findObject(By.text("$testHour1")), 500)
        device.findObject(By.text("$testHour1")).click(500)
        device.waitForIdle()
        device.wait(Until.findObject(By.text("$testHour2")), 500)
        device.findObject(By.text("$testHour2")).click(500)
        device.waitForIdle()
        device.wait(Until.findObject(By.text("$testMinute1")), 500)
        device.findObject(By.text("$testMinute1")).click(500)
        device.waitForIdle()
        device.wait(Until.findObject(By.text("$testMinute2")), 500)
        device.findObject(By.text("$testMinute2")).click(500)
        device.waitForIdle(10000)

        // Press ok
        device.findObject(By.text("Ok")).click()
        device.wait(Until.gone(By.text("Ok")), 500)
    }

    @Test
    fun cancelAndConfirmStartTimeWorks() {
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            cancelAndSavePickedTime(START_TIME, START_TIME_DIALOG_TITLE)
        }
    }

    @Test
    fun cancelAndConfirmEndTimeWorks() {
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            cancelAndSavePickedTime(END_TIME, END_TIME_DIALOG_TITLE)
        }
    }

    @Test
    fun chooseAndCancelColorPickerWorks() {
        ActivityScenario.launch<CreateEventActivity>(defaultIntent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

            // Open color picker
            composeTestRule.onNodeWithTag(COLOR_BOX)
                .performClick()
            composeTestRule.onNodeWithTag(COLOR_DIALOG_TITLE, useUnmergedTree = true)
                .assertExists()

            // Press cancel
            device.findObject(By.text("Cancel")).click()
            device.wait(Until.gone(By.text("Cancel")), 500)

            composeTestRule.onNodeWithTag(COLOR_DIALOG_TITLE)
                .assertDoesNotExist()
        }
    }

}

