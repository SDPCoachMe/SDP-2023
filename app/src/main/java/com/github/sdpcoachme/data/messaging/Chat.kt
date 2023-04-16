package com.github.sdpcoachme.data.messaging

/**
 * Data class representing a chat
 */
data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(), // emails of participants
    val messages: List<Message> = emptyList(),
) {
    // Constructor needed to make the data class serializable
    constructor() : this("", emptyList(), emptyList())
}
