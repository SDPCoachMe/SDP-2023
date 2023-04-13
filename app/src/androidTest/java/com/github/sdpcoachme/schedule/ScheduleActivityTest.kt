package com.github.sdpcoachme.schedule

import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import java.time.temporal.TemporalAdjusters

@RunWith(AndroidJUnit4::class)
class ScheduleActivityTest {
    private val database = (InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as CoachMeApplication).database
    private val defaultEmail = "example@email.com"
    private val currentMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private val nextMonday = currentMonday.plusDays(7)
    private val eventList = listOf(
        Event(
            name = "Google I/O Keynote",
            color = Color(0xFFAFBBF2).value.toString(),
            start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atTime(13, 0, 0).toString(),
            end = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atTime(15, 0, 0).toString(),
            description = "Tune in to find out about how we're furthering our mission to organize the world’s information and make it universally accessible and useful.",
        ),
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
        /*Event(
            name = "Jetpack Compose Basics",
            color = Color(0xFF1B998B).value.toString(),
            start = nextMonday.plusDays(4).atTime(9, 0, 0).toString(),
            end = nextMonday.plusDays(4).atTime(13, 0, 0).toString(),
            description = "This Workshop will take you through the basics of building your first app with Jetpack Compose, Android's new modern UI toolkit that simplifies and accelerates UI development on Android.",
        ),*/
    )

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
        val initiallyDisplayed = listOf(
            ScheduleActivity.TestTags.SCHEDULE_HEADER,
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
    fun errorPageIsShownWhenEditProfileIsLaunchedWithEmptyCurrentEmail() {
        database.setCurrentEmail("")
        ActivityScenario.launch<DashboardActivity>(Intent(ApplicationProvider.getApplicationContext(), ScheduleActivity::class.java)).use {
            // not possible to use Intents.init()... to check if the correct intent
            // is launched as the intents are launched from within the onCreate function
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.Buttons.GO_TO_LOGIN_BUTTON).assertIsDisplayed()
            composeTestRule.onNodeWithTag(IntentExtrasErrorHandlerActivity.TestTags.TextFields.ERROR_MESSAGE_FIELD).assertIsDisplayed()
        }
    }

    // TODO in next sprint: handle the exception thrown when the user is not found in the database
/*    @Test
    fun getExceptionIsThrownCorrectly() {
        val email = "throwGet@Exception.com"
        val scheduleIntent = Intent(ApplicationProvider.getApplicationContext(), ScheduleActivity::class.java)
            .putExtra("email", email)
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
    fun multiDayEventsOfCurrentWeekAreDisplayedCorrectly() {
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
}