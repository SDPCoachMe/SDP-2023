package com.github.sdpcoachme.data.schedule

data class Schedule(
    val events: List<Event> = emptyList(),
) {
    // Constructor needed to make the data class serializable
    constructor() : this(emptyList())
}
