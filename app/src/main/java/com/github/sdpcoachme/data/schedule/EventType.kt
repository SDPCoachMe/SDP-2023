package com.github.sdpcoachme.data.schedule

enum class EventType(val eventTypeName: String) {
    PRIVATE("Private"),
    GROUP("Group");

    companion object {
        infix fun fromString(eventTypeName: String): EventType? =
            values().find { it.eventTypeName == eventTypeName }
    }
}