package com.github.sdpcoachme.data

/**
 * Data class for the client user
 */
data class UserInfo(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val location: UserLocation,
    val coach: Boolean,
    val sports: List<Sports> = emptyList(),
    val events: List<Event> = emptyList()
) {
    // Constructor needed to make the data class serializable
    constructor() : this("", "", "", "", UserLocation(), false, emptyList(), emptyList())
}
