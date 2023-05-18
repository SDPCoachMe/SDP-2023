package com.github.sdpcoachme.data

class GroupEventSamples {
    companion object {
        val IN_THE_PAST = GroupEvent()

        val FULLY_BOOKED = GroupEvent()

        val AVAILABLE = GroupEvent()

        val ALL = listOf(IN_THE_PAST, FULLY_BOOKED, AVAILABLE)
    }
}