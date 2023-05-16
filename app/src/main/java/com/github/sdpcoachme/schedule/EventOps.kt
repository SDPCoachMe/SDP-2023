package com.github.sdpcoachme.schedule

import androidx.compose.ui.graphics.Color
import com.github.sdpcoachme.data.Address
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.GroupEvent
import com.github.sdpcoachme.data.schedule.Schedule
import com.github.sdpcoachme.data.schedule.ShownEvent
import com.github.sdpcoachme.database.CachingStore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.CompletableFuture

class EventOps {
    companion object {
        /**
         * A map to keep track of events that span multiple days. Has to be changed once the events are modified.
         */
        private val multiDayEventMap = mutableMapOf<Event, List<ShownEvent>>()
        private val EventDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val DayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
        private val startMonday: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        private val defaultEventStart: LocalDateTime = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0)
        private val defaultEventEnd: LocalDateTime = defaultEventStart.plusHours(2)

        private val oneDayEvents = listOf(
            Event(
                name = "Google I/O Keynote",
                color = Color(0xFFAFBBF2).value.toString(),
                start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atTime(13, 0, 0).toString(),
                end = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atTime(15, 0, 0).toString(),
                address = Address(),
                description = "Tune in to find out about how we're furthering our mission to organize the world’s information and make it universally accessible and useful.",
            ),
            Event(
                name = "Developer Keynote",
                color = Color(0xFFAFBBF2).value.toString(),
                start = startMonday.plusDays(2).atTime(7, 0, 0).toString(),
                end = startMonday.plusDays(2).atTime(9, 0, 0).toString(),
                address = Address(),
                description = "Learn about the latest updates to our developer products and platforms from Google Developers.",
            )
        )
        private val multiDayEvent = Event(
            name = "What's new in Machine Learning",
            color = Color(0xFFF4BFDB).value.toString(),
            start = startMonday.plusDays(2).atTime(22, 0, 0).toString(),
            end = startMonday.plusDays(3).atTime(4, 0, 0).toString(),
            address = Address(),
            description = "Learn about the latest and greatest in ML from Google. We’ll cover what’s available to developers when it comes to creating, understanding, and deploying models for a variety of different applications.",
        )
        private val multiWeekEvent = Event(
            name = "What's new in Material Design",
            color = Color(0xFF6DD3CE).value.toString(),
            start = startMonday.plusDays(3).atTime(13, 0, 0).toString(),
            end = startMonday.plusWeeks(1).atTime(15, 0, 0).toString(),
            address = Address(),
            description = "Learn about the latest design improvements to help you build personal dynamic experiences with Material Design."
        )

        private val nextWeekEvent = Event(
            name = "What's new in Android",
            color = Color(0xFF1B998B).value.toString(),
            start = startMonday.plusWeeks(1).plusDays(2).atTime(10, 0, 0).toString(),
            end = startMonday.plusWeeks(1).plusDays(2).atTime(12, 0, 0).toString(),
            address = Address(),
            description = "In this Keynote, Chet Haase, Dan Sandler, and Romain Guy discuss the latest Android features and enhancements for developers.",
        )

        private val previousWeekEvent = Event(
            name = "What's new in Android",
            color = Color(0xFF1B998B).value.toString(),
            start = startMonday.minusWeeks(1).plusDays(2).atTime(10, 0, 0).toString(),
            end = startMonday.minusWeeks(1).plusDays(2).atTime(12, 0, 0).toString(),
            address = Address(),
            description = "In this Keynote, Chet Haase, Dan Sandler, and Romain Guy discuss the latest Android features and enhancements for developers.",
        )


        private val eventList = oneDayEvents + multiDayEvent + multiWeekEvent + nextWeekEvent + previousWeekEvent

        fun getOneDayEvents(): List<Event> {
            return oneDayEvents
        }

        fun getMultiDayEvent(): Event {
            return multiDayEvent
        }

        fun getMultiWeekEvent(): Event {
            return multiWeekEvent
        }

        fun getNextWeekEvent(): Event {
            return nextWeekEvent
        }

        fun getPreviousWeekEvent(): Event {
            return previousWeekEvent
        }

        fun getEventList(): List<Event> {
            return eventList
        }

        fun getMultiDayEventMap() = multiDayEventMap

        fun getEventDateFormatter() = EventDateFormatter

        fun getTimeFormatter() = TimeFormatter

        fun getDayFormatter() = DayFormatter

        fun getStartMonday() = startMonday

