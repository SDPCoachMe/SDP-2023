package com.github.sdpcoachme.schedule

import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.Dashboard.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.Dashboard.TestTags.Companion.BAR_TITLE
import com.github.sdpcoachme.Dashboard.TestTags.Companion.DRAWER_HEADER
import com.github.sdpcoachme.data.Event
import com.github.sdpcoachme.data.ShownEvent
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.Buttons.Companion.GO_TO_LOGIN_BUTTON
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.TextFields.Companion.ERROR_MESSAGE_FIELD
import com.github.sdpcoachme.schedule.ScheduleActivity.TestTags.Companion.BASIC_SCHEDULE
import com.github.sdpcoachme.schedule.ScheduleActivity.TestTags.Companion.SCHEDULE_HEADER
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

@RunWith(AndroidJUnit4::class)
class ScheduleActivityTest {

    private val database = (InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as CoachMeApplication).database
    private val defaultEmail = "example@email.com"
    private val defaultIntent = Intent(ApplicationProvider.getApplicationContext(), ScheduleActivity::class.java)

    private val currentMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private val eventList = EventOpsTest.eventList

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        database.setCurrentEmail(defaultEmail)
    }

    @Test
    fun addEventsToDatabaseUpdatesUserInfoCorrectly() {
        defaultEmail
        val oldUserInfo = database.getUser(defaultEmail)

        database.addEventsToUser(defaultEmail, eventList)

        val newUserInfo = database.getUser(defaultEmail)
        newUserInfo.thenAccept {
            assertTrue(oldUserInfo != newUserInfo)
            assertTrue(it.events == eventList)
        }
    }

    @Test
    fun correctInitialScreenContent() {
        val initiallyDisplayed = listOf(SCHEDULE_HEADER, BASIC_SCHEDULE)
        ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
            initiallyDisplayed.forEach { tag ->
                composeTestRule.onNodeWithTag(tag).assertExists()
            }
        }
    }

    @Test
    fun errorPageIsShownWhenScheduleIsLaunchedWithEmptyCurrentEmail() {
        database.setCurrentEmail("")
        ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
            // not possible to use Intents.init()... to check if the correct intent
            // is launched as the intents are launched from within the onCreate function
            composeTestRule.onNodeWithTag(GO_TO_LOGIN_BUTTON).assertIsDisplayed()
            composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD).assertIsDisplayed()
        }
    }

    // TODO in next sprint: handle the exception thrown when the user is not found in the database
/*    @Test
    fun getExceptionIsThrownCorrectly() {
        val email = "throwGet@Exception.com"
        val scheduleIntent = defaultIntent.putExtra("email", email)
        ActivityScenario.launch<ScheduleActivity>(scheduleIntent).use {
            Intents.init()
            Intents.intended(allOf(
                IntentMatchers.hasComponent(IntentExtrasErrorHandlerActivity::class.java.name),
                IntentMatchers.hasExtra("errorMsg", "Schedule couldn't get the user information from the database." +
                    "\n Please return to the login page and try again."))
            )
            Intents.release()
        }
    }*/

    @Test
    fun eventsOfCurrentWeekAreDisplayedCorrectly() {
        database.addEventsToUser(defaultEmail, eventList).thenRun {
            ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
                composeTestRule.onNodeWithTag(BASIC_SCHEDULE).assertExists()
                val userInfo = database.getUser(defaultEmail)
                userInfo.thenAccept {
                    it.events.forEach { event ->
                        composeTestRule.onNodeWithText(event.name).assertExists()
                    }
                }
            }
        }
    }

    @Test
    fun multiDayEventsOfCurrentWeekAreDisplayedCorrectly() {
        val multiDayEvent = Event(
            name = "Multi Day Event",
            color = Color(0xFFAFBBF2).value.toString(),
            start = currentMonday.atTime(13, 0, 0).toString(),
            end = currentMonday.plusDays(2).atTime(15, 0, 0).toString(),
            description = "This is a multi day event.",
        )
        database.addEventsToUser(defaultEmail, listOf(multiDayEvent)).thenRun {
            val scheduleIntent = defaultIntent.putExtra("email", defaultEmail)
            ActivityScenario.launch<ScheduleActivity>(scheduleIntent).use {
                composeTestRule.onNodeWithTag(BASIC_SCHEDULE).assertExists()
                val userInfo = database.getUser(defaultEmail)
                userInfo.thenAccept {
                    val expectedShownEvents = listOf<ShownEvent>(
                        ShownEvent(
                            name = multiDayEvent.name,
                            color = multiDayEvent.color,
                            start = multiDayEvent.start,
                            startText = multiDayEvent.start,
                            end = LocalDateTime.parse(multiDayEvent.start).withHour(23).withMinute(59).withSecond(59).toString(),
                            endText = multiDayEvent.end,
                            description = multiDayEvent.description,
                        ),
                        ShownEvent(
                            name = multiDayEvent.name,
                            color = multiDayEvent.color,
                            start = LocalDateTime.parse(multiDayEvent.start).plusDays(1).withHour(0).withMinute(0).withSecond(0).toString(),
                            startText = multiDayEvent.start,
                            end = LocalDateTime.parse(multiDayEvent.start).plusDays(1).withHour(23).withMinute(59).withSecond(59).toString(),
                            endText = multiDayEvent.end,
                            description = multiDayEvent.description,
                        ),
                        ShownEvent(
                            name = multiDayEvent.name,
                            color = multiDayEvent.color,
                            start = LocalDateTime.parse(multiDayEvent.start).plusDays(2).withHour(0).withMinute(0).withSecond(0).toString(),
                            startText = multiDayEvent.start,
                            end = multiDayEvent.end,
                            endText = multiDayEvent.end,
                            description = multiDayEvent.description,
                        ),
                    )
                    val actualShownEvents = EventOps.eventsToWrappedEvents(it.events)
                    assertTrue(expectedShownEvents == actualShownEvents)
                }
            }
        }
    }

    @Test
    fun dashboardHasRightTitleOnSchedule() {
        ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(BAR_TITLE).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(BAR_TITLE).assert(hasText("Schedule"))
        }
    }
    @Test
    fun dashboardIsAccessibleAndDisplayableFromSchedule() {
        ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).assertExists().assertIsDisplayed()
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
            composeTestRule.onNodeWithTag(DRAWER_HEADER).assertExists().assertIsDisplayed()
        }
    }
}