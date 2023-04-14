package com.github.sdpcoachme.schedule

import androidx.compose.ui.graphics.Color
import com.github.sdpcoachme.data.Event
import com.github.sdpcoachme.data.ShownEvent
import junit.framework.TestCase
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

class EventOpsTest {
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
            start = currentMonday.plusDays(2).atTime(21, 0, 0).toString(),
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
        Event(
            name = "Jetpack Compose Basics",
            color = Color(0xFF1B998B).value.toString(),
            start = nextMonday.plusDays(4).atTime(9, 0, 0).toString(),
            end = nextMonday.plusDays(4).atTime(13, 0, 0).toString(),
            description = "This Workshop will take you through the basics of building your first app with Jetpack Compose, Android's new modern UI toolkit that simplifies and accelerates UI development on Android.",
        ),
    )

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
        TestCase.assertEquals(expectedMap, actualMap)
    }
}