        fun getDefaultEventStart() = defaultEventStart

        fun getDefaultEventEnd() = defaultEventEnd

        fun clearMultiDayEventMap() {
            multiDayEventMap.clear()
        }

        /**
         * Function to wrap an event that spans multiple days into multiple events of type ShownEvent, one for each day.
         *
         * @param event The event to wrap
         * @return A list of showable events that represent the event that spans multiple days
         *
         */
        private fun wrapEvent(
            event: Event
        ): List<ShownEvent> {
            val start = LocalDateTime.parse(event.start)
            val end = LocalDateTime.parse(event.end)
            val startDay = start.toLocalDate()
            val endDay = end.toLocalDate()

            val eventsToShow = mutableListOf<ShownEvent>()
            val daysToFill = ChronoUnit.DAYS.between(startDay, endDay).toInt() - 1

            val startEvent = ShownEvent(
                name = event.name,
                color = event.color,
                start = event.start,
                startText = event.start,
                end = start.withHour(23).withMinute(59).withSecond(59).toString(),
                endText = event.end,
                description = event.description,
            )

            val endEvent = ShownEvent(
                name = event.name,
                color = event.color,
                start = end.withHour(0).withMinute(0).withSecond(0).toString(),
                startText = event.start,
                end = event.end,
                endText = event.end,
                description = event.description,
            )

            eventsToShow.add(startEvent)
            multiDayEventMap[event] = listOf(startEvent, endEvent)

            if (daysToFill > 0) {
                val middleEvents = (1..daysToFill).map { day ->
                    ShownEvent(
                        name = event.name,
                        color = event.color,
                        start = startDay.plusDays(day.toLong()).atTime(0, 0, 0).toString(),
                        startText = event.start,
                        end = startDay.plusDays(day.toLong()).atTime(23, 59, 59).toString(),
                        endText = event.end,
                        description = event.description,
                    )
                }
                eventsToShow.addAll(middleEvents)
                multiDayEventMap[event] = multiDayEventMap[event]!! + middleEvents
            }

            eventsToShow.add(endEvent)
            return eventsToShow
        }

        /**
         * Function to convert a list of DB events to a list of events that can be shown on the schedule.
         * If an event spans multiple days, it will be split into multiple events of type ShownEvent, one for each day.
         *
         * @param events The list of events to convert
         * @return The list of events that can be shown on the schedule
         */
        fun eventsToWrappedEvents(events: List<Event>) : List<ShownEvent> {
            val eventsToShow = mutableListOf<ShownEvent>()
            events.forEach {
                /*val start = LocalDateTime.parse(it.start)
                val end = LocalDateTime.parse(it.end)*/
                val startDay = LocalDateTime.parse(it.start).toLocalDate()
                val endDay = LocalDateTime.parse(it.end).toLocalDate()

                if (startDay != endDay) {
                    val wrappedEvents = wrapEvent(it)
                    eventsToShow.addAll(wrappedEvents)
                } else {
                    val shownEvent = ShownEvent(
                        name = it.name,
                        color = it.color,
                        start = it.start,
                        startText = it.start,
                        end = it.end,
                        endText = it.end,
                        description = it.description,
                    )
                    eventsToShow.add(shownEvent)
                }
            }
            return eventsToShow
        }

        /**
         * Function to add an event to the database and update multiDayEventMap accordingly.
         * If the event spans multiple days, it will be split into multiple events of type ShownEvent, one for each day.
         *
         * @param event The event to add
         * @param store The database to add the event to
         * @return A completable future that will be completed when the event has been added to the database
         */
        fun addEvent(event: Event, store: CachingStore): CompletableFuture<Schedule> {
            val shownEvents = wrapEvent(event)
            if (shownEvents.size > 1) {
                multiDayEventMap[event] = shownEvents
            }
            return store.addEvent(event, startMonday)
        }

        fun groupEventsToEvents(groupEvents: List<GroupEvent>): List<Event> {
            val events = mutableListOf<Event>()
            groupEvents.forEach {
                val internalEvent = it.event
                val event = Event(
                    name = "${internalEvent.name} (group event)",
                    color = internalEvent.color,
                    start = internalEvent.start,
                    end = internalEvent.end,
                    sport = internalEvent.sport,
                    address = internalEvent.address,
                    description = "Organiser: ${it.organiser}\n" +
                            "Max participants: ${it.maxParticipants}\n" +
                            internalEvent.description,
                )
                events.add(event)
            }
            return events
        }
    }
}