package com.github.sdpcoachme.data.messaging

/**
 * Data class representing a message inside a chat
 */
data class Message(
    val sender: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: String = "",
    val readState: ReadState = ReadState.SENT,
    // map of users who have read the message
    // Storage and runtime wise the most efficient way (as the cloud function will mark
    // the message as received, we only still need to handle the read case)
    val readByUsers: Map<String, Boolean> = mapOf(),
) {
    // Constructor needed to make the data class serializable
    constructor() : this("", "", "", "", ReadState.SENT, mapOf())

    /**
     * Enum class representing the read state of a message
     */
    enum class ReadState {
        SENT,
        RECEIVED,
        READ,
    }
}
