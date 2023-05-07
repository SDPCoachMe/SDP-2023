package com.github.sdpcoachme.data.schedule

/**
 * Group event data class
 *
 * @property event
 * @property organiser email of organiser
 * @property maxParticipants
 * @property participants emails of participants
 * @constructor Create empty Group event
 */
data class GroupEvent(
    val groupEventId: String,
    val event: Event,
    val organiser: String,
    val maxParticipants: Int,
    val participants: Set<String>,
) {
    // Constructor needed to make the data class serializable
    constructor() : this("", Event(), "", 0, HashSet<String>())
}
