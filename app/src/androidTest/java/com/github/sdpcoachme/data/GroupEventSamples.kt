package com.github.sdpcoachme.data

import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACH_1
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACH_2
import com.github.sdpcoachme.data.UserInfoSamples.Companion.COACH_3
import com.github.sdpcoachme.data.UserInfoSamples.Companion.NON_COACH_1
import com.github.sdpcoachme.data.schedule.Event
import com.github.sdpcoachme.data.schedule.EventColors

class GroupEventSamples {
    companion object {

        val IN_THE_PAST = GroupEvent(
            event = Event(
                name = "This event is in the past",
                color = EventColors.ORANGE.color.value.toString(),
                start = "2023-01-07T13:00",
                end = "2023-01-07T16:00",
                sport = Sports.RUNNING,
                address = AddressSamples.LONDON,
                description = "Lorem ipsum dolor sit amet. Ab fugit eveniet ut ipsam tenetur sed " +
                        "iure illum vel nemo maxime. Non ullam harum non obcaecati odio a voluptate " +
                        "facilis ex internos galisum non placeat sunt ad quaerat nobis aut maiores " +
                        "molestiae."
            ),
            organizer = COACH_1.email,
            maxParticipants = 5,
            participants = listOf(COACH_2.email, NON_COACH_1.email)
        )

        val FULLY_BOOKED = GroupEvent(
            event = Event(
                name = "This event is booked",
                color = EventColors.RED.color.value.toString(),
                start = "2023-08-17T11:00",
                end = "2023-08-17T14:00",
                sport = Sports.SKI,
                address = AddressSamples.SYDNEY,
                description = "Lorem ipsum dolor sit amet. Ab fugit eveniet ut ipsam tenetur sed " +
                        "iure illum vel nemo maxime. Non ullam harum non obcaecati odio a voluptate " +
                        "facilis ex internos galisum non placeat sunt ad quaerat nobis aut maiores " +
                        "molestiae."
            ),
            organizer = COACH_1.email,
            maxParticipants = 2,
            //  right now, the organizer is added to the event by the database when the event is created... :(
            participants = listOf(COACH_2.email, NON_COACH_1.email),
            groupEventId = "@@event_booked"
        )

        val AVAILABLE = GroupEvent(
            event = Event(
                name = "This event is available",
                color = EventColors.RED.color.value.toString(),
                start = "2023-08-17T11:00",
                end = "2023-08-17T14:00",
                sport = Sports.SWIMMING,
                address = AddressSamples.SYDNEY,
                description = "Lorem ipsum dolor sit amet. Ab fugit eveniet ut ipsam tenetur sed " +
                        "iure illum vel nemo maxime. Non ullam harum non obcaecati odio a voluptate " +
                        "facilis ex internos galisum non placeat sunt ad quaerat nobis aut maiores " +
                        "molestiae."
            ),
            organizer = COACH_2.email,
            maxParticipants = 7,
            participants = listOf(COACH_1.email, NON_COACH_1.email)
        )

        val AVAILABLE_2 = GroupEvent(
            event = Event(
                name = "Another available event",
                color = EventColors.DARK_GREEN.color.value.toString(),
                start = "2023-08-20T23:00",
                end = "2023-08-21T01:00",
                sport = Sports.WORKOUT,
                address = AddressSamples.NEW_YORK,
                description = "Lorem ipsum dolor sit amet. Ab fugit eveniet ut ipsam tenetur sed " +
                        "iure illum vel nemo maxime. Non ullam harum non obcaecati odio a voluptate " +
                        "facilis ex internos galisum non placeat sunt ad quaerat nobis aut maiores " +
                        "molestiae."
            ),
            organizer = COACH_2.email,
            maxParticipants = 10,
            participants = listOf(COACH_1.email, NON_COACH_1.email, COACH_3.email)
        )

        val ALL = listOf(IN_THE_PAST, FULLY_BOOKED, AVAILABLE, AVAILABLE_2)
    }
}