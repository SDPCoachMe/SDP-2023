package com.github.sdpcoachme.data.schedule

data class ShownEvent(
    val name: String,
    val color: String,
    val start: String,
    val startText: String = start,
    val end: String,
    val endText: String = end,
    val description: String = "",
)
