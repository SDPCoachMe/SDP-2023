package com.github.sdpcoachme.schedule

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sdpcoachme.CoachMeApplication
import com.github.sdpcoachme.CoachMeTestApplication
import com.github.sdpcoachme.data.UserInfoSamples
import com.github.sdpcoachme.data.schedule.EventType
import com.github.sdpcoachme.data.schedule.ShownEvent
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.Buttons.Companion.GO_TO_LOGIN_BUTTON
import com.github.sdpcoachme.errorhandling.IntentExtrasErrorHandlerActivity.TestTags.TextFields.Companion.ERROR_MESSAGE_FIELD
import com.github.sdpcoachme.location.MapActivity
import com.github.sdpcoachme.schedule.ScheduleActivity.TestTags.Buttons.Companion.ADD_EVENT_BUTTON
import com.github.sdpcoachme.schedule.ScheduleActivity.TestTags.Buttons.Companion.ADD_GROUP_EVENT_BUTTON
import com.github.sdpcoachme.schedule.ScheduleActivity.TestTags.Buttons.Companion.ADD_PRIVATE_EVENT_BUTTON
import com.github.sdpcoachme.schedule.ScheduleActivity.TestTags.Buttons.Companion.LEFT_ARROW_BUTTON
import com.github.sdpcoachme.schedule.ScheduleActivity.TestTags.Buttons.Companion.RIGHT_ARROW_BUTTON
import com.github.sdpcoachme.schedule.ScheduleActivity.TestTags.Companion.BASIC_SCHEDULE
import com.github.sdpcoachme.schedule.ScheduleActivity.TestTags.Companion.WEEK_HEADER
import com.github.sdpcoachme.schedule.ScheduleActivity.TestTags.TextFields.Companion.CURRENT_WEEK_TEXT_FIELD
import com.github.sdpcoachme.ui.Dashboard
import com.github.sdpcoachme.ui.Dashboard.TestTags.Buttons.Companion.HAMBURGER_MENU
import com.github.sdpcoachme.weather.WeatherViewTest.Companion.WEATHER_CLOUD_DONE
import com.github.sdpcoachme.weather.WeatherViewTest.Companion.WEATHER_CLOUD_OFF
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

@RunWith(AndroidJUnit4::class)
class ScheduleActivityTest {
    private lateinit var store: CachingStore
    private val coachEmail = UserInfoSamples.COACH_1.email
    private val nonCoachEmail = UserInfoSamples.NON_COACH_1.email
    private val defaultIntent = Intent(ApplicationProvider.getApplicationContext(), ScheduleActivity::class.java)

    private val currentMonday = EventOps.getStartMonday()
    private val eventList = EventOps.getOneDayEvents()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setup() {
        (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).clearDataStoreAndResetCachingStore()
        store = (ApplicationProvider.getApplicationContext() as CoachMeApplication).store
        store.retrieveData.get(1, SECONDS)
        store.setCurrentEmail(coachEmail).get(1000, MILLISECONDS)
    }

