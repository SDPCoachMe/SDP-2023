package com.github.sdpcoachme.data

import com.github.sdpcoachme.data.schedule.Event

/**
 * Group event data class
 *
 * @property event
 * @property organiser email of the organiser
 * @property maxParticipants
 * @property participants email of the participants
 * @property groupEventId
 * @constructor Create empty Group event
 */
data class GroupEvent(
    val event: Event,
    val organiser: String,
    val maxParticipants: Int,
    val participants: List<String> = emptyList(),
    val groupEventId: String = "@@event" + organiser + event.start,
) {
    // Constructor needed to make the data class serializable
    constructor() : this(event = Event(), groupEventId = "", organiser = "", maxParticipants = 0, participants = emptyList())
}
