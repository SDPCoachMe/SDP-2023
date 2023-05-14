package com.github.sdpcoachme.data

/**
 * Data class for the client user
 */
data class UserInfo(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val address: Address,
    val coach: Boolean,
    val sports: List<Sports> = emptyList(),
    val chatContacts: List<String> = listOf(),
) {
    // Constructor needed to make the data class serializable
    constructor() : this("", "", "", "", Address(), false, emptyList(), emptyList())
}
