package com.github.sdpcoachme.data

class UserInfoSamples {
    companion object {
        val COACH_1 = UserInfo(
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@email.com",
            location = UserLocationSamples.PARIS,
            phone = "0123456789",
            sports = listOf(Sports.SKI, Sports.SWIMMING),
            coach = true
        )
        val COACH_2 = UserInfo(
            firstName = "Marc",
            lastName = "Delémont",
            email = "marc@email.com",
            location = UserLocationSamples.LAUSANNE,
            phone = "0123456789",
            sports = listOf(Sports.WORKOUT),
            coach = true
        )
        val COACH_3 = UserInfo(
            firstName = "Kate",
            lastName = "Senior",
            email = "katy@email.com",
            location = UserLocationSamples.LONDON,
            phone = "0123456789",
            sports = listOf(Sports.TENNIS, Sports.SWIMMING),
            coach = true
        )
        val COACHES = listOf(COACH_1, COACH_2, COACH_3)

        val NON_COACH_1 = UserInfo(
            firstName = "James",
            lastName = "Dolorian",
            email = "jammy@email.com",
            location = UserLocationSamples.TOKYO,
            phone = "0123456789",
            sports = listOf(Sports.SKI, Sports.SWIMMING),
            coach = false
        )
        val NON_COACH_2 = UserInfo(
            firstName = "Loris",
            lastName = "Gotti",
            email = "lolo@email.com",
            location = UserLocationSamples.SYDNEY,
            phone = "0123456789",
            sports = listOf(Sports.TENNIS),
            coach = false
        )
        val NON_COACHES = listOf(NON_COACH_1, NON_COACH_2)
    }
}