    @After
    fun teardown() {
        EventOps.clearMultiDayEventMap()
        store.setCurrentEmail("")
        // get application
        (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).clearDataStoreAndResetCachingStore()
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
        store.setCurrentEmail("")
        ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
            composeTestRule.onNodeWithTag(GO_TO_LOGIN_BUTTON).assertIsDisplayed()
            composeTestRule.onNodeWithTag(ERROR_MESSAGE_FIELD).assertIsDisplayed()
        }
    }

    @Test
    fun getExceptionIsThrownCorrectly() {
        store.setCurrentEmail("throwGetSchedule@Exception.com").get(100, MILLISECONDS)
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
        store.setCurrentEmail(coachEmail)
        store.addEvent(eventList[0], currentMonday).thenCompose {
            store.addEvent(eventList[1], currentMonday)
        }.thenRun {
            ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
                composeTestRule.onNodeWithTag(BASIC_SCHEDULE).assertExists()
                val schedule = store.getSchedule(currentMonday)
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
        val multiDayEvent = EventOps.getMultiDayEvent()

        store.addEvent(multiDayEvent, currentMonday).thenRun {
            ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
                composeTestRule.onNodeWithTag(BASIC_SCHEDULE).assertExists()

                val schedule = store.getSchedule(currentMonday)
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
        val multiWeekEvent = EventOps.getMultiWeekEvent()

        store.addEvent(multiWeekEvent, currentMonday).thenRun {
            ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
                composeTestRule.onNodeWithTag(BASIC_SCHEDULE).assertExists()

                val schedule = store.getSchedule(currentMonday)
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
        val nextWeekEvent = EventOps.getNextWeekEvent()

        store.addEvent(nextWeekEvent, currentMonday).thenRun {
            store.setCurrentEmail(coachEmail)
            ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
                composeTestRule.onNodeWithTag(BASIC_SCHEDULE).assertExists()

                val schedule = store.getSchedule(currentMonday)
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
        val previousWeekEvent = EventOps.getPreviousWeekEvent()

        store.addEvent(previousWeekEvent, currentMonday).thenRun {
            store.setCurrentEmail(coachEmail).thenApply {
                ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
                    composeTestRule.onNodeWithTag(BASIC_SCHEDULE).assertExists()

                    val schedule = store.getSchedule(currentMonday)
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

    @Test
    fun clickOnAddEventButtonTakesClientToCreateEventActivity() {
        Intents.init()
        store.setCurrentEmail(nonCoachEmail).thenApply {
            ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
                composeTestRule.onNodeWithTag(ADD_EVENT_BUTTON)
                    .assertExists()
                    .performClick()

                Intents.intended(hasExtra("eventType", EventType.PRIVATE.eventTypeName))
                Intents.intended(hasComponent(CreateEventActivity::class.java.name))
            }
        }

        Intents.release()
    }

    @Test
    fun clickOnAddEventButtonLetsCoachRedirectToCreatePrivateEventActivity() {
        Intents.init()
        store.setCurrentEmail(coachEmail).thenApply {
            ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {

                composeTestRule.onNodeWithTag(ADD_EVENT_BUTTON)
                    .assertExists()
                    .performClick() // should open a dropdown menu

                composeTestRule.onNodeWithTag(ADD_PRIVATE_EVENT_BUTTON)
                    .assertExists()
                    .performClick()

                Intents.intended(hasExtra("eventType", EventType.PRIVATE.eventTypeName))
                Intents.intended(hasComponent(CreateEventActivity::class.java.name))
            }
        }
        Intents.release()
    }

    @Test
    fun clickOnAddEventButtonLetsCoachRedirectToCreateGroupEventActivity() {
        Intents.init()
        store.setCurrentEmail(coachEmail).thenApply {
            ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {

                composeTestRule.onNodeWithTag(ADD_EVENT_BUTTON)
                    .assertExists()
                    .performClick() // should open a dropdown menu

                composeTestRule.onNodeWithTag(ADD_GROUP_EVENT_BUTTON)
                    .assertExists()
                    .performClick()

                Intents.intended(hasExtra("eventType", EventType.GROUP.eventTypeName))
                Intents.intended(hasComponent(CreateEventActivity::class.java.name))
            }
        }
        Intents.release()
    }

    @Test
    fun weatherForecastIsCorrectlyDisplayedInSchedule() {
        store.setCurrentEmail(coachEmail).thenApply {
            ActivityScenario.launch<ScheduleActivity>(defaultIntent).use {
                composeTestRule.onNodeWithTag("WEATHER_COLUMN")
                    .assertExists().assertIsDisplayed()
                composeTestRule.onNodeWithTag(WEATHER_CLOUD_OFF).assertExists().assertIsDisplayed()
                composeTestRule.onNodeWithTag(WEATHER_CLOUD_DONE).assertExists().assertIsDisplayed()
            }
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
