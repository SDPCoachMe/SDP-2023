package com.github.sdpcoachme.schedule

import android.content.Context
import android.content.Intent
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.ShownEvent
import com.github.sdpcoachme.database.Database
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

        fun launchAddEventActivity(context: Context) {
            val intent = Intent(context, CreateEventActivity::class.java)
            context.startActivity(intent)
        }

        fun addEvent(event: Event, database: Database) {
            database.addEvent(event, startMonday)
            val shownEvents = wrapEvent(event)
            if (shownEvents.size > 1) {
                multiDayEventMap[event] = shownEvents
            }
        }
    }
}