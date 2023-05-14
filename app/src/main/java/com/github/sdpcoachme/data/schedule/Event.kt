package com.github.sdpcoachme.data.schedule

import com.github.sdpcoachme.data.Sports
import com.github.sdpcoachme.data.UserAddress

/**
 * Event data class
 *
 * @property name
 * @property color
 * @property start
 * @property end
 * @property sport
 * @property location
 * @property description
 * @constructor Create empty Event
 */
data class Event(
    val name: String,
    val color: String,
    val start: String,
    val end: String,
    val sport: Sports = Sports.RUNNING, // TODO: Remove this default value during next task
    val location: UserAddress,
    val description: String = "",
) {
    // Constructor needed to make the data class serializable
    constructor() : this("", "", "", "", Sports.RUNNING, UserAddress(),"")
}
