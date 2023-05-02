package com.github.sdpcoachme.schedule

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.EventColors
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.database.MockDatabase
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.END_DATE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.END_TIME
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.SAVE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.START_DATE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Clickables.Companion.START_TIME
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Companion.COLOR_BOX
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Companion.SCAFFOLD
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Icons.Companion.CANCEL_ICON
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Icons.Companion.SAVE_ICON
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.TextFields.Companion.DESCRIPTION
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.TextFields.Companion.EVENT_NAME
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.ACTIVITY_TITLE
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.COLOR_TEXT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.END_DATE_TEXT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.END_TIME_TEXT
import com.github.sdpcoachme.schedule.CreateEventActivity.TestTags.Texts.Companion.START_DATE_TEXT
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
    private lateinit var database: Database
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
        database = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoachMeApplication).database
        database.setCurrentEmail(defaultEmail)
        Intents.init()
    }

    @After
    fun teardown() {
        database.setCurrentEmail("")
        if (database is MockDatabase) {
            (database as MockDatabase).restoreDefaultSchedulesSetup()
            println("MockDatabase was torn down")
        }
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

            database.getSchedule(currentWeekMonday).thenAccept {
                val actualEvents = it.events

                assertThat(actualEvents.size, `is`(1))
                assertThat(actualEvents[0], `is`(expectedEvent))
            }
        }
    }


}

