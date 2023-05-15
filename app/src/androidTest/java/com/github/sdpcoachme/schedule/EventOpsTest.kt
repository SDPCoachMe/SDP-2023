package com.github.sdpcoachme.schedule

import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sdpcoachme.CoachMeTestApplication
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.ShownEvent
import com.github.sdpcoachme.database.CachingStore
import junit.framework.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

class EventOpsTest {

    /**
     * @val eventList is set as a companion object so that it can be accessed by ScheduleActivityTest
     */
    companion object {
        private val currentMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val oneDayEvents = listOf(
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
        )
        val multiDayEvent = Event(
            name = "What's new in Machine Learning",
            color = Color(0xFFF4BFDB).value.toString(),
            start = currentMonday.plusDays(2).atTime(22, 0, 0).toString(),
            end = currentMonday.plusDays(3).atTime(4, 0, 0).toString(),
            description = "Learn about the latest and greatest in ML from Google. We’ll cover what’s available to developers when it comes to creating, understanding, and deploying models for a variety of different applications.",
        )
        val multiWeekEvent = Event(
            name = "What's new in Material Design",
            color = Color(0xFF6DD3CE).value.toString(),
            start = currentMonday.plusDays(3).atTime(13, 0, 0).toString(),
            end = currentMonday.plusWeeks(1).atTime(15, 0, 0).toString(),
            description = "Learn about the latest design improvements to help you build personal dynamic experiences with Material Design."
        )

        val eventList = oneDayEvents + multiDayEvent + multiWeekEvent
    }

    private lateinit var store: CachingStore
    private val defaultEmail = "example@email.com"

    @Before
    fun setUp() {
        (ApplicationProvider.getApplicationContext() as CoachMeTestApplication).clearDataStoreAndResetCachingStore()
        store = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as CoachMeTestApplication).store
        store.setCurrentEmail(defaultEmail)
    }

    @After
    fun clearMultiDayEventMap() {
        EventOps.clearMultiDayEventMap()
    }

    @Test
    fun eventsToWrappedEventsCreatesCorrectMap() {
        EventOps.eventsToWrappedEvents(eventList)
        val actualMap = EventOps.getMultiDayEventMap()
        val expectedMap = mutableMapOf<Event, List<ShownEvent>>()

        eventList.forEach { event ->
            val shownEvents = mutableListOf<ShownEvent>()
            val start = LocalDateTime.parse(event.start)
            val end = LocalDateTime.parse(event.end)
            val daysBetween = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()).toInt()
            if (daysBetween >= 1) {
                for (i in 0..daysBetween) {
                    val shownEvent = ShownEvent(
                        name = event.name,
                        color = event.color,
                        start = if (i == 0) event.start else start.plusDays(i.toLong()).withHour(0).withMinute(0).withSecond(0).toString(),
                        startText = event.start,
                        end = if (i == daysBetween) event.end else start.plusDays(i.toLong()).withHour(23).withMinute(59).withSecond(59).toString(),
                        endText = event.end,
                        description = event.description,
                    )
                    shownEvents.add(shownEvent)
                }
                expectedMap[event] = shownEvents
            }
        }

        for (event in expectedMap.keys) {
            for (shownEvent in expectedMap[event]!!) {
                TestCase.assertTrue(actualMap.containsKey(event))
                TestCase.assertTrue(actualMap[event]!!.contains(shownEvent))
            }
        }

        TestCase.assertEquals(expectedMap.size, actualMap.size)
    }

    @Test
    fun addEventUpdatesMultiDayEventMap() {
        EventOps.addEvent(multiDayEvent, store).thenRun {
            val actualMap = EventOps.getMultiDayEventMap()
            val expectedMap = mutableMapOf<Event, List<ShownEvent>>()

            val shownEvents = mutableListOf<ShownEvent>()
            val start = LocalDateTime.parse(multiDayEvent.start)
            val end = LocalDateTime.parse(multiDayEvent.end)
            val daysBetween = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()).toInt()
            if (daysBetween >= 1) {
                for (i in 0..daysBetween) {
                    val shownEvent = ShownEvent(
                        name = multiDayEvent.name,
                        color = multiDayEvent.color,
                        start = if (i == 0) multiDayEvent.start else start.plusDays(i.toLong()).withHour(0).withMinute(0).withSecond(0).toString(),
                        startText = multiDayEvent.start,
                        end = if (i == daysBetween) multiDayEvent.end else start.plusDays(i.toLong()).withHour(23).withMinute(59).withSecond(59).toString(),
                        endText = multiDayEvent.end,
                        description = multiDayEvent.description,
                    )
                    shownEvents.add(shownEvent)
                }
                expectedMap[multiDayEvent] = shownEvents
            }
            TestCase.assertEquals(expectedMap, actualMap)
        }
    }

    @Test
    fun addEventDoesNotUpdateMultiDayEventMapForOnedayEvents() {
        EventOps.addEvent(oneDayEvents[0], store).thenRun {
            val actualMap = EventOps.getMultiDayEventMap()
            val expectedMap = mutableMapOf<Event, List<ShownEvent>>()
            TestCase.assertEquals(expectedMap, actualMap)
        }
    }
}