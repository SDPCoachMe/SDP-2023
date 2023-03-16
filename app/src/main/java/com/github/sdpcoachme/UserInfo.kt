package com.github.sdpcoachme

/**
 * Data class for the client user
 */
data class UserInfo(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val location: String,
    val sports: List<ListSport>
)