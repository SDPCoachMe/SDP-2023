package com.github.sdpcoachme.data.messaging

/**
 * Data class representing a message inside a chat
 */
data class Message(
    val sender: String = "",
    val content: String = "",
    val timestamp: Long = 0,
) {
    // Constructor needed to make the data class serializable
    constructor() : this("", "", 0)
}
