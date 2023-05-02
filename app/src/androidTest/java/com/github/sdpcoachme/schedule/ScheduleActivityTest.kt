package com.github.sdpcoachme.schedule

import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.ui.Dashboard
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.ShownEvent
import com.github.sdpcoachme.database.Database
import com.github.sdpcoachme.database.MockDatabase
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.Buttons.Companion.GO_TO_LOGIN_BUTTON
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.TextFields.Companion.ERROR_MESSAGE_FIELD
import com.github.sdpcoachme.location.MapActivity
import com.github.sdpcoachme.schedule.ScheduleActivity.TestTags.Buttons.Companion.LEFT_ARROW_BUTTON
import com.github.sdpcoachme.schedule.ScheduleActivity.TestTags.Buttons.Companion.RIGHT_ARROW_BUTTON
import com.github.sdpcoachme.schedule.ScheduleActivity.TestTags.Companion.BASIC_SCHEDULE
import com.github.sdpcoachme.schedule.ScheduleActivity.TestTags.Companion.WEEK_HEADER
import com.github.sdpcoachme.schedule.ScheduleActivity.TestTags.TextFields.Companion.CURRENT_WEEK_TEXT_FIELD
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
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
import java.util.concurrent.TimeUnit.SECONDS

@RunWith(AndroidJUnit4::class)
class ScheduleActivityTest {
    private lateinit var database: Database
    private val defaultEmail = "example@email.com"
    private val defaultIntent = Intent(ApplicationProvider.getApplicationContext(), ScheduleActivity::class.java)

