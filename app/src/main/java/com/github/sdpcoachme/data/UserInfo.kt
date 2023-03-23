package com.github.sdpcoachme.data

/**
 * Data class for the client user
 */
data class UserInfo(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val location: String,
    val isCoach: Boolean,
    val sports: List<ListSport>,
    val events: List<Event>,
)
