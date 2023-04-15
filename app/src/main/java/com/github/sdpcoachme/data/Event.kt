package com.github.sdpcoachme.data


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

data class ShownEvent(
    val name: String,
    val color: String,
    val start: String,
    val startText: String = start,
    val end: String,
    val endText: String = end,
    val description: String = "",
)
