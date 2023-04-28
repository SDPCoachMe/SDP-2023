package com.github.sdpcoachme.schedule

import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.ShownEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

class EventOps {
    companion object {
        /**
         * A map to keep track of events that span multiple days. Has to be changed once the events are modified.
         */
        private val multiDayEventMap = mutableMapOf<Event, List<ShownEvent>>()
        private val EventTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val DayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
        private val startMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        fun getMultiDayEventMap() = multiDayEventMap

        fun getEventTimeFormatter() = EventTimeFormatter

        fun getDayFormatter() = DayFormatter

        fun getStartMonday() = startMonday

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
                val start = LocalDateTime.parse(it.start)
                val end = LocalDateTime.parse(it.end)
                val startDay = start.toLocalDate()
                val endDay = end.toLocalDate()

                if (start.toLocalDate() != end.toLocalDate()) {
                    val wrappedEvents = wrapEvent(startDay, endDay, it, start, end)
                    eventsToShow.addAll(wrappedEvents)
                } else {
                    val shownEvent = ShownEvent(
                        name = it.name,
                        color = it.color,
                        start = it.start,
                        startText = start.toString(),
                        end = it.end,
                        endText = end.toString(),
                        description = it.description,
                    )
                    eventsToShow.add(shownEvent)
                }
            }
            return eventsToShow
        }

        /**
         * Function to wrap an event that spans multiple days into multiple events of type ShownEvent, one for each day.
         *
         * @param startDay The day the event starts on
         * @param endDay The day the event ends on
         * @param event The event to wrap
         * @param start The start time of the event
         * @param end The end time of the event
         * @return A list of showable events that represent the event that spans multiple days
         *
         */
        private fun wrapEvent(
            startDay: LocalDate,
            endDay: LocalDate?,
            event: Event,
            start: LocalDateTime,
            end: LocalDateTime
        ): List<ShownEvent> {
            val eventsToShow = mutableListOf<ShownEvent>()
            val daysToFill = ChronoUnit.DAYS.between(startDay, endDay).toInt() - 1
            val startEvent = ShownEvent(
                name = event.name,
                color = event.color,
                start = start.toString(),
                startText = start.toString(),
                end = start.withHour(23).withMinute(59).withSecond(59).toString(),
                endText = end.toString(),
                description = event.description,
            )
            val endEvent = ShownEvent(
                name = event.name,
                color = event.color,
                start = end.withHour(0).withMinute(0).withSecond(0).toString(),
                startText = start.toString(),
                end = end.toString(),
                endText = end.toString(),
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
                        startText = start.toString(),
                        end = startDay.plusDays(day.toLong()).atTime(23, 59, 59).toString(),
                        endText = end.toString(),
                        description = event.description,
                    )
                }
                eventsToShow.addAll(middleEvents)
                multiDayEventMap[event] = multiDayEventMap[event]!! + middleEvents
            }
            eventsToShow.add(endEvent)

            return eventsToShow
        }
    }
}