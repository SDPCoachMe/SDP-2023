package com.github.sdpcoachme.data.schedule

data class Schedule(
    val events: List<Event> = emptyList(),          // List of private events
    val groupEvents: List<String> = emptyList(),    // List of group event IDs
) {
    // Constructor needed to make the data class serializable
    constructor() : this(emptyList(), emptyList())
}
