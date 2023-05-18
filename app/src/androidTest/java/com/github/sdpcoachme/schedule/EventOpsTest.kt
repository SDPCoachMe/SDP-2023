package com.github.sdpcoachme.schedule

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sdpcoachme.CoachMeTestApplication
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.ShownEvent
import com.github.sdpcoachme.database.CachingStore
import com.github.sdpcoachme.database.MockDatabase
import junit.framework.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class EventOpsTest {
    private lateinit var store: CachingStore
    private val defaultEmail = MockDatabase.getDefaultEmail()
    private val eventList = EventOps.getEventList()

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
        val multiDayEvent = EventOps.getMultiDayEvent()

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
    fun addGroupEventUpdatesMultiDayEventMap() {
        val multiDayEvent = EventOps.getMultiDayEvent()
        val multiDayGroupEvent = GroupEvent(
            event = multiDayEvent,
            organiser = defaultEmail,
            maxParticipants = 10,
            participants = mutableListOf(defaultEmail),
            groupEventId = "@@event" + defaultEmail.replace(".", ",") + multiDayEvent.start
        )

        EventOps.addGroupEvent(multiDayGroupEvent, store).thenRun {
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
        EventOps.addEvent(EventOps.getOneDayEvents()[0], store).thenRun {
            val actualMap = EventOps.getMultiDayEventMap()
            val expectedMap = mutableMapOf<Event, List<ShownEvent>>()
            TestCase.assertEquals(expectedMap, actualMap)
        }
    }

    @Test
    fun addGroupEventDoesNotUpdateMultiDayEventMapForOnedayEvents() {
        val oneDayEvent = EventOps.getOneDayEvents()[0]
        val oneDayGroupEvent = GroupEvent(
            event = oneDayEvent,
            organiser = defaultEmail,
            maxParticipants = 10,
            participants = mutableListOf(defaultEmail),
            groupEventId = "@@event" + defaultEmail.replace(".", ",") + oneDayEvent.start
        )

        EventOps.addGroupEvent(oneDayGroupEvent, store).thenRun {
            val actualMap = EventOps.getMultiDayEventMap()
            val expectedMap = mutableMapOf<Event, List<ShownEvent>>()
            TestCase.assertEquals(expectedMap, actualMap)
        }
    }
}