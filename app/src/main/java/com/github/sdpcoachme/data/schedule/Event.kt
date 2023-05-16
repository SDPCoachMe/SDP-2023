package com.github.sdpcoachme.data.schedule

import com.github.sdpcoachme.data.Address
import com.github.sdpcoachme.data.Sports

/**
 * Event data class
 *
 * @property name
 * @property color
 * @property start
 * @property end
 * @property sport
 * @property address
 * @property description
 * @constructor Create empty Event
 */
data class Event(
    val name: String,
    val color: String,
    val start: String,
    val end: String,
    val sport: Sports = Sports.RUNNING, // TODO: Remove this default value during next task
    val address: Address,
    val description: String = "",
) {
    // Constructor needed to make the data class serializable
    constructor() : this("", "", "", "", Sports.RUNNING, Address(),"")
}