    private val currentMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private val nextMonday = currentMonday.plusDays(7)
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
        database = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoachMeApplication).database
        database.setCurrentEmail(defaultEmail)
    }

    @After
    fun teardown() {
        database.setCurrentEmail("")
        if (database is MockDatabase) {
            (database as MockDatabase).restoreDefaultSchedulesSetup()
            println("MockDatabase was torn down")
        }
    }

    @Test
    fun correctInitialScreenContent() {
        val initiallyDisplayed = listOf(
            WEEK_HEADER,
            BASIC_SCHEDULE,
        )

        ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
            initiallyDisplayed.forEach { tag ->
                composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
            }
        }
    }

    @Test
    fun errorPageIsShownWhenScheduleIsLaunchedWithEmptyCurrentEmail() {
        database.setCurrentEmail("")
        ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(GO_TO_LOGIN_BUTTON).assertIsDisplayed()
            composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD).assertIsDisplayed()
        }
    }

    @Test
    fun getExceptionIsThrownCorrectly() {
        database.setCurrentEmail("throwGet@Exception.com")
        Intents.init()

        val mapIntent = Intent(ApplicationProvider.getApplicationContext(), MapActivity::class.java)
        ActivityScenario.launch<MapActivity>(mapIntent).use {
            composeTestRule.onNodeWithTag(HAMBURGER_MENU).performClick()
            composeTestRule.onNodeWithTag(Dashboard.TestTags.Buttons.SCHEDULE).performClick()
            composeTestRule.onNodeWithTag(GO_TO_LOGIN_BUTTON).assertIsDisplayed()
        }

        Intents.release()
    }

    @Test
    fun eventsOfCurrentWeekAreDisplayedCorrectly() {
        database.setCurrentEmail(defaultEmail)
        database.addEvent(eventList[0], currentMonday).thenCompose {
            database.addEvent(eventList[1], currentMonday)
        }.thenCompose {
            database.addEvent(eventList[2], currentMonday)
        }.thenCompose {
            database.addEvent(eventList[3], currentMonday)
        }.thenRun {
            ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
                composeTestRule.onNodeWithTag(BASIC_SCHEDULE).assertExists()
                val schedule = database.getSchedule(currentMonday)
                val nonnull = schedule.thenAccept {
                    it.events.forEach { event ->
                        composeTestRule.onNodeWithText(event.name).assertExists()
                    }
                }.exceptionally { null }.get(5, SECONDS)
                assertNotNull(nonnull)
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
        database.addEvent(multiDayEvent, currentMonday).thenRun {
            ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
                composeTestRule.onNodeWithTag(BASIC_SCHEDULE).assertExists()

                val schedule = database.getSchedule(currentMonday)
                val nonnull = schedule.thenAccept {
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
                }.exceptionally { null }.get(5, SECONDS)
                assertNotNull(nonnull)
            }
        }
    }

    @Test
    fun multiWeekEventIsDisplayedCorrectly() {
        val multiWeekEvent = Event(
            name = "Multi Week Event",
            color = Color(0xFFAFBBF2).value.toString(),
            start = currentMonday.plusDays(5).atTime(13, 0, 0).toString(),
            end = nextMonday.plusDays(1).atTime(15, 0, 0).toString(),
            description = "This is a multi week event.",
        )
        database.addEvent(multiWeekEvent, currentMonday).thenRun {
            ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
                composeTestRule.onNodeWithTag(BASIC_SCHEDULE).assertExists()

                val schedule = database.getSchedule(currentMonday)
                val nonnull = schedule.thenAccept {
                    val expectedShownEvents = listOf(
                        ShownEvent(
                            name = multiWeekEvent.name,
                            color = multiWeekEvent.color,
                            start = multiWeekEvent.start,
                            startText = multiWeekEvent.start,
                            end = LocalDateTime.parse(multiWeekEvent.start).withHour(23)
                                .withMinute(59).withSecond(59).toString(),
                            endText = multiWeekEvent.end,
                            description = multiWeekEvent.description,
                        ),
                        ShownEvent(
                            name = multiWeekEvent.name,
                            color = multiWeekEvent.color,
                            start = LocalDateTime.parse(multiWeekEvent.start).plusDays(1)
                                .withHour(0).withMinute(0).withSecond(0).toString(),
                            startText = multiWeekEvent.start,
                            end = LocalDateTime.parse(multiWeekEvent.start).plusDays(1).withHour(23)
                                .withMinute(59).withSecond(59).toString(),
                            endText = multiWeekEvent.end,
                            description = multiWeekEvent.description,
                        ),
                        ShownEvent(
                            name = multiWeekEvent.name,
                            color = multiWeekEvent.color,
                            start = LocalDateTime.parse(multiWeekEvent.start).plusDays(2)
                                .withHour(0).withMinute(0).withSecond(0).toString(),
                            startText = multiWeekEvent.start,
                            end = LocalDateTime.parse(multiWeekEvent.start).plusDays(2).withHour(23)
                                .withMinute(59).withSecond(59).toString(),
                            endText = multiWeekEvent.end,
                            description = multiWeekEvent.description,
                        ),
                        ShownEvent(
                            name = multiWeekEvent.name,
                            color = multiWeekEvent.color,
                            start = LocalDateTime.parse(multiWeekEvent.start).plusDays(3)
                                .withHour(0).withMinute(0).withSecond(0).toString(),
                            startText = multiWeekEvent.start,
                            end = multiWeekEvent.end,
                            endText = multiWeekEvent.end,
                            description = multiWeekEvent.description,
                        )
                    )
                    val actualShownEvents = EventOps.eventsToWrappedEvents(it.events)
                    assertTrue(expectedShownEvents == actualShownEvents)
                }.exceptionally { null }.get(5, SECONDS)
                assertNotNull(nonnull)
            }
        }
    }

    @Test
    fun eventsOfNextWeekAreDisplayedCorrectly() {
        val nextWeekEvent = Event(
            name = "Next Week Event",
            color = Color(0xFFAFBBF2).value.toString(),
            start = nextMonday.atTime(13, 0, 0).toString(),
            end = nextMonday.atTime(15, 0, 0).toString(),
            description = "This is an event of the next week.",
        )
        database.addEvent(nextWeekEvent, currentMonday).thenRun {
            database.setCurrentEmail(defaultEmail)
            ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
                composeTestRule.onNodeWithTag(BASIC_SCHEDULE).assertExists()

                val schedule = database.getSchedule(currentMonday)
                val nonnull = schedule.thenAccept {
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
                }.exceptionally { null }.get(5, SECONDS)
                assertNotNull(nonnull)
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
        database.addEvent(previousWeekEvent, currentMonday).thenRun {
            database.setCurrentEmail(defaultEmail)
            ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
                composeTestRule.onNodeWithTag(BASIC_SCHEDULE).assertExists()

                val schedule = database.getSchedule(currentMonday)
                val nonnull = schedule.thenAccept {
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
                }.exceptionally { null }.get(5, SECONDS)
                assertNotNull(nonnull)
            }
        }
    }

    private val formatter = DateTimeFormatter.ofPattern("d MMM")
    @Test
    fun clickOnRightArrowButtonChangesWeekCorrectly() {
        val scheduleIntent = Intent(ApplicationProvider.getApplicationContext(), ScheduleActivity::class.java)
        ActivityScenario.launch<ScheduleActivity>(scheduleIntent).use {
            composeTestRule.onNodeWithTag(BASIC_SCHEDULE).assertExists()
            composeTestRule.onNodeWithTag(RIGHT_ARROW_BUTTON).assertExists()
            composeTestRule.onNodeWithTag(RIGHT_ARROW_BUTTON).performClick()    // switch 1 week forward
            composeTestRule.onNodeWithTag(CURRENT_WEEK_TEXT_FIELD).assertTextContains("${currentMonday.plusDays(7).format(formatter)} - \n${currentMonday.plusDays(13).format(formatter)}")
            composeTestRule.onNodeWithTag(RIGHT_ARROW_BUTTON).performClick()    // switch 1 week forward
            composeTestRule.onNodeWithTag(CURRENT_WEEK_TEXT_FIELD).assertTextContains("${currentMonday.plusDays(14).format(formatter)} - \n${currentMonday.plusDays(20).format(formatter)}")
        }
    }

    @Test
    fun clickOnLeftArrowButtonChangesWeekCorrectly() {
        val scheduleIntent = Intent(ApplicationProvider.getApplicationContext(), ScheduleActivity::class.java)
        ActivityScenario.launch<ScheduleActivity>(scheduleIntent).use {
            composeTestRule.onNodeWithTag(BASIC_SCHEDULE).assertExists()
            composeTestRule.onNodeWithTag(LEFT_ARROW_BUTTON).assertExists()
            composeTestRule.onNodeWithTag(LEFT_ARROW_BUTTON).performClick()   // switch 1 week backward
            composeTestRule.onNodeWithTag(CURRENT_WEEK_TEXT_FIELD).assertTextContains("${currentMonday.minusDays(7).format(formatter)} - \n${currentMonday.minusDays(1).format(formatter)}")
            composeTestRule.onNodeWithTag(LEFT_ARROW_BUTTON).performClick()  // switch 1 week backward
            composeTestRule.onNodeWithTag(CURRENT_WEEK_TEXT_FIELD).assertTextContains("${currentMonday.minusDays(14).format(formatter)} - \n${currentMonday.minusDays(8).format(formatter)}")
        }
    }


    // This test does not work for now because of some bug of google that might be fixed in the future
/*@Test
    fun backArrowTakesUserToMap() {
        ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(BACK)
                .assertIsDisplayed()
                .performClick()
                .assertIsNotDisplayed()
            composeTestRule.onNodeWithTag(MapActivity.TestTags.MAP)
        }
    }*/
}
