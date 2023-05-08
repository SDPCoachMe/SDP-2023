package com.github.sdpcoachme.data.schedule

/**
 * Event data class
 *
 * @property name
 * @property color
 * @property start
 * @property end
 * @property description
 * @constructor Create empty Event
 */
data class Event(
    val name: String,
    val color: String,
    val start: String,
    val end: String,
    val description: String = "",
) {
    // Constructor needed to make the data class serializable
    constructor() : this("", "", "", "")
}
