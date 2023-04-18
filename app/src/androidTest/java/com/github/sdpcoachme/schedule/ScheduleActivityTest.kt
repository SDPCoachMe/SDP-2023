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
import com.github.sdpcoachme.DashboardActivity
import com.github.sdpcoachme.data.Event
import com.github.sdpcoachme.data.ShownEvent
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@RunWith(AndroidJUnit4::class)
class ScheduleActivityTest {
    private val database = (InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as CoachMeApplication).database
    private val defaultEmail = "example@email.com"
    private val currentMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private val eventList = listOf(
        Event(
            name = "Developer Keynote",
            color = Color(0xFFAFBBF2).value.toString(),
            start = currentMonday.plusDays(2).atTime(7, 0, 0).toString(),
            end = currentMonday.plusDays(2).atTime(9, 0, 0).toString(),
            description = "Learn about the latest updates to our developer products and platforms from Google Developers.",
        ),
        Event(
            name = "What's new in Android",
            color = Color(0xFF1B998B).value.toString(),
            start = currentMonday.plusDays(2).atTime(10, 0, 0).toString(),
            end = currentMonday.plusDays(2).atTime(12, 0, 0).toString(),
            description = "In this Keynote, Chet Haase, Dan Sandler, and Romain Guy discuss the latest Android features and enhancements for developers.",
        ),
        Event(
            name = "What's new in Machine Learning",
            color = Color(0xFFF4BFDB).value.toString(),
            start = currentMonday.plusDays(2).atTime(22, 0, 0).toString(),
            end = currentMonday.plusDays(3).atTime(4, 0, 0).toString(),
            description = "Learn about the latest and greatest in ML from Google. We’ll cover what’s available to developers when it comes to creating, understanding, and deploying models for a variety of different applications.",
        ),
        Event(
            name = "What's new in Material Design",
            color = Color(0xFF6DD3CE).value.toString(),
            start = currentMonday.plusDays(3).atTime(13, 0, 0).toString(),
            end = currentMonday.plusDays(3).atTime(15, 0, 0).toString(),
            description = "Learn about the latest design improvements to help you build personal dynamic experiences with Material Design.",
        ),
    )

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        database.setCurrentEmail(defaultEmail)
    }

    @Test
    fun addEventsToDatabaseUpdatesUserInfoCorrectly() {
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
        val initiallyDisplayed = listOf(
            ScheduleActivity.TestTags.WEEK_HEADER,
            ScheduleActivity.TestTags.BASIC_SCHEDULE,
        )
        val scheduleIntent = Intent(ApplicationProvider.getApplicationContext(), ScheduleActivity::class.java)
        ActivityScenario.launch<ScheduleActivity>(scheduleIntent).use {
            initiallyDisplayed.forEach { tag ->
                composeTestRule.onNodeWithTag(tag).assertExists()
            }
        }
    }

    @Test
    fun errorPageIsShownWhenScheduleIsLaunchedWithEmptyCurrentEmail() {
        database.setCurrentEmail("")
        ActivityScenario.launch<DashboardActivity>(Intent(ApplicationProvider.getApplicationContext(), ScheduleActivity::class.java)).use {
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.Buttons.GO_TO_LOGIN_BUTTON).assertIsDisplayed()
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.TextFields.ERROR_MESSAGE_FIELD).assertIsDisplayed()
        }
    }

    @Test
    fun getExceptionIsThrownCorrectly() {
        val email = "throwGet@Exception.com"
        database.setCurrentEmail(email)
        val scheduleIntent = Intent(ApplicationProvider.getApplicationContext(), ScheduleActivity::class.java)
        ActivityScenario.launch<ScheduleActivity>(scheduleIntent).use {
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.Buttons.GO_TO_LOGIN_BUTTON).assertIsDisplayed()
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.TextFields.ERROR_MESSAGE_FIELD).assertIsDisplayed()
        }
    }

    @Test
    fun eventsOfCurrentWeekAreDisplayedCorrectly() {
        database.addEventsToUser(defaultEmail, eventList).thenRun {
            val scheduleIntent = Intent(ApplicationProvider.getApplicationContext(), ScheduleActivity::class.java)
            ActivityScenario.launch<ScheduleActivity>(scheduleIntent).use {
                composeTestRule.onNodeWithTag(ScheduleActivity.TestTags.BASIC_SCHEDULE).assertExists()
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
    fun multiDayEventOfCurrentWeekIsDisplayedCorrectly() {
        val multiDayEvent = Event(
            name = "Multi Day Event",
            color = Color(0xFFAFBBF2).value.toString(),
            start = currentMonday.atTime(13, 0, 0).toString(),
            end = currentMonday.plusDays(2).atTime(15, 0, 0).toString(),
            description = "This is a multi day event.",
        )
        database.addEventsToUser(defaultEmail, listOf(multiDayEvent)).thenRun {
            val scheduleIntent = Intent(ApplicationProvider.getApplicationContext(), ScheduleActivity::class.java)
            scheduleIntent.putExtra("email", defaultEmail)
            ActivityScenario.launch<ScheduleActivity>(scheduleIntent).use {
                composeTestRule.onNodeWithTag(ScheduleActivity.TestTags.BASIC_SCHEDULE).assertExists()
                val userInfo = database.getUser(defaultEmail)
                userInfo.thenAccept {
                    val expectedShownEvents = listOf(
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
    fun eventsOfNextWeekAreDisplayedCorrectly() {
        val nextMonday = currentMonday.plusDays(7)
        val nextWeekEvent = Event(
            name = "Next Week Event",
            color = Color(0xFFAFBBF2).value.toString(),
            start = nextMonday.atTime(13, 0, 0).toString(),
            end = nextMonday.atTime(15, 0, 0).toString(),
            description = "This is an event of the next week.",
        )
        database.addEventsToUser(defaultEmail, listOf(nextWeekEvent)).thenRun {
            val scheduleIntent = Intent(ApplicationProvider.getApplicationContext(), ScheduleActivity::class.java)
            scheduleIntent.putExtra("email", defaultEmail)
            ActivityScenario.launch<ScheduleActivity>(scheduleIntent).use {
                composeTestRule.onNodeWithTag(ScheduleActivity.TestTags.BASIC_SCHEDULE).assertExists()
                val userInfo = database.getUser(defaultEmail)
                userInfo.thenAccept {
                    val expectedShownEvents = listOf(
                        ShownEvent(
                            name = nextWeekEvent.name,
                            color = nextWeekEvent.color,
                            start = nextWeekEvent.start,
                            startText = nextWeekEvent.start,
                            end = nextWeekEvent.end,
                            endText = nextWeekEvent.end,
                            description = nextWeekEvent.description,
                        ),
                    )
                    val actualShownEvents = EventOps.eventsToWrappedEvents(it.events)
                    assertTrue(expectedShownEvents == actualShownEvents)
                }
            }
        }
    }

    @Test
    fun eventsOfPreviousWeekAreDisplayedCorrectly() {
        val previousMonday = currentMonday.minusDays(7)
        val previousWeekEvent = Event(
            name = "Previous Week Event",
            color = Color(0xFFAFBBF2).value.toString(),
            start = previousMonday.atTime(13, 0, 0).toString(),
            end = previousMonday.atTime(15, 0, 0).toString(),
            description = "This is an event of the previous week.",
        )
        database.addEventsToUser(defaultEmail, listOf(previousWeekEvent)).thenRun {
            val scheduleIntent = Intent(ApplicationProvider.getApplicationContext(), ScheduleActivity::class.java)
            scheduleIntent.putExtra("email", defaultEmail)
            ActivityScenario.launch<ScheduleActivity>(scheduleIntent).use {
                composeTestRule.onNodeWithTag(ScheduleActivity.TestTags.BASIC_SCHEDULE).assertExists()
                val userInfo = database.getUser(defaultEmail)
                userInfo.thenAccept {
                    val expectedShownEvents = listOf(
                        ShownEvent(
                            name = previousWeekEvent.name,
                            color = previousWeekEvent.color,
                            start = previousWeekEvent.start,
                            startText = previousWeekEvent.start,
                            end = previousWeekEvent.end,
                            endText = previousWeekEvent.end,
                            description = previousWeekEvent.description,
                        ),
                    )
                    val actualShownEvents = EventOps.eventsToWrappedEvents(it.events)
                    assertTrue(expectedShownEvents == actualShownEvents)
                }
            }
        }
    }

    @Test
    fun clickOnRightArrowButtonChangesWeekCorrectly() {
        val formatter = DateTimeFormatter.ofPattern("d MMM")
        val scheduleIntent = Intent(ApplicationProvider.getApplicationContext(), ScheduleActivity::class.java)
        ActivityScenario.launch<ScheduleActivity>(scheduleIntent).use {
            composeTestRule.onNodeWithTag(ScheduleActivity.TestTags.BASIC_SCHEDULE).assertExists()
            composeTestRule.onNodeWithTag(ScheduleActivity.TestTags.Buttons.RIGHT_ARROW_BUTTON).assertExists()
            composeTestRule.onNodeWithTag(ScheduleActivity.TestTags.Buttons.RIGHT_ARROW_BUTTON).performClick()
            composeTestRule.onNodeWithTag(ScheduleActivity.TestTags.TextFields.CURRENT_WEEK_TEXT_FIELD).assertTextContains("${currentMonday.plusDays(7).format(formatter)} - ${currentMonday.plusDays(13).format(formatter)}")
        composeTestRule.onNodeWithTag(ScheduleActivity.TestTags.Buttons.RIGHT_ARROW_BUTTON).performClick()
            composeTestRule.onNodeWithTag(ScheduleActivity.TestTags.TextFields.CURRENT_WEEK_TEXT_FIELD).assertTextContains("${currentMonday.plusDays(14).format(formatter)} - ${currentMonday.plusDays(20).format(formatter)}")
        }
    }

    @Test
    fun clickOnLeftArrowButtonChangesWeekCorrectly() {
        val formatter = DateTimeFormatter.ofPattern("d MMM")
        val scheduleIntent = Intent(ApplicationProvider.getApplicationContext(), ScheduleActivity::class.java)
        ActivityScenario.launch<ScheduleActivity>(scheduleIntent).use {
            composeTestRule.onNodeWithTag(ScheduleActivity.TestTags.BASIC_SCHEDULE).assertExists()
            composeTestRule.onNodeWithTag(ScheduleActivity.TestTags.Buttons.LEFT_ARROW_BUTTON).assertExists()
            composeTestRule.onNodeWithTag(ScheduleActivity.TestTags.Buttons.LEFT_ARROW_BUTTON).performClick()
            composeTestRule.onNodeWithTag(ScheduleActivity.TestTags.TextFields.CURRENT_WEEK_TEXT_FIELD).assertTextContains("${currentMonday.minusDays(7).format(formatter)} - ${currentMonday.minusDays(1).format(formatter)}")
            composeTestRule.onNodeWithTag(ScheduleActivity.TestTags.Buttons.LEFT_ARROW_BUTTON).performClick()
            composeTestRule.onNodeWithTag(ScheduleActivity.TestTags.TextFields.CURRENT_WEEK_TEXT_FIELD).assertTextContains("${currentMonday.minusDays(14).format(formatter)} - ${currentMonday.minusDays(8).format(formatter)}")
        }
    }
}